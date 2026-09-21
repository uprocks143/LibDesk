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
    fun cleanToken(token: String): String {
        var clean = token.trim()
        if (clean.startsWith("Bearer ", ignoreCase = true)) {
            clean = clean.substring(7).trim()
        }
        return clean
    }

    private suspend fun getOrCreateBackupFolder(cleanAuth: String): String? = withContext(Dispatchers.IO) {
        try {
            val searchUrl = "https://www.googleapis.com/drive/v3/files?q=name='LibDesk+Backups'+and+mimeType='application/vnd.google-apps.folder'+and+trashed=false&fields=files(id)"
            val searchRequest = Request.Builder()
                .url(searchUrl)
                .addHeader("Authorization", "Bearer $cleanAuth")
                .get()
                .build()

            val existingFolderId = client.newCall(searchRequest).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: ""
                    val files = JSONObject(body).optJSONArray("files")
                    if (files != null && files.length() > 0) {
                        files.getJSONObject(0).optString("id")
                    } else null
                } else null
            }

            if (!existingFolderId.isNullOrBlank()) return@withContext existingFolderId

            val folderMeta = JSONObject().apply {
                put("name", "LibDesk Backups")
                put("mimeType", "application/vnd.google-apps.folder")
                put("description", "Dedicated storage folder for LibDesk database backups")
            }.toString()

            val createRequest = Request.Builder()
                .url("https://www.googleapis.com/drive/v3/files")
                .addHeader("Authorization", "Bearer $cleanAuth")
                .addHeader("Content-Type", "application/json; charset=UTF-8")
                .post(folderMeta.toRequestBody("application/json; charset=UTF-8".toMediaType()))
                .build()

            client.newCall(createRequest).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: ""
                    JSONObject(body).optString("id", null)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun uploadBackup(
        file: File,
        accessToken: String,
        mimeType: String = "application/octet-stream"
    ): DriveApiResult = withContext(Dispatchers.IO) {
        val cleanAuth = cleanToken(accessToken)
        if (cleanAuth.isBlank()) {
            return@withContext DriveApiResult.Error("No valid Google Drive access token found. Please configure token in Google Account settings.")
        }
        try {
            val folderId = getOrCreateBackupFolder(cleanAuth)
            val metadataJson = JSONObject().apply {
                put("name", file.name)
                put("description", "LibDesk Secure Database Backup")
                if (!folderId.isNullOrBlank()) {
                    put("parents", org.json.JSONArray().apply { put(folderId) })
                }
            }.toString()

            val multipartRelatedMediaType = "multipart/related".toMediaType()
            val requestBody = MultipartBody.Builder()
                .setType(multipartRelatedMediaType)
                .addPart(
                    Headers.headersOf("Content-Type", "application/json; charset=UTF-8"),
                    metadataJson.toRequestBody("application/json; charset=UTF-8".toMediaType())
                )
                .addPart(
                    Headers.headersOf("Content-Type", mimeType),
                    file.asRequestBody(mimeType.toMediaType())
                )
                .build()

            val request = Request.Builder()
                .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
                .addHeader("Authorization", "Bearer $cleanAuth")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(bodyStr)
                    val id = json.optString("id", "")
                    DriveApiResult.Success("Backup uploaded to Google Drive successfully!", id)
                } else {
                    val errorMsg = try {
                        val json = JSONObject(bodyStr)
                        val err = json.optJSONObject("error")
                        err?.optString("message", "") ?: bodyStr
                    } catch (e: Exception) {
                        bodyStr
                    }
                    val detailedMsg = when (response.code) {
                        401 -> "Google Drive authentication failed (HTTP 401: Token expired or invalid). Please update your OAuth token."
                        403 -> "Google Drive permission denied (HTTP 403: Insufficient permissions or quota). Error: $errorMsg"
                        else -> "Google Drive upload failed (HTTP ${response.code}): $errorMsg"
                    }
                    DriveApiResult.Error(detailedMsg)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            DriveApiResult.Error("Network error connecting to Google Drive: ${e.localizedMessage ?: "Check your internet connection"}")
        }
    }

    suspend fun listBackups(accessToken: String): List<DriveFileInfo> = withContext(Dispatchers.IO) {
        val cleanAuth = cleanToken(accessToken)
        if (cleanAuth.isBlank()) return@withContext emptyList()
        try {
            val url = "https://www.googleapis.com/drive/v3/files?q=name+contains+'LibDesk'+and+trashed=false&fields=files(id,name,size,createdTime)&orderBy=createdTime+desc"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $cleanAuth")
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
        val cleanAuth = cleanToken(accessToken)
        if (cleanAuth.isBlank()) return@withContext false
        try {
            val url = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $cleanAuth")
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

    suspend fun testConnection(accessToken: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val cleanAuth = cleanToken(accessToken)
        if (cleanAuth.isBlank()) return@withContext Pair(false, "Token is empty")
        try {
            val url = "https://www.googleapis.com/drive/v3/about?fields=user"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $cleanAuth")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(bodyStr)
                    val userObj = json.optJSONObject("user")
                    val displayName = userObj?.optString("displayName", "Google User") ?: "Google User"
                    val email = userObj?.optString("emailAddress", "") ?: ""
                    val info = if (email.isNotBlank()) "$displayName ($email)" else displayName
                    Pair(true, info)
                } else {
                    val errorMsg = try {
                        val json = JSONObject(bodyStr)
                        json.optJSONObject("error")?.optString("message", "") ?: bodyStr
                    } catch (e: Exception) {
                        bodyStr
                    }
                    Pair(false, "HTTP ${response.code}: $errorMsg")
                }
            }
        } catch (e: Exception) {
            Pair(false, e.localizedMessage ?: "Connection error")
        }
    }
}
