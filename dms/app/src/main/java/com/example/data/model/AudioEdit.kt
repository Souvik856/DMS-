package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audio_edits")
data class AudioEdit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val reverbAmt: Float = 0.0f,
    val echoDelay: Float = 0.0f,
    val pitchShift: Float = 1.0f,
    val playbackSpeed: Float = 1.0f,
    val gainFactor: Float = 1.0f,
    val waveformPoints: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
