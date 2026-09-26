package com.example.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NcertBook(
    val id: String = java.util.UUID.randomUUID().toString(),
    @SerialName("class_level")
    val classLevel: Int = 10,
    val subject: String = "",
    @SerialName("book_title")
    val bookTitle: String = "",
    val medium: String = "english", // english, hindi, urdu
    val language: String = "en", // en, hi, ur
    @SerialName("edition_year")
    val editionYear: String = "2026-27",
    @SerialName("source_name")
    val sourceName: String = "ncert", // ncert, epathshala, diksha
    @SerialName("source_url")
    val sourceUrl: String = "",
    @SerialName("thumbnail_url")
    val thumbnailUrl: String = "",
    @SerialName("page_count")
    val pageCount: Int = 0,
    @SerialName("file_size_bytes")
    val fileSizeBytes: Long = 0L,
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("last_verified")
    val lastVerified: String = "",
    @SerialName("created_at")
    val createdAt: String = "",
    @SerialName("updated_at")
    val updatedAt: String = "",

    // Local download state
    val localFilePath: String? = null,
    val isDownloaded: Boolean = false,
    val downloadProgress: Float = 0f
) {
    val displayLabel: String
        get() = "Class $classLevel • $subject • ${medium.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}"

    val editionBadge: String
        get() = when {
            classLevel in 10..11 -> "Valid for 2026-27 • NEP Revised coming 2027-28"
            classLevel in 1..9 || editionYear.contains("2026") -> "Updated for 2026-27"
            else -> "NCERT Prescribed Edition"
        }

    val isNewEdition2026: Boolean
        get() = editionYear.contains("2026") || classLevel in 1..9

    val formattedSize: String
        get() {
            if (fileSizeBytes <= 0) return "Direct Official Download"
            val mb = fileSizeBytes / (1024.0 * 1024.0)
            return String.format(java.util.Locale.US, "%.1f MB", mb)
        }
}
