package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {
    @Query("SELECT * FROM libraries WHERE id = :id LIMIT 1")
    fun getLibraryById(id: String): Flow<LibraryEntity?>

    @Query("SELECT * FROM libraries WHERE id = :id LIMIT 1")
    suspend fun getLibraryByIdDirect(id: String): LibraryEntity?

    @Query("SELECT * FROM libraries WHERE code = :code LIMIT 1")
    suspend fun getLibraryByCode(code: String): LibraryEntity?

    @Query("SELECT * FROM libraries ORDER BY createdAt DESC")
    fun getAllLibraries(): Flow<List<LibraryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLibrary(library: LibraryEntity)

    @Update
    suspend fun updateLibrary(library: LibraryEntity)

    @Delete
    suspend fun deleteLibrary(library: LibraryEntity)
}

@Dao
interface UserAccountDao {
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserAccountEntity?

    @Query("SELECT * FROM users WHERE email = :identifier OR phone = :identifier LIMIT 1")
    suspend fun getUserByIdentifier(identifier: String): UserAccountEntity?

    @Query("SELECT * FROM users WHERE (email = :identifier OR phone = :identifier) AND role = :role LIMIT 1")
    suspend fun getUserByIdentifierAndRole(identifier: String, role: String): UserAccountEntity?

    @Query("SELECT * FROM users WHERE role = :role")
    fun getUsersByRole(role: String): Flow<List<UserAccountEntity>>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun getUserById(id: String): Flow<UserAccountEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserAccountEntity)

    @Update
    suspend fun updateUser(user: UserAccountEntity)
}

@Dao
interface HallDao {
    @Query("SELECT * FROM halls WHERE libraryId = :libraryId ORDER BY name ASC")
    fun getHallsByLibrary(libraryId: String): Flow<List<HallEntity>>

    @Query("SELECT * FROM halls WHERE id = :hallId LIMIT 1")
    suspend fun getHallByIdDirect(hallId: String): HallEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHall(hall: HallEntity)

    @Update
    suspend fun updateHall(hall: HallEntity)

    @Delete
    suspend fun deleteHall(hall: HallEntity)
}

@Dao
interface CabinDao {
    @Query("SELECT * FROM cabins WHERE libraryId = :libraryId ORDER BY cabinNumber ASC")
    fun getCabinsByLibrary(libraryId: String): Flow<List<CabinEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCabin(cabin: CabinEntity)

    @Update
    suspend fun updateCabin(cabin: CabinEntity)

    @Delete
    suspend fun deleteCabin(cabin: CabinEntity)
}

@Dao
interface SectionDao {
    @Query("SELECT * FROM sections WHERE libraryId = :libraryId ORDER BY name ASC")
    fun getSectionsByLibrary(libraryId: String): Flow<List<SectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSection(section: SectionEntity)

    @Update
    suspend fun updateSection(section: SectionEntity)

    @Delete
    suspend fun deleteSection(section: SectionEntity)
}

@Dao
interface ShiftDao {
    @Query("SELECT * FROM shifts WHERE libraryId = :libraryId ORDER BY name ASC")
    fun getShiftsByLibrary(libraryId: String): Flow<List<ShiftEntity>>

    @Query("SELECT * FROM shifts WHERE id = :shiftId LIMIT 1")
    suspend fun getShiftByIdDirect(shiftId: String): ShiftEntity?

    @Query("SELECT * FROM shifts WHERE libraryId = :libraryId ORDER BY name ASC")
    suspend fun getShiftsDirect(libraryId: String): List<ShiftEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: ShiftEntity)

    @Update
    suspend fun updateShift(shift: ShiftEntity)

    @Delete
    suspend fun deleteShift(shift: ShiftEntity)
}

@Dao
interface MembershipPlanDao {
    @Query("SELECT * FROM membership_plans WHERE libraryId = :libraryId ORDER BY durationMonths ASC")
    fun getPlansByLibrary(libraryId: String): Flow<List<MembershipPlanEntity>>

    @Query("SELECT * FROM membership_plans WHERE id = :planId LIMIT 1")
    suspend fun getPlanByIdDirect(planId: String): MembershipPlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: MembershipPlanEntity)

    @Update
    suspend fun updatePlan(plan: MembershipPlanEntity)

    @Delete
    suspend fun deletePlan(plan: MembershipPlanEntity)
}

@Dao
interface SeatDao {
    @Query("SELECT * FROM seats WHERE libraryId = :libraryId ORDER BY seatNumber ASC")
    fun getSeatsByLibrary(libraryId: String): Flow<List<SeatEntity>>

    @Query("SELECT * FROM seats WHERE id = :seatId LIMIT 1")
    fun getSeatById(seatId: String): Flow<SeatEntity?>

    @Query("SELECT * FROM seats WHERE id = :seatId LIMIT 1")
    suspend fun getSeatByIdDirect(seatId: String): SeatEntity?

    @Query("SELECT * FROM seats WHERE libraryId = :libraryId AND assignedStudentId = :studentId LIMIT 1")
    fun getSeatByStudentId(libraryId: String, studentId: String): Flow<SeatEntity?>

    @Query("SELECT * FROM seats WHERE libraryId = :libraryId AND floor = :floor ORDER BY seatNumber ASC")
    fun getSeatsByFloor(libraryId: String, floor: String): Flow<List<SeatEntity>>

    @Query("SELECT * FROM seats WHERE libraryId = :libraryId AND (seatNumber = :seatNum OR id = :seatNum) LIMIT 1")
    suspend fun findSeatByNumberOrId(libraryId: String, seatNum: String): SeatEntity?

    @Query("UPDATE seats SET status = :status WHERE id = :seatId")
    suspend fun updateSeatStatus(seatId: String, status: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeat(seat: SeatEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeats(seats: List<SeatEntity>)

    @Update
    suspend fun updateSeat(seat: SeatEntity)

    @Delete
    suspend fun deleteSeat(seat: SeatEntity)

    @Query("DELETE FROM seats WHERE libraryId = :libraryId")
    suspend fun deleteAllSeatsInLibrary(libraryId: String)
}

@Dao
interface StudentDao {
    @Query("SELECT * FROM students WHERE libraryId = :libraryId ORDER BY fullName ASC")
    fun getStudentsByLibrary(libraryId: String): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE id = :studentId LIMIT 1")
    fun getStudentById(studentId: String): Flow<StudentEntity?>

    @Query("SELECT * FROM students WHERE id = :studentId LIMIT 1")
    suspend fun findStudentById(studentId: String): StudentEntity?

    @Query("SELECT * FROM students WHERE mobile = :identifier OR email = :identifier OR studentCode = :identifier OR id = :identifier LIMIT 1")
    suspend fun findStudentByGlobalIdentifier(identifier: String): StudentEntity?

    @Query("SELECT * FROM students WHERE libraryId = :libraryId AND (mobile = :mobileOrCode OR studentCode = :mobileOrCode OR rfidQrCode = :mobileOrCode OR id = :mobileOrCode OR seatNumber = :mobileOrCode) LIMIT 1")
    suspend fun findStudentByMobileOrCode(libraryId: String, mobileOrCode: String): StudentEntity?

    @Query("SELECT * FROM students WHERE libraryId = :libraryId AND (fullName LIKE '%' || :query || '%' OR studentCode LIKE '%' || :query || '%' OR mobile LIKE '%' || :query || '%' OR seatNumber LIKE '%' || :query || '%') ORDER BY fullName ASC")
    fun searchStudents(libraryId: String, query: String): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE libraryId = :libraryId AND dueAmount > 0 ORDER BY dueAmount DESC")
    fun getStudentsWithPendingDues(libraryId: String): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students")
    suspend fun getAllStudentsDirect(): List<StudentEntity>

    @Query("SELECT * FROM students WHERE libraryId = :libraryId")
    suspend fun getStudentsListByLibrary(libraryId: String): List<StudentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity)

    @Update
    suspend fun updateStudent(student: StudentEntity)

    @Delete
    suspend fun deleteStudent(student: StudentEntity)
}

@Dao
interface SeatAssignmentDao {
    @Query("SELECT * FROM seat_assignments WHERE libraryId = :libraryId ORDER BY createdAt DESC")
    fun getAssignmentsByLibrary(libraryId: String): Flow<List<SeatAssignmentEntity>>

    @Query("SELECT * FROM seat_assignments WHERE seatId = :seatId ORDER BY createdAt DESC")
    fun getAssignmentsForSeat(seatId: String): Flow<List<SeatAssignmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignment(assignment: SeatAssignmentEntity)

    @Update
    suspend fun updateAssignment(assignment: SeatAssignmentEntity)
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance WHERE libraryId = :libraryId ORDER BY timestamp DESC")
    fun getAttendanceByLibrary(libraryId: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE libraryId = :libraryId AND date = :date ORDER BY timestamp DESC")
    fun getAttendanceForDate(libraryId: String, date: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE studentId = :studentId ORDER BY timestamp DESC")
    fun getAttendanceForStudent(studentId: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE libraryId = :libraryId AND studentId = :studentId AND date = :date AND status = 'CHECKED_IN' LIMIT 1")
    suspend fun getActiveCheckIn(libraryId: String, studentId: String, date: String): AttendanceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: AttendanceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendances(attendances: List<AttendanceEntity>)

    @Update
    suspend fun updateAttendance(attendance: AttendanceEntity)

    @Delete
    suspend fun deleteAttendance(attendance: AttendanceEntity)

    @Query("DELETE FROM attendance WHERE id = :id")
    suspend fun deleteAttendanceById(id: String)

    @Query("SELECT * FROM attendance ORDER BY timestamp DESC")
    fun getAllAttendance(): Flow<List<AttendanceEntity>>

    @Query("SELECT DISTINCT date FROM attendance WHERE libraryId = :libraryId ORDER BY date DESC")
    fun getAttendanceDates(libraryId: String): Flow<List<String>>

    @Query("SELECT * FROM attendance WHERE libraryId = :libraryId AND date BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    fun getAttendanceForDateRange(libraryId: String, startDate: String, endDate: String): Flow<List<AttendanceEntity>>
}

@Dao
interface PhysicalBookDao {
    @Query("SELECT * FROM physical_books WHERE libraryId = :libraryId ORDER BY title ASC")
    fun getBooksByLibrary(libraryId: String): Flow<List<PhysicalBookEntity>>

    @Query("SELECT * FROM physical_books WHERE id = :id LIMIT 1")
    fun getBookById(id: String): Flow<PhysicalBookEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: PhysicalBookEntity)

    @Update
    suspend fun updateBook(book: PhysicalBookEntity)

    @Delete
    suspend fun deleteBook(book: PhysicalBookEntity)
}

@Dao
interface BookIssueDao {
    @Query("SELECT * FROM book_issues WHERE libraryId = :libraryId ORDER BY issueDate DESC")
    fun getIssuesByLibrary(libraryId: String): Flow<List<BookIssueEntity>>

    @Query("SELECT * FROM book_issues WHERE studentId = :studentId ORDER BY issueDate DESC")
    fun getIssuesForStudent(studentId: String): Flow<List<BookIssueEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIssue(issue: BookIssueEntity)

    @Update
    suspend fun updateIssue(issue: BookIssueEntity)
}

@Dao
interface DigitalMaterialDao {
    @Query("SELECT * FROM digital_materials WHERE libraryId = :libraryId ORDER BY title ASC")
    fun getMaterialsByLibrary(libraryId: String): Flow<List<DigitalMaterialEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterial(material: DigitalMaterialEntity)

    @Update
    suspend fun updateMaterial(material: DigitalMaterialEntity)

    @Delete
    suspend fun deleteMaterial(material: DigitalMaterialEntity)
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE libraryId = :libraryId ORDER BY createdAt DESC")
    fun getPaymentsByLibrary(libraryId: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE studentId = :studentId ORDER BY createdAt DESC")
    fun getPaymentsForStudent(studentId: String): Flow<List<PaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)

    @Update
    suspend fun updatePayment(payment: PaymentEntity)

    @Delete
    suspend fun deletePayment(payment: PaymentEntity)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE libraryId = :libraryId ORDER BY date DESC")
    fun getExpensesByLibrary(libraryId: String): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)
}

@Dao
interface FineDao {
    @Query("SELECT * FROM fines WHERE libraryId = :libraryId ORDER BY date DESC")
    fun getFinesByLibrary(libraryId: String): Flow<List<FineEntity>>

    @Query("SELECT * FROM fines WHERE studentId = :studentId ORDER BY date DESC")
    fun getFinesForStudent(studentId: String): Flow<List<FineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFine(fine: FineEntity)

    @Update
    suspend fun updateFine(fine: FineEntity)
}

@Dao
interface NoticeDao {
    @Query("SELECT * FROM notices WHERE libraryId = :libraryId AND isActive = 1 ORDER BY date DESC")
    fun getActiveNoticesByLibrary(libraryId: String): Flow<List<NoticeEntity>>

    @Query("SELECT * FROM notices WHERE libraryId = :libraryId ORDER BY date DESC")
    fun getAllNoticesByLibrary(libraryId: String): Flow<List<NoticeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotice(notice: NoticeEntity)

    @Update
    suspend fun updateNotice(notice: NoticeEntity)

    @Delete
    suspend fun deleteNotice(notice: NoticeEntity)
}

@Dao
interface FeedbackComplaintDao {
    @Query("SELECT * FROM feedback_complaints WHERE libraryId = :libraryId ORDER BY date DESC")
    fun getFeedbackByLibrary(libraryId: String): Flow<List<FeedbackComplaintEntity>>

    @Query("SELECT * FROM feedback_complaints WHERE studentId = :studentId ORDER BY date DESC")
    fun getFeedbackForStudent(studentId: String): Flow<List<FeedbackComplaintEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(feedback: FeedbackComplaintEntity)

    @Update
    suspend fun updateFeedback(feedback: FeedbackComplaintEntity)
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs WHERE libraryId = :libraryId ORDER BY timestamp DESC")
    fun getAuditLogsByLibrary(libraryId: String): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity)
}

@Dao
interface SaaSSubscriptionPlanDao {
    @Query("SELECT * FROM saas_plans ORDER BY price ASC")
    fun getAllPlans(): Flow<List<SaaSSubscriptionPlanEntity>>

    @Query("SELECT * FROM saas_plans WHERE id = :id LIMIT 1")
    suspend fun getPlanById(id: String): SaaSSubscriptionPlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: SaaSSubscriptionPlanEntity)

    @Update
    suspend fun updatePlan(plan: SaaSSubscriptionPlanEntity)

    @Delete
    suspend fun deletePlan(plan: SaaSSubscriptionPlanEntity)
}

@Dao
interface LibrarySubscriptionDao {
    @Query("SELECT * FROM library_subscriptions ORDER BY updatedAt DESC")
    fun getAllSubscriptions(): Flow<List<LibrarySubscriptionEntity>>

    @Query("SELECT * FROM library_subscriptions WHERE libraryId = :libraryId LIMIT 1")
    fun getSubscriptionByLibraryId(libraryId: String): Flow<LibrarySubscriptionEntity?>

    @Query("SELECT * FROM library_subscriptions WHERE libraryId = :libraryId LIMIT 1")
    suspend fun getSubscriptionDirect(libraryId: String): LibrarySubscriptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(sub: LibrarySubscriptionEntity)

    @Update
    suspend fun updateSubscription(sub: LibrarySubscriptionEntity)

    @Delete
    suspend fun deleteSubscription(sub: LibrarySubscriptionEntity)
}

@Dao
interface SuperAdminUserDao {
    @Query("SELECT * FROM super_admin_users WHERE email = :email LIMIT 1")
    suspend fun getSuperAdminByEmail(email: String): SuperAdminUserEntity?

    @Query("SELECT * FROM super_admin_users LIMIT 1")
    fun getPrimarySuperAdmin(): Flow<SuperAdminUserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuperAdmin(admin: SuperAdminUserEntity)
}

