package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.ArtistProfile
import com.example.data.model.AudioEdit
import com.example.data.model.SocialPost
import com.example.data.model.Track
import com.example.data.model.UserSubscription
import com.example.data.repository.MusicRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GoogleUserInfo(
    val email: String,
    val name: String,
    val avatarUrl: String
)

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("dms_user_preferences_v1", Context.MODE_PRIVATE)

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("dark_mode", true))
    val isDarkMode = _isDarkMode.asStateFlow()

    private val _googleUser = MutableStateFlow<GoogleUserInfo?>(
        if (prefs.getBoolean("google_logged_in", false)) {
            GoogleUserInfo(
                email = prefs.getString("google_email", "") ?: "",
                name = prefs.getString("google_name", "") ?: "",
                avatarUrl = prefs.getString("google_avatar", "") ?: ""
            )
        } else {
            null
        }
    )
    val googleUser = _googleUser.asStateFlow()

    fun toggleDarkMode() {
        val newMode = !_isDarkMode.value
        _isDarkMode.value = newMode
        prefs.edit().putBoolean("dark_mode", newMode).apply()
    }

    fun loginWithGoogle(email: String, name: String, avatarUrl: String) {
        val user = GoogleUserInfo(email, name, avatarUrl)
        _googleUser.value = user
        prefs.edit()
            .putBoolean("google_logged_in", true)
            .putString("google_email", email)
            .putString("google_name", name)
            .putString("google_avatar", avatarUrl)
            .apply()
    }

    fun logoutGoogle() {
        _googleUser.value = null
        prefs.edit()
            .putBoolean("google_logged_in", false)
            .remove("google_email")
            .remove("google_name")
            .remove("google_avatar")
            .apply()
    }

    private val db = AppDatabase.getDatabase(application)
    private val repository = MusicRepository(
        db.artistProfileDao(),
        db.trackDao(),
        db.socialPostDao(),
        db.audioEditDao(),
        db.subscriptionDao()
    )

    // Expose flows from Repository
    val artistProfile: StateFlow<ArtistProfile?> = repository.artistProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allTracks: StateFlow<List<Track>> = repository.allTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPosts: StateFlow<List<SocialPost>> = repository.allPosts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allEdits: StateFlow<List<AudioEdit>> = repository.allEdits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userSubscription: StateFlow<UserSubscription?> = repository.userSubscription
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Playback Engine using Android MediaPlayer
    private var mediaPlayer: android.media.MediaPlayer? = null

    private val _playingTrack = MutableStateFlow<Track?>(null)
    val playingTrack = _playingTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _playbackPosition = MutableStateFlow(0L)
    val playbackPosition = _playbackPosition.asStateFlow()

    private var playbackJob: Job? = null

    // Secure in-memory Admin authorization state
    private val _isAdminLoggedIn = MutableStateFlow(false)
    val isAdminLoggedIn = _isAdminLoggedIn.asStateFlow()

    // Loading states for skeleton loading visual optimization
    private val _isTracksLoading = MutableStateFlow(true)
    val isTracksLoading = _isTracksLoading.asStateFlow()

    private val _isProfileLoading = MutableStateFlow(true)
    val isProfileLoading = _isProfileLoading.asStateFlow()

    fun triggerTracksRefresh() {
        _isTracksLoading.value = true
        viewModelScope.launch {
            delay(1200)
            _isTracksLoading.value = false
        }
    }

    fun triggerProfileRefresh() {
        _isProfileLoading.value = true
        viewModelScope.launch {
            delay(1200)
            _isProfileLoading.value = false
        }
    }

    fun adminLogin(password: String): Boolean {
        return if (password == "4807") {
            _isAdminLoggedIn.value = true
            true
        } else {
            false
        }
    }

    fun adminLogout() {
        _isAdminLoggedIn.value = false
    }

    // Audio recording synthesizer
    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    private val _recordingDuration = MutableStateFlow(0)
    val recordingDuration = _recordingDuration.asStateFlow()

    private var recordingJob: Job? = null

    // Active synthesis filter variables
    var synthReverb = MutableStateFlow(0.15f)
    var synthEcho = MutableStateFlow(0.0f)
    var synthPitch = MutableStateFlow(1.0f)
    var synthSpeed = MutableStateFlow(1.0f)
    var synthGain = MutableStateFlow(1.0f)

    init {
        // Initial simulated fetching delays for loading skeletons
        viewModelScope.launch {
            delay(1200)
            _isTracksLoading.value = false
        }
        viewModelScope.launch {
            delay(1200)
            _isProfileLoading.value = false
        }

        // Hydrate database with mock essentials if Room is empty on first launch
        viewModelScope.launch {
            repository.artistProfile.collectLatest { profile ->
                if (profile == null) {
                    repository.insertOrUpdateProfile(ArtistProfile())
                }
            }
        }

        viewModelScope.launch {
            repository.userSubscription.collectLatest { sub ->
                if (sub == null) {
                    repository.insertOrUpdateSubscription(UserSubscription())
                }
            }
        }

        viewModelScope.launch {
            repository.allTracks.collectLatest { list ->
                if (list.isEmpty()) {
                    repository.insertTrack(
                        Track(
                            title = "Subliminal Wave",
                            album = "Space Continuum LP",
                            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                            coverUrl = "https://picsum.photos/seed/subliminal/200",
                            sampleUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                            durationMs = 372000,
                            youtubeUrl = "https://youtube.com",
                            rating = 4.8f
                        )
                    )
                    repository.insertTrack(
                        Track(
                            title = "Gravity Void",
                            album = "Ethereal Echoes",
                            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                            coverUrl = "https://picsum.photos/seed/gravity/200",
                            sampleUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                            durationMs = 423000,
                            youtubeUrl = "https://youtube.com",
                            rating = 4.9f
                        )
                    )
                }
            }
        }

        viewModelScope.launch {
            repository.allPosts.collectLatest { list ->
                if (list.isEmpty()) {
                    repository.insertAllPosts(
                        listOf(
                            SocialPost(
                                author = "Souvik Deb",
                                content = "Welcome to DMS - DEB MUSIC STUDIO! Messing around with real-time waveform drawing today. Check out the Custom Pitch Shifter inside Sound Station."
                            ),
                            SocialPost(
                                author = "Alex Mercer",
                                content = "Man, the high-fidelity sound output on Subliminal Wave is pristine. Best electronic companion."
                            ),
                            SocialPost(
                                author = "Lana Stark",
                                content = "Is the Sound Master tier coupon still active? I really want to unlock full 32-bit floating synth filters."
                            )
                        )
                    )
                }
            }
        }
    }

    // Artist Profile Actions
    fun updateArtistProfile(profile: ArtistProfile) {
        viewModelScope.launch {
            repository.insertOrUpdateProfile(profile)
        }
    }

    // Tracks Actions
    fun insertTrack(title: String, album: String, audioUrl: String, coverUrl: String, lyrics: String, youtubeUrl: String) {
        viewModelScope.launch {
            var extractedDuration = 180000L // Default: 3 minutes (180,000 ms)
            if (audioUrl.isNotBlank()) {
                val retriever = android.media.MediaMetadataRetriever()
                try {
                    if (audioUrl.startsWith("http://") || audioUrl.startsWith("https://") || audioUrl.startsWith("content://")) {
                        retriever.setDataSource(getApplication(), android.net.Uri.parse(audioUrl))
                    } else {
                        val path = if (audioUrl.startsWith("file://")) audioUrl.substring(7) else audioUrl
                        retriever.setDataSource(path)
                    }
                    val timeString = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                    if (timeString != null) {
                        extractedDuration = timeString.toLong()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("DurationExtractor", "Could not extract: ${e.message}")
                } finally {
                    try { retriever.release() } catch (ignored: Exception) {}
                }
            }

            repository.insertTrack(
                Track(
                    title = title,
                    album = album,
                    audioUrl = audioUrl,
                    coverUrl = coverUrl,
                    sampleUrl = audioUrl, // Sync for fallback
                    durationMs = extractedDuration,
                    lyrics = lyrics,
                    youtubeUrl = youtubeUrl,
                    isLocal = !audioUrl.startsWith("http")
                )
            )
        }
    }

    fun deleteTrack(track: Track) {
        viewModelScope.launch {
            if (_playingTrack.value?.id == track.id) {
                stopTrack()
            }
            repository.deleteTrack(track)
        }
    }

    // Playback Controls
    fun setPlayingTrack(track: Track) {
        stopTrack()
        _playingTrack.value = track
        _playbackPosition.value = 0L
        playTrack()
    }

    fun playTrack() {
        val track = _playingTrack.value ?: return
        _isPlaying.value = true
        playbackJob?.cancel()

        try {
            if (mediaPlayer == null) {
                mediaPlayer = android.media.MediaPlayer().apply {
                    setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    val source = if (track.audioUrl.isNotBlank()) track.audioUrl else track.sampleUrl
                    if (source.startsWith("http://") || source.startsWith("https://") || source.startsWith("content://")) {
                        setDataSource(getApplication<Application>(), android.net.Uri.parse(source))
                    } else {
                        val path = if (source.startsWith("file://")) source.substring(7) else source
                        setDataSource(path)
                    }
                    prepareAsync()
                    setOnPreparedListener { mp ->
                        mp.start()
                        if (_playbackPosition.value > 0) {
                            mp.seekTo(_playbackPosition.value.toInt())
                        }
                    }
                    setOnCompletionListener {
                        _isPlaying.value = false
                        _playbackPosition.value = 0L
                        _playingTrack.value = null
                        mediaPlayer?.release()
                        mediaPlayer = null
                    }
                    setOnErrorListener { _, what, extra ->
                        android.util.Log.e("MediaPlayer", "Error what:$what extra:$extra")
                        _isPlaying.value = false
                        _playingTrack.value = null
                        mediaPlayer?.release()
                        mediaPlayer = null
                        true
                    }
                }
            } else {
                mediaPlayer?.start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        playbackJob = viewModelScope.launch {
            while (_isPlaying.value) {
                delay(500)
                try {
                    mediaPlayer?.let { mp ->
                        if (mp.isPlaying) {
                            _playbackPosition.value = mp.currentPosition.toLong()
                        }
                    } ?: run {
                        _playbackPosition.value = (_playbackPosition.value + 500).coerceAtMost(track.durationMs)
                    }
                } catch (e: Exception) {
                    // Fallback path
                    _playbackPosition.value = (_playbackPosition.value + 500).coerceAtMost(track.durationMs)
                }
                if (_playbackPosition.value >= track.durationMs) {
                    _isPlaying.value = false
                    _playbackPosition.value = 0L
                    break
                }
            }
        }
    }

    fun pauseTrack() {
        _isPlaying.value = false
        playbackJob?.cancel()
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopTrack() {
        _isPlaying.value = false
        _playbackPosition.value = 0L
        _playingTrack.value = null
        playbackJob?.cancel()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun seekTo(position: Long) {
        val track = _playingTrack.value ?: return
        _playbackPosition.value = position.coerceIn(0L, track.durationMs)
        try {
            mediaPlayer?.seekTo(position.toInt())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Sound Station Recording Actions
    fun startRecording() {
        if (_isRecording.value) return
        _isRecording.value = true
        _recordingDuration.value = 0
        recordingJob = viewModelScope.launch {
            while (_isRecording.value) {
                delay(1000)
                _recordingDuration.value += 1
            }
        }
    }

    fun stopRecording(name: String, waveformPoints: String) {
        _isRecording.value = false
        recordingJob?.cancel()
        val durationSec = _recordingDuration.value
        if (durationSec > 0) {
            viewModelScope.launch {
                repository.insertEdit(
                    AudioEdit(
                        name = name.ifBlank { "Synthesized Clip #${System.currentTimeMillis() % 1000}" },
                        reverbAmt = synthReverb.value,
                        echoDelay = synthEcho.value,
                        pitchShift = synthPitch.value,
                        playbackSpeed = synthSpeed.value,
                        gainFactor = synthGain.value,
                        waveformPoints = waveformPoints
                    )
                )
            }
        }
    }

    fun deleteEditSession(edit: AudioEdit) {
        viewModelScope.launch {
            repository.deleteEdit(edit)
        }
    }

    // Social Media Actions
    fun addPost(author: String, content: String) {
        viewModelScope.launch {
            repository.insertPost(
                SocialPost(
                    author = author.ifBlank { "Fan" },
                    content = content
                )
            )
        }
    }

    fun likePost(post: SocialPost) {
        viewModelScope.launch {
            val updated = post.copy(
                likes = if (post.hasLiked) post.likes - 1 else post.likes + 1,
                hasLiked = !post.hasLiked
            )
            repository.updatePost(updated)
        }
    }

    fun deletePost(postId: Int) {
        viewModelScope.launch {
            repository.deletePostById(postId)
        }
    }

    fun editPost(postId: Int, newContent: String) {
        viewModelScope.launch {
            allPosts.value.find { it.id == postId }?.let { original ->
                repository.updatePost(original.copy(content = newContent))
            }
        }
    }

    // Monetization Subscription Actions
    fun activateSubscription(tier: String, coupon: String) {
        viewModelScope.launch {
            repository.insertOrUpdateSubscription(
                UserSubscription(
                    tier = tier,
                    isActive = true,
                    couponApplied = coupon
                )
            )
        }
    }

    fun cancelSubscription() {
        viewModelScope.launch {
            repository.insertOrUpdateSubscription(
                UserSubscription(
                    tier = "FREE",
                    isActive = false,
                    couponApplied = ""
                )
            )
        }
    }

    // AI Cohort / Co-Producer state flows
    private val _aiResponse = MutableStateFlow("")
    val aiResponse = _aiResponse.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading = _isAiLoading.asStateFlow()

    private val _parsedPreset = MutableStateFlow<Map<String, Float>?>(null)
    val parsedPreset = _parsedPreset.asStateFlow()

    fun queryAiCoProducer(prompt: String) {
        _isAiLoading.value = true
        _aiResponse.value = ""
        _parsedPreset.value = null
        viewModelScope.launch {
            try {
                val result = com.example.ui.api.GeminiManager.askCoProducer(prompt)
                _aiResponse.value = result
                parsePresetFromText(result)
            } catch (e: Exception) {
                _aiResponse.value = "AI Overload error: ${e.localizedMessage ?: e.message}"
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    private fun parsePresetFromText(text: String) {
        try {
            // Find [PRESET: Reverb=X, Echo=Y, Pitch=Z, Speed=W, Gain=K]
            val regex = Regex("\\[PRESET:\\s*Reverb=([\\d.\\-]+),\\s*Echo=([\\d.\\-]+),\\s*Pitch=([\\d.\\-]+),\\s*Speed=([\\d.\\-]+),\\s*Gain=([\\d.\\-]+)\\]")
            val matchResult = regex.find(text)
            if (matchResult != null) {
                val (reverb, echo, pitch, speed, gain) = matchResult.destructured
                _parsedPreset.value = mapOf(
                    "reverb" to (reverb.toFloatOrNull() ?: 0.15f).coerceIn(0.0f, 1.0f),
                    "echo" to (echo.toFloatOrNull() ?: 0.0f).coerceIn(0.0f, 1.0f),
                    "pitch" to (pitch.toFloatOrNull() ?: 1.0f).coerceIn(0.0f, 1.0f),
                    "speed" to (speed.toFloatOrNull() ?: 1.0f).coerceIn(0.0f, 1.0f),
                    "gain" to (gain.toFloatOrNull() ?: 1.0f).coerceIn(0.0f, 1.0f)
                )
            }
        } catch (e: Exception) {
            Log.e("MusicViewModel", "Failed to parse preset: ${e.message}")
        }
    }

    fun applyParsedPreset() {
        val preset = _parsedPreset.value ?: return
        synthReverb.value = preset["reverb"] ?: 0.15f
        synthEcho.value = preset["echo"] ?: 0.0f
        synthPitch.value = preset["pitch"] ?: 1.0f
        synthSpeed.value = preset["speed"] ?: 1.0f
        synthGain.value = preset["gain"] ?: 1.0f
        _parsedPreset.value = null
    }

    fun clearParsedPreset() {
        _parsedPreset.value = null
    }
}
