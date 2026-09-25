package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.MaterialSyncStatus
import com.example.data.model.StudyMaterial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local Data Access Object for Study Materials.
 * Provides offline caching, pending upload/delete queues, and reactive Flows.
 */
class StudyMaterialDao(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("libdesk_study_materials_cache", Context.MODE_PRIVATE)
    private val _materialsFlow = MutableStateFlow<List<StudyMaterial>>(loadFromDisk())

    val allMaterials: Flow<List<StudyMaterial>> = _materialsFlow.asStateFlow()

    fun getByOrg(orgId: String): Flow<List<StudyMaterial>> {
        return _materialsFlow.map { list ->
            list.filter { it.orgId == orgId && it.deletedAt == null && it.syncStatus != MaterialSyncStatus.PENDING_DELETE }
        }
    }

    fun getByCategory(orgId: String, category: String): Flow<List<StudyMaterial>> {
        return _materialsFlow.map { list ->
            list.filter {
                it.orgId == orgId &&
                it.deletedAt == null &&
                it.syncStatus != MaterialSyncStatus.PENDING_DELETE &&
                (category.equals("all", ignoreCase = true) || it.category.equals(category, ignoreCase = true))
            }
        }
    }

    suspend fun getPendingUploads(): List<StudyMaterial> = withContext(Dispatchers.IO) {
        _materialsFlow.value.filter { it.syncStatus == MaterialSyncStatus.PENDING_UPLOAD }
    }

    suspend fun insertOrUpdate(material: StudyMaterial) = withContext(Dispatchers.IO) {
        val current = _materialsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == material.id }
        if (index != -1) {
            current[index] = material
        } else {
            current.add(0, material)
        }
        _materialsFlow.value = current
        saveToDisk(current)
    }

    suspend fun insertAll(materials: List<StudyMaterial>) = withContext(Dispatchers.IO) {
        val currentMap = _materialsFlow.value.associateBy { it.id }.toMutableMap()
        for (m in materials) {
            val existing = currentMap[m.id]
            if (existing == null || existing.syncStatus == MaterialSyncStatus.SYNCED) {
                currentMap[m.id] = m
            }
        }
        val updated = currentMap.values.sortedByDescending { it.createdAt }
        _materialsFlow.value = updated
        saveToDisk(updated)
    }

    suspend fun updateSyncStatus(materialId: String, status: MaterialSyncStatus, storagePath: String? = null) = withContext(Dispatchers.IO) {
        val current = _materialsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == materialId }
        if (index != -1) {
            val item = current[index]
            current[index] = item.copy(
                syncStatus = status,
                storagePath = storagePath ?: item.storagePath
            )
            _materialsFlow.value = current
            saveToDisk(current)
        }
    }

    suspend fun delete(materialId: String) = withContext(Dispatchers.IO) {
        val current = _materialsFlow.value.filterNot { it.id == materialId }
        _materialsFlow.value = current
        saveToDisk(current)
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        _materialsFlow.value = emptyList()
        prefs.edit().clear().apply()
    }

    private fun loadFromDisk(): List<StudyMaterial> {
        val raw = prefs.getString("cached_materials", null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<StudyMaterial>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    StudyMaterial(
                        id = obj.optString("id"),
                        orgId = obj.optString("orgId"),
                        title = obj.optString("title"),
                        description = obj.optString("description"),
                        category = obj.optString("category"),
                        storagePath = obj.optString("storagePath"),
                        fileSizeBytes = obj.optLong("fileSizeBytes"),
                        pageCount = obj.optInt("pageCount"),
                        isFree = obj.optBoolean("isFree", true),
                        uploadedBy = obj.optString("uploadedBy"),
                        downloadCount = obj.optInt("downloadCount"),
                        createdAt = obj.optString("createdAt"),
                        updatedAt = obj.optString("updatedAt"),
                        deletedAt = obj.optString("deletedAt").takeIf { it.isNotBlank() },
                        localUri = obj.optString("localUri").takeIf { it.isNotBlank() },
                        syncStatus = try { MaterialSyncStatus.valueOf(obj.optString("syncStatus", "SYNCED")) } catch (_: Exception) { MaterialSyncStatus.SYNCED }
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveToDisk(list: List<StudyMaterial>) {
        try {
            val array = JSONArray()
            for (m in list) {
                val obj = JSONObject().apply {
                    put("id", m.id)
                    put("orgId", m.orgId)
                    put("title", m.title)
                    put("description", m.description)
                    put("category", m.category)
                    put("storagePath", m.storagePath)
                    put("fileSizeBytes", m.fileSizeBytes)
                    put("pageCount", m.pageCount)
                    put("isFree", m.isFree)
                    put("uploadedBy", m.uploadedBy)
                    put("downloadCount", m.downloadCount)
                    put("createdAt", m.createdAt)
                    put("updatedAt", m.updatedAt)
                    put("deletedAt", m.deletedAt ?: "")
                    put("localUri", m.localUri ?: "")
                    put("syncStatus", m.syncStatus.name)
                }
                array.put(obj)
            }
            prefs.edit().putString("cached_materials", array.toString()).apply()
        } catch (_: Exception) {}
    }
}
