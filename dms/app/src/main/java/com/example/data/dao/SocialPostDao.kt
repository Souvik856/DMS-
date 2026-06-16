package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.SocialPost
import kotlinx.coroutines.flow.Flow

@Dao
interface SocialPostDao {
    @Query("SELECT * FROM social_posts ORDER BY timestamp DESC")
    fun getAllPosts(): Flow<List<SocialPost>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: SocialPost)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPosts(posts: List<SocialPost>)

    @Update
    suspend fun updatePost(post: SocialPost)

    @Query("DELETE FROM social_posts WHERE id = :postId")
    suspend fun deletePostById(postId: Int)
}
