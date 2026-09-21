package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "libraries")
data class LibraryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val code: String, 
    val logoUrl: String = "",
    val description: String = "",
    val establishedDate: String = "",
    val regNumber: String = "",

    val ownerName: String = "",
    val ownerPhone: String = "",
    val ownerEmail: String = "",
    val ownerWhatsApp: String = "",
    val alternateContact: String = "",

    val address: String = "",
    val landmark: String = "",
    val city: String = "",
    val district: String = "",
    val state: String = "",
    val pincode: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val phone: String = "",
    val whatsapp: String = "",
    val email: String = "",
    val website: String = "",

    val upiId: String = "",
    val upiPayeeName: String = "",
    val receiptPrefix: String = "REC",
    val defaultFinePerDay: Double = 5.0,
    val borrowLimit: Int = 2,
    val loanDays: Int = 14,
    val qrAttendanceStrictShift: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "users")
data class UserAccountEntity(
    @PrimaryKey val id: String,
    val email: String,
    val password: String = "password123",
    val role: String, 
    val libraryId: String,
    val name: String,
    val phone: String = "",
    val avatarUrl: String = "",
    val studentIdRef: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "halls")
data class HallEntity(
    @PrimaryKey val id: String,
    val libraryId: String,
    val name: String,
    val type: String, 
    val floor: String = "Ground Floor",
    val isAc: Boolean = true,
    val description: String = "",
    val seatCount: Int = 0,
    val openingTime: String = "06:00 AM",
    val closingTime: String = "11:00 PM",
    val isActive: Boolean = true
)

@Entity(tableName = "cabins")
data class CabinEntity(
    @PrimaryKey val id: String,
    val libraryId: String,
    val cabinNumber: String,
    val name: String,
    val floor: String = "1st Floor",
    val isAc: Boolean = true,
    val isPrivate: Boolean = true,
    val seatCount: Int = 1,
    val monthlyFee: Double = 2500.0,
    val description: String = "",
    val isActive: Boolean = true
)

@Entity(tableName = "sections")
data class SectionEntity(
    @PrimaryKey val id: String,
    val libraryId: String,
    val name: String, 
    val description: String = "",
    val floor: String = "Ground Floor",
    val hallId: String = "",
    val cabinId: String = "",
    val isActive: Boolean = true
)

@Entity(tableName = "shifts")
data class ShiftEntity(
    @PrimaryKey val id: String,
    val libraryId: String,
    val name: String, 
    val startTime: String,
    val endTime: String,
    val fee: Double = 800.0,
    val description: String = "",
    val isActive: Boolean = true
)

@Entity(tableName = "membership_plans")
data class MembershipPlanEntity(
    @PrimaryKey val id: String,
    val libraryId: String,
    val name: String, 
    val durationMonths: Int = 1,
    val durationDays: Int = 30,
    val durationType: String = "MONTHS", // "MONTHS" or "DAYS"
    val baseFee: Double = 1000.0,
    val maintenanceFee: Double = 100.0,
    val securityDeposit: Double = 500.0,
    val discount: Double = 0.0,
    val seatType: String = "Standard",
    val shiftId: String = "",
    val facilities: String = "High-Speed Wi-Fi, RO Water, Charging Socket, Silent AC",
    val renewalRules: String = "Grace period of 3 days before seat release",
    val isActive: Boolean = true
)

@Entity(tableName = "seats")
data class SeatEntity(
    @PrimaryKey val id: String,
    val libraryId: String,
    val seatNumber: String, 
    val hallId: String = "",
    val hallName: String = "",
    val sectionId: String = "",
    val sectionName: String = "",
    val cabinId: String = "",
    val cabinName: String = "",
    val floor: String = "Ground Floor",
    val seatType: String = "Standard", 
    val monthlyFee: Double = 1000.0,
    val status: String = "AVAILABLE", 
    val assignedStudentId: String = "",
    val assignedStudentName: String = "",
    val assignedShiftId: String = "",
    val assignedShiftName: String = "",
    val validUntil: String = "",
    val gridRow: Int = 1,
    val gridCol: Int = 1,
    val floorZone: String = "General Study Zone"
)

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey val id: String,
    val libraryId: String,
    val studentCode: String, 
    val fullName: String,
    val photoUrl: String = "",
    val mobile: String,
    val email: String = "",
    val dob: String = "",
    val gender: String = "Male",
    val address: String = "",
    val parentName: String = "",
    val parentMobile: String = "",
    val courseClass: String = "",
    val college: String = "",
    val targetExam: String = "UPSC CSE", 
    val category: String = "General",
    val batch: String = "Morning Regular",
    val planId: String = "",
    val planName: String = "",
    val shiftId: String = "",
    val shiftName: String = "",
    val seatId: String = "",
    val seatNumber: String = "",
    val hallName: String = "",
    val joiningDate: String = "",
    val expiryDate: String = "",
    val totalFee: Double = 1000.0,
    val discount: Double = 0.0,
    val paidAmount: Double = 1000.0,
    val dueAmount: Double = 0.0,
    val status: String = "ACTIVE", 
    val rfidQrCode: String = "",
    val emergencyContact: String = "",
    val password: String = "password123",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "seat_assignments")
data class SeatAssignmentEntity(
    @PrimaryKey val id: String,
    val libraryId: String,
    val seatId: String,
    val seatNumber: String,
    val studentId: String,
    val studentName: String,
    val shiftId: String,
    val shiftName: String,
    val startDate: String,
    val endDate: String,
    val planId: String,
    val status: String = "ACTIVE", 
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "attendance")
data class AttendanceEntity(
    @PrimaryKey val id: String,
    val libraryId: String,
    val studentId: String,
    val studentName: String,
    val seatNumber: String = "",
    val hallName: String = "",
    val shiftName: String = "",
    val date: String, 
    val checkInTime: String, 
    val checkOutTime: String = "", 
    val durationMinutes: Int = 0,
    val status: String = "CHECKED_IN", 
    val mode: String = "QR_SCAN", 
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "physical_books")
data class PhysicalBookEntity(
    @PrimaryKey val id: String,
    val libraryId: String,
    val title: String,
    val author: String,
    val isbn: String = "",
    val publisher: String = "",
    val edition: String = "",
    val category: String = "Competitive Exams",
    val subject: String = "General Studies",
    val rack: String = "Rack A",
    val shelf: String = "Shelf 2",
    val accessionNumber: String = "ACC-001",
    val totalCopies: Int = 1,
    val availableCopies: Int = 1,
    val issuedCopies: Int = 0,
    val coverUrl: String = ""
)

@Entity(tableName = "book_issues")
data class BookIssueEntity(
    @PrimaryKey val id: String,
    val libraryId: String,
    val bookId: String,
    val bookTitle: String,
    val studentId: String,
    val studentName: String,
    val studentMobile: String = "",
    val issueDate: String,
    val dueDate: String,
    val returnDate: String = "",
    val fineAmount: Double = 0.0,
    val finePaid: Boolean = false,
    val status: String = "ISSUED", 
    val notes: String = ""
)

@Entity(tableName = "digital_materials")
data class DigitalMaterialEntity(
    @PrimaryKey val id: String,
    val libraryId: String,
    val title: String,
    val description: String = "",
    val category: String = "UPSC", 
    val subject: String = "General Studies",
    val exam: String = "All Exams",
    val fileType: String = "PDF", 
    val fileSize: String = "4.2 MB",
    val fileUrl: String = "",
    val accessPolicy: String = "ALL_STUDENTS", 
    val allowedGroup: String = "All",
    val downloadCount: Int = 0,
    val uploadDate: String = "",
    val isBookmarked: Boolean = false
)

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey val id: String,
    val libraryId: String,
    val receiptNumber: String,
    val studentId: String,
    val studentName: String,
    val amount: Double,
    val paymentMode: String = "UPI", 
    val date: String,
    val purpose: String = "MEMBERSHIP_FEE", 
    val referenceNumber: String = "",
    val notes: String = "",
    val remarks: String = "",
    val period: String = "",
    val dueBalance: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val libraryId: String,
    val category: String, 
    val amount: Double,
    val date: String,
    val description: String = "",
    val paymentMode: String = "UPI",
    val status: String = "PAID", 
    val receiptRef: String = ""
)

@Entity(tableName = "fines")
data class FineEntity(
    @PrimaryKey val id: String,
    val libraryId: String,
    val studentId: String,
    val studentName: String,
    val bookId: String = "",
    val bookTitle: String = "",
    val reason: String = "Late Book Return",
    val amount: Double = 25.0,
    val paid: Boolean = false,
    val date: String = ""
)

@Entity(tableName = "notices")
data class NoticeEntity(
    @PrimaryKey val id: String,
    val libraryId: String,
    val title: String,
    val content: String,
    val category: String = "GENERAL", 
    val priority: String = "NORMAL", 
    val date: String,
    val targetAudience: String = "ALL",
    val senderName: String = "LibDesk Admin",
    val isActive: Boolean = true
)

@Entity(tableName = "feedback_complaints")
data class FeedbackComplaintEntity(
    @PrimaryKey val id: String,
    val libraryId: String,
    val studentId: String,
    val studentName: String,
    val seatNumber: String = "",
    val type: String = "COMPLAINT", 
    val subject: String,
    val message: String,
    val status: String = "PENDING", 
    val reply: String = "",
    val date: String,
    val resolvedDate: String = ""
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey val id: String,
    val libraryId: String,
    val performedBy: String,
    val action: String,
    val recordType: String,
    val recordId: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "saas_plans")
data class SaaSSubscriptionPlanEntity(
    @PrimaryKey val id: String,
    val name: String,
    val durationMonths: Int, 
    val price: Double,
    val maxSeats: Int, 
    val features: String, 
    val isActive: Boolean = true,
    val badge: String = "" 
)

@Entity(tableName = "library_subscriptions")
data class LibrarySubscriptionEntity(
    @PrimaryKey val id: String,
    val libraryId: String,
    val libraryName: String,
    val planId: String,
    val planName: String,
    val status: String = "ACTIVE", 
    val startDate: String,
    val expiryDate: String,
    val durationDays: Int = 30,
    val durationUnit: String = "MONTHS", // "MONTHS" or "DAYS"
    val price: Double,
    val discount: Double = 0.0,
    val maxSeats: Int = 100,
    val autoRenew: Boolean = true,
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "super_admin_users")
data class SuperAdminUserEntity(
    @PrimaryKey val id: String = "SUPER-ADMIN-MASTER",
    val email: String = "",
    val name: String = "",
    val mobile: String = "",
    val role: String = "SUPER_ADMIN",
    val accessCode: String = "",
    val is2FaEnabled: Boolean = true,
    val isClaimed: Boolean = false, 
    val upiId: String = "libdesk.billing@upi",
    val upiPayeeName: String = "LibDesk Subscriptions",
    val createdAt: Long = System.currentTimeMillis()
)

