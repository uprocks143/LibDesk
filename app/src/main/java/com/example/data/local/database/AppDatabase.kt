package com.example.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.*
import com.example.data.local.entities.*

@Database(
    entities = [
        LibraryEntity::class,
        UserAccountEntity::class,
        HallEntity::class,
        CabinEntity::class,
        SectionEntity::class,
        ShiftEntity::class,
        MembershipPlanEntity::class,
        SeatEntity::class,
        StudentEntity::class,
        SeatAssignmentEntity::class,
        AttendanceEntity::class,
        PhysicalBookEntity::class,
        BookIssueEntity::class,
        DigitalMaterialEntity::class,
        PaymentEntity::class,
        ExpenseEntity::class,
        FineEntity::class,
        NoticeEntity::class,
        FeedbackComplaintEntity::class,
        AuditLogEntity::class,
        SaaSSubscriptionPlanEntity::class,
        LibrarySubscriptionEntity::class,
        SuperAdminUserEntity::class,
        SubscriptionPlans::class,
        UserSubscription::class
    ],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
    abstract fun userAccountDao(): UserAccountDao
    abstract fun hallDao(): HallDao
    abstract fun cabinDao(): CabinDao
    abstract fun sectionDao(): SectionDao
    abstract fun shiftDao(): ShiftDao
    abstract fun membershipPlanDao(): MembershipPlanDao
    abstract fun seatDao(): SeatDao
    abstract fun studentDao(): StudentDao
    abstract fun seatAssignmentDao(): SeatAssignmentDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun physicalBookDao(): PhysicalBookDao
    abstract fun bookIssueDao(): BookIssueDao
    abstract fun digitalMaterialDao(): DigitalMaterialDao
    abstract fun paymentDao(): PaymentDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun fineDao(): FineDao
    abstract fun noticeDao(): NoticeDao
    abstract fun feedbackComplaintDao(): FeedbackComplaintDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun saasSubscriptionPlanDao(): SaaSSubscriptionPlanDao
    abstract fun librarySubscriptionDao(): LibrarySubscriptionDao
    abstract fun superAdminUserDao(): SuperAdminUserDao
    abstract fun subscriptionPlansDao(): SubscriptionPlansDao
    abstract fun userSubscriptionDao(): UserSubscriptionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "libdesk_v4.db"
                ).fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
