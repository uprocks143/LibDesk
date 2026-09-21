package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entities.UserBookingCacheEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room Data Access Object for cached user bookings.
 * Follows the Room integration pattern:
 * - Returns Flow for reactive Compose observation
 * - Mark mutations as suspend
 */
@Dao
interface UserBookingCacheDao {

    @Query("SELECT * FROM user_booking_cache WHERE studentId = :studentId LIMIT 1")
    fun getBookingCache(studentId: String): Flow<UserBookingCacheEntity?>

    @Query("SELECT * FROM user_booking_cache WHERE studentId = :studentId LIMIT 1")
    suspend fun getBookingCacheDirect(studentId: String): UserBookingCacheEntity?

    @Query("SELECT * FROM user_booking_cache LIMIT 1")
    fun getAnyBookingCache(): Flow<UserBookingCacheEntity?>

    @Query("SELECT * FROM user_booking_cache")
    fun getAllBookingCaches(): Flow<List<UserBookingCacheEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCache(cache: UserBookingCacheEntity)

    @Query("DELETE FROM user_booking_cache WHERE studentId = :studentId")
    suspend fun deleteCache(studentId: String)

    @Query("DELETE FROM user_booking_cache")
    suspend fun clearAllCache()
}
