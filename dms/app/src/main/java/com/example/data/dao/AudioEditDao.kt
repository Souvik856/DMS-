package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.AudioEdit
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioEditDao {
    @Query("SELECT * FROM audio_edits ORDER BY timestamp DESC")
    fun getAllEdits(): Flow<List<AudioEdit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEdit(edit: AudioEdit)

    @Delete
    suspend fun deleteEdit(edit: AudioEdit)
}
