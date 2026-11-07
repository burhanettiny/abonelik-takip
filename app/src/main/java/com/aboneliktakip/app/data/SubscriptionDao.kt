package com.aboneliktakip.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<Subscription>>

    @Query("SELECT * FROM subscriptions ORDER BY name COLLATE NOCASE")
    suspend fun getAll(): List<Subscription>

    @Query("SELECT * FROM subscriptions WHERE id = :id")
    suspend fun getById(id: Long): Subscription?

    @Upsert
    suspend fun upsert(subscription: Subscription): Long

    @Insert
    suspend fun insertAll(subscriptions: List<Subscription>)

    @Query("UPDATE subscriptions SET active = :active WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean)

    @Delete
    suspend fun delete(subscription: Subscription)

    @Query("DELETE FROM subscriptions")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(subscriptions: List<Subscription>) {
        deleteAll()
        insertAll(subscriptions)
    }
}
