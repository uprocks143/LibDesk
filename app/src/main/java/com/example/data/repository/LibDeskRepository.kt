package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.entities.*
import com.example.data.remote.AuthGuardService
import com.example.data.remote.SupabaseClient
import com.example.viewmodel.LiveSubscriptionCheck
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class LibDeskRepository(val context: Context? = null) {
    private val TAG = "LibDeskRepository"
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    // In-memory reactive state stores (no local SQLite database)
    private val _libraries = MutableStateFlow<List<LibraryEntity>>(emptyList())
    private val _users = MutableStateFlow<List<UserAccountEntity>>(emptyList())
    private val _halls = MutableStateFlow<List<HallEntity>>(emptyList())
    private val _cabins = MutableStateFlow<List<CabinEntity>>(emptyList())
    private val _sections = MutableStateFlow<List<SectionEntity>>(emptyList())
    private val _shifts = MutableStateFlow<List<ShiftEntity>>(emptyList())
    private val _plans = MutableStateFlow<List<MembershipPlanEntity>>(emptyList())
    private val _seats = MutableStateFlow<List<SeatEntity>>(emptyList())
    private val _students = MutableStateFlow<List<StudentEntity>>(emptyList())
    private val _seatAssignments = MutableStateFlow<List<SeatAssignmentEntity>>(emptyList())
    private val _attendance = MutableStateFlow<List<AttendanceEntity>>(emptyList())
    private val _books = MutableStateFlow<List<PhysicalBookEntity>>(emptyList())
    private val _bookIssues = MutableStateFlow<List<BookIssueEntity>>(emptyList())
    private val _materials = MutableStateFlow<List<DigitalMaterialEntity>>(emptyList())
    private val _payments = MutableStateFlow<List<PaymentEntity>>(emptyList())
    private val _expenses = MutableStateFlow<List<ExpenseEntity>>(emptyList())
    private val _fines = MutableStateFlow<List<FineEntity>>(emptyList())
    private val _notices = MutableStateFlow<List<NoticeEntity>>(emptyList())
    private val _feedback = MutableStateFlow<List<FeedbackComplaintEntity>>(emptyList())
    private val _auditLogs = MutableStateFlow<List<AuditLogEntity>>(emptyList())
    private val _saasPlans = MutableStateFlow<List<SaaSSubscriptionPlanEntity>>(emptyList())
    private val _librarySubscriptions = MutableStateFlow<List<LibrarySubscriptionEntity>>(emptyList())
    private val _superAdmin = MutableStateFlow<SuperAdminUserEntity?>(null)
    private val _subscriptionPlans = MutableStateFlow<List<SubscriptionPlans>>(emptyList())
    private val _userSubscriptions = MutableStateFlow<List<UserSubscription>>(emptyList())

    init {
        // Seed default subscription plans into memory
        _subscriptionPlans.value = defaultSubscriptionPlansList()
    }

    // ==========================================
    // SAAS PLANS & SUBSCRIPTIONS
    // ==========================================

    fun getAllSaasPlans(): Flow<List<SaaSSubscriptionPlanEntity>> = _saasPlans.asStateFlow()
    
    suspend fun saveSaasPlan(plan: SaaSSubscriptionPlanEntity) = withContext(Dispatchers.IO) {
        _saasPlans.value = _saasPlans.value.filter { it.id != plan.id } + plan
        val arr = JSONArray().apply {
            put(JSONObject().apply {
                put("id", plan.id)
                put("name", plan.name)
                put("price", plan.price)
                put("durationMonths", plan.durationMonths)
                put("maxSeats", plan.maxSeats)
                put("features", plan.features)
                put("isActive", plan.isActive)
            })
        }
        SupabaseClient.upsertRecords("saas_plans", arr)
    }

    suspend fun deleteSaasPlan(plan: SaaSSubscriptionPlanEntity) = withContext(Dispatchers.IO) {
        _saasPlans.value = _saasPlans.value.filter { it.id != plan.id }
        SupabaseClient.deleteRecord("saas_plans", plan.id)
    }

    fun getActiveSubscriptionPlans(): Flow<List<SubscriptionPlans>> =
        _subscriptionPlans.map { list -> list.filter { it.isActive }.sortedBy { it.displayOrder } }

    fun getAllSubscriptionPlans(): Flow<List<SubscriptionPlans>> =
        _subscriptionPlans.map { list -> list.sortedBy { it.displayOrder } }

    suspend fun getSubscriptionPlanById(id: String): SubscriptionPlans? = withContext(Dispatchers.IO) {
        _subscriptionPlans.value.find { it.id == id }
    }

    suspend fun saveSubscriptionPlan(plan: SubscriptionPlans) = withContext(Dispatchers.IO) {
        _subscriptionPlans.value = _subscriptionPlans.value.filter { it.id != plan.id } + plan
        val arr = JSONArray().apply {
            put(JSONObject().apply {
                put("id", plan.id)
                put("name", plan.name)
                put("description", plan.description)
                put("price", plan.price)
                put("durationMonths", plan.durationMonths)
                put("durationDays", plan.durationDays)
                put("durationType", plan.durationType)
                put("maxSeats", plan.maxSeats)
                put("features", plan.features)
                put("badge", plan.badge)
                put("discountPercentage", plan.discountPercentage)
                put("upiId", plan.upiId)
                put("upiPayeeName", plan.upiPayeeName)
                put("supportWhatsApp", plan.supportWhatsApp)
                put("isActive", plan.isActive)
                put("displayOrder", plan.displayOrder)
                put("createdAt", plan.createdAt)
            })
        }
        SupabaseClient.upsertRecords("subscription_plans", arr)
    }

    suspend fun deleteSubscriptionPlan(plan: SubscriptionPlans) = withContext(Dispatchers.IO) {
        _subscriptionPlans.value = _subscriptionPlans.value.filter { it.id != plan.id }
        SupabaseClient.deleteRecord("subscription_plans", plan.id)
    }

    suspend fun updateAllPlansUpi(upiId: String, payeeName: String, supportWhatsApp: String = "") = withContext(Dispatchers.IO) {
        _subscriptionPlans.value = _subscriptionPlans.value.map {
            it.copy(
                upiId = upiId,
                upiPayeeName = payeeName,
                supportWhatsApp = if (supportWhatsApp.isNotBlank()) supportWhatsApp else it.supportWhatsApp
            )
        }
        try {
            val arr = JSONArray()
            for (plan in _subscriptionPlans.value) {
                arr.put(JSONObject().apply {
                    put("id", plan.id)
                    put("name", plan.name)
                    put("description", plan.description)
                    put("price", plan.price)
                    put("durationMonths", plan.durationMonths)
                    put("durationDays", plan.durationDays)
                    put("durationType", plan.durationType)
                    put("maxSeats", plan.maxSeats)
                    put("features", plan.features)
                    put("badge", plan.badge)
                    put("discountPercentage", plan.discountPercentage)
                    put("upiId", plan.upiId)
                    put("upi_id", plan.upiId)
                    put("upiPayeeName", plan.upiPayeeName)
                    put("upi_payee_name", plan.upiPayeeName)
                    put("supportWhatsApp", plan.supportWhatsApp)
                    put("support_whatsapp", plan.supportWhatsApp)
                    put("isActive", plan.isActive)
                    put("is_active", plan.isActive)
                    put("displayOrder", plan.displayOrder)
                    put("createdAt", plan.createdAt)
                })
            }
            SupabaseClient.upsertRecords("subscription_plans", arr)
        } catch (e: Exception) {
            Log.w(TAG, "updateAllPlansUpi Supabase sync: ${e.message}")
        }
    }

    fun getUserSubscription(libraryId: String): Flow<UserSubscription?> =
        _userSubscriptions.map { list -> list.find { it.libraryId == libraryId } }

    suspend fun getUserSubscriptionDirect(libraryId: String): UserSubscription? = withContext(Dispatchers.IO) {
        _userSubscriptions.value.find { it.libraryId == libraryId }
            ?: run {
                val (ok, arr) = SupabaseClient.queryTable("user_subscriptions?libraryId=eq.$libraryId&select=*")
                if (ok && arr != null && arr.length() > 0) {
                    parseUserSubscription(arr.getJSONObject(0))
                } else null
            }
    }

    suspend fun saveUserSubscription(sub: UserSubscription) = withContext(Dispatchers.IO) {
        _userSubscriptions.value = _userSubscriptions.value.filter { it.id != sub.id } + sub
        val arr = JSONArray().apply {
            put(JSONObject().apply {
                put("id", sub.id)
                put("libraryId", sub.libraryId)
                put("userId", sub.userId)
                put("ownerName", sub.ownerName)
                put("ownerMobile", sub.ownerMobile)
                put("ownerEmail", sub.ownerEmail)
                put("libraryName", sub.libraryName)
                put("planId", sub.planId)
                put("planName", sub.planName)
                put("amountPaid", sub.amountPaid)
                put("billingCycle", sub.billingCycle)
                put("status", sub.status)
                put("startDate", sub.startDate)
                put("expiryDate", sub.expiryDate)
                put("paymentMethod", sub.paymentMethod)
                put("paymentReferenceId", sub.paymentReferenceId)
                put("receiptImageUrl", sub.receiptImageUrl)
                put("isVerifiedByAdmin", sub.isVerifiedByAdmin)
                put("notes", sub.notes)
                put("createdAt", sub.createdAt)
                put("updatedAt", sub.updatedAt)
            })
        }
        SupabaseClient.upsertRecords("user_subscriptions", arr)

        saveLibrarySubscription(
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
        if (_subscriptionPlans.value.isEmpty()) {
            _subscriptionPlans.value = defaultSubscriptionPlansList()
        }
    }

    private fun defaultSubscriptionPlansList(): List<SubscriptionPlans> {
        return listOf(
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
                supportWhatsApp = "",
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
                supportWhatsApp = "",
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
                supportWhatsApp = "",
                isActive = true,
                displayOrder = 3
            )
        )
    }

    fun getAllLibrarySubscriptions(): Flow<List<LibrarySubscriptionEntity>> = _librarySubscriptions.asStateFlow()

    fun getSubscriptionForLibrary(libraryId: String): Flow<LibrarySubscriptionEntity?> =
        _librarySubscriptions.map { list -> list.find { it.libraryId == libraryId } }

    suspend fun getSubscriptionDirect(libraryId: String): LibrarySubscriptionEntity? = withContext(Dispatchers.IO) {
        _librarySubscriptions.value.find { it.libraryId == libraryId }
            ?: run {
                val (ok, arr) = SupabaseClient.queryTable("library_subscriptions?libraryId=eq.$libraryId&select=*")
                if (ok && arr != null && arr.length() > 0) {
                    parseLibrarySubscription(arr.getJSONObject(0))
                } else null
            }
    }

    suspend fun saveLibrarySubscription(sub: LibrarySubscriptionEntity) = withContext(Dispatchers.IO) {
        _librarySubscriptions.value = _librarySubscriptions.value.filter { it.id != sub.id } + sub
        try {
            val isSubActive = sub.status.equals("ACTIVE", ignoreCase = true) || sub.status.equals("TRIAL", ignoreCase = true)
            val subArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("id", sub.id)
                    put("libraryId", sub.libraryId)
                    put("libraryName", sub.libraryName)
                    put("planId", sub.planId)
                    put("planName", sub.planName)
                    put("status", sub.status)
                    put("startDate", sub.startDate)
                    put("expiryDate", sub.expiryDate)
                    put("price", sub.price)
                    put("discount", sub.discount)
                    put("maxSeats", sub.maxSeats)
                    put("autoRenew", sub.autoRenew)
                    put("notes", sub.notes)
                    put("subscriptionActive", isSubActive)
                    put("subscription_active", isSubActive)
                })
            }
            SupabaseClient.upsertRecords("library_subscriptions", subArray)
        } catch (e: Exception) {
            Log.w(TAG, "Supabase saveLibrarySubscription warning: ${e.message}")
        }
    }

    suspend fun checkLibraryTrialEligibility(
        email: String,
        phone: String,
        excludeLibraryId: String? = null
    ): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        val digitsOnly = phone.filter { it.isDigit() }
        val last10Digits = if (digitsOnly.length >= 10) digitsOnly.takeLast(10) else digitsOnly

        // Check directly on Supabase database
        try {
            if (cleanEmail.isNotBlank()) {
                val (emailOk, emailArr) = SupabaseClient.queryTable(
                    "libraries?or=(ownerEmail.ilike.$cleanEmail,email.ilike.$cleanEmail)&select=id,name,ownerEmail,email,ownerPhone,phone"
                )
                if (emailOk && emailArr != null && emailArr.length() > 0) {
                    for (i in 0 until emailArr.length()) {
                        val obj = emailArr.getJSONObject(i)
                        val id = obj.optString("id")
                        if (excludeLibraryId != null && id == excludeLibraryId) continue
                        val libName = obj.optString("name", "Library")
                        return@withContext Pair(
                            false,
                            "इस Email ($cleanEmail) पर पहले से Library '$libName' रजिस्टर्ड है। 15-Day Free Trial केवल एक बार ही मिलता है। कृपया अपने पुराने अकाउंट में लॉगिन करें।"
                        )
                    }
                }
            }

            if (last10Digits.length >= 7) {
                val (phoneOk, phoneArr) = SupabaseClient.queryTable(
                    "libraries?or=(ownerPhone.ilike.*$last10Digits*,phone.ilike.*$last10Digits*)&select=id,name,ownerEmail,email,ownerPhone,phone"
                )
                if (phoneOk && phoneArr != null && phoneArr.length() > 0) {
                    for (i in 0 until phoneArr.length()) {
                        val obj = phoneArr.getJSONObject(i)
                        val id = obj.optString("id")
                        if (excludeLibraryId != null && id == excludeLibraryId) continue
                        val libName = obj.optString("name", "Library")
                        return@withContext Pair(
                            false,
                            "इस Mobile Number ($phone) पर पहले से Library '$libName' रजिस्टर्ड है। 15-Day Free Trial केवल एक बार ही मिलता है। कृपया अपने पुराने अकाउंट में लॉगिन करें।"
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Supabase checkLibraryTrialEligibility warning: ${e.message}")
        }

        Pair(true, null)
    }

    suspend fun deleteLibrarySubscription(sub: LibrarySubscriptionEntity) = withContext(Dispatchers.IO) {
        saveLibrarySubscription(sub.copy(status = "ARCHIVED", notes = "Archived Subscription"))
    }

    suspend fun archiveLibrary(libraryId: String, reason: String = "Deactivated") = withContext(Dispatchers.IO) {
        val existingSub = getSubscriptionDirect(libraryId)
        if (existingSub != null) {
            saveLibrarySubscription(
                existingSub.copy(
                    status = "ARCHIVED",
                    notes = "Archived ($reason). Details preserved for future outreach & campaigns.",
                    updatedAt = System.currentTimeMillis()
                )
            )
        } else {
            val lib = _libraries.value.find { it.id == libraryId }
            if (lib != null) {
                saveLibrarySubscription(
                    LibrarySubscriptionEntity(
                        id = "SUB-ARCHIVED-${lib.id}",
                        libraryId = lib.id,
                        libraryName = lib.name,
                        planId = "PLAN-ARCHIVED",
                        planName = "Archived Library",
                        status = "ARCHIVED",
                        startDate = dateFormat.format(Date()),
                        expiryDate = "2000-01-01",
                        price = 0.0,
                        notes = "Archived: $reason. Retained for future records and advertisement."
                    )
                )
            }
        }
        logAudit(libraryId, "SuperAdmin", "ARCHIVE_LIBRARY", "Library", libraryId, "Archived library details retained.")
    }

    suspend fun reactivateLibrary(libraryId: String) = withContext(Dispatchers.IO) {
        val existingSub = getSubscriptionDirect(libraryId)
        if (existingSub != null) {
            saveLibrarySubscription(
                existingSub.copy(
                    status = "ACTIVE",
                    notes = "Reactivated Library",
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        logAudit(libraryId, "SuperAdmin", "REACTIVATE_LIBRARY", "Library", libraryId, "Reactivated library to active status.")
    }

    fun getSuperAdmin(): Flow<SuperAdminUserEntity?> = _superAdmin.asStateFlow()

    suspend fun getSuperAdminByEmail(email: String): SuperAdminUserEntity? = withContext(Dispatchers.IO) {
        if (_superAdmin.value?.email.equals(email, ignoreCase = true)) {
            _superAdmin.value
        } else {
            val (ok, arr) = SupabaseClient.queryTable("super_admin_users?email=ilike.$email&select=*")
            if (ok && arr != null && arr.length() > 0) {
                val obj = arr.getJSONObject(0)
                val admin = SuperAdminUserEntity(
                    id = obj.optString("id", "SUPER-ADMIN-MASTER"),
                    name = obj.optString("name", "Super Administrator"),
                    email = obj.optString("email", email),
                    mobile = obj.optString("mobile", obj.optString("phone", "")),
                    accessCode = obj.optString("accessCode", ""),
                    upiId = obj.optString("upiId", "libdesk.billing@upi"),
                    upiPayeeName = obj.optString("upiPayeeName", "LibDesk Cloud Subscriptions")
                )
                _superAdmin.value = admin
                admin
            } else null
        }
    }

    suspend fun saveSuperAdmin(admin: SuperAdminUserEntity) = withContext(Dispatchers.IO) {
        _superAdmin.value = admin
        val arr = JSONArray().apply {
            put(JSONObject().apply {
                put("id", admin.id)
                put("name", admin.name)
                put("email", admin.email)
                put("mobile", admin.mobile)
                put("phone", admin.mobile)
                put("supportWhatsApp", admin.mobile)
                put("support_whatsapp", admin.mobile)
                put("accessCode", admin.accessCode)
                put("access_code", admin.accessCode)
                put("upiId", admin.upiId)
                put("upi_id", admin.upiId)
                put("upiPayeeName", admin.upiPayeeName)
                put("upi_payee_name", admin.upiPayeeName)
                put("is2FaEnabled", admin.is2FaEnabled)
                put("is_2fa_enabled", admin.is2FaEnabled)
                put("isClaimed", admin.isClaimed)
                put("is_claimed", admin.isClaimed)
                put("updatedAt", System.currentTimeMillis())
                put("updated_at", System.currentTimeMillis())
            })
        }
        SupabaseClient.upsertRecords("super_admin_users", arr)

        // Automatically synchronize the updated contact number and UPI details across all plans
        updateAllPlansUpi(admin.upiId, admin.upiPayeeName, admin.mobile)

        // Cascade update to Super Admin user account if present in users table
        val superUser = _users.value.find { it.role.equals("SUPER_ADMIN", ignoreCase = true) || it.email.equals(admin.email, ignoreCase = true) }
        if (superUser != null) {
            saveUser(superUser.copy(name = admin.name, phone = admin.mobile, email = admin.email))
        }
    }

    // ==========================================
    // LIBRARY OPERATIONS
    // ==========================================

    fun getLibraryById(id: String): Flow<LibraryEntity?> =
        _libraries.map { list -> list.find { it.id == id } }

    fun getAllLibraries(): Flow<List<LibraryEntity>> = _libraries.asStateFlow()

    suspend fun getLibraryByCode(code: String): LibraryEntity? = withContext(Dispatchers.IO) {
        _libraries.value.find { it.code.equals(code, ignoreCase = true) }
            ?: run {
                val (ok, arr) = SupabaseClient.queryTable("libraries?code=ilike.$code&select=*")
                if (ok && arr != null && arr.length() > 0) {
                    val lib = parseLibrary(arr.getJSONObject(0))
                    _libraries.value = _libraries.value.filter { it.id != lib.id } + lib
                    lib
                } else null
            }
    }

    suspend fun saveLibrary(library: LibraryEntity) = withContext(Dispatchers.IO) {
        _libraries.value = _libraries.value.filter { it.id != library.id } + library

        // Cascade updated library name and owner details to active subscription state
        _librarySubscriptions.value = _librarySubscriptions.value.map {
            if (it.libraryId == library.id) it.copy(libraryName = library.name, updatedAt = System.currentTimeMillis()) else it
        }
        _userSubscriptions.value = _userSubscriptions.value.map {
            if (it.libraryId == library.id) it.copy(
                libraryName = library.name,
                ownerName = library.ownerName.ifBlank { it.ownerName },
                ownerMobile = library.ownerPhone.ifBlank { it.ownerMobile },
                ownerEmail = library.ownerEmail.ifBlank { it.ownerEmail },
                updatedAt = System.currentTimeMillis()
            ) else it
        }

        val arr = JSONArray().apply {
            put(JSONObject().apply {
                put("id", library.id)
                put("name", library.name)
                put("code", library.code)
                put("logoUrl", library.logoUrl)
                put("description", library.description)
                put("establishedDate", library.establishedDate)
                put("regNumber", library.regNumber)
                put("ownerName", library.ownerName)
                put("ownerPhone", library.ownerPhone)
                put("ownerEmail", library.ownerEmail)
                put("ownerWhatsApp", library.ownerWhatsApp)
                put("alternateContact", library.alternateContact)
                put("address", library.address)
                put("landmark", library.landmark)
                put("city", library.city)
                put("district", library.district)
                put("state", library.state)
                put("pincode", library.pincode)
                put("latitude", library.latitude)
                put("longitude", library.longitude)
                put("phone", library.phone)
                put("whatsapp", library.whatsapp)
                put("email", library.email)
                put("website", library.website)
                put("upiId", library.upiId)
                put("upiPayeeName", library.upiPayeeName)
                put("receiptPrefix", library.receiptPrefix)
                put("defaultFinePerDay", library.defaultFinePerDay)
                put("borrowLimit", library.borrowLimit)
                put("loanDays", library.loanDays)
                put("qrAttendanceStrictShift", library.qrAttendanceStrictShift)
                put("createdAt", library.createdAt)
                put("updatedAt", library.updatedAt)
            })
        }
        SupabaseClient.upsertRecords("libraries", arr)
    }

    // ==========================================
    // USER ACCOUNT OPERATIONS
    // ==========================================

    suspend fun getUserByEmail(email: String): UserAccountEntity? = withContext(Dispatchers.IO) {
        _users.value.find { it.email.equals(email, ignoreCase = true) }
            ?: run {
                val (ok, arr) = SupabaseClient.queryTable("users?email=ilike.$email&select=*")
                if (ok && arr != null && arr.length() > 0) {
                    parseUser(arr.getJSONObject(0))
                } else null
            }
    }

    suspend fun getUserByIdentifier(identifier: String): UserAccountEntity? = withContext(Dispatchers.IO) {
        _users.value.find { it.email.equals(identifier, ignoreCase = true) || it.phone == identifier }
            ?: run {
                val (ok, arr) = SupabaseClient.queryTable("users?or=(email.ilike.$identifier,phone.eq.$identifier)&select=*")
                if (ok && arr != null && arr.length() > 0) {
                    parseUser(arr.getJSONObject(0))
                } else null
            }
    }

    suspend fun getUserByIdentifierAndRole(identifier: String, role: String): UserAccountEntity? = withContext(Dispatchers.IO) {
        _users.value.find { (it.email.equals(identifier, ignoreCase = true) || it.phone == identifier) && it.role.equals(role, ignoreCase = true) }
            ?: run {
                val (ok, arr) = SupabaseClient.queryTable("users?or=(email.ilike.$identifier,phone.eq.$identifier)&role=ilike.$role&select=*")
                if (ok && arr != null && arr.length() > 0) {
                    parseUser(arr.getJSONObject(0))
                } else null
            }
    }

    fun getUserById(id: String): Flow<UserAccountEntity?> =
        _users.map { list -> list.find { it.id == id } }

    suspend fun findStudentByIdentifier(identifier: String): StudentEntity? = withContext(Dispatchers.IO) {
        val clean = identifier.trim()
        _students.value.find {
            it.mobile.equals(clean, ignoreCase = true) ||
            it.studentCode.equals(clean, ignoreCase = true) ||
            it.email.equals(clean, ignoreCase = true) ||
            it.id == clean ||
            it.rfidQrCode == clean
        } ?: run {
            val (ok, arr) = SupabaseClient.queryTable("students?or=(mobile.eq.$clean,studentCode.ilike.$clean,email.ilike.$clean,id.eq.$clean)&select=*")
            if (ok && arr != null && arr.length() > 0) {
                parseStudent(arr.getJSONObject(0))
            } else null
        }
    }

    suspend fun saveUser(user: UserAccountEntity) = withContext(Dispatchers.IO) {
        _users.value = _users.value.filter { it.id != user.id } + user
        val arr = JSONArray().apply {
            put(JSONObject().apply {
                put("id", user.id)
                put("email", user.email)
                put("password", user.password)
                put("role", user.role)
                put("libraryId", user.libraryId)
                put("name", user.name)
                put("phone", user.phone)
                put("avatarUrl", user.avatarUrl)
                put("studentIdRef", user.studentIdRef)
                put("isActive", user.isActive)
                put("createdAt", user.createdAt)
            })
        }
        SupabaseClient.upsertRecords("users", arr)
    }

    // ==========================================
    // HALLS, CABINS, SECTIONS, SHIFTS, PLANS
    // ==========================================

    fun getHalls(libraryId: String): Flow<List<HallEntity>> =
        _halls.map { list -> list.filter { it.libraryId == libraryId } }

    suspend fun saveHall(hall: HallEntity) = insertHall(hall)

    suspend fun insertHall(hall: HallEntity) = withContext(Dispatchers.IO) {
        _halls.value = _halls.value.filter { it.id != hall.id } + hall
        val arr = JSONArray().apply {
            put(JSONObject().apply {
                put("id", hall.id)
                put("libraryId", hall.libraryId)
                put("name", hall.name)
                put("type", hall.type)
                put("floor", hall.floor)
                put("isAc", hall.isAc)
                put("description", hall.description)
                put("seatCount", hall.seatCount)
                put("openingTime", hall.openingTime)
                put("closingTime", hall.closingTime)
                put("isActive", hall.isActive)
            })
        }
        SupabaseClient.upsertRecords("halls", arr)
    }

    suspend fun updateHall(hall: HallEntity) = insertHall(hall)

    suspend fun deleteHall(hall: HallEntity) = withContext(Dispatchers.IO) {
        _halls.value = _halls.value.filter { it.id != hall.id }
        SupabaseClient.deleteRecord("halls", hall.id)
    }

    fun getCabins(libraryId: String): Flow<List<CabinEntity>> =
        _cabins.map { list -> list.filter { it.libraryId == libraryId } }

    suspend fun saveCabin(cabin: CabinEntity) = withContext(Dispatchers.IO) {
        _cabins.value = _cabins.value.filter { it.id != cabin.id } + cabin
        val arr = JSONArray().apply {
            put(JSONObject().apply {
                put("id", cabin.id)
                put("libraryId", cabin.libraryId)
                put("cabinNumber", cabin.cabinNumber)
                put("name", cabin.name)
                put("floor", cabin.floor)
                put("isAc", cabin.isAc)
                put("isPrivate", cabin.isPrivate)
                put("seatCount", cabin.seatCount)
                put("monthlyFee", cabin.monthlyFee)
                put("description", cabin.description)
                put("isActive", cabin.isActive)
            })
        }
        SupabaseClient.upsertRecords("cabins", arr)
    }

    suspend fun deleteCabin(cabin: CabinEntity) = withContext(Dispatchers.IO) {
        _cabins.value = _cabins.value.filter { it.id != cabin.id }
        SupabaseClient.deleteRecord("cabins", cabin.id)
    }

    fun getSections(libraryId: String): Flow<List<SectionEntity>> =
        _sections.map { list -> list.filter { it.libraryId == libraryId } }

    suspend fun saveSection(section: SectionEntity) = withContext(Dispatchers.IO) {
        _sections.value = _sections.value.filter { it.id != section.id } + section
        val arr = JSONArray().apply {
            put(JSONObject().apply {
                put("id", section.id)
                put("libraryId", section.libraryId)
                put("name", section.name)
                put("description", section.description)
                put("floor", section.floor)
                put("hallId", section.hallId)
                put("cabinId", section.cabinId)
                put("isActive", section.isActive)
            })
        }
        SupabaseClient.upsertRecords("sections", arr)
    }

    suspend fun deleteSection(section: SectionEntity) = withContext(Dispatchers.IO) {
        _sections.value = _sections.value.filter { it.id != section.id }
        SupabaseClient.deleteRecord("sections", section.id)
    }

    fun getShifts(libraryId: String): Flow<List<ShiftEntity>> =
        _shifts.map { list -> list.filter { it.libraryId == libraryId } }

    suspend fun saveShift(shift: ShiftEntity) = insertShift(shift)

    suspend fun insertShift(shift: ShiftEntity) = withContext(Dispatchers.IO) {
        _shifts.value = _shifts.value.filter { it.id != shift.id } + shift
        val arr = JSONArray().apply {
            put(JSONObject().apply {
                put("id", shift.id)
                put("libraryId", shift.libraryId)
                put("name", shift.name)
                put("startTime", shift.startTime)
                put("endTime", shift.endTime)
                put("fee", shift.fee)
                put("description", shift.description)
                put("isActive", shift.isActive)
            })
        }
        SupabaseClient.upsertRecords("shifts", arr)
    }

    suspend fun updateShift(shift: ShiftEntity) = insertShift(shift)

    suspend fun deleteShift(shift: ShiftEntity) = withContext(Dispatchers.IO) {
        _shifts.value = _shifts.value.filter { it.id != shift.id }
        SupabaseClient.deleteRecord("shifts", shift.id)
    }

    fun getPlans(libraryId: String): Flow<List<MembershipPlanEntity>> =
        _plans.map { list -> list.filter { it.libraryId == libraryId } }

    suspend fun savePlan(plan: MembershipPlanEntity) = insertMembershipPlan(plan)

    suspend fun insertMembershipPlan(plan: MembershipPlanEntity) = withContext(Dispatchers.IO) {
        _plans.value = _plans.value.filter { it.id != plan.id } + plan
        val arr = JSONArray().apply {
            put(JSONObject().apply {
                put("id", plan.id)
                put("libraryId", plan.libraryId)
                put("name", plan.name)
                put("durationMonths", plan.durationMonths)
                put("durationDays", plan.durationDays)
                put("durationType", plan.durationType)
                put("baseFee", plan.baseFee)
                put("maintenanceFee", plan.maintenanceFee)
                put("securityDeposit", plan.securityDeposit)
                put("discount", plan.discount)
                put("seatType", plan.seatType)
                put("shiftId", plan.shiftId)
                put("facilities", plan.facilities)
                put("renewalRules", plan.renewalRules)
                put("isActive", plan.isActive)
            })
        }
        SupabaseClient.upsertRecords("membership_plans", arr)
    }

    suspend fun updateMembershipPlan(plan: MembershipPlanEntity) = insertMembershipPlan(plan)

    suspend fun deletePlan(plan: MembershipPlanEntity) = withContext(Dispatchers.IO) {
        _plans.value = _plans.value.filter { it.id != plan.id }
        SupabaseClient.deleteRecord("membership_plans", plan.id)
    }

    // ==========================================
    // SEATS OPERATIONS
    // ==========================================

    private fun naturalSeatSort(list: List<SeatEntity>): List<SeatEntity> {
        return list.sortedWith(Comparator { s1, s2 ->
            val floorComp = (s1.floor ?: "").compareTo(s2.floor ?: "", ignoreCase = true)
            if (floorComp != 0) return@Comparator floorComp
            val hallComp = (s1.hallName ?: "").compareTo(s2.hallName ?: "", ignoreCase = true)
            if (hallComp != 0) return@Comparator hallComp
            val secComp = (s1.sectionName ?: "").compareTo(s2.sectionName ?: "", ignoreCase = true)
            if (secComp != 0) return@Comparator secComp
            compareAlphanumeric(s1.seatNumber ?: "", s2.seatNumber ?: "")
        })
    }

    private fun compareAlphanumeric(a: String, b: String): Int {
        val pattern = Regex("(\\d+)|(\\D+)")
        val aTokens = pattern.findAll(a).map { it.value }.toList()
        val bTokens = pattern.findAll(b).map { it.value }.toList()
        for (i in 0 until minOf(aTokens.size, bTokens.size)) {
            val aToken = aTokens[i]
            val bToken = bTokens[i]
            val aNum = aToken.toLongOrNull()
            val bNum = bToken.toLongOrNull()
            if (aNum != null && bNum != null) {
                val numComp = aNum.compareTo(bNum)
                if (numComp != 0) return numComp
            } else {
                val strComp = aToken.compareTo(bToken, ignoreCase = true)
                if (strComp != 0) return strComp
            }
        }
        return aTokens.size.compareTo(bTokens.size)
    }

    fun getSeats(libraryId: String): Flow<List<SeatEntity>> =
        _seats.map { list -> naturalSeatSort(list.filter { it.libraryId == libraryId }) }

    fun getSeatById(seatId: String): Flow<SeatEntity?> =
        _seats.map { list -> list.find { it.id == seatId } }

    fun getSeatByStudentId(libraryId: String, studentId: String): Flow<SeatEntity?> =
        _seats.map { list -> list.find { it.libraryId == libraryId && it.assignedStudentId == studentId } }

    suspend fun saveSeat(seat: SeatEntity) = withContext(Dispatchers.IO) {
        val currentList = _seats.value
        val index = currentList.indexOfFirst { it.id == seat.id }
        if (index >= 0) {
            val updated = currentList.toMutableList()
            updated[index] = seat
            _seats.value = updated
        } else {
            _seats.value = currentList + seat
        }
        pushSeatToSupabase(seat)
    }

    suspend fun deleteSeat(seat: SeatEntity) = withContext(Dispatchers.IO) {
        _seats.value = _seats.value.filter { it.id != seat.id }
        SupabaseClient.deleteRecord("seats", seat.id)
    }

    private suspend fun pushSeatToSupabase(seat: SeatEntity) {
        val arr = JSONArray().apply {
            put(JSONObject().apply {
                put("id", seat.id)
                put("libraryId", seat.libraryId)
                put("seatNumber", seat.seatNumber)
                put("hallId", seat.hallId)
                put("hallName", seat.hallName)
                put("sectionId", seat.sectionId)
                put("sectionName", seat.sectionName)
                put("floor", seat.floor)
                put("seatType", seat.seatType)
                put("monthlyFee", seat.monthlyFee)
                put("status", seat.status)
                put("assignedStudentId", seat.assignedStudentId)
                put("assignedStudentName", seat.assignedStudentName)
                put("assignedShiftId", seat.assignedShiftId)
                put("assignedShiftName", seat.assignedShiftName)
                put("validUntil", seat.validUntil)
                put("gridRow", seat.gridRow)
                put("gridCol", seat.gridCol)
            })
        }
        SupabaseClient.upsertRecords("seats", arr)
    }

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
        val existingSeats = _seats.value.filter { it.libraryId == libraryId }
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
        _seats.value = _seats.value + newSeats
        val arr = JSONArray()
        newSeats.forEach { s ->
            arr.put(JSONObject().apply {
                put("id", s.id)
                put("libraryId", s.libraryId)
                put("seatNumber", s.seatNumber)
                put("hallId", s.hallId)
                put("hallName", s.hallName)
                put("sectionId", s.sectionId)
                put("sectionName", s.sectionName)
                put("floor", s.floor)
                put("seatType", s.seatType)
                put("monthlyFee", s.monthlyFee)
                put("status", s.status)
            })
        }
        SupabaseClient.upsertRecords("seats", arr)
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
        saveSeat(updatedSeat)

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
        saveStudent(updatedStudent)

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
        _seatAssignments.value = _seatAssignments.value + assignment

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
        saveSeat(clearedOldSeat)

        val occupiedNewSeat = newSeat.copy(
            status = "OCCUPIED",
            assignedStudentId = student.id,
            assignedStudentName = student.fullName,
            assignedShiftId = oldSeat.assignedShiftId,
            assignedShiftName = oldSeat.assignedShiftName,
            validUntil = oldSeat.validUntil
        )
        saveSeat(occupiedNewSeat)

        val updatedStudent = student.copy(
            seatId = newSeat.id,
            seatNumber = newSeat.seatNumber,
            hallName = newSeat.hallName
        )
        saveStudent(updatedStudent)

        logAudit(libraryId, "Manager", "TRANSFER_SEAT", "Seat", newSeat.id, "Transferred ${student.fullName} from ${oldSeat.seatNumber} to ${newSeat.seatNumber}")
    }

    suspend fun releaseSeat(libraryId: String, seat: SeatEntity) = withContext(Dispatchers.IO) {
        val studentId = seat.assignedStudentId
        if (studentId.isNotEmpty()) {
            val student = _students.value.find { it.id == studentId || it.mobile == studentId }
            if (student != null) {
                saveStudent(student.copy(seatId = "", seatNumber = ""))
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
        saveSeat(cleared)
        logAudit(libraryId, "Manager", "RELEASE_SEAT", "Seat", seat.id, "Released seat ${seat.seatNumber}")
    }

    suspend fun updateSeatStatus(libraryId: String, seat: SeatEntity, newStatus: String) = withContext(Dispatchers.IO) {
        val updated = seat.copy(status = newStatus)
        saveSeat(updated)
        logAudit(libraryId, "Manager", "UPDATE_SEAT_STATUS", "Seat", seat.id, "Updated status to $newStatus")
    }

    suspend fun toggleSeatReservation(libraryId: String, seat: SeatEntity): String = withContext(Dispatchers.IO) {
        val newStatus = if (seat.status.equals("RESERVED", ignoreCase = true)) "AVAILABLE" else "RESERVED"
        val updated = seat.copy(
            status = newStatus,
            assignedStudentId = if (newStatus == "AVAILABLE") "" else seat.assignedStudentId,
            assignedStudentName = if (newStatus == "AVAILABLE") "" else seat.assignedStudentName
        )
        saveSeat(updated)
        logAudit(libraryId, "Manager", "TOGGLE_SEAT_RESERVATION", "Seat", seat.id, "Toggled seat ${seat.seatNumber} status to $newStatus")
        newStatus
    }

    // ==========================================
    // STUDENTS OPERATIONS
    // ==========================================

    fun getStudents(libraryId: String): Flow<List<StudentEntity>> =
        _students.map { list -> list.filter { it.libraryId == libraryId } }

    fun getStudentById(studentId: String): Flow<StudentEntity?> =
        _students.map { list -> list.find { it.id == studentId } }

    suspend fun saveStudent(student: StudentEntity) = withContext(Dispatchers.IO) {
        _students.value = _students.value.filter { it.id != student.id } + student

        // Cascade student name, shift & expiry date update to assigned seat
        if (student.seatId.isNotBlank() || student.seatNumber.isNotBlank()) {
            val matchingSeat = _seats.value.find { 
                it.id == student.seatId || (it.libraryId == student.libraryId && it.seatNumber.equals(student.seatNumber, ignoreCase = true))
            }
            if (matchingSeat != null && matchingSeat.assignedStudentId == student.id) {
                val updatedSeat = matchingSeat.copy(
                    assignedStudentName = student.fullName,
                    assignedShiftId = student.shiftId,
                    assignedShiftName = student.shiftName,
                    validUntil = student.expiryDate
                )
                if (updatedSeat != matchingSeat) {
                    saveSeat(updatedSeat)
                }
            }
        }

        // Cascade update to UserAccount if created for this student
        val userAcc = _users.value.find { 
            it.studentIdRef == student.id || (it.libraryId == student.libraryId && it.email.equals(student.email, ignoreCase = true) && student.email.isNotBlank())
        }
        if (userAcc != null) {
            saveUser(userAcc.copy(name = student.fullName, phone = student.mobile, email = student.email.ifBlank { userAcc.email }))
        }

        pushStudentToSupabase(student)
        logAudit(student.libraryId, "Manager", "SAVE_STUDENT", "Student", student.id, "Saved student ${student.fullName}")
    }

    suspend fun deleteStudent(student: StudentEntity) = withContext(Dispatchers.IO) {
        archiveStudent(student.libraryId, student, "Requested Deletion / Exit")
    }

    suspend fun archiveStudent(libraryId: String, student: StudentEntity, reason: String = "Left Library") = withContext(Dispatchers.IO) {
        if (student.seatId.isNotBlank()) {
            val seat = _seats.value.find { it.id == student.seatId }
            if (seat != null) {
                saveSeat(seat.copy(status = "AVAILABLE", assignedStudentId = "", assignedStudentName = "", assignedShiftId = "", assignedShiftName = "", validUntil = ""))
            }
        }
        _seats.value.filter { it.libraryId == libraryId && it.assignedStudentId == student.id }.forEach { seat ->
            saveSeat(seat.copy(status = "AVAILABLE", assignedStudentId = "", assignedStudentName = "", assignedShiftId = "", assignedShiftName = "", validUntil = ""))
        }

        val archived = student.copy(
            status = "ARCHIVED",
            seatId = "",
            seatNumber = if (student.seatNumber.isNotBlank() && !student.seatNumber.contains("(Past)")) "${student.seatNumber} (Past)" else student.seatNumber
        )
        saveStudent(archived)
        logAudit(libraryId, "Manager", "ARCHIVE_STUDENT", "Student", student.id, "Archived student ${student.fullName} (Reason: $reason).")
    }

    suspend fun reactivateStudent(libraryId: String, student: StudentEntity) = withContext(Dispatchers.IO) {
        val reactivated = student.copy(
            status = "ACTIVE",
            seatNumber = student.seatNumber.replace(" (Past)", "")
        )
        saveStudent(reactivated)
        logAudit(libraryId, "Manager", "REACTIVATE_STUDENT", "Student", student.id, "Reactivated student ${student.fullName}.")
    }

    private suspend fun pushStudentToSupabase(student: StudentEntity) {
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
                put("courseClass", student.courseClass)
                put("college", student.college)
                put("targetExam", student.targetExam)
                put("category", student.category)
                put("batch", student.batch)
                put("planId", student.planId)
                put("planName", student.planName)
                put("shiftId", student.shiftId)
                put("shiftName", student.shiftName)
                put("seatId", student.seatId)
                put("seatNumber", student.seatNumber)
                put("hallName", student.hallName)
                put("joiningDate", student.joiningDate)
                put("expiryDate", student.expiryDate)
                put("totalFee", student.totalFee)
                put("discount", student.discount)
                put("paidAmount", student.paidAmount)
                put("dueAmount", student.dueAmount)
                put("status", student.status)
                put("rfidQrCode", student.rfidQrCode)
                put("emergencyContact", student.emergencyContact)
                put("password", student.password)
                put("createdAt", student.createdAt)
            })
        }
        SupabaseClient.upsertRecords("students", arr)
    }

    // ==========================================
    // ATTENDANCE OPERATIONS
    // ==========================================

    fun getAttendance(libraryId: String): Flow<List<AttendanceEntity>> =
        _attendance.map { list -> list.filter { it.libraryId == libraryId } }

    fun getTodayAttendance(libraryId: String): Flow<List<AttendanceEntity>> {
        val today = dateFormat.format(Date())
        return _attendance.map { list -> list.filter { it.libraryId == libraryId && it.date == today } }
    }

    fun getStudentAttendance(studentId: String): Flow<List<AttendanceEntity>> =
        _attendance.map { list -> list.filter { it.studentId == studentId } }

    suspend fun pushAttendanceToSupabase(att: AttendanceEntity): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val jsonArray = JSONArray().apply {
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
            SupabaseClient.upsertRecords("attendance", jsonArray)
        } catch (e: Exception) {
            Pair(false, "Server connection error: ${e.localizedMessage ?: "Unreachable"}")
        }
    }

    suspend fun processQrAttendance(
        libraryId: String,
        qrCodeOrStudentCode: String,
        studentIdContext: String? = null,
        locationNote: String? = null
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val liveCheck = AuthGuardService.verifyLibrarySubscription(libraryId)
        if (liveCheck !is LiveSubscriptionCheck.Active) {
            val reason = when (liveCheck) {
                LiveSubscriptionCheck.Inactive -> "Organization subscription is inactive"
                LiveSubscriptionCheck.Expired -> "Organization subscription has expired"
                LiveSubscriptionCheck.Suspended -> "Organization account is suspended"
                LiveSubscriptionCheck.PendingVerification -> "Organization subscription is pending verification"
                LiveSubscriptionCheck.NoSubscription -> "No active organization subscription found"
                LiveSubscriptionCheck.NetworkError -> "Real-time cloud authentication required"
                else -> "Access restricted"
            }
            return@withContext Pair(false, "⛔ Access Blocked: $reason")
        }

        val trimmed = qrCodeOrStudentCode.trim()
        val isGateAttendanceQr = trimmed.startsWith("LIBDESK_GATE_ATTENDANCE:") ||
                trimmed.startsWith("LIBDESK_GATE:") ||
                trimmed.startsWith("GATE_ATTENDANCE:") ||
                trimmed.startsWith("LIBDESK:ATTENDANCE:")

        val cleanCode = when {
            isGateAttendanceQr -> trimmed
            trimmed.startsWith("LIBDESK:SEAT:") -> trimmed.removePrefix("LIBDESK:SEAT:").trim()
            trimmed.startsWith("SEAT:") -> trimmed.removePrefix("SEAT:").trim()
            trimmed.startsWith("SEAT-") -> trimmed.removePrefix("SEAT-").trim()
            trimmed.startsWith("QR-SEAT-") -> trimmed.removePrefix("QR-SEAT-").trim()
            trimmed.startsWith("QR-") -> trimmed.removePrefix("QR-").trim()
            trimmed.startsWith("LIBDESK:STUDENT:") -> trimmed.removePrefix("LIBDESK:STUDENT:").trim()
            trimmed.startsWith("STUDENT:") -> trimmed.removePrefix("STUDENT:").trim()
            trimmed.startsWith("STU:") -> trimmed.removePrefix("STU:").trim()
            trimmed.contains(":") -> trimmed.substringAfterLast(":").trim()
            else -> trimmed
        }

        var student: StudentEntity? = if (!studentIdContext.isNullOrBlank()) {
            _students.value.find { it.id == studentIdContext }
        } else null

        var isStudentIdScan = false

        if (student == null && !isGateAttendanceQr) {
            student = _students.value.find {
                (it.libraryId == libraryId || libraryId.isEmpty()) &&
                (it.mobile == cleanCode || it.studentCode.equals(cleanCode, ignoreCase = true) || it.rfidQrCode == cleanCode || it.id == cleanCode)
            }
            if (student != null) isStudentIdScan = true
        }

        if (student == null && !isGateAttendanceQr) {
            val seat = _seats.value.find {
                it.libraryId == libraryId && (it.seatNumber.equals(cleanCode, ignoreCase = true) || it.id == cleanCode)
            }
            if (seat != null && seat.assignedStudentId.isNotBlank()) {
                student = _students.value.find { it.id == seat.assignedStudentId || it.mobile == seat.assignedStudentId }
            }
        }

        if (student == null) {
            return@withContext Pair(false, "❌ Invalid QR Code: No member or allocated seat record found for '$qrCodeOrStudentCode'")
        }

        if (!student.status.equals("ACTIVE", ignoreCase = true)) {
            return@withContext Pair(false, "❌ Account Inactive: Membership status is '${student.status}'. Please contact library desk.")
        }

        val today = dateFormat.format(Date())
        if (student.expiryDate.isNotBlank() && student.expiryDate < today) {
            return@withContext Pair(false, "❌ Membership Expired: Expired on ${student.expiryDate}. Please renew to check in.")
        }

        if (!isGateAttendanceQr && !isStudentIdScan) {
            val scannedSeatUpper = cleanCode.uppercase()
            val allocatedSeatUpper = student.seatNumber.trim().uppercase()

            if (allocatedSeatUpper.isNotBlank()) {
                if (scannedSeatUpper != allocatedSeatUpper) {
                    return@withContext Pair(
                        false,
                        "❌ Seat Allocation Mismatch: Scanned Seat '$cleanCode' does not match your assigned Seat '${student.seatNumber}'."
                    )
                }
            } else {
                val seat = _seats.value.find { it.libraryId == libraryId && it.seatNumber.equals(cleanCode, ignoreCase = true) }
                if (seat != null && seat.assignedStudentId.isNotBlank() && seat.assignedStudentId != student.id) {
                    return@withContext Pair(false, "❌ Seat Reserved: Seat '$cleanCode' is allocated to another library member.")
                }
            }
        }

        val nowTime = timeFormat.format(Date())
        val activeCheckIn = _attendance.value.find {
            it.libraryId == libraryId && it.studentId == student.id && it.date == today && it.status == "CHECKED_IN"
        }

        val modeLabel = if (isStudentIdScan) {
            "LIBRARIAN_ID_SCAN"
        } else if (isGateAttendanceQr) {
            if (!locationNote.isNullOrBlank()) "GATE_QR (GPS Verified)" else "GATE_QR"
        } else {
            if (!locationNote.isNullOrBlank()) "SEAT_QR (GPS Verified)" else "SEAT_QR"
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
                notes = if (locationNote != null) "${activeCheckIn.notes} | Out: $locationNote".trimStart(' ', '|') else activeCheckIn.notes,
                timestamp = System.currentTimeMillis()
            )

            updateAttendance(checkOutUpdated)
            logAudit(libraryId, "QR Scanner", "QR_CHECK_OUT", "Attendance", checkOutUpdated.id, "Check-out for ${student.fullName} at $nowTime [${modeLabel}]")
            val seatDisplay = if (student.seatNumber.isNotBlank()) "Seat: ${student.seatNumber}" else "General Desk"
            Pair(true, "✅ Checked OUT: ${student.fullName} ($seatDisplay)\nSession: ${duration / 60}h ${duration % 60}m • $nowTime")
        } else {
            val resolvedSeat = if (isStudentIdScan || isGateAttendanceQr) {
                student.seatNumber.ifBlank { "General Desk" }
            } else {
                cleanCode.ifBlank { student.seatNumber.ifBlank { "General Desk" } }
            }

            val newCheckIn = AttendanceEntity(
                id = UUID.randomUUID().toString(),
                libraryId = libraryId,
                studentId = student.id,
                studentName = student.fullName,
                seatNumber = resolvedSeat,
                hallName = student.hallName.ifEmpty { "Main Study Hall" },
                shiftName = student.shiftName.ifEmpty { "Full Day Shift" },
                date = today,
                checkInTime = nowTime,
                checkOutTime = "",
                status = "CHECKED_IN",
                mode = modeLabel,
                notes = if (isStudentIdScan) "Librarian ID Scan" else (locationNote ?: "Verified Seat QR Attendance"),
                timestamp = System.currentTimeMillis()
            )

            manualAttendance(newCheckIn)
            logAudit(libraryId, "QR Scanner", "QR_CHECK_IN", "Attendance", newCheckIn.id, "Check-in for ${student.fullName} at $nowTime [${modeLabel}]")
            val hallDisplay = student.hallName.ifBlank { "Main Study Hall" }
            val shiftDisplay = student.shiftName.ifBlank { "Full Day Shift" }
            Pair(true, "✅ Checked IN: ${student.fullName} (Seat $resolvedSeat)\n$hallDisplay • $shiftDisplay • $nowTime")
        }
    }

    suspend fun manualAttendance(attendance: AttendanceEntity) = withContext(Dispatchers.IO) {
        _attendance.value = _attendance.value.filter { it.id != attendance.id } + attendance
        pushAttendanceToSupabase(attendance)
        logAudit(attendance.libraryId, "Manager", "MANUAL_ATTENDANCE", "Attendance", attendance.id, "Attendance recorded for ${attendance.studentName} on ${attendance.date}")
    }

    suspend fun checkoutAttendance(attendanceId: String, checkOutTime: String? = null) = withContext(Dispatchers.IO) {
        val now = checkOutTime ?: timeFormat.format(Date())
        val att = _attendance.value.find { it.id == attendanceId }
        if (att != null) {
            val updated = att.copy(checkOutTime = now, status = "CHECKED_OUT")
            updateAttendance(updated)
        }
    }

    suspend fun updateAttendance(attendance: AttendanceEntity) = withContext(Dispatchers.IO) {
        _attendance.value = _attendance.value.filter { it.id != attendance.id } + attendance
        pushAttendanceToSupabase(attendance)
    }

    suspend fun deleteAttendance(attendanceId: String) = withContext(Dispatchers.IO) {
        _attendance.value = _attendance.value.filter { it.id != attendanceId }
        SupabaseClient.deleteRecord("attendance", attendanceId)
    }

    fun getAllAttendance(): Flow<List<AttendanceEntity>> = _attendance.asStateFlow()

    fun getAttendanceForDateRange(libraryId: String, startDate: String, endDate: String): Flow<List<AttendanceEntity>> =
        _attendance.map { list ->
            list.filter { it.libraryId == libraryId && it.date in startDate..endDate }
        }

    // ==========================================
    // BOOKS & LIBRARY ASSETS
    // ==========================================

    fun getBooks(libraryId: String): Flow<List<PhysicalBookEntity>> =
        _books.map { list -> list.filter { it.libraryId == libraryId } }

    fun getBookIssues(libraryId: String): Flow<List<BookIssueEntity>> =
        _bookIssues.map { list -> list.filter { it.libraryId == libraryId } }

    fun getStudentBookIssues(studentId: String): Flow<List<BookIssueEntity>> =
        _bookIssues.map { list -> list.filter { it.studentId == studentId } }

    suspend fun saveBook(book: PhysicalBookEntity) = withContext(Dispatchers.IO) {
        _books.value = _books.value.filter { it.id != book.id } + book
        val arr = JSONArray().apply {
            put(JSONObject().apply {
                put("id", book.id)
                put("libraryId", book.libraryId)
                put("title", book.title)
                put("author", book.author)
                put("isbn", book.isbn)
                put("category", book.category)
                put("publisher", book.publisher)
                put("edition", book.edition)
                put("totalCopies", book.totalCopies)
                put("availableCopies", book.availableCopies)
                put("issuedCopies", book.issuedCopies)
                put("rack", book.rack)
                put("shelf", book.shelf)
                put("coverUrl", book.coverUrl)
            })
        }
        SupabaseClient.upsertRecords("physical_books", arr)
    }

    suspend fun deleteBook(book: PhysicalBookEntity) = withContext(Dispatchers.IO) {
        _books.value = _books.value.filter { it.id != book.id }
        SupabaseClient.deleteRecord("physical_books", book.id)
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
        _bookIssues.value = _bookIssues.value + issue

        val updatedBook = book.copy(
            availableCopies = book.availableCopies - 1,
            issuedCopies = book.issuedCopies + 1
        )
        saveBook(updatedBook)

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
        _bookIssues.value = _bookIssues.value.filter { it.id != issue.id } + updatedIssue

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
            _fines.value = _fines.value + fine
        }

        logAudit(libraryId, "Manager", "RETURN_BOOK", "BookIssue", issue.id, "Returned '${issue.bookTitle}'. Fine: ₹$fineAmount")
        Pair(true, if (fineAmount > 0) "Book returned with overdue fine of ₹$fineAmount" else "Book returned successfully with zero fines!")
    }

    fun getDigitalMaterials(libraryId: String): Flow<List<DigitalMaterialEntity>> =
        _materials.map { list -> list.filter { it.libraryId == libraryId } }

    suspend fun saveDigitalMaterial(material: DigitalMaterialEntity) = withContext(Dispatchers.IO) {
        _materials.value = _materials.value.filter { it.id != material.id } + material
        val arr = JSONArray().apply {
            put(JSONObject().apply {
                put("id", material.id)
                put("libraryId", material.libraryId)
                put("title", material.title)
                put("category", material.category)
                put("fileUrl", material.fileUrl)
                put("fileType", material.fileType)
                put("isBookmarked", material.isBookmarked)
            })
        }
        SupabaseClient.upsertRecords("digital_materials", arr)
    }

    suspend fun toggleBookmarkMaterial(material: DigitalMaterialEntity) = withContext(Dispatchers.IO) {
        val updated = material.copy(isBookmarked = !material.isBookmarked)
        saveDigitalMaterial(updated)
    }

    suspend fun deleteDigitalMaterial(material: DigitalMaterialEntity) = withContext(Dispatchers.IO) {
        _materials.value = _materials.value.filter { it.id != material.id }
        SupabaseClient.deleteRecord("digital_materials", material.id)
    }

    // ==========================================
    // PAYMENTS & EXPENSES
    // ==========================================

    fun getPayments(libraryId: String): Flow<List<PaymentEntity>> =
        _payments.map { list -> list.filter { it.libraryId == libraryId } }

    fun getStudentPayments(studentId: String): Flow<List<PaymentEntity>> =
        _payments.map { list -> list.filter { it.studentId == studentId } }

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
        _payments.value = _payments.value + payment

        val arr = JSONArray().apply {
            put(JSONObject().apply {
                put("id", payment.id)
                put("libraryId", payment.libraryId)
                put("receiptNumber", payment.receiptNumber)
                put("studentId", payment.studentId)
                put("studentName", payment.studentName)
                put("amount", payment.amount)
                put("paymentMode", payment.paymentMode)
                put("date", payment.date)
                put("purpose", payment.purpose)
                put("referenceNumber", payment.referenceNumber)
                put("notes", payment.notes)
                put("remarks", payment.remarks)
                put("period", payment.period)
                put("dueBalance", payment.dueBalance)
                put("createdAt", payment.createdAt)
            })
        }
        SupabaseClient.upsertRecords("payments", arr)

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
        saveStudent(updatedStudent)

        logAudit(libraryId, "Manager", "RECORD_PAYMENT", "Payment", payment.id, "Receipt $receiptNumber: ₹$amount for ${student.fullName}")
        payment
    }

    fun getExpenses(libraryId: String): Flow<List<ExpenseEntity>> =
        _expenses.map { list -> list.filter { it.libraryId == libraryId } }

    suspend fun saveExpense(expense: ExpenseEntity) = withContext(Dispatchers.IO) {
        _expenses.value = _expenses.value.filter { it.id != expense.id } + expense
        val arr = JSONArray().apply {
            put(JSONObject().apply {
                put("id", expense.id)
                put("libraryId", expense.libraryId)
                put("category", expense.category)
                put("description", expense.description)
                put("amount", expense.amount)
                put("date", expense.date)
                put("paymentMode", expense.paymentMode)
                put("status", expense.status)
                put("receiptRef", expense.receiptRef)
            })
        }
        SupabaseClient.upsertRecords("expenses", arr)
        logAudit(expense.libraryId, "Manager", "RECORD_EXPENSE", "Expense", expense.id, "Expense of ₹${expense.amount} for ${expense.category}")
    }

    suspend fun updateExpense(expense: ExpenseEntity) = saveExpense(expense)

    suspend fun deleteExpense(expense: ExpenseEntity) = withContext(Dispatchers.IO) {
        _expenses.value = _expenses.value.filter { it.id != expense.id }
        SupabaseClient.deleteRecord("expenses", expense.id)
        logAudit(expense.libraryId, "Manager", "DELETE_EXPENSE", "Expense", expense.id, "Deleted expense of ₹${expense.amount}")
    }

    suspend fun updatePayment(payment: PaymentEntity) = withContext(Dispatchers.IO) {
        _payments.value = _payments.value.filter { it.id != payment.id } + payment
        logAudit(payment.libraryId, "Manager", "UPDATE_PAYMENT", "Payment", payment.id, "Updated payment receipt #${payment.receiptNumber}: ₹${payment.amount}")
    }

    suspend fun deletePayment(payment: PaymentEntity) = withContext(Dispatchers.IO) {
        _payments.value = _payments.value.filter { it.id != payment.id }
        SupabaseClient.deleteRecord("payments", payment.id)
        logAudit(payment.libraryId, "Manager", "DELETE_PAYMENT", "Payment", payment.id, "Deleted payment receipt #${payment.receiptNumber}")
    }

    fun getFines(libraryId: String): Flow<List<FineEntity>> =
        _fines.map { list -> list.filter { it.libraryId == libraryId } }

    fun getStudentFines(studentId: String): Flow<List<FineEntity>> =
        _fines.map { list -> list.filter { it.studentId == studentId } }

    suspend fun markFinePaid(fine: FineEntity) = withContext(Dispatchers.IO) {
        val updated = fine.copy(paid = true)
        _fines.value = _fines.value.filter { it.id != fine.id } + updated
    }

    // ==========================================
    // NOTICES OPERATIONS
    // ==========================================

    fun getActiveNotices(libraryId: String): Flow<List<NoticeEntity>> =
        _notices.map { list -> list.filter { it.libraryId == libraryId && it.isActive } }

    fun getAllNotices(libraryId: String): Flow<List<NoticeEntity>> =
        _notices.map { list -> list.filter { it.libraryId == libraryId } }

    fun getAllBroadcastNotices(): Flow<List<NoticeEntity>> =
        _notices.map { list -> list.filter { it.targetAudience.equals("ALL", ignoreCase = true) } }

    suspend fun saveNotice(notice: NoticeEntity) = withContext(Dispatchers.IO) {
        _notices.value = _notices.value.filter { it.id != notice.id } + notice
        val arr = JSONArray().apply {
            put(JSONObject().apply {
                put("id", notice.id)
                put("libraryId", notice.libraryId)
                put("title", notice.title)
                put("content", notice.content)
                put("category", notice.category)
                put("priority", notice.priority)
                put("date", notice.date)
                put("targetAudience", notice.targetAudience)
                put("isActive", notice.isActive)
            })
        }
        SupabaseClient.upsertRecords("notices", arr)
        logAudit(notice.libraryId, "Manager", "PUBLISH_NOTICE", "Notice", notice.id, "Published notice '${notice.title}'")
    }

    suspend fun deleteNotice(notice: NoticeEntity) = withContext(Dispatchers.IO) {
        _notices.value = _notices.value.filter { it.id != notice.id }
        SupabaseClient.deleteRecord("notices", notice.id)
    }

    suspend fun deleteNoticeById(id: String) = withContext(Dispatchers.IO) {
        _notices.value = _notices.value.filter { it.id != id }
        SupabaseClient.deleteRecord("notices", id)
    }

    // ==========================================
    // FEEDBACK & COMPLAINTS
    // ==========================================

    fun getFeedback(libraryId: String): Flow<List<FeedbackComplaintEntity>> =
        _feedback.map { list -> list.filter { it.libraryId == libraryId } }

    fun getStudentFeedback(studentId: String): Flow<List<FeedbackComplaintEntity>> =
        _feedback.map { list -> list.filter { it.studentId == studentId } }

    suspend fun submitFeedback(feedback: FeedbackComplaintEntity) = withContext(Dispatchers.IO) {
        _feedback.value = _feedback.value.filter { it.id != feedback.id } + feedback
        val arr = JSONArray().apply {
            put(JSONObject().apply {
                put("id", feedback.id)
                put("libraryId", feedback.libraryId)
                put("studentId", feedback.studentId)
                put("studentName", feedback.studentName)
                put("seatNumber", feedback.seatNumber)
                put("type", feedback.type)
                put("subject", feedback.subject)
                put("message", feedback.message)
                put("status", feedback.status)
                put("reply", feedback.reply)
                put("date", feedback.date)
                put("resolvedDate", feedback.resolvedDate)
            })
        }
        SupabaseClient.upsertRecords("feedback_complaints", arr)
    }

    suspend fun updateFeedbackStatus(feedback: FeedbackComplaintEntity, newStatus: String, reply: String) = withContext(Dispatchers.IO) {
        val resolvedDate = if (newStatus == "RESOLVED" || newStatus == "CLOSED") dateFormat.format(Date()) else feedback.resolvedDate
        val updated = feedback.copy(status = newStatus, reply = reply, resolvedDate = resolvedDate)
        submitFeedback(updated)
    }

    // ==========================================
    // AUDIT LOGS
    // ==========================================

    fun getAuditLogs(libraryId: String): Flow<List<AuditLogEntity>> =
        _auditLogs.map { list -> list.filter { it.libraryId == libraryId } }

    suspend fun logAudit(
        libraryId: String,
        performedBy: String,
        action: String,
        recordType: String,
        recordId: String,
        details: String
    ) = withContext(Dispatchers.IO) {
        val log = AuditLogEntity(
            id = UUID.randomUUID().toString(),
            libraryId = libraryId,
            performedBy = performedBy,
            action = action,
            recordType = recordType,
            recordId = recordId,
            details = details,
            timestamp = System.currentTimeMillis()
        )
        _auditLogs.value = _auditLogs.value + log
    }

    // ==========================================
    // CLOUD REFRESH (PULL SUPABASE DATA INTO MEMORY) - PARALLEL FETCH
    // ==========================================

    suspend fun pullAllLibrariesFromCloud(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val (ok, arr) = SupabaseClient.queryTable("libraries?select=*")
            if (ok && arr != null) {
                val list = mutableListOf<LibraryEntity>()
                for (i in 0 until arr.length()) {
                    list.add(parseLibrary(arr.getJSONObject(i)))
                }
                if (list.isNotEmpty()) {
                    _libraries.value = list
                }
                Pair(true, "Fetched ${list.size} libraries from cloud")
            } else {
                Pair(false, "Failed to fetch libraries from cloud")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pulling libraries from cloud", e)
            Pair(false, e.localizedMessage ?: "Error pulling libraries")
        }
    }

    suspend fun pullAllSubscriptionPlansFromCloud(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val (ok, arr) = SupabaseClient.queryTable("subscription_plans?select=*")
            if (ok && arr != null && arr.length() > 0) {
                val list = mutableListOf<SubscriptionPlans>()
                for (i in 0 until arr.length()) {
                    list.add(parseSubscriptionPlan(arr.getJSONObject(i)))
                }
                _subscriptionPlans.value = list
                Pair(true, "Fetched ${list.size} subscription plans")
            } else {
                Pair(false, "No subscription plans found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pulling subscription plans from cloud", e)
            Pair(false, e.localizedMessage ?: "Error")
        }
    }

    suspend fun pullSuperAdminFromCloud(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val (ok, arr) = SupabaseClient.queryTable("super_admin_users?select=*")
            if (ok && arr != null && arr.length() > 0) {
                val obj = arr.getJSONObject(0)
                val admin = SuperAdminUserEntity(
                    id = optStringAny(obj, "id", fallback = "SUPER-ADMIN-MASTER"),
                    name = optStringAny(obj, "name", fallback = "Super Administrator"),
                    email = optStringAny(obj, "email", fallback = ""),
                    mobile = optStringAny(obj, "mobile", "phone", "support_whatsapp", "supportWhatsApp", fallback = ""),
                    accessCode = optStringAny(obj, "accessCode", "access_code", fallback = ""),
                    upiId = optStringAny(obj, "upiId", "upi_id", fallback = "libdesk.billing@upi"),
                    upiPayeeName = optStringAny(obj, "upiPayeeName", "upi_payee_name", fallback = "LibDesk Cloud Subscriptions"),
                    is2FaEnabled = optBooleanAny(obj, "is2FaEnabled", "is_2fa_enabled", fallback = true),
                    isClaimed = optBooleanAny(obj, "isClaimed", "is_claimed", fallback = true)
                )
                _superAdmin.value = admin
                // Propagate the latest helpline contact & UPI to plans in memory if plans exist
                if (admin.mobile.isNotBlank() || admin.upiId.isNotBlank()) {
                    _subscriptionPlans.value = _subscriptionPlans.value.map { plan ->
                        plan.copy(
                            upiId = if (admin.upiId.isNotBlank()) admin.upiId else plan.upiId,
                            upiPayeeName = if (admin.upiPayeeName.isNotBlank()) admin.upiPayeeName else plan.upiPayeeName,
                            supportWhatsApp = if (admin.mobile.isNotBlank()) admin.mobile else plan.supportWhatsApp
                        )
                    }
                }
                Pair(true, "Fetched super admin")
            } else {
                Pair(false, "No super admin found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pulling super admin from cloud", e)
            Pair(false, e.localizedMessage ?: "Error")
        }
    }

    suspend fun pullFromCloud(libraryId: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (libraryId.isBlank()) return@withContext Pair(false, "Library ID is empty")
        try {
            coroutineScope {
                // Launch all table fetches concurrently in parallel for 10x faster performance
                val libDeferred = async { SupabaseClient.queryTable("libraries?id=eq.$libraryId&select=*") }
                val hallsDeferred = async { SupabaseClient.fetchRecords("halls", libraryId) }
                val cabinsDeferred = async { SupabaseClient.fetchRecords("cabins", libraryId) }
                val sectionsDeferred = async { SupabaseClient.fetchRecords("sections", libraryId) }
                val shiftsDeferred = async { SupabaseClient.fetchRecords("shifts", libraryId) }
                val plansDeferred = async { SupabaseClient.fetchRecords("membership_plans", libraryId) }
                val studentsDeferred = async { SupabaseClient.fetchRecords("students", libraryId) }
                val seatsDeferred = async { SupabaseClient.fetchRecords("seats", libraryId) }
                val noticesDeferred = async { SupabaseClient.fetchRecords("notices", libraryId) }
                val paymentsDeferred = async { SupabaseClient.fetchRecords("payments", libraryId) }
                val attendanceDeferred = async { SupabaseClient.fetchRecords("attendance", libraryId) }
                val booksDeferred = async { SupabaseClient.fetchRecords("physical_books", libraryId) }
                val issuesDeferred = async { SupabaseClient.fetchRecords("book_issues", libraryId) }
                val materialsDeferred = async { SupabaseClient.fetchRecords("digital_materials", libraryId) }
                val expensesDeferred = async { SupabaseClient.fetchRecords("expenses", libraryId) }
                val finesDeferred = async { SupabaseClient.fetchRecords("fines", libraryId) }
                val feedbackDeferred = async { SupabaseClient.fetchRecords("feedback_complaints", libraryId) }
                val subDeferred = async { SupabaseClient.queryTable("library_subscriptions?libraryId=eq.$libraryId&select=*") }
                val userSubDeferred = async { SupabaseClient.queryTable("user_subscriptions?libraryId=eq.$libraryId&select=*") }

                // 1. Process Library
                val (libOk, libArr) = libDeferred.await()
                if (libOk && libArr != null && libArr.length() > 0) {
                    val lib = parseLibrary(libArr.getJSONObject(0))
                    _libraries.value = _libraries.value.filter { it.id != lib.id } + lib
                }

                // 2. Process Halls
                val (hOk, hArr) = hallsDeferred.await()
                if (hOk && hArr != null) {
                    val hallsList = mutableListOf<HallEntity>()
                    for (i in 0 until hArr.length()) {
                        hallsList.add(parseHall(hArr.getJSONObject(i), libraryId))
                    }
                    _halls.value = _halls.value.filter { it.libraryId != libraryId } + hallsList
                }

                // 2b. Process Cabins
                val (cabOk, cabArr) = cabinsDeferred.await()
                if (cabOk && cabArr != null) {
                    val cabList = mutableListOf<CabinEntity>()
                    for (i in 0 until cabArr.length()) {
                        cabList.add(parseCabin(cabArr.getJSONObject(i), libraryId))
                    }
                    _cabins.value = _cabins.value.filter { it.libraryId != libraryId } + cabList
                }

                // 2c. Process Sections
                val (secOk, secArr) = sectionsDeferred.await()
                if (secOk && secArr != null) {
                    val secList = mutableListOf<SectionEntity>()
                    for (i in 0 until secArr.length()) {
                        secList.add(parseSection(secArr.getJSONObject(i), libraryId))
                    }
                    _sections.value = _sections.value.filter { it.libraryId != libraryId } + secList
                }

                // 3. Process Shifts
                val (shOk, shArr) = shiftsDeferred.await()
                if (shOk && shArr != null) {
                    val shiftsList = mutableListOf<ShiftEntity>()
                    for (i in 0 until shArr.length()) {
                        shiftsList.add(parseShift(shArr.getJSONObject(i), libraryId))
                    }
                    _shifts.value = _shifts.value.filter { it.libraryId != libraryId } + shiftsList
                }

                // 4. Process Plans
                val (planOk, planArr) = plansDeferred.await()
                if (planOk && planArr != null) {
                    val plansList = mutableListOf<MembershipPlanEntity>()
                    for (i in 0 until planArr.length()) {
                        plansList.add(parseMembershipPlan(planArr.getJSONObject(i), libraryId))
                    }
                    _plans.value = _plans.value.filter { it.libraryId != libraryId } + plansList
                }

                // 5. Process Students
                val (sOk, sArr) = studentsDeferred.await()
                var studentCount = 0
                if (sOk && sArr != null) {
                    val studentsList = mutableListOf<StudentEntity>()
                    for (i in 0 until sArr.length()) {
                        studentsList.add(parseStudent(sArr.getJSONObject(i), libraryId))
                        studentCount++
                    }
                    _students.value = _students.value.filter { it.libraryId != libraryId } + studentsList
                }

                // 6. Process Seats
                val (seatOk, seatArr) = seatsDeferred.await()
                if (seatOk && seatArr != null) {
                    val seatsList = mutableListOf<SeatEntity>()
                    for (i in 0 until seatArr.length()) {
                        seatsList.add(parseSeat(seatArr.getJSONObject(i), libraryId))
                    }
                    _seats.value = _seats.value.filter { it.libraryId != libraryId } + seatsList
                }

                // 7. Process Notices
                val (notOk, notArr) = noticesDeferred.await()
                if (notOk && notArr != null) {
                    val noticesList = mutableListOf<NoticeEntity>()
                    for (i in 0 until notArr.length()) {
                        noticesList.add(parseNotice(notArr.getJSONObject(i), libraryId))
                    }
                    _notices.value = _notices.value.filter { it.libraryId != libraryId } + noticesList
                }

                // 8. Process Payments
                val (payOk, payArr) = paymentsDeferred.await()
                if (payOk && payArr != null) {
                    val paymentsList = mutableListOf<PaymentEntity>()
                    for (i in 0 until payArr.length()) {
                        paymentsList.add(parsePayment(payArr.getJSONObject(i), libraryId))
                    }
                    _payments.value = _payments.value.filter { it.libraryId != libraryId } + paymentsList
                }

                // 9. Process Attendance
                val (attOk, attArr) = attendanceDeferred.await()
                if (attOk && attArr != null) {
                    val attList = mutableListOf<AttendanceEntity>()
                    for (i in 0 until attArr.length()) {
                        attList.add(parseAttendance(attArr.getJSONObject(i), libraryId))
                    }
                    _attendance.value = _attendance.value.filter { it.libraryId != libraryId } + attList
                }

                // 10. Process Books
                val (bkOk, bkArr) = booksDeferred.await()
                if (bkOk && bkArr != null) {
                    val bkList = mutableListOf<PhysicalBookEntity>()
                    for (i in 0 until bkArr.length()) {
                        bkList.add(parseBook(bkArr.getJSONObject(i), libraryId))
                    }
                    _books.value = _books.value.filter { it.libraryId != libraryId } + bkList
                }

                // 11. Process Book Issues
                val (issOk, issArr) = issuesDeferred.await()
                if (issOk && issArr != null) {
                    val issList = mutableListOf<BookIssueEntity>()
                    for (i in 0 until issArr.length()) {
                        issList.add(parseBookIssue(issArr.getJSONObject(i), libraryId))
                    }
                    _bookIssues.value = _bookIssues.value.filter { it.libraryId != libraryId } + issList
                }

                // 12. Process Digital Materials
                val (matOk, matArr) = materialsDeferred.await()
                if (matOk && matArr != null) {
                    val matList = mutableListOf<DigitalMaterialEntity>()
                    for (i in 0 until matArr.length()) {
                        matList.add(parseDigitalMaterial(matArr.getJSONObject(i), libraryId))
                    }
                    _materials.value = _materials.value.filter { it.libraryId != libraryId } + matList
                }

                // 13. Process Expenses
                val (expOk, expArr) = expensesDeferred.await()
                if (expOk && expArr != null) {
                    val expList = mutableListOf<ExpenseEntity>()
                    for (i in 0 until expArr.length()) {
                        expList.add(parseExpense(expArr.getJSONObject(i), libraryId))
                    }
                    _expenses.value = _expenses.value.filter { it.libraryId != libraryId } + expList
                }

                // 14. Process Fines
                val (fineOk, fineArr) = finesDeferred.await()
                if (fineOk && fineArr != null) {
                    val fineList = mutableListOf<FineEntity>()
                    for (i in 0 until fineArr.length()) {
                        fineList.add(parseFine(fineArr.getJSONObject(i), libraryId))
                    }
                    _fines.value = _fines.value.filter { it.libraryId != libraryId } + fineList
                }

                // 15. Process Feedback Complaints
                val (fbOk, fbArr) = feedbackDeferred.await()
                if (fbOk && fbArr != null) {
                    val fbList = mutableListOf<FeedbackComplaintEntity>()
                    for (i in 0 until fbArr.length()) {
                        fbList.add(parseFeedback(fbArr.getJSONObject(i), libraryId))
                    }
                    _feedback.value = _feedback.value.filter { it.libraryId != libraryId } + fbList
                }

                // 16. Process Subscriptions
                val (subOk, subArr) = subDeferred.await()
                if (subOk && subArr != null && subArr.length() > 0) {
                    val sub = parseLibrarySubscription(subArr.getJSONObject(0))
                    _librarySubscriptions.value = _librarySubscriptions.value.filter { it.libraryId != libraryId } + sub
                }

                // 17. Process User Subscriptions
                val (uSubOk, uSubArr) = userSubDeferred.await()
                if (uSubOk && uSubArr != null && uSubArr.length() > 0) {
                    val uSub = parseUserSubscription(uSubArr.getJSONObject(0))
                    _userSubscriptions.value = _userSubscriptions.value.filter { it.libraryId != libraryId } + uSub
                }

                Pair(true, "Cloud sync completed ($studentCount students synchronized)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pulling Supabase data", e)
            Pair(false, "Cloud pull error: ${e.localizedMessage}")
        }
    }

    // ==========================================
    // JSON PARSING HELPERS (RESILIENT CAMELCASE + SNAKE_CASE SUPPORT)
    // ==========================================

    private fun optStringAny(obj: JSONObject, vararg keys: String, fallback: String = ""): String {
        for (key in keys) {
            if (obj.has(key) && !obj.isNull(key)) {
                val v = obj.optString(key, "")
                if (v.isNotBlank() && v != "null") return v
            }
        }
        return fallback
    }

    private fun optDoubleAny(obj: JSONObject, vararg keys: String, fallback: Double = 0.0): Double {
        for (key in keys) {
            if (obj.has(key) && !obj.isNull(key)) {
                return obj.optDouble(key, fallback)
            }
        }
        return fallback
    }

    private fun optIntAny(obj: JSONObject, vararg keys: String, fallback: Int = 0): Int {
        for (key in keys) {
            if (obj.has(key) && !obj.isNull(key)) {
                return obj.optInt(key, fallback)
            }
        }
        return fallback
    }

    private fun optLongAny(obj: JSONObject, vararg keys: String, fallback: Long = 0L): Long {
        for (key in keys) {
            if (obj.has(key) && !obj.isNull(key)) {
                return obj.optLong(key, fallback)
            }
        }
        return fallback
    }

    private fun optBooleanAny(obj: JSONObject, vararg keys: String, fallback: Boolean = false): Boolean {
        for (key in keys) {
            if (obj.has(key) && !obj.isNull(key)) {
                return obj.optBoolean(key, fallback)
            }
        }
        return fallback
    }

    private fun parseLibrary(obj: JSONObject): LibraryEntity {
        return LibraryEntity(
            id = optStringAny(obj, "id", "library_id", "libraryId", fallback = UUID.randomUUID().toString()),
            name = optStringAny(obj, "name", "library_name", "libraryName", fallback = "Library"),
            code = optStringAny(obj, "code", "library_code", "libraryCode", fallback = ""),
            logoUrl = optStringAny(obj, "logoUrl", "logo_url", "logo", fallback = ""),
            description = optStringAny(obj, "description", "desc", fallback = ""),
            establishedDate = optStringAny(obj, "establishedDate", "established_date", "est_date", fallback = ""),
            regNumber = optStringAny(obj, "regNumber", "reg_number", "registration_number", "registrationNumber", fallback = ""),
            ownerName = optStringAny(obj, "ownerName", "owner_name", "manager_name", "managerName", fallback = ""),
            ownerPhone = optStringAny(obj, "ownerPhone", "owner_phone", "manager_phone", "phone", fallback = ""),
            ownerEmail = optStringAny(obj, "ownerEmail", "owner_email", "manager_email", "email", fallback = ""),
            ownerWhatsApp = optStringAny(obj, "ownerWhatsApp", "owner_whatsapp", "whatsapp", fallback = ""),
            alternateContact = optStringAny(obj, "alternateContact", "alternate_contact", "alt_phone", fallback = ""),
            address = optStringAny(obj, "address", "street_address", "location", fallback = ""),
            landmark = optStringAny(obj, "landmark", fallback = ""),
            city = optStringAny(obj, "city", fallback = ""),
            district = optStringAny(obj, "district", fallback = ""),
            state = optStringAny(obj, "state", fallback = ""),
            pincode = optStringAny(obj, "pincode", "pin_code", "zip", fallback = ""),
            latitude = optDoubleAny(obj, "latitude", "lat", fallback = 0.0),
            longitude = optDoubleAny(obj, "longitude", "lng", "lon", fallback = 0.0),
            phone = optStringAny(obj, "phone", "ownerPhone", "owner_phone", fallback = ""),
            whatsapp = optStringAny(obj, "whatsapp", "ownerWhatsApp", "owner_whatsapp", fallback = ""),
            email = optStringAny(obj, "email", "ownerEmail", "owner_email", fallback = ""),
            website = optStringAny(obj, "website", "web", fallback = ""),
            upiId = optStringAny(obj, "upiId", "upi_id", "upi", "vpa", fallback = ""),
            upiPayeeName = optStringAny(obj, "upiPayeeName", "upi_payee_name", "payee_name", "payeeName", fallback = ""),
            receiptPrefix = optStringAny(obj, "receiptPrefix", "receipt_prefix", fallback = "REC"),
            defaultFinePerDay = optDoubleAny(obj, "defaultFinePerDay", "default_fine_per_day", "fine_per_day", fallback = 5.0),
            borrowLimit = optIntAny(obj, "borrowLimit", "borrow_limit", fallback = 2),
            loanDays = optIntAny(obj, "loanDays", "loan_days", fallback = 14),
            qrAttendanceStrictShift = optBooleanAny(obj, "qrAttendanceStrictShift", "qr_attendance_strict_shift", fallback = false),
            createdAt = optLongAny(obj, "createdAt", "created_at", fallback = System.currentTimeMillis()),
            updatedAt = optLongAny(obj, "updatedAt", "updated_at", fallback = System.currentTimeMillis())
        )
    }

    private fun parseUser(obj: JSONObject): UserAccountEntity {
        return UserAccountEntity(
            id = optStringAny(obj, "id", "user_id", fallback = UUID.randomUUID().toString()),
            email = optStringAny(obj, "email", fallback = ""),
            password = optStringAny(obj, "password", fallback = "password123"),
            role = optStringAny(obj, "role", fallback = "Student"),
            libraryId = optStringAny(obj, "libraryId", "library_id", fallback = ""),
            name = optStringAny(obj, "name", "full_name", fallback = ""),
            phone = optStringAny(obj, "phone", "mobile", fallback = ""),
            avatarUrl = optStringAny(obj, "avatarUrl", "avatar_url", fallback = ""),
            studentIdRef = optStringAny(obj, "studentIdRef", "student_id_ref", "student_id", fallback = "").takeIf { it.isNotBlank() },
            isActive = optBooleanAny(obj, "isActive", "is_active", fallback = true),
            createdAt = optLongAny(obj, "createdAt", "created_at", fallback = System.currentTimeMillis())
        )
    }

    private fun parseHall(obj: JSONObject, defaultLibId: String): HallEntity {
        return HallEntity(
            id = optStringAny(obj, "id", fallback = UUID.randomUUID().toString()),
            libraryId = optStringAny(obj, "libraryId", "library_id", fallback = defaultLibId),
            name = optStringAny(obj, "name", "hall_name", fallback = "Main Hall"),
            type = optStringAny(obj, "type", "hall_type", fallback = "AC Hall"),
            floor = optStringAny(obj, "floor", fallback = "Ground Floor"),
            isAc = optBooleanAny(obj, "isAc", "is_ac", fallback = true),
            description = optStringAny(obj, "description", fallback = ""),
            seatCount = optIntAny(obj, "seatCount", "seat_count", fallback = 0),
            openingTime = optStringAny(obj, "openingTime", "opening_time", fallback = "06:00 AM"),
            closingTime = optStringAny(obj, "closingTime", "closing_time", fallback = "11:00 PM"),
            isActive = optBooleanAny(obj, "isActive", "is_active", fallback = true)
        )
    }

    private fun parseShift(obj: JSONObject, defaultLibId: String): ShiftEntity {
        return ShiftEntity(
            id = optStringAny(obj, "id", fallback = UUID.randomUUID().toString()),
            libraryId = optStringAny(obj, "libraryId", "library_id", fallback = defaultLibId),
            name = optStringAny(obj, "name", "shift_name", fallback = "Shift"),
            startTime = optStringAny(obj, "startTime", "start_time", fallback = "08:00 AM"),
            endTime = optStringAny(obj, "endTime", "end_time", fallback = "02:00 PM"),
            fee = optDoubleAny(obj, "fee", fallback = 800.0),
            description = optStringAny(obj, "description", fallback = ""),
            isActive = optBooleanAny(obj, "isActive", "is_active", fallback = true)
        )
    }

    private fun parseMembershipPlan(obj: JSONObject, defaultLibId: String): MembershipPlanEntity {
        return MembershipPlanEntity(
            id = optStringAny(obj, "id", fallback = UUID.randomUUID().toString()),
            libraryId = optStringAny(obj, "libraryId", "library_id", fallback = defaultLibId),
            name = optStringAny(obj, "name", "plan_name", fallback = "Monthly"),
            durationMonths = optIntAny(obj, "durationMonths", "duration_months", fallback = 1),
            durationDays = optIntAny(obj, "durationDays", "duration_days", fallback = 30),
            durationType = optStringAny(obj, "durationType", "duration_type", fallback = "MONTHS"),
            baseFee = optDoubleAny(obj, "baseFee", "base_fee", "fee", fallback = 1000.0),
            maintenanceFee = optDoubleAny(obj, "maintenanceFee", "maintenance_fee", fallback = 100.0),
            securityDeposit = optDoubleAny(obj, "securityDeposit", "security_deposit", fallback = 500.0),
            discount = optDoubleAny(obj, "discount", fallback = 0.0),
            seatType = optStringAny(obj, "seatType", "seat_type", fallback = "Standard"),
            shiftId = optStringAny(obj, "shiftId", "shift_id", fallback = ""),
            facilities = optStringAny(obj, "facilities", fallback = ""),
            renewalRules = optStringAny(obj, "renewalRules", "renewal_rules", fallback = ""),
            isActive = optBooleanAny(obj, "isActive", "is_active", fallback = true)
        )
    }

    private fun parseStudent(obj: JSONObject, defaultLibId: String = ""): StudentEntity {
        return StudentEntity(
            id = optStringAny(obj, "id", fallback = UUID.randomUUID().toString()),
            libraryId = optStringAny(obj, "libraryId", "library_id", fallback = defaultLibId),
            studentCode = optStringAny(obj, "studentCode", "student_code", fallback = "STU-001"),
            fullName = optStringAny(obj, "fullName", "full_name", "name", fallback = "Unknown"),
            mobile = optStringAny(obj, "mobile", "phone", fallback = ""),
            email = optStringAny(obj, "email", fallback = ""),
            gender = optStringAny(obj, "gender", fallback = "Other"),
            address = optStringAny(obj, "address", fallback = ""),
            parentName = optStringAny(obj, "parentName", "parent_name", "guardian_name", fallback = ""),
            parentMobile = optStringAny(obj, "parentMobile", "parent_mobile", "guardian_phone", fallback = ""),
            courseClass = optStringAny(obj, "courseClass", "course_class", "course", fallback = ""),
            college = optStringAny(obj, "college", fallback = ""),
            targetExam = optStringAny(obj, "targetExam", "target_exam", fallback = "General"),
            category = optStringAny(obj, "category", fallback = "General"),
            batch = optStringAny(obj, "batch", fallback = "Morning"),
            planId = optStringAny(obj, "planId", "plan_id", fallback = ""),
            planName = optStringAny(obj, "planName", "plan_name", fallback = "Monthly"),
            shiftId = optStringAny(obj, "shiftId", "shift_id", fallback = ""),
            shiftName = optStringAny(obj, "shiftName", "shift_name", fallback = "Full Day"),
            seatId = optStringAny(obj, "seatId", "seat_id", fallback = ""),
            seatNumber = optStringAny(obj, "seatNumber", "seat_number", fallback = ""),
            hallName = optStringAny(obj, "hallName", "hall_name", fallback = ""),
            joiningDate = optStringAny(obj, "joiningDate", "joining_date", fallback = ""),
            expiryDate = optStringAny(obj, "expiryDate", "expiry_date", fallback = ""),
            totalFee = optDoubleAny(obj, "totalFee", "total_fee", fallback = 1000.0),
            discount = optDoubleAny(obj, "discount", fallback = 0.0),
            paidAmount = optDoubleAny(obj, "paidAmount", "paid_amount", fallback = 0.0),
            dueAmount = optDoubleAny(obj, "dueAmount", "due_amount", fallback = 0.0),
            status = optStringAny(obj, "status", fallback = "ACTIVE"),
            rfidQrCode = optStringAny(obj, "rfidQrCode", "rfid_qr_code", "qr_code", fallback = ""),
            emergencyContact = optStringAny(obj, "emergencyContact", "emergency_contact", fallback = ""),
            password = optStringAny(obj, "password", fallback = "password123"),
            createdAt = optLongAny(obj, "createdAt", "created_at", fallback = System.currentTimeMillis())
        )
    }

    private fun parseSeat(obj: JSONObject, defaultLibId: String): SeatEntity {
        return SeatEntity(
            id = optStringAny(obj, "id", fallback = UUID.randomUUID().toString()),
            libraryId = optStringAny(obj, "libraryId", "library_id", fallback = defaultLibId),
            seatNumber = optStringAny(obj, "seatNumber", "seat_number", fallback = "A-01"),
            hallId = optStringAny(obj, "hallId", "hall_id", fallback = ""),
            hallName = optStringAny(obj, "hallName", "hall_name", fallback = ""),
            sectionId = optStringAny(obj, "sectionId", "section_id", fallback = ""),
            sectionName = optStringAny(obj, "sectionName", "section_name", fallback = ""),
            floor = optStringAny(obj, "floor", fallback = "Ground Floor"),
            seatType = optStringAny(obj, "seatType", "seat_type", fallback = "Standard"),
            monthlyFee = optDoubleAny(obj, "monthlyFee", "monthly_fee", fallback = 1000.0),
            status = optStringAny(obj, "status", fallback = "AVAILABLE"),
            assignedStudentId = optStringAny(obj, "assignedStudentId", "assigned_student_id", fallback = ""),
            assignedStudentName = optStringAny(obj, "assignedStudentName", "assigned_student_name", fallback = ""),
            assignedShiftId = optStringAny(obj, "assignedShiftId", "assigned_shift_id", fallback = ""),
            assignedShiftName = optStringAny(obj, "assignedShiftName", "assigned_shift_name", fallback = ""),
            validUntil = optStringAny(obj, "validUntil", "valid_until", fallback = ""),
            gridRow = optIntAny(obj, "gridRow", "grid_row", fallback = 1),
            gridCol = optIntAny(obj, "gridCol", "grid_col", fallback = 1)
        )
    }

    private fun parseNotice(obj: JSONObject, defaultLibId: String): NoticeEntity {
        return NoticeEntity(
            id = optStringAny(obj, "id", fallback = UUID.randomUUID().toString()),
            libraryId = optStringAny(obj, "libraryId", "library_id", fallback = defaultLibId),
            title = optStringAny(obj, "title", fallback = "Notice"),
            content = optStringAny(obj, "content", fallback = ""),
            category = optStringAny(obj, "category", fallback = "GENERAL"),
            priority = optStringAny(obj, "priority", fallback = "NORMAL"),
            date = optStringAny(obj, "date", fallback = ""),
            targetAudience = optStringAny(obj, "targetAudience", "target_audience", fallback = "ALL"),
            isActive = optBooleanAny(obj, "isActive", "is_active", fallback = true)
        )
    }

    private fun parsePayment(obj: JSONObject, defaultLibId: String): PaymentEntity {
        return PaymentEntity(
            id = optStringAny(obj, "id", fallback = UUID.randomUUID().toString()),
            libraryId = optStringAny(obj, "libraryId", "library_id", fallback = defaultLibId),
            receiptNumber = optStringAny(obj, "receiptNumber", "receipt_number", fallback = "REC-001"),
            studentId = optStringAny(obj, "studentId", "student_id", fallback = ""),
            studentName = optStringAny(obj, "studentName", "student_name", fallback = ""),
            amount = optDoubleAny(obj, "amount", fallback = 0.0),
            paymentMode = optStringAny(obj, "paymentMode", "payment_mode", fallback = "CASH"),
            date = optStringAny(obj, "date", fallback = ""),
            purpose = optStringAny(obj, "purpose", fallback = "Fee"),
            referenceNumber = optStringAny(obj, "referenceNumber", "reference_number", fallback = ""),
            notes = optStringAny(obj, "notes", fallback = ""),
            remarks = optStringAny(obj, "remarks", fallback = ""),
            period = optStringAny(obj, "period", fallback = ""),
            dueBalance = optDoubleAny(obj, "dueBalance", "due_balance", fallback = 0.0),
            createdAt = optLongAny(obj, "createdAt", "created_at", fallback = System.currentTimeMillis())
        )
    }

    private fun parseAttendance(obj: JSONObject, defaultLibId: String): AttendanceEntity {
        return AttendanceEntity(
            id = optStringAny(obj, "id", fallback = UUID.randomUUID().toString()),
            libraryId = optStringAny(obj, "libraryId", "library_id", fallback = defaultLibId),
            studentId = optStringAny(obj, "studentId", "student_id", fallback = ""),
            studentName = optStringAny(obj, "studentName", "student_name", fallback = ""),
            seatNumber = optStringAny(obj, "seatNumber", "seat_number", fallback = ""),
            hallName = optStringAny(obj, "hallName", "hall_name", fallback = ""),
            shiftName = optStringAny(obj, "shiftName", "shift_name", fallback = ""),
            date = optStringAny(obj, "date", fallback = ""),
            checkInTime = optStringAny(obj, "checkInTime", "check_in_time", fallback = ""),
            checkOutTime = optStringAny(obj, "checkOutTime", "check_out_time", fallback = ""),
            durationMinutes = optIntAny(obj, "durationMinutes", "duration_minutes", fallback = 0),
            status = optStringAny(obj, "status", fallback = "CHECKED_IN"),
            mode = optStringAny(obj, "mode", fallback = "QR"),
            notes = optStringAny(obj, "notes", fallback = ""),
            timestamp = optLongAny(obj, "timestamp", fallback = System.currentTimeMillis())
        )
    }

    private fun parseLibrarySubscription(obj: JSONObject): LibrarySubscriptionEntity {
        return LibrarySubscriptionEntity(
            id = optStringAny(obj, "id", fallback = UUID.randomUUID().toString()),
            libraryId = optStringAny(obj, "libraryId", "library_id", fallback = ""),
            libraryName = optStringAny(obj, "libraryName", "library_name", fallback = ""),
            planId = optStringAny(obj, "planId", "plan_id", fallback = ""),
            planName = optStringAny(obj, "planName", "plan_name", fallback = ""),
            status = optStringAny(obj, "status", fallback = "ACTIVE"),
            startDate = optStringAny(obj, "startDate", "start_date", fallback = ""),
            expiryDate = optStringAny(obj, "expiryDate", "expiry_date", fallback = ""),
            price = optDoubleAny(obj, "price", fallback = 0.0),
            discount = optDoubleAny(obj, "discount", fallback = 0.0),
            maxSeats = optIntAny(obj, "maxSeats", "max_seats", fallback = 100),
            autoRenew = optBooleanAny(obj, "autoRenew", "auto_renew", fallback = false),
            notes = optStringAny(obj, "notes", fallback = ""),
            updatedAt = optLongAny(obj, "updatedAt", "updated_at", fallback = System.currentTimeMillis())
        )
    }

    private fun parseUserSubscription(obj: JSONObject): UserSubscription {
        return UserSubscription(
            id = optStringAny(obj, "id", fallback = UUID.randomUUID().toString()),
            libraryId = optStringAny(obj, "libraryId", "library_id", fallback = ""),
            userId = optStringAny(obj, "userId", "user_id", fallback = ""),
            ownerName = optStringAny(obj, "ownerName", "owner_name", fallback = ""),
            ownerMobile = optStringAny(obj, "ownerMobile", "owner_mobile", fallback = ""),
            ownerEmail = optStringAny(obj, "ownerEmail", "owner_email", fallback = ""),
            libraryName = optStringAny(obj, "libraryName", "library_name", fallback = ""),
            planId = optStringAny(obj, "planId", "plan_id", fallback = ""),
            planName = optStringAny(obj, "planName", "plan_name", fallback = ""),
            amountPaid = optDoubleAny(obj, "amountPaid", "amount_paid", fallback = 0.0),
            billingCycle = optStringAny(obj, "billingCycle", "billing_cycle", fallback = "MONTHLY"),
            status = optStringAny(obj, "status", fallback = "ACTIVE"),
            startDate = optStringAny(obj, "startDate", "start_date", fallback = ""),
            expiryDate = optStringAny(obj, "expiryDate", "expiry_date", fallback = ""),
            paymentMethod = optStringAny(obj, "paymentMethod", "payment_method", fallback = "UPI_MANUAL"),
            paymentReferenceId = optStringAny(obj, "paymentReferenceId", "payment_reference_id", fallback = ""),
            receiptImageUrl = optStringAny(obj, "receiptImageUrl", "receipt_image_url", fallback = ""),
            isVerifiedByAdmin = optBooleanAny(obj, "isVerifiedByAdmin", "is_verified_by_admin", fallback = false),
            notes = optStringAny(obj, "notes", fallback = ""),
            createdAt = optLongAny(obj, "createdAt", "created_at", fallback = System.currentTimeMillis()),
            updatedAt = optLongAny(obj, "updatedAt", "updated_at", fallback = System.currentTimeMillis())
        )
    }

    private fun parseCabin(obj: JSONObject, defaultLibId: String): CabinEntity {
        return CabinEntity(
            id = optStringAny(obj, "id", fallback = UUID.randomUUID().toString()),
            libraryId = optStringAny(obj, "libraryId", "library_id", fallback = defaultLibId),
            cabinNumber = optStringAny(obj, "cabinNumber", "cabin_number", fallback = "C-01"),
            name = optStringAny(obj, "name", "cabin_name", fallback = "Cabin"),
            floor = optStringAny(obj, "floor", fallback = "1st Floor"),
            isAc = optBooleanAny(obj, "isAc", "is_ac", fallback = true),
            isPrivate = optBooleanAny(obj, "isPrivate", "is_private", fallback = true),
            seatCount = optIntAny(obj, "seatCount", "seat_count", fallback = 1),
            monthlyFee = optDoubleAny(obj, "monthlyFee", "monthly_fee", fallback = 2500.0),
            description = optStringAny(obj, "description", fallback = ""),
            isActive = optBooleanAny(obj, "isActive", "is_active", fallback = true)
        )
    }

    private fun parseSection(obj: JSONObject, defaultLibId: String): SectionEntity {
        return SectionEntity(
            id = optStringAny(obj, "id", fallback = UUID.randomUUID().toString()),
            libraryId = optStringAny(obj, "libraryId", "library_id", fallback = defaultLibId),
            name = optStringAny(obj, "name", "section_name", fallback = "Section A"),
            description = optStringAny(obj, "description", fallback = ""),
            floor = optStringAny(obj, "floor", fallback = "Ground Floor"),
            hallId = optStringAny(obj, "hallId", "hall_id", fallback = ""),
            cabinId = optStringAny(obj, "cabinId", "cabin_id", fallback = ""),
            isActive = optBooleanAny(obj, "isActive", "is_active", fallback = true)
        )
    }

    private fun parseBook(obj: JSONObject, defaultLibId: String): PhysicalBookEntity {
        return PhysicalBookEntity(
            id = optStringAny(obj, "id", fallback = UUID.randomUUID().toString()),
            libraryId = optStringAny(obj, "libraryId", "library_id", fallback = defaultLibId),
            title = optStringAny(obj, "title", fallback = "Book Title"),
            author = optStringAny(obj, "author", fallback = "Author"),
            isbn = optStringAny(obj, "isbn", fallback = ""),
            publisher = optStringAny(obj, "publisher", fallback = ""),
            edition = optStringAny(obj, "edition", fallback = ""),
            category = optStringAny(obj, "category", fallback = "General"),
            subject = optStringAny(obj, "subject", fallback = "General"),
            rack = optStringAny(obj, "rack", fallback = "Rack A"),
            shelf = optStringAny(obj, "shelf", fallback = "Shelf 1"),
            accessionNumber = optStringAny(obj, "accessionNumber", "accession_number", fallback = "ACC-001"),
            totalCopies = optIntAny(obj, "totalCopies", "total_copies", fallback = 1),
            availableCopies = optIntAny(obj, "availableCopies", "available_copies", fallback = 1),
            issuedCopies = optIntAny(obj, "issuedCopies", "issued_copies", fallback = 0),
            coverUrl = optStringAny(obj, "coverUrl", "cover_url", fallback = "")
        )
    }

    private fun parseBookIssue(obj: JSONObject, defaultLibId: String): BookIssueEntity {
        return BookIssueEntity(
            id = optStringAny(obj, "id", fallback = UUID.randomUUID().toString()),
            libraryId = optStringAny(obj, "libraryId", "library_id", fallback = defaultLibId),
            bookId = optStringAny(obj, "bookId", "book_id", fallback = ""),
            bookTitle = optStringAny(obj, "bookTitle", "book_title", fallback = ""),
            studentId = optStringAny(obj, "studentId", "student_id", fallback = ""),
            studentName = optStringAny(obj, "studentName", "student_name", fallback = ""),
            studentMobile = optStringAny(obj, "studentMobile", "student_mobile", fallback = ""),
            issueDate = optStringAny(obj, "issueDate", "issue_date", fallback = ""),
            dueDate = optStringAny(obj, "dueDate", "due_date", fallback = ""),
            returnDate = optStringAny(obj, "returnDate", "return_date", fallback = ""),
            fineAmount = optDoubleAny(obj, "fineAmount", "fine_amount", fallback = 0.0),
            finePaid = optBooleanAny(obj, "finePaid", "fine_paid", fallback = false),
            status = optStringAny(obj, "status", fallback = "ISSUED"),
            notes = optStringAny(obj, "notes", fallback = "")
        )
    }

    private fun parseDigitalMaterial(obj: JSONObject, defaultLibId: String): DigitalMaterialEntity {
        return DigitalMaterialEntity(
            id = optStringAny(obj, "id", fallback = UUID.randomUUID().toString()),
            libraryId = optStringAny(obj, "libraryId", "library_id", fallback = defaultLibId),
            title = optStringAny(obj, "title", fallback = "Digital PDF"),
            description = optStringAny(obj, "description", fallback = ""),
            category = optStringAny(obj, "category", fallback = "NCERT"),
            subject = optStringAny(obj, "subject", fallback = "All"),
            exam = optStringAny(obj, "exam", fallback = "All"),
            fileType = optStringAny(obj, "fileType", "file_type", fallback = "PDF"),
            fileSize = optStringAny(obj, "fileSize", "file_size", fallback = "2.5 MB"),
            fileUrl = optStringAny(obj, "fileUrl", "file_url", fallback = ""),
            accessPolicy = optStringAny(obj, "accessPolicy", "access_policy", fallback = "ALL_STUDENTS"),
            allowedGroup = optStringAny(obj, "allowedGroup", "allowed_group", fallback = "All"),
            downloadCount = optIntAny(obj, "downloadCount", "download_count", fallback = 0),
            uploadDate = optStringAny(obj, "uploadDate", "upload_date", fallback = ""),
            isBookmarked = optBooleanAny(obj, "isBookmarked", "is_bookmarked", fallback = false)
        )
    }

    private fun parseExpense(obj: JSONObject, defaultLibId: String): ExpenseEntity {
        return ExpenseEntity(
            id = optStringAny(obj, "id", fallback = UUID.randomUUID().toString()),
            libraryId = optStringAny(obj, "libraryId", "library_id", fallback = defaultLibId),
            category = optStringAny(obj, "category", fallback = "General"),
            amount = optDoubleAny(obj, "amount", fallback = 0.0),
            date = optStringAny(obj, "date", fallback = ""),
            description = optStringAny(obj, "description", fallback = ""),
            paymentMode = optStringAny(obj, "paymentMode", "payment_mode", fallback = "UPI"),
            status = optStringAny(obj, "status", fallback = "PAID"),
            receiptRef = optStringAny(obj, "receiptRef", "receipt_ref", fallback = "")
        )
    }

    private fun parseFine(obj: JSONObject, defaultLibId: String): FineEntity {
        return FineEntity(
            id = optStringAny(obj, "id", fallback = UUID.randomUUID().toString()),
            libraryId = optStringAny(obj, "libraryId", "library_id", fallback = defaultLibId),
            studentId = optStringAny(obj, "studentId", "student_id", fallback = ""),
            studentName = optStringAny(obj, "studentName", "student_name", fallback = ""),
            bookId = optStringAny(obj, "bookId", "book_id", fallback = ""),
            bookTitle = optStringAny(obj, "bookTitle", "book_title", fallback = ""),
            reason = optStringAny(obj, "reason", fallback = "Late Return"),
            amount = optDoubleAny(obj, "amount", fallback = 10.0),
            paid = optBooleanAny(obj, "paid", fallback = false),
            date = optStringAny(obj, "date", fallback = "")
        )
    }

    private fun parseFeedback(obj: JSONObject, defaultLibId: String): FeedbackComplaintEntity {
        return FeedbackComplaintEntity(
            id = optStringAny(obj, "id", fallback = UUID.randomUUID().toString()),
            libraryId = optStringAny(obj, "libraryId", "library_id", fallback = defaultLibId),
            studentId = optStringAny(obj, "studentId", "student_id", fallback = ""),
            studentName = optStringAny(obj, "studentName", "student_name", fallback = ""),
            seatNumber = optStringAny(obj, "seatNumber", "seat_number", fallback = ""),
            type = optStringAny(obj, "type", fallback = "COMPLAINT"),
            subject = optStringAny(obj, "subject", fallback = "Subject"),
            message = optStringAny(obj, "message", fallback = ""),
            status = optStringAny(obj, "status", fallback = "PENDING"),
            reply = optStringAny(obj, "reply", fallback = ""),
            date = optStringAny(obj, "date", fallback = ""),
            resolvedDate = optStringAny(obj, "resolvedDate", "resolved_date", fallback = "")
        )
    }

    private fun parseSubscriptionPlan(obj: JSONObject): SubscriptionPlans {
        return SubscriptionPlans(
            id = optStringAny(obj, "id", fallback = UUID.randomUUID().toString()),
            name = optStringAny(obj, "name", fallback = "Standard Plan"),
            description = optStringAny(obj, "description", fallback = ""),
            price = optDoubleAny(obj, "price", fallback = 999.0),
            durationMonths = optIntAny(obj, "durationMonths", "duration_months", fallback = 1),
            durationDays = optIntAny(obj, "durationDays", "duration_days", fallback = 30),
            durationType = optStringAny(obj, "durationType", "duration_type", fallback = "MONTHS"),
            maxSeats = optIntAny(obj, "maxSeats", "max_seats", fallback = 100),
            features = optStringAny(obj, "features", fallback = ""),
            badge = optStringAny(obj, "badge", fallback = ""),
            discountPercentage = optDoubleAny(obj, "discountPercentage", "discount_percentage", fallback = 0.0),
            upiId = optStringAny(obj, "upiId", "upi_id", fallback = "libdesk.billing@upi"),
            upiPayeeName = optStringAny(obj, "upiPayeeName", "upi_payee_name", fallback = "LibDesk Cloud Subscriptions"),
            supportWhatsApp = optStringAny(obj, "supportWhatsApp", "support_whatsapp", fallback = ""),
            isActive = optBooleanAny(obj, "isActive", "is_active", fallback = true),
            displayOrder = optIntAny(obj, "displayOrder", "display_order", fallback = 1),
            createdAt = optLongAny(obj, "createdAt", "created_at", fallback = System.currentTimeMillis())
        )
    }
}
