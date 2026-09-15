package com.example.data.repository

import com.example.data.local.database.AppDatabase
import com.example.data.local.entities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class LibDeskRepository(private val database: AppDatabase) {
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
    suspend fun deleteShift(shift: ShiftEntity) = withContext(Dispatchers.IO) { shiftDao.deleteShift(shift) }

    fun getPlans(libraryId: String): Flow<List<MembershipPlanEntity>> = planDao.getPlansByLibrary(libraryId)
    suspend fun savePlan(plan: MembershipPlanEntity) = withContext(Dispatchers.IO) { planDao.insertPlan(plan) }
    suspend fun insertMembershipPlan(plan: MembershipPlanEntity) = withContext(Dispatchers.IO) { planDao.insertPlan(plan) }
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
    suspend fun saveNotice(notice: NoticeEntity) = withContext(Dispatchers.IO) {
        noticeDao.insertNotice(notice)
        logAudit(notice.libraryId, "Manager", "PUBLISH_NOTICE", "Notice", notice.id, "Published notice '${notice.title}'")
    }
    suspend fun deleteNotice(notice: NoticeEntity) = withContext(Dispatchers.IO) {
        noticeDao.deleteNotice(notice)
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

    
    suspend fun seedDemoData(): String = withContext(Dispatchers.IO) {
        val libId = "LIB-VANGUARD-01"
        val existing = libraryDao.getLibraryByCode("VNG-789")
        if (existing != null) return@withContext existing.id

        val demoLib = LibraryEntity(
            id = libId,
            name = "Vanguard Central Study Point & Digital Library",
            code = "VNG-789",
            logoUrl = "",
            description = "Premium air-conditioned 24x7 study hall and physical library for competitive exam aspirants (UPSC, State PSC, SSC, Banking, NEET, JEE).",
            establishedDate = "2021-06-15",
            regNumber = "LIB/DEL/2021/8940",
            ownerName = "Rajesh Verma",
            ownerPhone = "+91 98765 43210",
            ownerEmail = "manager@vanguardlibrary.in",
            ownerWhatsApp = "+91 98765 43210",
            address = "Plot 42, 2nd Floor, Apex Tower, Near Metro Station, Mukherjee Nagar",
            landmark = "Opposite Central Park",
            city = "New Delhi",
            district = "North Delhi",
            state = "Delhi",
            pincode = "110009",
            latitude = 28.7041,
            longitude = 77.1025,
            phone = "011-27654321",
            whatsapp = "+91 98765 43210",
            email = "contact@vanguardlibrary.in",
            website = "https://vanguardlibrary.in",
            upiId = "vanguardlib@okhdfcbank",
            upiPayeeName = "Vanguard Library & Study Hub",
            receiptPrefix = "VNG",
            defaultFinePerDay = 5.0,
            borrowLimit = 2,
            loanDays = 14
        )
        libraryDao.insertLibrary(demoLib)

        
        userAccountDao.insertUser(
            UserAccountEntity(
                id = "USER-MGR-01",
                email = "manager@vanguardlibrary.in",
                password = "password123",
                role = "MANAGER",
                libraryId = libId,
                name = "Rajesh Verma",
                phone = "+91 98765 43210"
            )
        )
        userAccountDao.insertUser(
            UserAccountEntity(
                id = "USER-MGR-02",
                email = "admin@libdesk.io",
                password = "password123",
                role = "MANAGER",
                libraryId = libId,
                name = "Rajesh Verma",
                phone = "+91 98765 43210"
            )
        )

        
        val hall1 = HallEntity(
            id = "HALL-01",
            libraryId = libId,
            name = "Main Silent Reading Hall (AC)",
            type = "Silent AC Study Hall",
            floor = "2nd Floor",
            isAc = true,
            description = "Pin-drop silence hall with ergonomic cubicle seats and power sockets.",
            seatCount = 18,
            openingTime = "06:00 AM",
            closingTime = "11:00 PM"
        )
        val hall2 = HallEntity(
            id = "HALL-02",
            libraryId = libId,
            name = "Girls Study & Discussion Hall",
            type = "Girls Study Hall",
            floor = "2nd Floor",
            isAc = true,
            description = "Exclusive study zone equipped with high-speed fiber Wi-Fi.",
            seatCount = 8,
            openingTime = "06:00 AM",
            closingTime = "10:00 PM"
        )
        hallDao.insertHall(hall1)
        hallDao.insertHall(hall2)

        
        val cabin1 = CabinEntity(
            id = "CABIN-01",
            libraryId = libId,
            cabinNumber = "CAB-101",
            name = "Private Executive Cabin 1",
            floor = "2nd Floor",
            isAc = true,
            isPrivate = true,
            seatCount = 1,
            monthlyFee = 3000.0,
            description = "Private sound-insulated study cabin for interview & high-focus prep."
        )
        cabinDao.insertCabin(cabin1)

        
        val sec1 = SectionEntity("SEC-01", libId, "UPSC / Civil Services", "Civil Services exam study enclave", "2nd Floor", hall1.id)
        val sec2 = SectionEntity("SEC-02", libId, "SSC & Banking", "General competitive exams section", "2nd Floor", hall1.id)
        val sec3 = SectionEntity("SEC-03", libId, "NEET & JEE", "Medical and Engineering prep zone", "2nd Floor", hall2.id)
        sectionDao.insertSection(sec1)
        sectionDao.insertSection(sec2)
        sectionDao.insertSection(sec3)

        
        val shiftMorning = ShiftEntity("SHIFT-01", libId, "Morning Shift", "06:00 AM", "01:30 PM", 900.0, "Morning study batch")
        val shiftEvening = ShiftEntity("SHIFT-02", libId, "Evening Shift", "01:30 PM", "08:30 PM", 900.0, "Evening study batch")
        val shiftFullDay = ShiftEntity("SHIFT-03", libId, "Full Day (24x7 Access)", "06:00 AM", "11:00 PM", 1600.0, "Full access to reading hall")
        shiftDao.insertShift(shiftMorning)
        shiftDao.insertShift(shiftEvening)
        shiftDao.insertShift(shiftFullDay)

        
        val planMonthly = MembershipPlanEntity("PLAN-01", libId, "Standard Monthly", 1, 1000.0, 100.0, 500.0, 0.0, "Standard")
        val planQuarterly = MembershipPlanEntity("PLAN-02", libId, "Quarterly Scholar (10% Off)", 3, 2700.0, 200.0, 500.0, 300.0, "Standard")
        val planFullDay = MembershipPlanEntity("PLAN-03", libId, "Full Day Unlimited (1 Month)", 1, 1600.0, 100.0, 500.0, 0.0, "Ergonomic")
        planDao.insertPlan(planMonthly)
        planDao.insertPlan(planQuarterly)
        planDao.insertPlan(planFullDay)

        
        val seats = mutableListOf<SeatEntity>()
        for (i in 1..16) {
            val numStr = String.format("%02d", i)
            val isOcc = i in listOf(1, 2, 4, 7, 9, 12)
            val isRes = i in listOf(3, 11)
            val isMaint = i == 16
            val status = when {
                isOcc -> "OCCUPIED"
                isRes -> "RESERVED"
                isMaint -> "MAINTENANCE"
                else -> "AVAILABLE"
            }
            seats.add(
                SeatEntity(
                    id = "SEAT-A$numStr",
                    libraryId = libId,
                    seatNumber = "A-$numStr",
                    hallId = hall1.id,
                    hallName = hall1.name,
                    sectionId = if (i <= 8) sec1.id else sec2.id,
                    sectionName = if (i <= 8) sec1.name else sec2.name,
                    floor = "2nd Floor",
                    seatType = if (i % 3 == 0) "Corner Window" else "Standard",
                    monthlyFee = 1000.0,
                    status = status
                )
            )
        }
        for (j in 1..6) {
            val numStr = String.format("%02d", j)
            val isOcc = j in listOf(1, 3)
            seats.add(
                SeatEntity(
                    id = "SEAT-B$numStr",
                    libraryId = libId,
                    seatNumber = "B-$numStr",
                    hallId = hall2.id,
                    hallName = hall2.name,
                    sectionId = sec3.id,
                    sectionName = sec3.name,
                    floor = "2nd Floor",
                    seatType = "Ergonomic",
                    monthlyFee = 1100.0,
                    status = if (isOcc) "OCCUPIED" else "AVAILABLE"
                )
            )
        }
        seatDao.insertSeats(seats)

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        fun getRelativeDate(daysOffset: Int): String {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, daysOffset)
            return sdf.format(cal.time)
        }

        
        val student1 = StudentEntity(
            id = "STU-001",
            libraryId = libId,
            studentCode = "STU-1001",
            fullName = "Aditya Sharma",
            mobile = "9812345670",
            email = "aditya.upsc@gmail.com",
            dob = "2000-04-12",
            gender = "Male",
            address = "Flat 12B, Old Rajinder Nagar, New Delhi",
            parentName = "Sunil Sharma",
            parentMobile = "9812345679",
            courseClass = "B.Tech",
            college = "Delhi Technological University",
            targetExam = "UPSC CSE 2027",
            category = "General",
            batch = "Morning Regular",
            planId = planMonthly.id,
            planName = planMonthly.name,
            shiftId = shiftMorning.id,
            shiftName = shiftMorning.name,
            seatId = "SEAT-A01",
            seatNumber = "A-01",
            hallName = hall1.name,
            joiningDate = getRelativeDate(-60),
            expiryDate = getRelativeDate(30),
            totalFee = 3000.0,
            paidAmount = 3000.0,
            dueAmount = 0.0,
            status = "ACTIVE",
            rfidQrCode = "QR-STU-1001"
        )
        val student2 = StudentEntity(
            id = "STU-002",
            libraryId = libId,
            studentCode = "STU-1002",
            fullName = "Priya Singh",
            mobile = "9823456781",
            email = "priya.neet@gmail.com",
            dob = "2002-09-18",
            gender = "Female",
            address = "House 45, GTB Nagar, New Delhi",
            parentName = "R. K. Singh",
            parentMobile = "9823456780",
            courseClass = "12th Passed",
            college = "Modern School",
            targetExam = "NEET UG",
            category = "OBC",
            batch = "Full Day",
            planId = planFullDay.id,
            planName = planFullDay.name,
            shiftId = shiftFullDay.id,
            shiftName = shiftFullDay.name,
            seatId = "SEAT-A02",
            seatNumber = "A-02",
            hallName = hall1.name,
            joiningDate = getRelativeDate(-30),
            expiryDate = getRelativeDate(3),
            totalFee = 1600.0,
            paidAmount = 1000.0,
            dueAmount = 600.0,
            status = "ACTIVE",
            rfidQrCode = "QR-STU-1002"
        )
        val student3 = StudentEntity(
            id = "STU-003",
            libraryId = libId,
            studentCode = "STU-1003",
            fullName = "Rohan Verma",
            mobile = "9834567892",
            email = "rohan.banking@gmail.com",
            dob = "1999-11-25",
            gender = "Male",
            address = "Sector 14, Rohini, New Delhi",
            parentName = "Mahesh Verma",
            parentMobile = "9834567890",
            courseClass = "B.Com",
            college = "Delhi University",
            targetExam = "IBPS PO / SBI PO",
            category = "General",
            batch = "Evening Shift",
            planId = planMonthly.id,
            planName = planMonthly.name,
            shiftId = shiftEvening.id,
            shiftName = shiftEvening.name,
            seatId = "SEAT-A04",
            seatNumber = "A-04",
            hallName = hall1.name,
            joiningDate = getRelativeDate(-45),
            expiryDate = getRelativeDate(-5),
            totalFee = 1000.0,
            paidAmount = 1000.0,
            dueAmount = 0.0,
            status = "EXPIRED",
            rfidQrCode = "QR-STU-1003"
        )
        studentDao.insertStudent(student1)
        studentDao.insertStudent(student2)
        studentDao.insertStudent(student3)

        
        userAccountDao.insertUser(
            UserAccountEntity(
                id = "USER-STU-01",
                email = "student@vanguardlibrary.in",
                password = "password123",
                role = "STUDENT",
                libraryId = libId,
                name = "Aditya Sharma",
                phone = "9812345670",
                studentIdRef = student1.id
            )
        )
        userAccountDao.insertUser(
            UserAccountEntity(
                id = "USER-STU-02",
                email = "aditya.sharma@gmail.com",
                password = "password123",
                role = "STUDENT",
                libraryId = libId,
                name = "Aditya Sharma",
                phone = "9812345670",
                studentIdRef = student1.id
            )
        )

        
        val books = listOf(
            PhysicalBookEntity("BOOK-01", libId, "Indian Polity (7th Edition)", "M. Laxmikanth", "978-9356063464", "McGraw Hill", "7th", "UPSC & State PSC", "General Studies II", "Rack A", "Shelf 1", "ACC-0101", 3, 2, 1),
            PhysicalBookEntity("BOOK-02", libId, "Certificate Physical & Human Geography", "G.C. Leong", "978-0195628166", "Oxford Press", "3rd", "Geography", "General Studies I", "Rack A", "Shelf 2", "ACC-0102", 2, 2, 0),
            PhysicalBookEntity("BOOK-03", libId, "Quantitative Aptitude for Competitive Examinations", "Dr. R.S. Aggarwal", "978-9352534029", "S. Chand", "Latest", "Banking & SSC", "Mathematics", "Rack B", "Shelf 1", "ACC-0201", 4, 3, 1),
            PhysicalBookEntity("BOOK-04", libId, "A Brief History of Modern India", "Rajiv Ahir (Spectrum)", "978-8179307779", "Spectrum Books", "2025 Edition", "History", "General Studies I", "Rack A", "Shelf 3", "ACC-0103", 3, 3, 0),
            PhysicalBookEntity("BOOK-05", libId, "Concepts of Physics (Vol 1 & 2)", "H.C. Verma", "978-8177091878", "Bharati Bhawan", "Reprint", "JEE & NEET", "Physics", "Rack C", "Shelf 1", "ACC-0301", 2, 2, 0)
        )
        books.forEach { bookDao.insertBook(it) }

        
        val issue1 = BookIssueEntity(
            id = "ISSUE-01",
            libraryId = libId,
            bookId = "BOOK-01",
            bookTitle = "Indian Polity (7th Edition)",
            studentId = student1.id,
            studentName = student1.fullName,
            studentMobile = student1.mobile,
            issueDate = getRelativeDate(-5),
            dueDate = getRelativeDate(9),
            status = "ISSUED"
        )
        bookIssueDao.insertIssue(issue1)

        
        val digitalDocs = listOf(
            DigitalMaterialEntity("DM-01", libId, "Economic Survey Summary & Mindmaps", "Comprehensive chapter-wise key takeaways, infographics, and policy trends.", "UPSC", "Economics", "UPSC CSE 2027", "PDF", "6.4 MB", "https://example.com/materials/eco-survey.pdf", "ALL_STUDENTS", "All", 142, getRelativeDate(-20)),
            DigitalMaterialEntity("DM-02", libId, "Monthly Current Affairs Dossier", "National, International, Science & Tech, Environment, and Government Schemes.", "Notes", "Current Affairs", "All Competitive Exams", "PDF", "8.1 MB", "https://example.com/materials/current-affairs.pdf", "ALL_STUDENTS", "All", 215, getRelativeDate(-7), isBookmarked = true),
            DigitalMaterialEntity("DM-03", libId, "Banking General Awareness & Static GK Capsules", "High-yield formulas, monetary policies, RBI circulars summary.", "Banking", "Banking Awareness", "SBI / IBPS PO", "PDF", "3.8 MB", "https://example.com/materials/banking-gk.pdf", "ALL_STUDENTS", "All", 98, getRelativeDate(-25)),
            DigitalMaterialEntity("DM-04", libId, "Modern Indian History Timeline & Charter Acts", "Chronological flowcharts and quick revision tables for prelims.", "NCERT", "History", "UPSC / State PSC", "PDF", "4.2 MB", "https://example.com/materials/history-timeline.pdf", "ALL_STUDENTS", "All", 180, getRelativeDate(-15))
        )
        digitalDocs.forEach { materialDao.insertMaterial(it) }

        
        val p1 = PaymentEntity("PAY-01", libId, "VNG-8901", student1.id, student1.fullName, 1500.0, "UPI", getRelativeDate(-60), "MEMBERSHIP_FEE", "UPI/623891024", "Quarterly renewal (Term 1)", "", "3 Months", 0.0, createdAt = System.currentTimeMillis() - 60L * 86400000L)
        val p1b = PaymentEntity("PAY-01B", libId, "VNG-8905", student1.id, student1.fullName, 1500.0, "UPI", getRelativeDate(-20), "MEMBERSHIP_FEE", "UPI/984210344", "Quarterly renewal (Term 2)", "", "3 Months", 0.0, createdAt = System.currentTimeMillis() - 20L * 86400000L)
        val p2 = PaymentEntity("PAY-02", libId, "VNG-8902", student2.id, student2.fullName, 1000.0, "CASH", getRelativeDate(-15), "SEAT_FEE", "", "Advance installment", "", "1 Month", 600.0, createdAt = System.currentTimeMillis() - 15L * 86400000L)
        val p3 = PaymentEntity("PAY-03", libId, "VNG-8903", student3.id, student3.fullName, 1000.0, "UPI", getRelativeDate(-40), "MEMBERSHIP_FEE", "UPI/441209382", "Monthly Standard Plan", "", "1 Month", 0.0, createdAt = System.currentTimeMillis() - 40L * 86400000L)
        paymentDao.insertPayment(p1)
        paymentDao.insertPayment(p1b)
        paymentDao.insertPayment(p2)
        paymentDao.insertPayment(p3)

        
        val e1 = ExpenseEntity("EXP-01", libId, "Electricity", 8400.0, getRelativeDate(-12), "Commercial power bill for AC study halls", "UPI", "PAID")
        val e2 = ExpenseEntity("EXP-02", libId, "Wi-Fi Internet", 1500.0, getRelativeDate(-8), "Airtel 300 Mbps dual-band optical fiber", "UPI", "PAID")
        val e3 = ExpenseEntity("EXP-03", libId, "Water & Tea Service", 1200.0, getRelativeDate(-3), "RO maintenance & mineral water jars", "CASH", "PAID")
        expenseDao.insertExpense(e1)
        expenseDao.insertExpense(e2)
        expenseDao.insertExpense(e3)

        
        val n1 = NoticeEntity(
            id = "NOT-01",
            libraryId = libId,
            title = "Library Timings & Study Hall Holiday Schedule",
            content = "Dear Students, Hall A will remain open 24x7 as usual. Office staff assistance will be available from 9 AM to 5 PM.",
            category = "HOLIDAY",
            priority = "NORMAL",
            date = getRelativeDate(-3)
        )
        val n2 = NoticeEntity(
            id = "NOT-02",
            libraryId = libId,
            title = "Strict Silence Protocol in Silent Hall A",
            content = "Please ensure all phone calls are taken outside in the reception corridor. Earphones must be kept at low volume.",
            category = "RULES",
            priority = "HIGH",
            date = getRelativeDate(-1)
        )
        noticeDao.insertNotice(n1)
        noticeDao.insertNotice(n2)

        
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val seededAttendance = listOf(
            AttendanceEntity("ATT-01", libId, student1.id, student1.fullName, student1.seatNumber, hall1.name, shiftMorning.name, todayStr, "06:45 AM", "", 0, "CHECKED_IN", "QR_SCAN", "Main Gate GPS QR Scanner"),
            AttendanceEntity("ATT-02", libId, student2.id, student2.fullName, student2.seatNumber, hall1.name, shiftFullDay.name, todayStr, "07:15 AM", "", 0, "CHECKED_IN", "QR_SCAN", "Desk Seat QR Verified"),
            AttendanceEntity("ATT-03", libId, student3.id, student3.fullName, student3.seatNumber, hall1.name, shiftEvening.name, todayStr, "02:00 PM", "06:30 PM", 270, "CHECKED_OUT", "MANUAL", "Reception Counter Punch"),

            AttendanceEntity("ATT-04", libId, student1.id, student1.fullName, student1.seatNumber, hall1.name, shiftMorning.name, getRelativeDate(-1), "06:30 AM", "01:15 PM", 405, "CHECKED_OUT", "QR_SCAN", "Normal Exit Scan"),
            AttendanceEntity("ATT-05", libId, student2.id, student2.fullName, student2.seatNumber, hall1.name, shiftFullDay.name, getRelativeDate(-1), "08:00 AM", "07:45 PM", 705, "CHECKED_OUT", "QR_SCAN", "Extended Study Session"),
            AttendanceEntity("ATT-06", libId, student3.id, student3.fullName, student3.seatNumber, hall1.name, shiftEvening.name, getRelativeDate(-1), "02:30 PM", "06:00 PM", 210, "CHECKED_OUT", "MANUAL", "Staff verified"),

            AttendanceEntity("ATT-07", libId, student1.id, student1.fullName, student1.seatNumber, hall1.name, shiftMorning.name, getRelativeDate(-2), "06:40 AM", "01:00 PM", 380, "CHECKED_OUT", "QR_SCAN", "Gate Entrance Scan"),
            AttendanceEntity("ATT-08", libId, student2.id, student2.fullName, student2.seatNumber, hall1.name, shiftFullDay.name, getRelativeDate(-2), "07:30 AM", "08:15 PM", 765, "CHECKED_OUT", "QR_SCAN", "Full Day Completed"),

            AttendanceEntity("ATT-09", libId, student1.id, student1.fullName, student1.seatNumber, hall1.name, shiftMorning.name, getRelativeDate(-3), "07:00 AM", "01:30 PM", 390, "CHECKED_OUT", "QR_SCAN", "Regular Session"),
            AttendanceEntity("ATT-10", libId, student3.id, student3.fullName, student3.seatNumber, hall1.name, shiftEvening.name, getRelativeDate(-3), "02:00 PM", "06:45 PM", 285, "CHECKED_OUT", "QR_SCAN", "Evening Shift"),

            AttendanceEntity("ATT-11", libId, student2.id, student2.fullName, student2.seatNumber, hall1.name, shiftFullDay.name, getRelativeDate(-4), "08:15 AM", "07:30 PM", 675, "CHECKED_OUT", "QR_SCAN", "Full Day Study"),
            AttendanceEntity("ATT-12", libId, student3.id, student3.fullName, student3.seatNumber, hall1.name, shiftEvening.name, getRelativeDate(-4), "02:15 PM", "06:30 PM", 255, "CHECKED_OUT", "MANUAL", "Manual Register"),

            AttendanceEntity("ATT-13", libId, student1.id, student1.fullName, student1.seatNumber, hall1.name, shiftMorning.name, getRelativeDate(-5), "06:45 AM", "01:15 PM", 390, "CHECKED_OUT", "QR_SCAN", "Gate Scan"),
            AttendanceEntity("ATT-14", libId, student2.id, student2.fullName, student2.seatNumber, hall1.name, shiftFullDay.name, getRelativeDate(-5), "08:00 AM", "08:00 PM", 720, "CHECKED_OUT", "QR_SCAN", "Completed"),
            AttendanceEntity("ATT-15", libId, student1.id, student1.fullName, student1.seatNumber, hall1.name, shiftMorning.name, getRelativeDate(-6), "06:30 AM", "01:00 PM", 390, "CHECKED_OUT", "QR_SCAN", "Morning Session")
        )
        seededAttendance.forEach { attendanceDao.insertAttendance(it) }

        
        val saasPlans = listOf(
            SaaSSubscriptionPlanEntity(
                id = "PLAN-STARTER",
                name = "Starter Launch",
                durationMonths = 1,
                price = 699.0,
                maxSeats = 50,
                features = "Up to 50 Seats, QR Attendance, Basic WhatsApp Alerts, 1 AC Hall, Cash/UPI Ledger",
                isActive = true,
                badge = "Entry Level"
            ),
            SaaSSubscriptionPlanEntity(
                id = "PLAN-PRO",
                name = "Growth Pro",
                durationMonths = 3,
                price = 1899.0,
                maxSeats = 150,
                features = "Up to 150 Seats, QR Scanner, Auto WhatsApp Fee Reminders, Multi-Halls, Digital Library, Supabase Sync",
                isActive = true,
                badge = "Most Popular"
            ),
            SaaSSubscriptionPlanEntity(
                id = "PLAN-ENTERPRISE",
                name = "Enterprise Annual",
                durationMonths = 12,
                price = 5999.0,
                maxSeats = 9999,
                features = "Unlimited Seats, Multi-Branch, Custom App Branding, VIP 24x7 Support, Full P&L Analytics, Unlimited Cloud Sync",
                isActive = true,
                badge = "Best Value (Save 40%)"
            ),
            SaaSSubscriptionPlanEntity(
                id = "PLAN-TRIAL",
                name = "15-Day Free Trial",
                durationMonths = 1,
                price = 0.0,
                maxSeats = 50,
                features = "All Premium Features Unlocked, 50 Seats Limit, 15 Days Free Trial, Instant Cloud Setup",
                isActive = true,
                badge = "15 Days Free Trial"
            )
        )
        saasPlans.forEach { saasPlanDao.insertPlan(it) }

        
        val superAdmin = SuperAdminUserEntity(
            id = "SUPER-ADMIN-MASTER",
            email = "superadmin@libdesk.io",
            name = "SaaS Super Administrator",
            mobile = "+91 98765 00000",
            role = "SUPER_ADMIN",
            accessCode = "ADMIN99",
            is2FaEnabled = true,
            isClaimed = false
        )
        superAdminDao.insertSuperAdmin(superAdmin)

        
        val vanguardSub = LibrarySubscriptionEntity(
            id = "SUB-VANGUARD-01",
            libraryId = libId,
            libraryName = "Vanguard Digital Study Library",
            planId = "PLAN-PRO",
            planName = "Growth Pro (3 Months)",
            status = "ACTIVE",
            startDate = getRelativeDate(-30),
            expiryDate = getRelativeDate(60),
            price = 1899.0,
            discount = 100.0,
            maxSeats = 150,
            autoRenew = true,
            notes = "Paid via UPI / HDFC Bank. Premium client."
        )
        librarySubDao.insertSubscription(vanguardSub)

        listOf(student1.id, student2.id, student3.id).forEach { cacheUserBooking(it) }

        logAudit(libId, "System", "SEED_DEMO_DATA", "Library", libId, "Initialized production SaaS template with Vanguard Library")
        libId
    }
}
