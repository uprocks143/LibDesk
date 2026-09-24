package com.example.data.remote.model

import com.example.data.local.entities.LibraryEntity
import com.example.data.local.entities.MembershipPlanEntity
import com.example.data.local.entities.SeatEntity
import com.example.data.local.entities.StudentEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Supabase Data Model for 'libraries' table.
 */
@Serializable
data class Library(
    @SerialName("id")
    val id: String = "",
    @SerialName("name")
    val name: String = "",
    @SerialName("code")
    val code: String = "",
    @SerialName("logo_url")
    val logoUrl: String = "",
    @SerialName("description")
    val description: String = "",
    @SerialName("established_date")
    val establishedDate: String = "",
    @SerialName("reg_number")
    val regNumber: String = "",
    @SerialName("owner_name")
    val ownerName: String = "",
    @SerialName("owner_phone")
    val ownerPhone: String = "",
    @SerialName("owner_email")
    val ownerEmail: String = "",
    @SerialName("owner_whatsapp")
    val ownerWhatsApp: String = "",
    @SerialName("alternate_contact")
    val alternateContact: String = "",
    @SerialName("address")
    val address: String = "",
    @SerialName("landmark")
    val landmark: String = "",
    @SerialName("city")
    val city: String = "",
    @SerialName("district")
    val district: String = "",
    @SerialName("state")
    val state: String = "",
    @SerialName("pincode")
    val pincode: String = "",
    @SerialName("latitude")
    val latitude: Double = 0.0,
    @SerialName("longitude")
    val longitude: Double = 0.0,
    @SerialName("phone")
    val phone: String = "",
    @SerialName("whatsapp")
    val whatsapp: String = "",
    @SerialName("email")
    val email: String = "",
    @SerialName("website")
    val website: String = "",
    @SerialName("upi_id")
    val upiId: String = "",
    @SerialName("upi_payee_name")
    val upiPayeeName: String = "",
    @SerialName("receipt_prefix")
    val receiptPrefix: String = "REC",
    @SerialName("default_fine_per_day")
    val defaultFinePerDay: Double = 5.0,
    @SerialName("borrow_limit")
    val borrowLimit: Int = 2,
    @SerialName("loan_days")
    val loanDays: Int = 14,
    @SerialName("qr_attendance_strict_shift")
    val qrAttendanceStrictShift: Boolean = false,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
) {
    fun toEntity(): LibraryEntity = LibraryEntity(
        id = id,
        name = name,
        code = code,
        logoUrl = logoUrl,
        description = description,
        establishedDate = establishedDate,
        regNumber = regNumber,
        ownerName = ownerName,
        ownerPhone = ownerPhone,
        ownerEmail = ownerEmail,
        ownerWhatsApp = ownerWhatsApp,
        alternateContact = alternateContact,
        address = address,
        landmark = landmark,
        city = city,
        district = district,
        state = state,
        pincode = pincode,
        latitude = latitude,
        longitude = longitude,
        phone = phone,
        whatsapp = whatsapp,
        email = email,
        website = website,
        upiId = upiId,
        upiPayeeName = upiPayeeName,
        receiptPrefix = receiptPrefix,
        defaultFinePerDay = defaultFinePerDay,
        borrowLimit = borrowLimit,
        loanDays = loanDays,
        qrAttendanceStrictShift = qrAttendanceStrictShift
    )
}

fun LibraryEntity.toSupabaseModel(): Library = Library(
    id = id,
    name = name,
    code = code,
    logoUrl = logoUrl,
    description = description,
    establishedDate = establishedDate,
    regNumber = regNumber,
    ownerName = ownerName,
    ownerPhone = ownerPhone,
    ownerEmail = ownerEmail,
    ownerWhatsApp = ownerWhatsApp,
    alternateContact = alternateContact,
    address = address,
    landmark = landmark,
    city = city,
    district = district,
    state = state,
    pincode = pincode,
    latitude = latitude,
    longitude = longitude,
    phone = phone,
    whatsapp = whatsapp,
    email = email,
    website = website,
    upiId = upiId,
    upiPayeeName = upiPayeeName,
    receiptPrefix = receiptPrefix,
    defaultFinePerDay = defaultFinePerDay,
    borrowLimit = borrowLimit,
    loanDays = loanDays,
    qrAttendanceStrictShift = qrAttendanceStrictShift
)

/**
 * Supabase Data Model for 'seats' table.
 */
@Serializable
data class Seat(
    @SerialName("id")
    val id: String = "",
    @SerialName("library_id")
    val libraryId: String = "",
    @SerialName("seat_number")
    val seatNumber: String = "",
    @SerialName("hall_id")
    val hallId: String = "",
    @SerialName("hall_name")
    val hallName: String = "",
    @SerialName("section_id")
    val sectionId: String = "",
    @SerialName("section_name")
    val sectionName: String = "",
    @SerialName("cabin_id")
    val cabinId: String = "",
    @SerialName("cabin_name")
    val cabinName: String = "",
    @SerialName("floor")
    val floor: String = "Ground Floor",
    @SerialName("seat_type")
    val seatType: String = "Standard",
    @SerialName("monthly_fee")
    val monthlyFee: Double = 1000.0,
    @SerialName("status")
    val status: String = "AVAILABLE",
    @SerialName("assigned_student_id")
    val assignedStudentId: String = "",
    @SerialName("assigned_student_name")
    val assignedStudentName: String = "",
    @SerialName("assigned_shift_id")
    val assignedShiftId: String = "",
    @SerialName("assigned_shift_name")
    val assignedShiftName: String = "",
    @SerialName("valid_until")
    val validUntil: String = "",
    @SerialName("grid_row")
    val gridRow: Int = 1,
    @SerialName("grid_col")
    val gridCol: Int = 1,
    @SerialName("floor_zone")
    val floorZone: String = "General Study Zone",
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
) {
    fun toEntity(): SeatEntity = SeatEntity(
        id = id,
        libraryId = libraryId,
        seatNumber = seatNumber,
        hallId = hallId,
        hallName = hallName,
        sectionId = sectionId,
        sectionName = sectionName,
        cabinId = cabinId,
        cabinName = cabinName,
        floor = floor,
        seatType = seatType,
        monthlyFee = monthlyFee,
        status = status,
        assignedStudentId = assignedStudentId,
        assignedStudentName = assignedStudentName,
        assignedShiftId = assignedShiftId,
        assignedShiftName = assignedShiftName,
        validUntil = validUntil,
        gridRow = gridRow,
        gridCol = gridCol,
        floorZone = floorZone
    )
}

fun SeatEntity.toSupabaseModel(): Seat = Seat(
    id = id,
    libraryId = libraryId,
    seatNumber = seatNumber,
    hallId = hallId,
    hallName = hallName,
    sectionId = sectionId,
    sectionName = sectionName,
    cabinId = cabinId,
    cabinName = cabinName,
    floor = floor,
    seatType = seatType,
    monthlyFee = monthlyFee,
    status = status,
    assignedStudentId = assignedStudentId,
    assignedStudentName = assignedStudentName,
    assignedShiftId = assignedShiftId,
    assignedShiftName = assignedShiftName,
    validUntil = validUntil,
    gridRow = gridRow,
    gridCol = gridCol,
    floorZone = floorZone
)

/**
 * Supabase Data Model for 'membership_plans' / 'memberships' table.
 */
@Serializable
data class Membership(
    @SerialName("id")
    val id: String = "",
    @SerialName("library_id")
    val libraryId: String = "",
    @SerialName("name")
    val name: String = "",
    @SerialName("duration_months")
    val durationMonths: Int = 1,
    @SerialName("duration_days")
    val durationDays: Int = 30,
    @SerialName("duration_type")
    val durationType: String = "MONTHS",
    @SerialName("base_fee")
    val baseFee: Double = 1000.0,
    @SerialName("maintenance_fee")
    val maintenanceFee: Double = 100.0,
    @SerialName("security_deposit")
    val securityDeposit: Double = 500.0,
    @SerialName("discount")
    val discount: Double = 0.0,
    @SerialName("seat_type")
    val seatType: String = "Standard",
    @SerialName("shift_id")
    val shiftId: String = "",
    @SerialName("facilities")
    val facilities: String = "High-Speed Wi-Fi, RO Water, Charging Socket, Silent AC",
    @SerialName("renewal_rules")
    val renewalRules: String = "Grace period of 3 days before seat release",
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
) {
    fun toEntity(): MembershipPlanEntity = MembershipPlanEntity(
        id = id,
        libraryId = libraryId,
        name = name,
        durationMonths = durationMonths,
        durationDays = durationDays,
        durationType = durationType,
        baseFee = baseFee,
        maintenanceFee = maintenanceFee,
        securityDeposit = securityDeposit,
        discount = discount,
        seatType = seatType,
        shiftId = shiftId,
        facilities = facilities,
        renewalRules = renewalRules,
        isActive = isActive
    )
}

fun MembershipPlanEntity.toSupabaseModel(): Membership = Membership(
    id = id,
    libraryId = libraryId,
    name = name,
    durationMonths = durationMonths,
    durationDays = durationDays,
    durationType = durationType,
    baseFee = baseFee,
    maintenanceFee = maintenanceFee,
    securityDeposit = securityDeposit,
    discount = discount,
    seatType = seatType,
    shiftId = shiftId,
    facilities = facilities,
    renewalRules = renewalRules,
    isActive = isActive
)

/**
 * Supabase Data Model for 'students' table.
 */
@Serializable
data class Student(
    @SerialName("id")
    val id: String = "",
    @SerialName("library_id")
    val libraryId: String = "",
    @SerialName("student_code")
    val studentCode: String = "",
    @SerialName("full_name")
    val fullName: String = "",
    @SerialName("photo_url")
    val photoUrl: String = "",
    @SerialName("mobile")
    val mobile: String = "",
    @SerialName("email")
    val email: String = "",
    @SerialName("dob")
    val dob: String = "",
    @SerialName("gender")
    val gender: String = "Male",
    @SerialName("address")
    val address: String = "",
    @SerialName("parent_name")
    val parentName: String = "",
    @SerialName("parent_mobile")
    val parentMobile: String = "",
    @SerialName("course_class")
    val courseClass: String = "",
    @SerialName("college")
    val college: String = "",
    @SerialName("target_exam")
    val targetExam: String = "UPSC CSE",
    @SerialName("category")
    val category: String = "General",
    @SerialName("batch")
    val batch: String = "Morning Regular",
    @SerialName("plan_id")
    val planId: String = "",
    @SerialName("plan_name")
    val planName: String = "",
    @SerialName("shift_id")
    val shiftId: String = "",
    @SerialName("shift_name")
    val shiftName: String = "",
    @SerialName("seat_id")
    val seatId: String = "",
    @SerialName("seat_number")
    val seatNumber: String = "",
    @SerialName("hall_name")
    val hallName: String = "",
    @SerialName("joining_date")
    val joiningDate: String = "",
    @SerialName("expiry_date")
    val expiryDate: String = "",
    @SerialName("total_fee")
    val totalFee: Double = 1000.0,
    @SerialName("discount")
    val discount: Double = 0.0,
    @SerialName("paid_amount")
    val paidAmount: Double = 1000.0,
    @SerialName("due_amount")
    val dueAmount: Double = 0.0,
    @SerialName("status")
    val status: String = "ACTIVE",
    @SerialName("rfid_qr_code")
    val rfidQrCode: String = "",
    @SerialName("emergency_contact")
    val emergencyContact: String = "",
    @SerialName("password")
    val password: String = "password123",
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
) {
    fun toEntity(): StudentEntity = StudentEntity(
        id = id,
        libraryId = libraryId,
        studentCode = studentCode,
        fullName = fullName,
        photoUrl = photoUrl,
        mobile = mobile,
        email = email,
        dob = dob,
        gender = gender,
        address = address,
        parentName = parentName,
        parentMobile = parentMobile,
        courseClass = courseClass,
        college = college,
        targetExam = targetExam,
        category = category,
        batch = batch,
        planId = planId,
        planName = planName,
        shiftId = shiftId,
        shiftName = shiftName,
        seatId = seatId,
        seatNumber = seatNumber,
        hallName = hallName,
        joiningDate = joiningDate,
        expiryDate = expiryDate,
        totalFee = totalFee,
        discount = discount,
        paidAmount = paidAmount,
        dueAmount = dueAmount,
        status = status,
        rfidQrCode = rfidQrCode,
        emergencyContact = emergencyContact,
        password = password
    )
}

fun StudentEntity.toSupabaseModel(): Student = Student(
    id = id,
    libraryId = libraryId,
    studentCode = studentCode,
    fullName = fullName,
    photoUrl = photoUrl,
    mobile = mobile,
    email = email,
    dob = dob,
    gender = gender,
    address = address,
    parentName = parentName,
    parentMobile = parentMobile,
    courseClass = courseClass,
    college = college,
    targetExam = targetExam,
    category = category,
    batch = batch,
    planId = planId,
    planName = planName,
    shiftId = shiftId,
    shiftName = shiftName,
    seatId = seatId,
    seatNumber = seatNumber,
    hallName = hallName,
    joiningDate = joiningDate,
    expiryDate = expiryDate,
    totalFee = totalFee,
    discount = discount,
    paidAmount = paidAmount,
    dueAmount = dueAmount,
    status = status,
    rfidQrCode = rfidQrCode,
    emergencyContact = emergencyContact,
    password = password
)
