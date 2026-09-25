package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.*
import com.example.data.local.StudyMaterialDao
import com.example.data.model.MaterialSyncStatus
import com.example.data.model.StudyMaterial
import com.example.data.remote.SupabaseStorageDataSource
import com.example.worker.StudyMaterialUploadWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.TimeUnit

class StudyMaterialRepository(
    private val context: Context,
    private val storageDataSource: SupabaseStorageDataSource = SupabaseStorageDataSource(context),
    private val dao: StudyMaterialDao = StudyMaterialDao(context)
) {
    companion object {
        private const val TAG = "StudyMaterialRepo"
        const val MAX_STORAGE_LIMIT_BYTES = 1024L * 1024L * 1024L // 1 GB Free Tier
    }

    val allMaterials: Flow<List<StudyMaterial>> = dao.allMaterials

    fun getMaterialsByOrg(orgId: String): Flow<List<StudyMaterial>> = dao.getByOrg(orgId)

    fun getMaterialsByCategory(orgId: String, category: String): Flow<List<StudyMaterial>> =
        dao.getByCategory(orgId, category)

    /**
     * Storage Usage Meter: returns total bytes used by organization and percentage of 1 GB quota.
     */
    fun getOrgStorageUsage(orgId: String): Flow<Pair<Long, Float>> {
        return dao.getByOrg(orgId).map { materials ->
            val totalBytes = materials.sumOf { it.fileSizeBytes }
            val fraction = (totalBytes.toFloat() / MAX_STORAGE_LIMIT_BYTES).coerceIn(0f, 1f)
            Pair(totalBytes, fraction)
        }
    }

    /**
     * Returns list of materials not accessed/downloaded in past 90 days for auto-suggestion.
     */
    fun getOldInactiveMaterials(orgId: String): Flow<List<StudyMaterial>> {
        val ninetyDaysAgo = System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000)
        return dao.getByOrg(orgId).map { list ->
            list.filter { material ->
                val createdEpoch = try {
                    java.time.Instant.parse(material.createdAt).toEpochMilli()
                } catch (_: Exception) {
                    0L
                }
                createdEpoch in 1..ninetyDaysAgo && material.downloadCount == 0
            }
        }
    }

    /**
     * Refreshes study materials list from Supabase PostgREST for the specified organization.
     */
    suspend fun refreshStudyMaterials(orgId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val result = storageDataSource.fetchStudyMaterials(orgId)
            if (result.isSuccess) {
                val list = result.getOrNull() ?: emptyList()
                dao.insertAll(list)
                Result.success(Unit)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Unknown error fetching materials"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing materials", e)
            Result.failure(e)
        }
    }

    /**
     * Saves locally first with PENDING_UPLOAD status, then triggers immediate upload
     * or enqueues WorkManager with exponential backoff on network failure.
     */
    suspend fun uploadMaterial(
        orgId: String,
        title: String,
        description: String,
        category: String,
        fileUri: Uri,
        fileName: String,
        isFree: Boolean,
        uploadedBy: String,
        fileSizeBytes: Long,
        onProgress: ((Int) -> Unit)? = null
    ): Result<StudyMaterial> = withContext(Dispatchers.IO) {
        val materialId = UUID.randomUUID().toString()
        val initialMaterial = StudyMaterial(
            id = materialId,
            orgId = orgId,
            title = title,
            description = description,
            category = category,
            storagePath = "",
            fileSizeBytes = fileSizeBytes,
            pageCount = 0,
            isFree = isFree,
            uploadedBy = uploadedBy,
            localUri = fileUri.toString(),
            syncStatus = MaterialSyncStatus.PENDING_UPLOAD,
            createdAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", java.util.Locale.US).format(java.util.Date())
        )

        // 1. Save local offline first
        dao.insertOrUpdate(initialMaterial)

        // 2. Attempt immediate upload
        val uploadResult = storageDataSource.uploadStudyMaterial(
            orgId = orgId,
            category = category,
            fileUri = fileUri,
            fileName = fileName,
            onProgress = onProgress
        )

        if (uploadResult.isSuccess) {
            val storagePath = uploadResult.getOrThrow()
            val completedMaterial = initialMaterial.copy(
                storagePath = storagePath,
                syncStatus = MaterialSyncStatus.SYNCED
            )

            // Insert database record
            storageDataSource.insertStudyMaterialRecord(completedMaterial)
            dao.insertOrUpdate(completedMaterial)
            Result.success(completedMaterial)
        } else {
            // Failed immediately (e.g. offline) - schedule WorkManager with exponential backoff
            scheduleUploadWorker(materialId)
            Result.failure(uploadResult.exceptionOrNull() ?: Exception("Upload enqueued for retry."))
        }
    }

    /**
     * Obtains a signed download URL for student or owner viewing (max 1 hour expiry).
     */
    suspend fun getDownloadUrl(storagePath: String): Result<String> {
        return storageDataSource.getSignedDownloadUrl(storagePath, expiresInSeconds = 3600)
    }

    /**
     * Increments download count and logs download record for student.
     */
    suspend fun recordDownload(studentId: String, materialId: String): Result<Unit> = withContext(Dispatchers.IO) {
        storageDataSource.incrementDownloadCount(materialId)
        storageDataSource.logDownload(studentId, materialId, "OWNER_UPLOAD")
    }

    /**
     * Soft-deletes material in database and removes object from Supabase Storage.
     */
    suspend fun deleteMaterial(materialId: String, storagePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        dao.delete(materialId)
        val softDel = storageDataSource.softDeleteStudyMaterial(materialId)
        if (storagePath.isNotBlank()) {
            storageDataSource.deleteStorageObject(storagePath)
        }
        softDel
    }

    private fun scheduleUploadWorker(materialId: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = workDataOf("material_id" to materialId)

        val workRequest = OneTimeWorkRequestBuilder<StudyMaterialUploadWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "upload_material_$materialId",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
}
