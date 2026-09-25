package com.example.data.remote

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.model.StudyMaterial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

class SupabaseStorageDataSource(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val TAG = "SupabaseStorageDS"
        const val BUCKET_STUDY_MATERIALS = "study-materials"
        const val MAX_FILE_SIZE_BYTES = 20 * 1024 * 1024L // 20 MB Limit
        const val WARNING_FILE_SIZE_BYTES = 10 * 1024 * 1024L // 10 MB Warning
        const val FREE_STORAGE_QUOTA_BYTES = 1024 * 1024 * 1024L // 1 GB Total
    }

    private fun getBaseUrl(): String = SupabaseClient.getEffectiveUrl()
    private fun getApiKey(): String = SupabaseClient.getEffectiveApiKey()
    private fun getAuthToken(): String? = SupabaseClient.currentAuthToken

    /**
     * Uploads study material PDF into the org-isolated folder path:
     * study-materials/{orgId}/{category}/{uuid}.pdf
     * Rejects files > 20 MB client-side before any network request.
     */
    suspend fun uploadStudyMaterial(
        orgId: String,
        category: String,
        fileUri: Uri,
        fileName: String,
        onProgress: ((Int) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (orgId.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Organization ID cannot be empty."))
            }

            // Inspect file size before uploading
            val contentResolver = context.contentResolver
            val stream = contentResolver.openInputStream(fileUri)
                ?: return@withContext Result.failure(IllegalStateException("Cannot open selected file stream."))

            val bytes = stream.use { it.readBytes() }
            if (bytes.size > MAX_FILE_SIZE_BYTES) {
                return@withContext Result.failure(
                    IllegalArgumentException("File size exceeds 20 MB limit (${bytes.size / (1024 * 1024)} MB). Please compress or reduce file size.")
                )
            }

            val sanitizedCategory = category.trim().lowercase().replace(" ", "-")
            val objectUuid = UUID.randomUUID().toString()
            val storagePath = "$orgId/$sanitizedCategory/$objectUuid.pdf"

            val url = "${getBaseUrl()}/storage/v1/object/$BUCKET_STUDY_MATERIALS/$storagePath"
            val token = getAuthToken() ?: getApiKey()

            val requestBody = bytes.toRequestBody("application/pdf".toMediaType())

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", getApiKey())
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/pdf")
                .addHeader("x-upsert", "true")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    onProgress?.invoke(100)
                    Log.d(TAG, "Uploaded study material successfully to $storagePath")
                    Result.success(storagePath)
                } else {
                    val errorBody = response.body?.string() ?: ""
                    Log.e(TAG, "Storage upload failed: HTTP ${response.code} - $errorBody")
                    Result.failure(Exception("Upload failed (HTTP ${response.code}): $errorBody"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading study material", e)
            Result.failure(e)
        }
    }

    /**
     * Generates a signed download URL valid for 1 hour (3600 seconds) max.
     */
    suspend fun getSignedDownloadUrl(
        storagePath: String,
        expiresInSeconds: Int = 3600
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = "${getBaseUrl()}/storage/v1/object/sign/$BUCKET_STUDY_MATERIALS/$storagePath"
            val token = getAuthToken() ?: getApiKey()
            val payload = JSONObject().apply {
                put("expiresIn", expiresInSeconds.coerceAtMost(3600))
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", getApiKey())
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(body)
                    val signedPath = json.optString("signedURL")
                    val fullSignedUrl = if (signedPath.startsWith("http")) signedPath else "${getBaseUrl()}$signedPath"
                    Result.success(fullSignedUrl)
                } else {
                    Result.failure(Exception("Failed to generate signed download URL: HTTP ${response.code} $body"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating signed URL", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes a study material file from Supabase Storage.
     */
    suspend fun deleteStorageObject(storagePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = "${getBaseUrl()}/storage/v1/object/$BUCKET_STUDY_MATERIALS/$storagePath"
            val token = getAuthToken() ?: getApiKey()

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", getApiKey())
                .addHeader("Authorization", "Bearer $token")
                .delete()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 404) {
                    Result.success(Unit)
                } else {
                    val body = response.body?.string() ?: ""
                    Result.failure(Exception("Failed to delete storage file: HTTP ${response.code} $body"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting storage object", e)
            Result.failure(e)
        }
    }

    /**
     * Increments material download count via PostgreSQL RPC.
     */
    suspend fun incrementDownloadCount(materialId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = "${getBaseUrl()}/rest/v1/rpc/increment_download_count"
            val token = getAuthToken() ?: getApiKey()
            val payload = JSONObject().apply {
                put("p_material_id", materialId)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", getApiKey())
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("RPC failed HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Logs download activity to download_logs table for audit trail.
     */
    suspend fun logDownload(
        studentId: String,
        materialId: String,
        source: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = "${getBaseUrl()}/rest/v1/download_logs"
            val token = getAuthToken() ?: getApiKey()
            val payload = JSONObject().apply {
                put("student_id", studentId)
                put("material_id", materialId)
                put("source", source)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", getApiKey())
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Log download failed HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches study materials list from the database table for an organization.
     */
    suspend fun fetchStudyMaterials(orgId: String): Result<List<StudyMaterial>> = withContext(Dispatchers.IO) {
        try {
            val url = "${getBaseUrl()}/rest/v1/study_materials?org_id=eq.$orgId&deleted_at=is.null&order=created_at.desc"
            val token = getAuthToken() ?: getApiKey()

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", getApiKey())
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val list = mutableListOf<StudyMaterial>()
                    val array = JSONArray(body)
                    for (i in 0 until array.length()) {
                        val item = array.getJSONObject(i)
                        list.add(
                            StudyMaterial(
                                id = item.optString("id"),
                                orgId = item.optString("org_id"),
                                title = item.optString("title"),
                                description = item.optString("description"),
                                category = item.optString("category"),
                                storagePath = item.optString("storage_path"),
                                fileSizeBytes = item.optLong("file_size_bytes"),
                                pageCount = item.optInt("page_count"),
                                isFree = item.optBoolean("is_free", true),
                                uploadedBy = item.optString("uploaded_by"),
                                downloadCount = item.optInt("download_count"),
                                createdAt = item.optString("created_at"),
                                updatedAt = item.optString("updated_at")
                            )
                        )
                    }
                    Result.success(list)
                } else {
                    Result.failure(Exception("Failed to fetch study materials HTTP ${response.code} $body"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching study materials", e)
            Result.failure(e)
        }
    }

    /**
     * Inserts study material metadata record in the database.
     */
    suspend fun insertStudyMaterialRecord(material: StudyMaterial): Result<StudyMaterial> = withContext(Dispatchers.IO) {
        try {
            val url = "${getBaseUrl()}/rest/v1/study_materials"
            val token = getAuthToken() ?: getApiKey()
            val payload = JSONObject().apply {
                put("id", material.id)
                put("org_id", material.orgId)
                put("title", material.title)
                put("description", material.description)
                put("category", material.category)
                put("storage_path", material.storagePath)
                put("file_size_bytes", material.fileSizeBytes)
                put("page_count", material.pageCount)
                put("is_free", material.isFree)
                put("uploaded_by", material.uploadedBy)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", getApiKey())
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(material)
                } else {
                    val body = response.body?.string() ?: ""
                    Result.failure(Exception("Failed to insert material record HTTP ${response.code} $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Soft deletes study material by setting deleted_at timestamp.
     */
    suspend fun softDeleteStudyMaterial(materialId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = "${getBaseUrl()}/rest/v1/study_materials?id=eq.$materialId"
            val token = getAuthToken() ?: getApiKey()
            val payload = JSONObject().apply {
                put("deleted_at", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", java.util.Locale.US).format(java.util.Date()))
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", getApiKey())
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .patch(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Soft delete failed HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
