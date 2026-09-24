package com.example.data.remote

import android.util.Log
import com.example.data.local.entities.StudentEntity
import com.example.data.repository.LibDeskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SupabaseSyncManager(
    private val repository: LibDeskRepository? = null
) {
    private val TAG = "SupabaseSyncManager"

    suspend fun syncLocalToSupabase(libraryId: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        // Direct cloud architecture: data is already saved to Supabase on creation/update
        Pair(true, "Cloud state synchronized")
    }

    suspend fun pullSupabaseToLocal(libraryId: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (repository != null) {
            repository.pullFromCloud(libraryId)
        } else {
            Pair(true, "Cloud pull skipped: repository is null")
        }
    }

    suspend fun pushStudent(student: StudentEntity) = withContext(Dispatchers.IO) {
        repository?.saveStudent(student)
    }
}
