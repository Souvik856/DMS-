package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_subscriptions")
data class UserSubscription(
    @PrimaryKey val id: Int = 1,
    val tier: String = "FREE",
    val isActive: Boolean = false,
    val purchaseDate: Long = System.currentTimeMillis(),
    val couponApplied: String = ""
)
