package com.example.data.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupMetadata(
    val backupFormatVersion: Int,
    val appVersion: String,
    val databaseVersion: Int,
    val createdAt: Long,
    val supabaseUserId: String?,
    val backupId: String,
    val containsDatabase: Boolean,
    val containsMedia: Boolean,
    val source: String = "LOCAL"
)
