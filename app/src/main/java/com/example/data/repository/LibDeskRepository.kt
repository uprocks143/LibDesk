package com.example.data.repository

import com.example.data.local.database.AppDatabase
import com.example.data.local.entities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class LibDeskRepository(val database: AppDatabase) {
    private val libraryDao = database.libraryDao()
    private val userAccountDao = database.userAccountDao()
    private val hallDao = database.hallDao()
    private val cabinDao = database.cabinDao()
    private val sectionDao = database.sectionDao()
    private val shiftDao = database.shiftDao()
    private val planDao = database.membershipPlanDao()
    private val seatDao = database.seatDao()
    private val studentDao = database.studentDao()
    private val seatAssignmentDao = database.seatAssignmentDao()
    private val attendanceDao = database.attendanceDao()
    private val bookDao = database.physicalBookDao()
    private val bookIssueDao = database.bookIssueDao()
    private val materialDao = database.digitalMaterialDao()
    private val paymentDao = database.paymentDao()
    private val expenseDao = database.expenseDao()
    private val fineDao = database.fineDao()
    private val noticeDao = database.noticeDao()
    private val feedbackDao = database.feedbackComplaintDao()
    private val auditLogDao = database.auditLogDao()
    private val saasPlanDao = database.saasSubscriptionPlanDao()
    private val librarySubDao = database.librarySubscriptionDao()
    private val superAdminDao = database.superAdminUserDao()
    private val userBookingCacheDao = database.userBookingCacheDao()
    private val subscriptionPlansDao = database.subscriptionPlansDao()
    private val userSubscriptionDao = database.userSubscriptionDao()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    // Room Database User Booking Cache for 100% Offline Access
    fun getCachedUserBooking(studentId: String): Flow<UserBookingCacheEntity?> =
        userBookingCacheDao.getBookingCache(studentId)

    fun getAnyCachedUserBooking(): Flow<UserBookingCacheEntity?> =
        userBookingCacheDao.getAnyBookingCache()

    fun getAllCachedUserBookings(): Flow<List<UserBookingCacheEntity>> =
        userBookingCacheDao.getAllBookingCaches()

    suspend fun getCachedUserBookingDirect(studentId: String): UserBookingCacheEntity? = withContext(Dispatchers.IO) {
        userBookingCacheDao.getBookingCacheDirect(studentId)
    }

    suspend fun cacheUserBooking(studentId: String) = withContext(Dispatchers.IO) {
        if (studentId.isBlank()) return@withContext
        val student = studentDao.findStudentById(studentId) ?: return@withContext
        val library = libraryDao.getLibraryByIdDirect(student.libraryId)
        val seat = if (student.seatId.isNotBlank()) {
            seatDao.getSeatByIdDirect(student.seatId)
        } else if (student.seatNumber.isNotBlank()) {
            seatDao.findSeatByNumberOrId(student.libraryId, student.seatNumber)
        } else null

        val shift = if (student.shiftId.isNotBlank()) {
            shiftDao.getShiftByIdDirect(student.shiftId)
        } else if (student.shiftName.isNotBlank()) {
            shiftDao.getShiftsDirect(student.libraryId).find { it.name.equals(student.shiftName, ignoreCase = true) }
        } else null

        val hall = if (seat != null && seat.hallId.isNotBlank()) {
            hallDao.getHallByIdDirect(seat.hallId)
        } else {
            hallDao.getHallsByLibrary(student.libraryId).firstOrNull()?.firstOrNull()
        }

        val plan = if (student.planId.isNotBlank()) {
            planDao.getPlanByIdDirect(student.planId)
        } else if (student.planName.isNotBlank()) {
            planDao.getPlansByLibrary(student.libraryId).firstOrNull()?.find { it.name.equals(student.planName, ignoreCase = true) }
        } else null

        val today = dateFormat.format(Date())
        val activeCheckIn = attendanceDao.getActiveCheckIn(student.libraryId, student.id, today)

        val cache = UserBookingCacheEntity(
            studentId = student.id,
            studentCode = student.studentCode,
            fullName = student.fullName,
            email = student.email,
            phone = student.mobile,
            seatId = seat?.id ?: student.seatId,
            seatNumber = student.seatNumber.ifBlank { seat?.seatNumber ?: "" },
            seatType = seat?.seatType ?: "Reserved AC Study Desk",
            hallId = hall?.id ?: "",
            hallName = hall?.name ?: seat?.hallName ?: "Main Study Hall",
            floor = seat?.floor ?: hall?.floor ?: "Ground Floor",
            shiftId = shift?.id ?: student.shiftId,
            shiftName = student.shiftName.ifBlank { shift?.name ?: "Full Day Shift" },
            shiftTimings = if (shift != null) "${shift.startTime} – ${shift.endTime}" else "08:00 AM – 08:00 PM",
            planId = plan?.id ?: student.planId,
            planName = student.planName.ifBlank { plan?.name ?: "Monthly Standard" },
            membershipStatus = student.status,
            startDate = student.joiningDate,
            expiryDate = student.expiryDate,
            rfidQrCode = student.rfidQrCode.ifBlank { student.studentCode },
            libraryId = student.libraryId,
            libraryName = library?.name ?: "LibDesk Study Hub",
            libraryAddress = library?.address ?: "Campus Library",
            lastCheckInTime = activeCheckIn?.checkInTime ?: "",
            isCheckedIn = activeCheckIn != null && activeCheckIn.status == "CHECKED_IN",
            hasAc = true,
            hasPowerSocket = true,
            hasReadingLamp = true,
            hasLocker = false,
            cachedTimestamp = System.currentTimeMillis()
        )
        userBookingCacheDao.insertOrUpdateCache(cache)
    }

    
    fun getAllSaasPlans(): Flow<List<SaaSSubscriptionPlanEntity>> = saasPlanDao.getAllPlans()
    suspend fun saveSaasPlan(plan: SaaSSubscriptionPlanEntity) = withContext(Dispatchers.IO) {
        saasPlanDao.insertPlan(plan)
    }
    suspend fun deleteSaasPlan(plan: SaaSSubscriptionPlanEntity) = withContext(Dispatchers.IO) {
        saasPlanDao.deletePlan(plan)
    }

    // Room Database SubscriptionPlans & UserSubscription methods
    fun getActiveSubscriptionPlans(): Flow<List<SubscriptionPlans>> = subscriptionPlansDao.getActivePlans()
    fun getAllSubscriptionPlans(): Flow<List<SubscriptionPlans>> = subscriptionPlansDao.getAllPlans()
    suspend fun getSubscriptionPlanById(id: String): SubscriptionPlans? = withContext(Dispatchers.IO) {
        subscriptionPlansDao.getPlanByIdDirect(id)
    }
    suspend fun saveSubscriptionPlan(plan: SubscriptionPlans) = withContext(Dispatchers.IO) {
        subscriptionPlansDao.insertPlan(plan)
    }
    suspend fun deleteSubscriptionPlan(plan: SubscriptionPlans) = withContext(Dispatchers.IO) {
        subscriptionPlansDao.deletePlan(plan)
    }
    suspend fun updateAllPlansUpi(upiId: String, payeeName: String) = withContext(Dispatchers.IO) {
        subscriptionPlansDao.updateAllPlansUpi(upiId, payeeName)
    }

    fun getUserSubscription(libraryId: String): Flow<UserSubscription?> = userSubscriptionDao.getUserSubscriptionForLibrary(libraryId)
    suspend fun getUserSubscriptionDirect(libraryId: String): UserSubscription? = withContext(Dispatchers.IO) {
        userSubscriptionDao.getUserSubscriptionForLibraryDirect(libraryId)
    }
    suspend fun saveUserSubscription(sub: UserSubscription) = withContext(Dispatchers.IO) {
        userSubscriptionDao.insertUserSubscription(sub)
        // Keep library_subscriptions in sync for backward compatibility
        librarySubDao.insertSubscription(
            LibrarySubscriptionEntity(
                id = sub.id,
                libraryId = sub.libraryId,
                libraryName = sub.libraryName,
                planId = sub.planId,
                planName = sub.planName,
                status = sub.status,
                startDate = sub.startDate,
                expiryDate = sub.expiryDate,
                price = sub.amountPaid,
                discount = 0.0,
                maxSeats = 200,
                autoRenew = false,
                notes = "Manual UPI Transfer, Ref: ${sub.paymentReferenceId}",
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun ensureDefaultSubscriptionPlansSeeded() = withContext(Dispatchers.IO) {
        val existingPlans = subscriptionPlansDao.getActivePlans().firstOrNull()
        if (existingPlans.isNullOrEmpty()) {
            val defaultPlans = listOf(
                SubscriptionPlans(
                    id = "SUB-PLAN-STARTER",
                    name = "Starter Launch",
                    description = "Essential digital library suite for small halls & study rooms",
                    price = 499.0,
                    durationMonths = 1,
                    maxSeats = 60,
                    features = "Up to 60 Dedicated Seats\nSmart Gate QR Code Attendance\nCash & UPI Fee Ledger\nReal-time Student Directory\nDigital Notice Board Broadcast\nInstant Setup in 2 Minutes",
                    badge = "Starter Pack",
                    discountPercentage = 0.0,
                    upiId = "libdesk.billing@upi",
                    upiPayeeName = "LibDesk Cloud Subscriptions",
                    supportWhatsApp = "", // TODO: set your real support WhatsApp number
                    isActive = true,
                    displayOrder = 1
                ),
                SubscriptionPlans(
                    id = "SUB-PLAN-PRO",
                    name = "Growth Pro",
                    description = "Most popular choice for growing libraries with multiple shifts",
                    price = 999.0,
                    durationMonths = 1,
                    maxSeats = 160,
                    features = "Up to 160 Dedicated & Flexible Seats\n3 Shifts Support (Morning/Evening/Full Day)\nDirect WhatsApp Fee Slips & Reminders\nStudent Self-Service Portal Access\nDigital E-Book Catalog & Issues\nFull Daily P&L Expense Tracking\nCloud-Synchronized Multi-Tenant Security",
                    badge = "Most Popular",
                    discountPercentage = 15.0,
                    upiId = "libdesk.billing@upi",
                    upiPayeeName = "LibDesk Cloud Subscriptions",
                    supportWhatsApp = "", // TODO: set your real support WhatsApp number
                    isActive = true,
                    displayOrder = 2
                ),
                SubscriptionPlans(
                    id = "SUB-PLAN-ENTERPRISE",
                    name = "Enterprise Annual",
                    description = "Maximum scale with unlimited seats, custom branding & VIP support",
                    price = 7999.0,
                    durationMonths = 12,
                    maxSeats = 9999,
                    features = "Unlimited Seats & Multi-Halls\nCustom UPI QR Code for Member Fees\n2 Months Free on Annual Billing\nAutomated Cloud Sync & Backup\nPriority 24x7 WhatsApp VIP Support\nBiometric & RFID Turnstile Ready\nAdvanced Monthly Financial Reports",
                    badge = "Best Value (Save 35%)",
                    discountPercentage = 35.0,
                    upiId = "libdesk.billing@upi",
                    upiPayeeName = "LibDesk Cloud Subscriptions",
                    supportWhatsApp = "", // TODO: set your real support WhatsApp number
                    isActive = true,
                    displayOrder = 3
                )
            )
            subscriptionPlansDao.insertPlans(defaultPlans)
        }
    }

    fun getAllLibrarySubscriptions(): Flow<List<LibrarySubscriptionEntity>> = librarySubDao.getAllSubscriptions()
    fun getSubscriptionForLibrary(libraryId: String): Flow<LibrarySubscriptionEntity?> = librarySubDao.getSubscriptionByLibraryId(libraryId)
    suspend fun getSubscriptionDirect(libraryId: String): LibrarySubscriptionEntity? = librarySubDao.getSubscriptionDirect(libraryId)
    suspend fun saveLibrarySubscription(sub: LibrarySubscriptionEntity) = withContext(Dispatchers.IO) {
        librarySubDao.insertSubscription(sub)
    }
    suspend fun deleteLibrarySubscription(sub: LibrarySubscriptionEntity) = withContext(Dispatchers.IO) {
        // Retain subscription in archive state rather than hard-deleting
        librarySubDao.insertSubscription(sub.copy(status = "ARCHIVED", notes = "Archived Subscription"))
    }

    suspend fun archiveLibrary(libraryId: String, reason: String = "Deactivated") = withContext(Dispatchers.IO) {
        val existingSub = librarySubDao.getSubscriptionDirect(libraryId)
        if (existingSub != null) {
            librarySubDao.insertSubscription(
                existingSub.copy(
                    status = "ARCHIVED",
                    notes = "Archived ($reason). Details preserved for future outreach & campaigns.",
                    updatedAt = System.currentTimeMillis()
                )
            )
        } else {
            val lib = libraryDao.getLibraryById(libraryId).firstOrNull()
            if (lib != null) {
                librarySubDao.insertSubscription(
                    LibrarySubscriptionEntity(
                        id = "SUB-ARCHIVED-${lib.id}",
                        libraryId = lib.id,
                        libraryName = lib.name,
                        planId = "PLAN-ARCHIVED",
                        planName = "Archived Library",
                        status = "ARCHIVED",
                        startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                        expiryDate = "2000-01-01",
                        price = 0.0,
                        notes = "Archived: $reason. Retained for future records and advertisement."
                    )
                )
            }
        }
        logAudit(libraryId, "SuperAdmin", "ARCHIVE_LIBRARY", "Library", libraryId, "Archived library details retained for future records/advertisement.")
    }

    suspend fun reactivateLibrary(libraryId: String) = withContext(Dispatchers.IO) {
        val existingSub = librarySubDao.getSubscriptionDirect(libraryId)
        if (existingSub != null) {
            librarySubDao.insertSubscription(
                existingSub.copy(
                    status = "ACTIVE",
                    notes = "Reactivated Library",
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        logAudit(libraryId, "SuperAdmin", "REACTIVATE_LIBRARY", "Library", libraryId, "Reactivated library to active status.")
    }

    fun getSuperAdmin(): Flow<SuperAdminUserEntity?> = superAdminDao.getPrimarySuperAdmin()
    suspend fun getSuperAdminByEmail(email: String): SuperAdminUserEntity? = superAdminDao.getSuperAdminByEmail(email)
    suspend fun saveSuperAdmin(admin: SuperAdminUserEntity) = withContext(Dispatchers.IO) {
        superAdminDao.insertSuperAdmin(admin)
    }

    
    fun getLibraryById(id: String): Flow<LibraryEntity?> = libraryDao.getLibraryById(id)
    fun getAllLibraries(): Flow<List<LibraryEntity>> = libraryDao.getAllLibraries()
    suspend fun getLibraryByCode(code: String): LibraryEntity? = libraryDao.getLibraryByCode(code)
    suspend fun saveLibrary(library: LibraryEntity) = withContext(Dispatchers.IO) {
        libraryDao.insertLibrary(library)
    }

    
    suspend fun getUserByEmail(email: String): UserAccountEntity? = userAccountDao.getUserByEmail(email)
    suspend fun getUserByIdentifier(identifier: String): UserAccountEntity? = userAccountDao.getUserByIdentifier(identifier)
    suspend fun getUserByIdentifierAndRole(identifier: String, role: String): UserAccountEntity? = userAccountDao.getUserByIdentifierAndRole(identifier, role)
    fun getUserById(id: String): Flow<UserAccountEntity?> = userAccountDao.getUserById(id)
    suspend fun findStudentByIdentifier(identifier: String): StudentEntity? = studentDao.findStudentByGlobalIdentifier(identifier)
    suspend fun saveUser(user: UserAccountEntity) = withContext(Dispatchers.IO) {
        userAccountDao.insertUser(user)
    }

    
    fun getHalls(libraryId: String): Flow<List<HallEntity>> = hallDao.getHallsByLibrary(libraryId)
    suspend fun saveHall(hall: HallEntity) = withContext(Dispatchers.IO) { hallDao.insertHall(hall) }
    suspend fun insertHall(hall: HallEntity) = withContext(Dispatchers.IO) { hallDao.insertHall(hall) }
    suspend fun updateHall(hall: HallEntity) = withContext(Dispatchers.IO) { hallDao.updateHall(hall) }
    suspend fun deleteHall(hall: HallEntity) = withContext(Dispatchers.IO) { hallDao.deleteHall(hall) }

    fun getCabins(libraryId: String): Flow<List<CabinEntity>> = cabinDao.getCabinsByLibrary(libraryId)
    suspend fun saveCabin(cabin: CabinEntity) = withContext(Dispatchers.IO) { cabinDao.insertCabin(cabin) }
    suspend fun deleteCabin(cabin: CabinEntity) = withContext(Dispatchers.IO) { cabinDao.deleteCabin(cabin) }

    fun getSections(libraryId: String): Flow<List<SectionEntity>> = sectionDao.getSectionsByLibrary(libraryId)
    suspend fun saveSection(section: SectionEntity) = withContext(Dispatchers.IO) { sectionDao.insertSection(section) }
    suspend fun deleteSection(section: SectionEntity) = withContext(Dispatchers.IO) { sectionDao.deleteSection(section) }

    fun getShifts(libraryId: String): Flow<List<ShiftEntity>> = shiftDao.getShiftsByLibrary(libraryId)
    suspend fun saveShift(shift: ShiftEntity) = withContext(Dispatchers.IO) { shiftDao.insertShift(shift) }
    suspend fun insertShift(shift: ShiftEntity) = withContext(Dispatchers.IO) { shiftDao.insertShift(shift) }
    suspend fun updateShift(shift: ShiftEntity) = withContext(Dispatchers.IO) { shiftDao.updateShift(shift) }
    suspend fun deleteShift(shift: ShiftEntity) = withContext(Dispatchers.IO) { shiftDao.deleteShift(shift) }

    fun getPlans(libraryId: String): Flow<List<MembershipPlanEntity>> = planDao.getPlansByLibrary(libraryId)
    suspend fun savePlan(plan: MembershipPlanEntity) = withContext(Dispatchers.IO) { planDao.insertPlan(plan) }
    suspend fun insertMembershipPlan(plan: MembershipPlanEntity) = withContext(Dispatchers.IO) { planDao.insertPlan(plan) }
    suspend fun updateMembershipPlan(plan: MembershipPlanEntity) = withContext(Dispatchers.IO) { planDao.updatePlan(plan) }
    suspend fun deletePlan(plan: MembershipPlanEntity) = withContext(Dispatchers.IO) { planDao.deletePlan(plan) }

    
    fun getSeats(libraryId: String): Flow<List<SeatEntity>> = seatDao.getSeatsByLibrary(libraryId)
    fun getSeatById(seatId: String): Flow<SeatEntity?> = seatDao.getSeatById(seatId)
    fun getSeatByStudentId(libraryId: String, studentId: String): Flow<SeatEntity?> =
        seatDao.getSeatByStudentId(libraryId, studentId)

    suspend fun saveSeat(seat: SeatEntity) = withContext(Dispatchers.IO) { seatDao.insertSeat(seat) }
    suspend fun deleteSeat(seat: SeatEntity) = withContext(Dispatchers.IO) { seatDao.deleteSeat(seat) }

    suspend fun generateBatchSeats(
        libraryId: String,
        prefix: String,
        count: Int,
        hallId: String,
        hallName: String,
        sectionId: String,
        sectionName: String,
        floor: String,
        seatType: String,
        fee: Double,
        startNumber: Int? = null
    ) = withContext(Dispatchers.IO) {
        val existingSeats = seatDao.getSeatsByLibrary(libraryId).firstOrNull() ?: emptyList()
        val calculatedStart = if (startNumber != null && startNumber > 0) {
            startNumber
        } else {
            val maxExisting = existingSeats
                .filter { it.seatNumber.startsWith(prefix, ignoreCase = true) }
                .mapNotNull {
                    it.seatNumber.removePrefix(prefix).removePrefix("-").trim().toIntOrNull()
                }
                .maxOrNull() ?: 0
            maxExisting + 1
        }
        val newSeats = (calculatedStart until (calculatedStart + count)).map { num ->
            val formattedNum = String.format("%02d", num)
            val seatNumber = "$prefix-$formattedNum"
            SeatEntity(
                id = UUID.randomUUID().toString(),
                libraryId = libraryId,
                seatNumber = seatNumber,
                hallId = hallId,
                hallName = hallName,
                sectionId = sectionId,
                sectionName = sectionName,
                floor = floor,
                seatType = seatType,
                monthlyFee = fee,
                status = "AVAILABLE"
            )
        }
        seatDao.insertSeats(newSeats)
        val endNum = calculatedStart + count - 1
        logAudit(libraryId, "Manager", "BATCH_GENERATE_SEATS", "Seats", "", "Generated $count seats ($prefix-${String.format("%02d", calculatedStart)} to $prefix-${String.format("%02d", endNum)})")
    }

    
    suspend fun assignSeat(
        libraryId: String,
        seat: SeatEntity,
        student: StudentEntity,
        shift: ShiftEntity?,
        plan: MembershipPlanEntity?,
        validUntilDate: String
    ) = withContext(Dispatchers.IO) {

        val updatedSeat = seat.copy(
            status = "OCCUPIED",
            assignedStudentId = student.id,
            assignedStudentName = student.fullName,
            assignedShiftId = shift?.id ?: "",
            assignedShiftName = shift?.name ?: "",
            validUntil = validUntilDate
        )
        seatDao.insertSeat(updatedSeat)

        
        val updatedStudent = student.copy(
            seatId = seat.id,
            seatNumber = seat.seatNumber,
            hallName = seat.hallName,
            shiftId = shift?.id ?: student.shiftId,
            shiftName = shift?.name ?: student.shiftName,
            planId = plan?.id ?: student.planId,
            planName = plan?.name ?: student.planName,
            expiryDate = if (validUntilDate.isNotEmpty()) validUntilDate else student.expiryDate
        )
        studentDao.insertStudent(updatedStudent)

        
        val assignment = SeatAssignmentEntity(
            id = UUID.randomUUID().toString(),
            libraryId = libraryId,
            seatId = seat.id,
            seatNumber = seat.seatNumber,
            studentId = student.id,
            studentName = student.fullName,
            shiftId = shift?.id ?: "",
            shiftName = shift?.name ?: "",
            startDate = dateFormat.format(Date()),
            endDate = validUntilDate,
            planId = plan?.id ?: "",
            status = "ACTIVE",
            notes = "Assigned to ${student.fullName}"
        )
        seatAssignmentDao.insertAssignment(assignment)
        logAudit(libraryId, "Manager", "ASSIGN_SEAT", "Seat", seat.id, "Assigned ${seat.seatNumber} to ${student.fullName}")
    }

    suspend fun transferSeat(
        libraryId: String,
        oldSeat: SeatEntity,
        newSeat: SeatEntity,
        student: StudentEntity
    ) = withContext(Dispatchers.IO) {

        val clearedOldSeat = oldSeat.copy(
            status = "AVAILABLE",
            assignedStudentId = "",
            assignedStudentName = "",
            assignedShiftId = "",
            assignedShiftName = "",
            validUntil = ""
        )
        seatDao.insertSeat(clearedOldSeat)

        
        val occupiedNewSeat = newSeat.copy(
            status = "OCCUPIED",
            assignedStudentId = student.id,
            assignedStudentName = student.fullName,
            assignedShiftId = oldSeat.assignedShiftId,
            assignedShiftName = oldSeat.assignedShiftName,
            validUntil = oldSeat.validUntil
        )
        seatDao.insertSeat(occupiedNewSeat)

        
        val updatedStudent = student.copy(
            seatId = newSeat.id,
            seatNumber = newSeat.seatNumber,
            hallName = newSeat.hallName
        )
        studentDao.insertStudent(updatedStudent)

        
        val transferRecord = SeatAssignmentEntity(
            id = UUID.randomUUID().toString(),
            libraryId = libraryId,
            seatId = newSeat.id,
            seatNumber = newSeat.seatNumber,
            studentId = student.id,
            studentName = student.fullName,
            shiftId = oldSeat.assignedShiftId,
            shiftName = oldSeat.assignedShiftName,
            startDate = dateFormat.format(Date()),
            endDate = oldSeat.validUntil,
            planId = student.planId,
            status = "TRANSFERRED",
            notes = "Transferred from ${oldSeat.seatNumber} to ${newSeat.seatNumber}"
        )
        seatAssignmentDao.insertAssignment(transferRecord)
        logAudit(libraryId, "Manager", "TRANSFER_SEAT", "Seat", newSeat.id, "Transferred ${student.fullName} from ${oldSeat.seatNumber} to ${newSeat.seatNumber}")
    }

    suspend fun releaseSeat(libraryId: String, seat: SeatEntity) = withContext(Dispatchers.IO) {
        val studentId = seat.assignedStudentId
        if (studentId.isNotEmpty()) {
            val student = studentDao.findStudentByMobileOrCode(libraryId, studentId)
            if (student != null) {
                studentDao.insertStudent(student.copy(seatId = "", seatNumber = ""))
            }
        }
        val cleared = seat.copy(
            status = "AVAILABLE",
            assignedStudentId = "",
            assignedStudentName = "",
            assignedShiftId = "",
            assignedShiftName = "",
            validUntil = ""
        )
        seatDao.insertSeat(cleared)
        logAudit(libraryId, "Manager", "RELEASE_SEAT", "Seat", seat.id, "Released seat ${seat.seatNumber}")
    }

    suspend fun updateSeatStatus(libraryId: String, seat: SeatEntity, newStatus: String) = withContext(Dispatchers.IO) {
        val updated = seat.copy(status = newStatus)
        seatDao.insertSeat(updated)
        logAudit(libraryId, "Manager", "UPDATE_SEAT_STATUS", "Seat", seat.id, "Updated status to $newStatus")
    }

    suspend fun toggleSeatReservation(libraryId: String, seat: SeatEntity): String = withContext(Dispatchers.IO) {
        val newStatus = if (seat.status.equals("RESERVED", ignoreCase = true)) "AVAILABLE" else "RESERVED"
        val updated = seat.copy(
            status = newStatus,
            assignedStudentId = if (newStatus == "AVAILABLE") "" else seat.assignedStudentId,
            assignedStudentName = if (newStatus == "AVAILABLE") "" else seat.assignedStudentName
        )
        seatDao.insertSeat(updated)
        logAudit(libraryId, "Manager", "TOGGLE_SEAT_RESERVATION", "Seat", seat.id, "Toggled seat ${seat.seatNumber} status to $newStatus")
        newStatus
    }

    
    fun getStudents(libraryId: String): Flow<List<StudentEntity>> = studentDao.getStudentsByLibrary(libraryId)
    fun getStudentById(studentId: String): Flow<StudentEntity?> = studentDao.getStudentById(studentId)
    suspend fun saveStudent(student: StudentEntity) = withContext(Dispatchers.IO) {
        studentDao.insertStudent(student)
        cacheUserBooking(student.id)
        logAudit(student.libraryId, "Manager", "SAVE_STUDENT", "Student", student.id, "Saved student ${student.fullName}")
    }
    suspend fun deleteStudent(student: StudentEntity) = withContext(Dispatchers.IO) {
        archiveStudent(student.libraryId, student, "Requested Deletion / Exit")
    }

    suspend fun archiveStudent(libraryId: String, student: StudentEntity, reason: String = "Left Library") = withContext(Dispatchers.IO) {
        // 1. Free up any seat allocated to this student so new students can occupy it
        if (student.seatId.isNotBlank()) {
            val seat = seatDao.getSeatById(student.seatId).firstOrNull()
            if (seat != null) {
                seatDao.insertSeat(
                    seat.copy(
                        status = "AVAILABLE",
                        assignedStudentId = "",
                        assignedStudentName = "",
                        assignedShiftId = "",
                        assignedShiftName = "",
                        validUntil = ""
                    )
                )
            }
        }
        val seatsInLib = seatDao.getSeatsByLibrary(libraryId).firstOrNull() ?: emptyList()
        seatsInLib.filter { it.assignedStudentId == student.id }.forEach { seat ->
            seatDao.insertSeat(
                seat.copy(
                    status = "AVAILABLE",
                    assignedStudentId = "",
                    assignedStudentName = "",
                    assignedShiftId = "",
                    assignedShiftName = "",
                    validUntil = ""
                )
            )
        }

        // 2. Retain student record intact in database with ARCHIVED status
        val archived = student.copy(
            status = "ARCHIVED",
            seatId = "",
            seatNumber = if (student.seatNumber.isNotBlank() && !student.seatNumber.contains("(Past)")) "${student.seatNumber} (Past)" else student.seatNumber
        )
        studentDao.insertStudent(archived)
        logAudit(libraryId, "Manager", "ARCHIVE_STUDENT", "Student", student.id, "Archived student ${student.fullName} (Reason: $reason). Historical details retained for future records & advertisement.")
    }

    suspend fun reactivateStudent(libraryId: String, student: StudentEntity) = withContext(Dispatchers.IO) {
        val reactivated = student.copy(
            status = "ACTIVE",
            seatNumber = student.seatNumber.replace(" (Past)", "")
        )
        studentDao.insertStudent(reactivated)
        logAudit(libraryId, "Manager", "REACTIVATE_STUDENT", "Student", student.id, "Reactivated student ${student.fullName} back to active membership.")
    }

    
    fun getAttendance(libraryId: String): Flow<List<AttendanceEntity>> = attendanceDao.getAttendanceByLibrary(libraryId)
    fun getTodayAttendance(libraryId: String): Flow<List<AttendanceEntity>> {
        val today = dateFormat.format(Date())
        return attendanceDao.getAttendanceForDate(libraryId, today)
    }
    fun getStudentAttendance(studentId: String): Flow<List<AttendanceEntity>> =
        attendanceDao.getAttendanceForStudent(studentId)

    suspend fun processQrAttendance(
        libraryId: String,
        qrCodeOrStudentCode: String,
        studentIdContext: String? = null,
        locationNote: String? = null
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val trimmed = qrCodeOrStudentCode.trim()
        val isGateAttendanceQr = trimmed.startsWith("LIBDESK_GATE_ATTENDANCE:") ||
                trimmed.startsWith("LIBDESK_GATE:") ||
                trimmed.startsWith("GATE_ATTENDANCE:") ||
                trimmed.startsWith("LIBDESK:ATTENDANCE:")

        val cleanCode = when {
            isGateAttendanceQr -> trimmed
            trimmed.startsWith("QR-") -> trimmed.removePrefix("QR-")
            trimmed.startsWith("LIBDESK:SEAT:") -> trimmed.removePrefix("LIBDESK:SEAT:")
            trimmed.startsWith("SEAT:") -> trimmed.removePrefix("SEAT:")
            trimmed.contains(":") -> trimmed.substringAfterLast(":")
            else -> trimmed
        }

        
        var student: StudentEntity? = null
        if (isGateAttendanceQr && !studentIdContext.isNullOrBlank()) {
            student = studentDao.findStudentById(studentIdContext)
        }

        
        if (student == null) {
            student = studentDao.findStudentByMobileOrCode(libraryId, cleanCode)
                ?: studentDao.findStudentByMobileOrCode(libraryId, trimmed)
        }

        
        if (student == null && !studentIdContext.isNullOrBlank()) {
            val candidateStudent = studentDao.findStudentById(studentIdContext)
            if (candidateStudent != null) {
                val seat = seatDao.findSeatByNumberOrId(libraryId, cleanCode)
                if (seat != null) {
                    student = candidateStudent
                }
            }
        }

        
        if (student == null) {
            val seat = seatDao.findSeatByNumberOrId(libraryId, cleanCode)
            if (seat != null && seat.assignedStudentId.isNotBlank()) {
                student = studentDao.findStudentByMobileOrCode(libraryId, seat.assignedStudentId)
            }
        }

        
        if (student == null && !studentIdContext.isNullOrBlank()) {
            student = studentDao.findStudentById(studentIdContext)
        }

        if (student == null) {
            return@withContext Pair(false, "No member or seat record found for code: $qrCodeOrStudentCode")
        }

        val today = dateFormat.format(Date())
        val nowTime = timeFormat.format(Date())

        val activeCheckIn = attendanceDao.getActiveCheckIn(libraryId, student.id, today)

        val modeLabel = if (isGateAttendanceQr) {
            if (!locationNote.isNullOrBlank()) "GATE_QR (GPS Verified)" else "GATE_QR"
        } else {
            if (!locationNote.isNullOrBlank()) "SEAT_QR (GPS Verified)" else "QR_SCAN"
        }

        if (activeCheckIn != null) {

            val duration = try {
                val t1 = timeFormat.parse(activeCheckIn.checkInTime)
                val t2 = timeFormat.parse(nowTime)
                if (t1 != null && t2 != null) {
                    val diff = (t2.time - t1.time) / (60 * 1000)
                    if (diff > 0) diff.toInt() else 60
                } else 180
            } catch (e: Exception) { 180 }

            val checkOutUpdated = activeCheckIn.copy(
                checkOutTime = nowTime,
                status = "CHECKED_OUT",
                durationMinutes = duration,
                notes = if (locationNote != null) "${activeCheckIn.notes} | Out: $locationNote".trimStart(' ', '|') else activeCheckIn.notes
            )
            attendanceDao.updateAttendance(checkOutUpdated)
            cacheUserBooking(student.id)
            logAudit(libraryId, "QR Scanner", "QR_CHECK_OUT", "Attendance", checkOutUpdated.id, "Check-out for ${student.fullName} at $nowTime [${modeLabel}]")
            Pair(true, "✅ Checked OUT: ${student.fullName} at $nowTime.\nSession logged: ${duration / 60}h ${duration % 60}m (${modeLabel})")
        } else {

            val newCheckIn = AttendanceEntity(
                id = UUID.randomUUID().toString(),
                libraryId = libraryId,
                studentId = student.id,
                studentName = student.fullName,
                seatNumber = student.seatNumber,
                hallName = student.hallName,
                shiftName = student.shiftName,
                date = today,
                checkInTime = nowTime,
                checkOutTime = "",
                status = "CHECKED_IN",
                mode = modeLabel,
                notes = locationNote ?: "Verified Attendance"
            )
            attendanceDao.insertAttendance(newCheckIn)
            cacheUserBooking(student.id)
            logAudit(libraryId, "QR Scanner", "QR_CHECK_IN", "Attendance", newCheckIn.id, "Check-in for ${student.fullName} at $nowTime [${modeLabel}]")
            Pair(true, "✅ Checked IN: ${student.fullName} at $nowTime.\nSeat: ${student.seatNumber.ifEmpty { "General Desk" }} (${modeLabel})")
        }
    }

    suspend fun manualAttendance(attendance: AttendanceEntity) = withContext(Dispatchers.IO) {
        attendanceDao.insertAttendance(attendance)
        logAudit(attendance.libraryId, "Manager", "MANUAL_ATTENDANCE", "Attendance", attendance.id, "Manual attendance recorded for ${attendance.studentName} on ${attendance.date}")
    }

    suspend fun checkoutAttendance(attendanceId: String, checkOutTime: String? = null) = withContext(Dispatchers.IO) {
        val now = checkOutTime ?: timeFormat.format(Date())
        val records = attendanceDao.getAttendanceByLibrary("").firstOrNull() // fallback query or update
    }

    suspend fun updateAttendance(attendance: AttendanceEntity) = withContext(Dispatchers.IO) {
        attendanceDao.updateAttendance(attendance)
    }

    suspend fun deleteAttendance(attendanceId: String) = withContext(Dispatchers.IO) {
        attendanceDao.deleteAttendanceById(attendanceId)
    }

    fun getAllAttendance(): Flow<List<AttendanceEntity>> = attendanceDao.getAllAttendance()
    fun getAttendanceForDateRange(libraryId: String, startDate: String, endDate: String): Flow<List<AttendanceEntity>> =
        attendanceDao.getAttendanceForDateRange(libraryId, startDate, endDate)

    
    fun getBooks(libraryId: String): Flow<List<PhysicalBookEntity>> = bookDao.getBooksByLibrary(libraryId)
    fun getBookIssues(libraryId: String): Flow<List<BookIssueEntity>> = bookIssueDao.getIssuesByLibrary(libraryId)
    fun getStudentBookIssues(studentId: String): Flow<List<BookIssueEntity>> = bookIssueDao.getIssuesForStudent(studentId)

    suspend fun saveBook(book: PhysicalBookEntity) = withContext(Dispatchers.IO) {
        bookDao.insertBook(book)
    }
    suspend fun deleteBook(book: PhysicalBookEntity) = withContext(Dispatchers.IO) {
        bookDao.deleteBook(book)
    }

    suspend fun issueBook(
        libraryId: String,
        book: PhysicalBookEntity,
        student: StudentEntity,
        loanDays: Int = 14
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (book.availableCopies <= 0) {
            return@withContext Pair(false, "No available copies for '${book.title}'")
        }

        val cal = Calendar.getInstance()
        val issueDateStr = dateFormat.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, loanDays)
        val dueDateStr = dateFormat.format(cal.time)

        val issue = BookIssueEntity(
            id = UUID.randomUUID().toString(),
            libraryId = libraryId,
            bookId = book.id,
            bookTitle = book.title,
            studentId = student.id,
            studentName = student.fullName,
            studentMobile = student.mobile,
            issueDate = issueDateStr,
            dueDate = dueDateStr,
            status = "ISSUED"
        )
        bookIssueDao.insertIssue(issue)

        val updatedBook = book.copy(
            availableCopies = book.availableCopies - 1,
            issuedCopies = book.issuedCopies + 1
        )
        bookDao.insertBook(updatedBook)

        logAudit(libraryId, "Manager", "ISSUE_BOOK", "BookIssue", issue.id, "Issued '${book.title}' to ${student.fullName} (Due: $dueDateStr)")
        Pair(true, "Book issued successfully to ${student.fullName}. Due: $dueDateStr")
    }

    suspend fun returnBook(
        libraryId: String,
        issue: BookIssueEntity,
        finePerDay: Double = 5.0
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val today = dateFormat.format(Date())
        var fineAmount = 0.0

        try {
            val due = dateFormat.parse(issue.dueDate)
            val todayDate = dateFormat.parse(today)
            if (due != null && todayDate != null && todayDate.after(due)) {
                val diffDays = ((todayDate.time - due.time) / (1000 * 60 * 60 * 24)).toInt()
                if (diffDays > 0) {
                    fineAmount = diffDays * finePerDay
                }
            }
        } catch (_: Exception) {}

        val updatedIssue = issue.copy(
            returnDate = today,
            fineAmount = fineAmount,
            status = "RETURNED"
        )
        bookIssueDao.updateIssue(updatedIssue)

        
        if (fineAmount > 0) {
            val fine = FineEntity(
                id = UUID.randomUUID().toString(),
                libraryId = libraryId,
                studentId = issue.studentId,
                studentName = issue.studentName,
                bookId = issue.bookId,
                bookTitle = issue.bookTitle,
                reason = "Late Return of '${issue.bookTitle}'",
                amount = fineAmount,
                paid = false,
                date = today
            )
            fineDao.insertFine(fine)
        }

        

        logAudit(libraryId, "Manager", "RETURN_BOOK", "BookIssue", issue.id, "Returned '${issue.bookTitle}'. Fine: ₹$fineAmount")
        Pair(true, if (fineAmount > 0) "Book returned with overdue fine of ₹$fineAmount" else "Book returned successfully with zero fines!")
    }

    
    fun getDigitalMaterials(libraryId: String): Flow<List<DigitalMaterialEntity>> =
        materialDao.getMaterialsByLibrary(libraryId)
    suspend fun saveDigitalMaterial(material: DigitalMaterialEntity) = withContext(Dispatchers.IO) {
        materialDao.insertMaterial(material)
    }
    suspend fun toggleBookmarkMaterial(material: DigitalMaterialEntity) = withContext(Dispatchers.IO) {
        materialDao.updateMaterial(material.copy(isBookmarked = !material.isBookmarked))
    }
    suspend fun deleteDigitalMaterial(material: DigitalMaterialEntity) = withContext(Dispatchers.IO) {
        materialDao.deleteMaterial(material)
    }

    
    fun getPayments(libraryId: String): Flow<List<PaymentEntity>> = paymentDao.getPaymentsByLibrary(libraryId)
    fun getStudentPayments(studentId: String): Flow<List<PaymentEntity>> = paymentDao.getPaymentsForStudent(studentId)

    suspend fun recordPayment(
        libraryId: String,
        student: StudentEntity,
        amount: Double,
        mode: String,
        purpose: String,
        refNum: String,
        receiptPrefix: String = "REC",
        discount: Double = 0.0,
        remarks: String = "",
        period: String = ""
    ): PaymentEntity = withContext(Dispatchers.IO) {
        val receiptNumber = "$receiptPrefix-${System.currentTimeMillis().toString().takeLast(6)}"
        val today = dateFormat.format(Date())
        val validDiscount = discount.coerceAtLeast(0.0)
        val effectiveReduction = amount + validDiscount
        val newDue = (student.dueAmount - effectiveReduction).coerceAtLeast(0.0)
        val newPaid = student.paidAmount + amount

        val newTotalFee = maxOf(student.totalFee, newPaid + newDue)

        val noteText = if (remarks.isNotBlank()) {
            remarks
        } else if (validDiscount > 0) {
            "Payment via $mode for $purpose (Discount: ₹${validDiscount.toInt()})"
        } else {
            "Payment via $mode for $purpose"
        }

        val payment = PaymentEntity(
            id = UUID.randomUUID().toString(),
            libraryId = libraryId,
            receiptNumber = receiptNumber,
            studentId = student.id,
            studentName = student.fullName,
            amount = amount,
            paymentMode = mode,
            date = today,
            purpose = purpose,
            referenceNumber = refNum,
            notes = noteText,
            remarks = remarks,
            period = period,
            dueBalance = newDue
        )
        paymentDao.insertPayment(payment)

        
        val toDateFromPeriod: String? = when {
            period.contains(" to ", ignoreCase = true) -> period.substringAfter(" to ", "").trim().takeIf { it.isNotBlank() }
            period.contains(" - ") -> period.substringAfter(" - ", "").trim().takeIf { it.isNotBlank() }
            else -> null
        }

        val updatedExpiryDate = if (!toDateFromPeriod.isNullOrBlank()) {
            toDateFromPeriod
        } else {
            student.expiryDate
        }

        val updatedStatus = if (student.status.equals("EXPIRED", ignoreCase = true) && !updatedExpiryDate.isNullOrBlank()) {
            "ACTIVE"
        } else {
            student.status
        }

        val updatedStudent = student.copy(
            totalFee = newTotalFee,
            paidAmount = newPaid,
            dueAmount = newDue,
            expiryDate = updatedExpiryDate,
            status = updatedStatus
        )
        studentDao.insertStudent(updatedStudent)

        logAudit(libraryId, "Manager", "RECORD_PAYMENT", "Payment", payment.id, "Receipt $receiptNumber: ₹$amount for ${student.fullName}")
        payment
    }

    fun getExpenses(libraryId: String): Flow<List<ExpenseEntity>> = expenseDao.getExpensesByLibrary(libraryId)
    suspend fun saveExpense(expense: ExpenseEntity) = withContext(Dispatchers.IO) {
        expenseDao.insertExpense(expense)
        logAudit(expense.libraryId, "Manager", "RECORD_EXPENSE", "Expense", expense.id, "Expense of ₹${expense.amount} for ${expense.category}")
    }
    suspend fun updateExpense(expense: ExpenseEntity) = withContext(Dispatchers.IO) {
        expenseDao.updateExpense(expense)
        logAudit(expense.libraryId, "Manager", "UPDATE_EXPENSE", "Expense", expense.id, "Updated expense of ₹${expense.amount} for ${expense.category}")
    }
    suspend fun deleteExpense(expense: ExpenseEntity) = withContext(Dispatchers.IO) {
        expenseDao.deleteExpense(expense)
        logAudit(expense.libraryId, "Manager", "DELETE_EXPENSE", "Expense", expense.id, "Deleted expense of ₹${expense.amount}")
    }

    suspend fun updatePayment(payment: PaymentEntity) = withContext(Dispatchers.IO) {
        paymentDao.updatePayment(payment)
        logAudit(payment.libraryId, "Manager", "UPDATE_PAYMENT", "Payment", payment.id, "Updated payment receipt #${payment.receiptNumber}: ₹${payment.amount}")
    }

    suspend fun deletePayment(payment: PaymentEntity) = withContext(Dispatchers.IO) {
        paymentDao.deletePayment(payment)
        logAudit(payment.libraryId, "Manager", "DELETE_PAYMENT", "Payment", payment.id, "Deleted payment receipt #${payment.receiptNumber}")
    }

    
    fun getFines(libraryId: String): Flow<List<FineEntity>> = fineDao.getFinesByLibrary(libraryId)
    fun getStudentFines(studentId: String): Flow<List<FineEntity>> = fineDao.getFinesForStudent(studentId)
    suspend fun markFinePaid(fine: FineEntity) = withContext(Dispatchers.IO) {
        fineDao.updateFine(fine.copy(paid = true))
    }

    
    fun getActiveNotices(libraryId: String): Flow<List<NoticeEntity>> = noticeDao.getActiveNoticesByLibrary(libraryId)
    fun getAllNotices(libraryId: String): Flow<List<NoticeEntity>> = noticeDao.getAllNoticesByLibrary(libraryId)
    fun getAllBroadcastNotices(): Flow<List<NoticeEntity>> = noticeDao.getAllBroadcastNotices()
    suspend fun saveNotice(notice: NoticeEntity) = withContext(Dispatchers.IO) {
        noticeDao.insertNotice(notice)
        logAudit(notice.libraryId, "Manager", "PUBLISH_NOTICE", "Notice", notice.id, "Published notice '${notice.title}'")
    }
    suspend fun deleteNotice(notice: NoticeEntity) = withContext(Dispatchers.IO) {
        noticeDao.deleteNotice(notice)
    }
    suspend fun deleteNoticeById(id: String) = withContext(Dispatchers.IO) {
        noticeDao.deleteNoticeById(id)
    }

    
    fun getFeedback(libraryId: String): Flow<List<FeedbackComplaintEntity>> =
        feedbackDao.getFeedbackByLibrary(libraryId)
    fun getStudentFeedback(studentId: String): Flow<List<FeedbackComplaintEntity>> =
        feedbackDao.getFeedbackForStudent(studentId)
    suspend fun submitFeedback(feedback: FeedbackComplaintEntity) = withContext(Dispatchers.IO) {
        feedbackDao.insertFeedback(feedback)
    }
    suspend fun updateFeedbackStatus(feedback: FeedbackComplaintEntity, newStatus: String, reply: String) = withContext(Dispatchers.IO) {
        val resolvedDate = if (newStatus == "RESOLVED" || newStatus == "CLOSED") dateFormat.format(Date()) else feedback.resolvedDate
        val updated = feedback.copy(
            status = newStatus,
            reply = reply,
            resolvedDate = resolvedDate
        )
        feedbackDao.updateFeedback(updated)
    }

    
    fun getAuditLogs(libraryId: String): Flow<List<AuditLogEntity>> = auditLogDao.getAuditLogsByLibrary(libraryId)
    suspend fun logAudit(
        libraryId: String,
        performedBy: String,
        action: String,
        recordType: String,
        recordId: String,
        details: String
    ) = withContext(Dispatchers.IO) {
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                libraryId = libraryId,
                performedBy = performedBy,
                action = action,
                recordType = recordType,
                recordId = recordId,
                details = details,
                timestamp = System.currentTimeMillis()
            )
        )
    }

}
