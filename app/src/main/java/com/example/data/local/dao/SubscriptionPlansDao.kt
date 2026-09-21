package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.SubscriptionPlans
import com.example.data.local.entities.UserSubscription
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for SubscriptionPlans in the Room Database.
 */
@Dao
interface SubscriptionPlansDao {
    @Query("SELECT * FROM subscription_plans WHERE isActive = 1 ORDER BY displayOrder ASC, price ASC")
    fun getActivePlans(): Flow<List<SubscriptionPlans>>

    @Query("SELECT * FROM subscription_plans ORDER BY displayOrder ASC, price ASC")
    fun getAllPlans(): Flow<List<SubscriptionPlans>>

    @Query("SELECT * FROM subscription_plans WHERE id = :id LIMIT 1")
    fun getPlanById(id: String): Flow<SubscriptionPlans?>

    @Query("SELECT * FROM subscription_plans WHERE id = :id LIMIT 1")
    suspend fun getPlanByIdDirect(id: String): SubscriptionPlans?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: SubscriptionPlans)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlans(plans: List<SubscriptionPlans>)

    @Update
    suspend fun updatePlan(plan: SubscriptionPlans)

    @Delete
    suspend fun deletePlan(plan: SubscriptionPlans)

    @Query("UPDATE subscription_plans SET upiId = :upiId, upiPayeeName = :payeeName")
    suspend fun updateAllPlansUpi(upiId: String, payeeName: String)

    @Query("DELETE FROM subscription_plans")
    suspend fun clearAllPlans()
}

/**
 * Data Access Object for UserSubscription table tracking active library owner memberships.
 */
@Dao
interface UserSubscriptionDao {
    @Query("SELECT * FROM user_subscriptions ORDER BY updatedAt DESC")
    fun getAllUserSubscriptions(): Flow<List<UserSubscription>>

    @Query("SELECT * FROM user_subscriptions WHERE libraryId = :libraryId ORDER BY updatedAt DESC LIMIT 1")
    fun getUserSubscriptionForLibrary(libraryId: String): Flow<UserSubscription?>

    @Query("SELECT * FROM user_subscriptions WHERE libraryId = :libraryId ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getUserSubscriptionForLibraryDirect(libraryId: String): UserSubscription?

    @Query("SELECT * FROM user_subscriptions WHERE libraryId = :libraryId AND status = 'ACTIVE' ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getActiveSubscriptionForLibrary(libraryId: String): UserSubscription?

    @Query("SELECT * FROM user_subscriptions WHERE userId = :userId ORDER BY updatedAt DESC")
    fun getUserSubscriptionsByUserId(userId: String): Flow<List<UserSubscription>>

    @Query("SELECT * FROM user_subscriptions WHERE id = :id LIMIT 1")
    suspend fun getUserSubscriptionById(id: String): UserSubscription?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserSubscription(subscription: UserSubscription)

    @Update
    suspend fun updateUserSubscription(subscription: UserSubscription)

    @Delete
    suspend fun deleteUserSubscription(subscription: UserSubscription)

    @Query("DELETE FROM user_subscriptions WHERE libraryId = :libraryId")
    suspend fun deleteSubscriptionsByLibraryId(libraryId: String)
}
