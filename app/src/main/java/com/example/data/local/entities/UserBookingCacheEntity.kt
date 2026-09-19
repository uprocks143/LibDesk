package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room database entity caching user booking and reservation data locally.
 * Enables the Profile Screen and Entrance QR Pass to remain 100% accessible
 * and fully functional even when the user has no active internet connection.
 */
@Entity(tableName = "user_booking_cache")
data class UserBookingCacheEntity(
    @PrimaryKey
    val studentId: String,
    val studentCode: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val seatId: String,
    val seatNumber: String,
    val seatType: String,
    val hallId: String,
    val hallName: String,
    val floor: String,
    val shiftId: String,
    val shiftName: String,
    val shiftTimings: String,
    val planId: String,
    val planName: String,
    val membershipStatus: String,
    val startDate: String,
    val expiryDate: String,
    val rfidQrCode: String,
    val libraryId: String,
    val libraryName: String,
    val libraryAddress: String,
    val lastCheckInTime: String = "",
    val isCheckedIn: Boolean = false,
    val hasAc: Boolean = true,
    val hasPowerSocket: Boolean = true,
    val hasReadingLamp: Boolean = true,
    val hasLocker: Boolean = false,
    val cachedTimestamp: Long = System.currentTimeMillis()
)
