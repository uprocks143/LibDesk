package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver for scheduled student notifications (membership expiry & shift start).
 */
class StudentReminderAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_STUDENT_SHIFT_REMINDER = "com.example.ACTION_STUDENT_SHIFT_REMINDER"
        const val ACTION_STUDENT_EXPIRY_CHECK = "com.example.ACTION_STUDENT_EXPIRY_CHECK"
        const val EXTRA_STUDENT_ID = "extra_student_id"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val studentId = intent.getStringExtra(EXTRA_STUDENT_ID)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)

                when (action) {
                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_MY_PACKAGE_REPLACED,
                    "android.intent.action.QUICKBOOT_POWERON" -> {
                        // Re-schedule alarms after device reboot
                        val students = db.studentDao().getAllStudentsDirect()
                        students.take(5).forEach { st ->
                            val shift = db.shiftDao().getShiftsDirect(st.libraryId)
                                .find { it.name.equals(st.shiftName, ignoreCase = true) }
                            StudentNotificationHelper.scheduleSeatShiftReminderAlarm(context, st, shift)
                        }
                    }

                    ACTION_STUDENT_SHIFT_REMINDER -> {
                        if (StudentNotificationHelper.isSeatReminderEnabled(context)) {
                            val student = if (!studentId.isNullOrBlank()) {
                                db.studentDao().findStudentById(studentId)
                            } else {
                                db.studentDao().getAllStudentsDirect().firstOrNull()
                            }

                            if (student != null) {
                                val library = db.libraryDao().getLibraryByIdDirect(student.libraryId)
                                val libraryName = library?.name ?: "Your Library"
                                val shift = db.shiftDao().getShiftsDirect(student.libraryId)
                                    .find { it.name.equals(student.shiftName, ignoreCase = true) }

                                StudentNotificationHelper.sendSeatReservationStartReminder(
                                    context = context,
                                    student = student,
                                    shift = shift,
                                    libraryName = libraryName,
                                    force = false
                                )

                                // Reschedule for next day
                                StudentNotificationHelper.scheduleSeatShiftReminderAlarm(context, student, shift)
                            }
                        }
                    }

                    ACTION_STUDENT_EXPIRY_CHECK -> {
                        if (StudentNotificationHelper.isExpiryReminderEnabled(context)) {
                            val student = if (!studentId.isNullOrBlank()) {
                                db.studentDao().findStudentById(studentId)
                            } else null

                            if (student != null) {
                                val library = db.libraryDao().getLibraryByIdDirect(student.libraryId)
                                StudentNotificationHelper.sendMembershipExpiryReminder(
                                    context = context,
                                    student = student,
                                    libraryName = library?.name ?: "Your Library",
                                    force = false
                                )
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            } finally {
                pendingResult.finish()
            }
        }
    }
}
