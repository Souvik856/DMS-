package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class Track(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val album: String,
    val audioUrl: String = "",
    val coverUrl: String = "",
    val sampleUrl: String = "",
    val durationMs: Long = 180000, // 3 minutes default
    val lyrics: String = "",
    val youtubeUrl: String = "",
    val rating: Float = 0.0f,
    val isLocal: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
