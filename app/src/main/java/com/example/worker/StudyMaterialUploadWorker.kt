package com.example.worker

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.StudyMaterialDao
import com.example.data.model.MaterialSyncStatus
import com.example.data.remote.SupabaseStorageDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StudyMaterialUploadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "StudyMaterialUploadWkr"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val materialId = inputData.getString("material_id")
        val dao = StudyMaterialDao(applicationContext)
        val storageDs = SupabaseStorageDataSource(applicationContext)

        val pendingList = if (!materialId.isNullOrBlank()) {
            dao.getPendingUploads().filter { it.id == materialId }
        } else {
            dao.getPendingUploads()
        }

        if (pendingList.isEmpty()) {
            Log.d(TAG, "No pending uploads found.")
            return@withContext Result.success()
        }

        var anyFailed = false

        for (material in pendingList) {
            val uriStr = material.localUri
            if (uriStr.isNullOrBlank()) {
                dao.updateSyncStatus(material.id, MaterialSyncStatus.FAILED)
                continue
            }

            val fileUri = Uri.parse(uriStr)
            val uploadResult = storageDs.uploadStudyMaterial(
                orgId = material.orgId,
                category = material.category,
                fileUri = fileUri,
                fileName = "${material.title}.pdf"
            )

            if (uploadResult.isSuccess) {
                val storagePath = uploadResult.getOrThrow()
                val updated = material.copy(
                    storagePath = storagePath,
                    syncStatus = MaterialSyncStatus.SYNCED
                )
                storageDs.insertStudyMaterialRecord(updated)
                dao.insertOrUpdate(updated)
                Log.d(TAG, "Successfully synced pending upload for ${material.title}")
            } else {
                Log.w(TAG, "Failed to upload ${material.title}, will retry with backoff")
                anyFailed = true
            }
        }

        if (anyFailed) {
            Result.retry()
        } else {
            Result.success()
        }
    }
}
