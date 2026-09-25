package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.local.StudyMaterialDao
import com.example.data.model.MaterialSyncStatus
import com.example.data.model.StudyMaterial
import com.example.data.remote.SupabaseStorageDataSource
import com.example.data.repository.StudyMaterialRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class StudyMaterialRepositoryTest {

    private lateinit var context: Context
    private lateinit var dao: StudyMaterialDao
    private lateinit var repository: StudyMaterialRepository

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        dao = StudyMaterialDao(context)
        dao.clearCache()
        repository = StudyMaterialRepository(context, dao = dao)
    }

    @Test
    fun testOfflineCacheInsertionAndRetrieval() = runBlocking {
        val testMaterial = StudyMaterial(
            id = "test-mat-1",
            orgId = "org-123",
            title = "Polity Handwritten Notes",
            category = "notes",
            fileSizeBytes = 5 * 1024 * 1024L, // 5 MB
            syncStatus = MaterialSyncStatus.SYNCED
        )

        dao.insertOrUpdate(testMaterial)

        val materials = dao.getByOrg("org-123").first()
        assertEquals(1, materials.size)
        assertEquals("Polity Handwritten Notes", materials[0].title)
        assertEquals("5.0 MB", materials[0].formattedSize)
    }

    @Test
    fun testStorageQuotaCalculation() = runBlocking {
        val mat1 = StudyMaterial(
            id = "m1",
            orgId = "org-quota",
            title = "Test 1",
            category = "notes",
            fileSizeBytes = 200 * 1024 * 1024L // 200 MB
        )
        val mat2 = StudyMaterial(
            id = "m2",
            orgId = "org-quota",
            title = "Test 2",
            category = "mock-tests",
            fileSizeBytes = 620 * 1024 * 1024L // 620 MB
        )

        dao.insertAll(listOf(mat1, mat2))

        val usage = repository.getOrgStorageUsage("org-quota").first()
        val totalBytes = usage.first
        val fraction = usage.second

        assertEquals(820 * 1024 * 1024L, totalBytes)
        assertTrue("Storage fraction should be >= 80%", fraction >= 0.80f)
    }

    @Test
    fun testFileLimitConstants() {
        assertEquals(20 * 1024 * 1024L, SupabaseStorageDataSource.MAX_FILE_SIZE_BYTES)
        assertEquals(10 * 1024 * 1024L, SupabaseStorageDataSource.WARNING_FILE_SIZE_BYTES)
        assertEquals(1024L * 1024L * 1024L, StudyMaterialRepository.MAX_STORAGE_LIMIT_BYTES)
    }
}
