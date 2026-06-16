package com.example.data.repository

import com.example.data.dao.ArtistProfileDao
import com.example.data.dao.TrackDao
import com.example.data.dao.SocialPostDao
import com.example.data.dao.AudioEditDao
import com.example.data.dao.SubscriptionDao
import com.example.data.model.ArtistProfile
import com.example.data.model.Track
import com.example.data.model.SocialPost
import com.example.data.model.AudioEdit
import com.example.data.model.UserSubscription
import kotlinx.coroutines.flow.Flow

class MusicRepository(
    private val artistProfileDao: ArtistProfileDao,
    private val trackDao: TrackDao,
    private val socialPostDao: SocialPostDao,
    private val audioEditDao: AudioEditDao,
    private val subscriptionDao: SubscriptionDao
) {
    // Artist profile
    val artistProfile: Flow<ArtistProfile?> = artistProfileDao.getProfile()
    suspend fun insertOrUpdateProfile(profile: ArtistProfile) = artistProfileDao.insertOrUpdateProfile(profile)

    // Tracks
    val allTracks: Flow<List<Track>> = trackDao.getAllTracks()
    suspend fun insertTrack(track: Track) = trackDao.insertTrack(track)
    suspend fun deleteTrack(track: Track) = trackDao.deleteTrack(track)
    suspend fun clearTracks() = trackDao.clearTracks()

    // Social Posts
    val allPosts: Flow<List<SocialPost>> = socialPostDao.getAllPosts()
    suspend fun insertPost(post: SocialPost) = socialPostDao.insertPost(post)
    suspend fun insertAllPosts(posts: List<SocialPost>) = socialPostDao.insertAllPosts(posts)
    suspend fun updatePost(post: SocialPost) = socialPostDao.updatePost(post)
    suspend fun deletePostById(postId: Int) = socialPostDao.deletePostById(postId)

    // Audio Edits (Sound Station Lab Sessions)
    val allEdits: Flow<List<AudioEdit>> = audioEditDao.getAllEdits()
    suspend fun insertEdit(edit: AudioEdit) = audioEditDao.insertEdit(edit)
    suspend fun deleteEdit(edit: AudioEdit) = audioEditDao.deleteEdit(edit)

    // Subscriptions
    val userSubscription: Flow<UserSubscription?> = subscriptionDao.getSubscription()
    suspend fun insertOrUpdateSubscription(subscription: UserSubscription) = subscriptionDao.insertOrUpdateSubscription(subscription)
}
