package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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

@Database(
    entities = [
        ArtistProfile::class,
        Track::class,
        SocialPost::class,
        AudioEdit::class,
        UserSubscription::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun artistProfileDao(): ArtistProfileDao
    abstract fun trackDao(): TrackDao
    abstract fun socialPostDao(): SocialPostDao
    abstract fun audioEditDao(): AudioEditDao
    abstract fun subscriptionDao(): SubscriptionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dms_database"
                )
                .fallbackToDestructiveMigration() // Drop and recreate tables if schematic shifts occur
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
