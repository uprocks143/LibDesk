package com.example.data.backup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

data class DriveFileInfo(
    val id: String,
    val name: String,
    val sizeBytes: Long,
    val createdTime: String
)

sealed class DriveApiResult {
    data class Success(val message: String, val fileId: String? = null) : DriveApiResult()
    data class Error(val message: String) : DriveApiResult()
}

class GoogleDriveBackupManager(
    private val client: OkHttpClient = OkHttpClient()
) {
    suspend fun uploadBackup(
        file: File,
        accessToken: String,
        mimeType: String = "application/octet-stream"
    ): DriveApiResult = withContext(Dispatchers.IO) {
        try {
            val metadata = """
                {
                  "name": "${file.name}",
                  "description": "LibDesk Secure Database Backup"
                }
            """.trimIndent()

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "metadata", 
                    null,
                    metadata.toRequestBody("application/json".toMediaType())
                )
                .addFormDataPart(
                    "file",
                    file.name,
                    file.asRequestBody(mimeType.toMediaType())
                )
                .build()

            val request = Request.Builder()
                .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
                .addHeader("Authorization", "Bearer $accessToken")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    val id = json.optString("id", "")
                    DriveApiResult.Success("Backup uploaded to Google Drive successfully!", id)
                } else {
                    DriveApiResult.Error("Google Drive upload failed: HTTP ${response.code} - ${response.message}")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            DriveApiResult.Error("Network error: ${e.localizedMessage ?: "Could not connect to Google Drive"}")
        }
    }

    suspend fun listBackups(accessToken: String): List<DriveFileInfo> = withContext(Dispatchers.IO) {
        try {
            val url = "https://www.googleapis.com/drive/v3/files?q=name+contains+'LibDesk_Backup'+and+trashed=false&fields=files(id,name,size,createdTime)&orderBy=createdTime+desc"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val json = JSONObject(bodyStr)
                    val filesArray = json.optJSONArray("files") ?: return@withContext emptyList()
                    val result = mutableListOf<DriveFileInfo>()
                    for (i in 0 until filesArray.length()) {
                        val item = filesArray.getJSONObject(i)
                        result.add(
                            DriveFileInfo(
                                id = item.optString("id"),
                                name = item.optString("name"),
                                sizeBytes = item.optLong("size", 0L),
                                createdTime = item.optString("createdTime")
                            )
                        )
                    }
                    result
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun downloadBackup(
        fileId: String,
        destinationFile: File,
        accessToken: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful && response.body != null) {
                    response.body!!.byteStream().use { input ->
                        FileOutputStream(destinationFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    true
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun testConnection(accessToken: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "https://www.googleapis.com/drive/v3/about?fields=user"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }
}
