package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "artist_profile")
data class ArtistProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "Souvik Deb",
    val bio: String = "Sound designer, multi-instrumentalist, and electronic music producer exploring cinematic soundscapes. Reimagining modern music workflows directly from the studio deck.",
    val avatarUrl: String = "",
    val bannerUrl: String = "",
    val genres: String = "Cinematic Electronic / Sound Design",
    val spotifyUrl: String = "",
    val youtubeUrl: String = "",
    val instagramUrl: String = "",
    val twitterUrl: String = ""
)
