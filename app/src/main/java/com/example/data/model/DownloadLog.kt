package com.example.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DownloadLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    @SerialName("student_id")
    val studentId: String = "",
    @SerialName("material_id")
    val materialId: String = "",
    val source: String = "OWNER_UPLOAD", // OWNER_UPLOAD or NCERT_OFFICIAL
    @SerialName("created_at")
    val createdAt: String = ""
)
