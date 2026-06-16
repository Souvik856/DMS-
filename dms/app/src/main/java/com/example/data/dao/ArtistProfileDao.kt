package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ArtistProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface ArtistProfileDao {
    @Query("SELECT * FROM artist_profile WHERE id = 1 LIMIT 1")
    fun getProfile(): Flow<ArtistProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: ArtistProfile)
}
