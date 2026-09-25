package com.example.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class MaterialSyncStatus {
    @SerialName("SYNCED")
    SYNCED,
    @SerialName("PENDING_UPLOAD")
    PENDING_UPLOAD,
    @SerialName("PENDING_DELETE")
    PENDING_DELETE,
    @SerialName("FAILED")
    FAILED
}

@Serializable
data class StudyMaterial(
    val id: String = java.util.UUID.randomUUID().toString(),
    @SerialName("org_id")
    val orgId: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "notes", // ncert, mock-tests, notes, competitive, other
    @SerialName("storage_path")
    val storagePath: String = "",
    @SerialName("file_size_bytes")
    val fileSizeBytes: Long = 0L,
    @SerialName("page_count")
    val pageCount: Int = 0,
    @SerialName("is_free")
    val isFree: Boolean = true,
    @SerialName("shift_access")
    val shiftAccess: List<String>? = null,
    @SerialName("uploaded_by")
    val uploadedBy: String = "",
    @SerialName("download_count")
    val downloadCount: Int = 0,
    @SerialName("created_at")
    val createdAt: String = "",
    @SerialName("updated_at")
    val updatedAt: String = "",
    @SerialName("deleted_at")
    val deletedAt: String? = null,

    // Local-only state for offline handling & WorkManager sync
    val localUri: String? = null,
    val syncStatus: MaterialSyncStatus = MaterialSyncStatus.SYNCED
) {
    val formattedSize: String
        get() {
            if (fileSizeBytes <= 0) return "0 KB"
            val kb = fileSizeBytes / 1024.0
            val mb = kb / 1024.0
            return if (mb >= 1.0) {
                String.format(java.util.Locale.US, "%.1f MB", mb)
            } else {
                String.format(java.util.Locale.US, "%.0f KB", kb)
            }
        }

    val categoryDisplayName: String
        get() = when (category.lowercase()) {
            "ncert" -> "NCERT Reference"
            "mock-tests" -> "Mock Tests & Papers"
            "notes" -> "Faculty / Study Notes"
            "competitive" -> "Competitive Exams (UPSC/SSC/NEET/JEE)"
            else -> "Other Resources"
        }
}
