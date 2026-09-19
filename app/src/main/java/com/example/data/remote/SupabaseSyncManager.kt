package com.example.data.remote

import android.util.Log
import com.example.data.local.database.AppDatabase
import com.example.data.local.entities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject


class SupabaseSyncManager(
    private val database: AppDatabase
) {
    private val TAG = "SupabaseSyncManager"

    private val studentDao = database.studentDao()
    private val seatDao = database.seatDao()
    private val attendanceDao = database.attendanceDao()
    private val paymentDao = database.paymentDao()
    private val libraryDao = database.libraryDao()
    private val expenseDao = database.expenseDao()
    private val noticeDao = database.noticeDao()
    private val feedbackComplaintDao = database.feedbackComplaintDao()
    private val physicalBookDao = database.physicalBookDao()
    private val bookIssueDao = database.bookIssueDao()
    private val digitalMaterialDao = database.digitalMaterialDao()
    private val shiftDao = database.shiftDao()
    private val membershipPlanDao = database.membershipPlanDao()
    private val hallDao = database.hallDao()
    private val cabinDao = database.cabinDao()
    private val sectionDao = database.sectionDao()

    
    suspend fun syncLocalToSupabase(libraryId: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            var syncedCount = 0

            
            val lib = libraryDao.getLibraryById(libraryId).firstOrNull()
            if (lib != null) {
                val libArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("id", lib.id)
                        put("name", lib.name)
                        put("code", lib.code)
                        put("address", lib.address)
                        put("city", lib.city)
                        put("state", lib.state)
                        put("pincode", lib.pincode)
                        put("ownerName", lib.ownerName)
                        put("ownerEmail", lib.ownerEmail)
                        put("ownerPhone", lib.ownerPhone)
                        put("upiId", lib.upiId)
                        put("upiPayeeName", lib.upiPayeeName)
                        put("createdAt", lib.createdAt)
                    })
                }
                SupabaseClient.upsertRecords("libraries", libArray)
            }

            
            val students = studentDao.getStudentsByLibrary(libraryId).firstOrNull() ?: emptyList()
            if (students.isNotEmpty()) {
                val studentArray = JSONArray()
                students.forEach { s ->
                    studentArray.put(JSONObject().apply {
                        put("id", s.id)
                        put("libraryId", s.libraryId)
                        put("fullName", s.fullName)
                        put("studentCode", s.studentCode)
                        put("mobile", s.mobile)
                        put("email", s.email)
                        put("gender", s.gender)
                        put("address", s.address)
                        put("parentName", s.parentName)
                        put("parentMobile", s.parentMobile)
                        put("targetExam", s.targetExam)
                        put("seatNumber", s.seatNumber)
                        put("hallName", s.hallName)
                        put("shiftName", s.shiftName)
                        put("planName", s.planName)
                        put("joiningDate", s.joiningDate)
                        put("expiryDate", s.expiryDate)
                        put("totalFee", s.totalFee)
                        put("discount", s.discount)
                        put("paidAmount", s.paidAmount)
                        put("dueAmount", s.dueAmount)
                        put("status", s.status)
                        put("rfidQrCode", s.rfidQrCode)
                        put("createdAt", s.createdAt)
                    })
                }
                SupabaseClient.upsertRecords("students", studentArray)
                syncedCount += students.size
            }

            
            val seats = seatDao.getSeatsByLibrary(libraryId).firstOrNull() ?: emptyList()
            if (seats.isNotEmpty()) {
                val seatArray = JSONArray()
                seats.forEach { st ->
                    seatArray.put(JSONObject().apply {
                        put("id", st.id)
                        put("libraryId", st.libraryId)
                        put("hallId", st.hallId)
                        put("hallName", st.hallName)
                        put("seatNumber", st.seatNumber)
                        put("floor", st.floor)
                        put("seatType", st.seatType)
                        put("status", st.status)
                        put("monthlyFee", st.monthlyFee)
                        put("assignedStudentId", st.assignedStudentId)
                        put("assignedStudentName", st.assignedStudentName)
                        put("assignedShiftName", st.assignedShiftName)
                        put("gridRow", st.gridRow)
                        put("gridCol", st.gridCol)
                    })
                }
                SupabaseClient.upsertRecords("seats", seatArray)
            }

            
            val attendances = attendanceDao.getAttendanceByLibrary(libraryId).firstOrNull() ?: emptyList()
            if (attendances.isNotEmpty()) {
                val attArray = JSONArray()
                attendances.take(150).forEach { a ->
                    attArray.put(JSONObject().apply {
                        put("id", a.id)
                        put("libraryId", a.libraryId)
                        put("studentId", a.studentId)
                        put("studentName", a.studentName)
                        put("seatNumber", a.seatNumber)
                        put("hallName", a.hallName)
                        put("shiftName", a.shiftName)
                        put("date", a.date)
                        put("checkInTime", a.checkInTime)
                        put("checkOutTime", a.checkOutTime)
                        put("durationMinutes", a.durationMinutes)
                        put("status", a.status)
                        put("mode", a.mode)
                        put("notes", a.notes)
                        put("timestamp", a.timestamp)
                    })
                }
                SupabaseClient.upsertRecords("attendance", attArray)
            }

            
            val payments = paymentDao.getPaymentsByLibrary(libraryId).firstOrNull() ?: emptyList()
            if (payments.isNotEmpty()) {
                val payArray = JSONArray()
                payments.take(100).forEach { p ->
                    payArray.put(JSONObject().apply {
                        put("id", p.id)
                        put("libraryId", p.libraryId)
                        put("receiptNumber", p.receiptNumber)
                        put("studentId", p.studentId)
                        put("studentName", p.studentName)
                        put("amount", p.amount)
                        put("paymentMode", p.paymentMode)
                        put("date", p.date)
                        put("purpose", p.purpose)
                        put("referenceNumber", p.referenceNumber)
                        put("notes", p.notes)
                        put("dueBalance", p.dueBalance)
                        put("createdAt", p.createdAt)
                    })
                }
                SupabaseClient.upsertRecords("payments", payArray)
            }

            
            val expenses = expenseDao.getExpensesByLibrary(libraryId).firstOrNull() ?: emptyList()
            if (expenses.isNotEmpty()) {
                val expArray = JSONArray()
                expenses.take(100).forEach { e ->
                    expArray.put(JSONObject().apply {
                        put("id", e.id)
                        put("libraryId", e.libraryId)
                        put("category", e.category)
                        put("amount", e.amount)
                        put("date", e.date)
                        put("description", e.description)
                        put("paymentMode", e.paymentMode)
                        put("status", e.status)
                        put("receiptRef", e.receiptRef)
                    })
                }
                SupabaseClient.upsertRecords("expenses", expArray)
            }

            
            val notices = noticeDao.getAllNoticesByLibrary(libraryId).firstOrNull() ?: emptyList()
            if (notices.isNotEmpty()) {
                val notArray = JSONArray()
                notices.forEach { n ->
                    notArray.put(JSONObject().apply {
                        put("id", n.id)
                        put("libraryId", n.libraryId)
                        put("title", n.title)
                        put("content", n.content)
                        put("category", n.category)
                        put("priority", n.priority)
                        put("date", n.date)
                        put("targetAudience", n.targetAudience)
                        put("isActive", n.isActive)
                    })
                }
                SupabaseClient.upsertRecords("notices", notArray)
            }

            
            val complaints = feedbackComplaintDao.getFeedbackByLibrary(libraryId).firstOrNull() ?: emptyList()
            if (complaints.isNotEmpty()) {
                val compArray = JSONArray()
                complaints.forEach { c ->
                    compArray.put(JSONObject().apply {
                        put("id", c.id)
                        put("libraryId", c.libraryId)
                        put("studentId", c.studentId)
                        put("studentName", c.studentName)
                        put("seatNumber", c.seatNumber)
                        put("type", c.type)
                        put("subject", c.subject)
                        put("message", c.message)
                        put("status", c.status)
                        put("reply", c.reply)
                        put("date", c.date)
                        put("resolvedDate", c.resolvedDate)
                    })
                }
                SupabaseClient.upsertRecords("feedback_complaints", compArray)
            }

            
            val books = physicalBookDao.getBooksByLibrary(libraryId).firstOrNull() ?: emptyList()
            if (books.isNotEmpty()) {
                val bookArr = JSONArray()
                books.forEach { b ->
                    bookArr.put(JSONObject().apply {
                        put("id", b.id)
                        put("libraryId", b.libraryId)
                        put("title", b.title)
                        put("author", b.author)
                        put("isbn", b.isbn)
                        put("category", b.category)
                        put("subject", b.subject)
                        put("rack", b.rack)
                        put("shelf", b.shelf)
                        put("accessionNumber", b.accessionNumber)
                        put("totalCopies", b.totalCopies)
                        put("availableCopies", b.availableCopies)
                        put("issuedCopies", b.issuedCopies)
                    })
                }
                SupabaseClient.upsertRecords("physical_books", bookArr)
            }

            
            val materials = digitalMaterialDao.getMaterialsByLibrary(libraryId).firstOrNull() ?: emptyList()
            if (materials.isNotEmpty()) {
                val matArr = JSONArray()
                materials.forEach { m ->
                    matArr.put(JSONObject().apply {
                        put("id", m.id)
                        put("libraryId", m.libraryId)
                        put("title", m.title)
                        put("description", m.description)
                        put("category", m.category)
                        put("subject", m.subject)
                        put("exam", m.exam)
                        put("fileType", m.fileType)
                        put("fileSize", m.fileSize)
                        put("fileUrl", m.fileUrl)
                        put("accessPolicy", m.accessPolicy)
                    })
                }
                SupabaseClient.upsertRecords("digital_materials", matArr)
            }

            Pair(true, "Cloud Sync Successful: ${students.size} students, ${seats.size} seats, ${attendances.size} logs, and ${payments.size} payments synced with Supabase!")
        } catch (e: Exception) {
            Log.e(TAG, "Error in local to cloud sync", e)
            Pair(false, "Sync failed: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    
    suspend fun pullSupabaseToLocal(libraryId: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            var totalPulled = 0

            
            val (sSuccess, sArray) = SupabaseClient.fetchRecords("students", libraryId)
            if (sSuccess && sArray != null) {
                for (i in 0 until sArray.length()) {
                    val obj = sArray.getJSONObject(i)
                    val student = StudentEntity(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        libraryId = obj.optString("libraryId", libraryId),
                        studentCode = obj.optString("studentCode", "STU-NEW"),
                        fullName = obj.optString("fullName", "Unknown"),
                        mobile = obj.optString("mobile", ""),
                        email = obj.optString("email", ""),
                        gender = obj.optString("gender", "Other"),
                        address = obj.optString("address", ""),
                        parentName = obj.optString("parentName", ""),
                        parentMobile = obj.optString("parentMobile", ""),
                        targetExam = obj.optString("targetExam", "General"),
                        seatNumber = obj.optString("seatNumber", ""),
                        hallName = obj.optString("hallName", ""),
                        shiftName = obj.optString("shiftName", "Full Day"),
                        planName = obj.optString("planName", "Monthly"),
                        joiningDate = obj.optString("joiningDate", ""),
                        expiryDate = obj.optString("expiryDate", ""),
                        totalFee = obj.optDouble("totalFee", 1000.0),
                        discount = obj.optDouble("discount", 0.0),
                        paidAmount = obj.optDouble("paidAmount", 0.0),
                        dueAmount = obj.optDouble("dueAmount", 0.0),
                        status = obj.optString("status", "ACTIVE"),
                        rfidQrCode = obj.optString("rfidQrCode", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                    studentDao.insertStudent(student)
                    totalPulled++
                }
            }

            
            val (seatSuccess, seatArray) = SupabaseClient.fetchRecords("seats", libraryId)
            if (seatSuccess && seatArray != null) {
                for (i in 0 until seatArray.length()) {
                    val obj = seatArray.getJSONObject(i)
                    val seat = SeatEntity(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        libraryId = obj.optString("libraryId", libraryId),
                        seatNumber = obj.optString("seatNumber", "A-01"),
                        hallId = obj.optString("hallId", ""),
                        hallName = obj.optString("hallName", ""),
                        floor = obj.optString("floor", "Ground Floor"),
                        seatType = obj.optString("seatType", "Standard"),
                        monthlyFee = obj.optDouble("monthlyFee", 1000.0),
                        status = obj.optString("status", "AVAILABLE"),
                        assignedStudentId = obj.optString("assignedStudentId", ""),
                        assignedStudentName = obj.optString("assignedStudentName", ""),
                        assignedShiftName = obj.optString("assignedShiftName", ""),
                        gridRow = obj.optInt("gridRow", 1),
                        gridCol = obj.optInt("gridCol", 1)
                    )
                    seatDao.insertSeat(seat)
                }
            }

            
            val (notSuccess, notArray) = SupabaseClient.fetchRecords("notices", libraryId)
            if (notSuccess && notArray != null) {
                for (i in 0 until notArray.length()) {
                    val obj = notArray.getJSONObject(i)
                    val notice = NoticeEntity(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        libraryId = obj.optString("libraryId", libraryId),
                        title = obj.optString("title", "Notice"),
                        content = obj.optString("content", ""),
                        category = obj.optString("category", "GENERAL"),
                        priority = obj.optString("priority", "NORMAL"),
                        date = obj.optString("date", ""),
                        targetAudience = obj.optString("targetAudience", "ALL"),
                        isActive = obj.optBoolean("isActive", true)
                    )
                    noticeDao.insertNotice(notice)
                }
            }

            Pair(true, "Successfully pulled and updated local database from Supabase Cloud ($totalPulled students synchronized)!")
        } catch (e: Exception) {
            Log.e(TAG, "Error pulling Supabase data", e)
            Pair(false, "Cloud pull error: ${e.localizedMessage}")
        }
    }

    
    suspend fun pushStudent(student: StudentEntity) = withContext(Dispatchers.IO) {
        val arr = JSONArray().apply {
            put(JSONObject().apply {
                put("id", student.id)
                put("libraryId", student.libraryId)
                put("fullName", student.fullName)
                put("studentCode", student.studentCode)
                put("mobile", student.mobile)
                put("email", student.email)
                put("gender", student.gender)
                put("address", student.address)
                put("parentName", student.parentName)
                put("parentMobile", student.parentMobile)
                put("targetExam", student.targetExam)
                put("seatNumber", student.seatNumber)
                put("hallName", student.hallName)
                put("shiftName", student.shiftName)
                put("planName", student.planName)
                put("joiningDate", student.joiningDate)
                put("expiryDate", student.expiryDate)
                put("totalFee", student.totalFee)
                put("discount", student.discount)
                put("paidAmount", student.paidAmount)
                put("dueAmount", student.dueAmount)
                put("status", student.status)
                put("rfidQrCode", student.rfidQrCode)
                put("createdAt", student.createdAt)
            })
        }
        SupabaseClient.upsertRecords("students", arr)
    }

    suspend fun pushAttendance(att: AttendanceEntity) = withContext(Dispatchers.IO) {
        val arr = JSONArray().apply {
            put(JSONObject().apply {
                put("id", att.id)
                put("libraryId", att.libraryId)
                put("studentId", att.studentId)
                put("studentName", att.studentName)
                put("seatNumber", att.seatNumber)
                put("hallName", att.hallName)
                put("shiftName", att.shiftName)
                put("date", att.date)
                put("checkInTime", att.checkInTime)
                put("checkOutTime", att.checkOutTime)
                put("durationMinutes", att.durationMinutes)
                put("status", att.status)
                put("mode", att.mode)
                put("notes", att.notes)
                put("timestamp", att.timestamp)
            })
        }
        SupabaseClient.upsertRecords("attendance", arr)
    }

    suspend fun pushPayment(p: PaymentEntity) = withContext(Dispatchers.IO) {
        val arr = JSONArray().apply {
            put(JSONObject().apply {
                put("id", p.id)
                put("libraryId", p.libraryId)
                put("receiptNumber", p.receiptNumber)
                put("studentId", p.studentId)
                put("studentName", p.studentName)
                put("amount", p.amount)
                put("paymentMode", p.paymentMode)
                put("date", p.date)
                put("purpose", p.purpose)
                put("referenceNumber", p.referenceNumber)
                put("notes", p.notes)
                put("remarks", p.remarks)
                put("period", p.period)
                put("dueBalance", p.dueBalance)
                put("createdAt", p.createdAt)
            })
        }
        SupabaseClient.upsertRecords("payments", arr)
    }

    suspend fun pushNotice(n: NoticeEntity) = withContext(Dispatchers.IO) {
        val arr = JSONArray().apply {
            put(JSONObject().apply {
                put("id", n.id)
                put("libraryId", n.libraryId)
                put("title", n.title)
                put("content", n.content)
                put("category", n.category)
                put("priority", n.priority)
                put("date", n.date)
                put("targetAudience", n.targetAudience)
                put("isActive", n.isActive)
            })
        }
        SupabaseClient.upsertRecords("notices", arr)
    }

    suspend fun pushFeedbackComplaint(c: FeedbackComplaintEntity) = withContext(Dispatchers.IO) {
        val arr = JSONArray().apply {
            put(JSONObject().apply {
                put("id", c.id)
                put("libraryId", c.libraryId)
                put("studentId", c.studentId)
                put("studentName", c.studentName)
                put("seatNumber", c.seatNumber)
                put("type", c.type)
                put("subject", c.subject)
                put("message", c.message)
                put("status", c.status)
                put("reply", c.reply)
                put("date", c.date)
                put("resolvedDate", c.resolvedDate)
            })
        }
        SupabaseClient.upsertRecords("feedback_complaints", arr)
    }
}
