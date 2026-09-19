package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room database entity storing SaaS subscription plan details for libraries.
 */
@Entity(tableName = "subscription_plans")
data class SubscriptionPlans(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    val price: Double,
    val durationMonths: Int = 1,
    val maxSeats: Int = 100,
    val features: String = "", // Comma or newline-separated feature items
    val badge: String = "", // e.g. "MOST POPULAR", "BEST VALUE", "STARTER"
    val discountPercentage: Double = 0.0,
    val upiId: String = "libdesk.billing@upi",
    val upiPayeeName: String = "LibDesk Subscriptions",
    val supportWhatsApp: String = "",
    val isActive: Boolean = true,
    val displayOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Room database entity to track active library owner memberships and manual payments.
 */
@Entity(tableName = "user_subscriptions")
data class UserSubscription(
    @PrimaryKey val id: String,
    val libraryId: String,
    val userId: String = "", // Library owner user ID or email
    val ownerName: String = "",
    val ownerMobile: String = "",
    val ownerEmail: String = "",
    val libraryName: String = "",
    val planId: String,
    val planName: String,
    val amountPaid: Double,
    val billingCycle: String = "MONTHLY", // MONTHLY, QUARTERLY, ANNUAL
    val status: String = "ACTIVE", // ACTIVE, PENDING_VERIFICATION, EXPIRED, CANCELLED
    val startDate: String,
    val expiryDate: String,
    val paymentMethod: String = "UPI_MANUAL",
    val paymentReferenceId: String = "", // UTR / UPI Transaction Reference Number
    val receiptImageUrl: String = "",
    val isVerifiedByAdmin: Boolean = false,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
