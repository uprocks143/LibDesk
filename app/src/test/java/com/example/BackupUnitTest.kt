package com.example

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.backup.GoogleDriveBackupManager
import com.example.worker.BackupWorker
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class BackupUnitTest {

    @Test
    fun testGoogleDriveManagerTokenCleaning() {
        val manager = GoogleDriveBackupManager()
        val rawTokenWithBearer = "Bearer ya29.test_token_value_xyz"
        val cleaned = manager.cleanToken(rawTokenWithBearer)
        assertEquals("ya29.test_token_value_xyz", cleaned)

        val pureToken = "ya29.pure_token"
        assertEquals("ya29.pure_token", manager.cleanToken(pureToken))
    }

    @Test
    fun testBackupWorkerSchedulingDoesNotThrow() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        // Scheduling daily should execute without exception
        BackupWorker.schedule(context, "Daily", "Wi-Fi or cellular")
        BackupWorker.schedule(context, "Never", "Wi-Fi")
        BackupWorker.scheduleFromPreferences(context)
        assertTrue(true)
    }
}
