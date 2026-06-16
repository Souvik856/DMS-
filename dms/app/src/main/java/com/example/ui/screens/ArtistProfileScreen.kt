package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ArtistProfile
import com.example.data.model.SocialPost
import com.example.ui.theme.*
import com.example.ui.viewmodel.MusicViewModel
import com.example.R

fun copyUriToInternalStorage(context: Context, uri: Uri, fileName: String): Uri? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val file = java.io.File(context.filesDir, fileName)
        val outputStream = java.io.FileOutputStream(file)
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        Uri.fromFile(file)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun getCoilModel(url: String): Any {
    if (url.isBlank()) return ""
    return if (url.startsWith("file://")) {
        java.io.File(url.substring(7))
    } else if (url.startsWith("/")) {
        java.io.File(url)
    } else {
        url
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistProfileScreen(
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("dms_prefs", Context.MODE_PRIVATE)

    val intentLauncher = { url: String ->
        try {
            if (url.contains("@") && !url.contains("/") && !url.startsWith("http://") && !url.startsWith("https://")) {
                val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:$url")
                }
                context.startActivity(intent)
            } else {
                val parsedUri = Uri.parse(
                    if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
                )
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, parsedUri)
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Could not launch: $url", Toast.LENGTH_SHORT).show()
        }
    }

    val profileState by viewModel.artistProfile.collectAsState()
    val allPosts by viewModel.allPosts.collectAsState()
    val isAdminLoggedIn by viewModel.isAdminLoggedIn.collectAsState()
    val googleUser by viewModel.googleUser.collectAsState()
    val isProfileLoading by viewModel.isProfileLoading.collectAsState()

    var showGoogleSimDialog by remember { mutableStateOf(false) }
    var simNameInput by remember { mutableStateOf("") }
    var simEmailInput by remember { mutableStateOf("") }

    val profile = profileState ?: ArtistProfile()

    // Geometry cropping controls states
    var avatarZoom by remember { mutableStateOf(prefs.getFloat("avatar_zoom", 1.0f)) }
    var avatarPanX by remember { mutableStateOf(prefs.getFloat("avatar_pan_x", 0f)) }
    var avatarPanY by remember { mutableStateOf(prefs.getFloat("avatar_pan_y", 0f)) }
    var avatarRotate by remember { mutableStateOf(prefs.getFloat("avatar_rotate", 0f)) }

    var bannerZoom by remember { mutableStateOf(prefs.getFloat("banner_zoom", 1.0f)) }
    var bannerPanX by remember { mutableStateOf(prefs.getFloat("banner_pan_x", 0f)) }
    var bannerPanY by remember { mutableStateOf(prefs.getFloat("banner_pan_y", 0f)) }
    var bannerRotate by remember { mutableStateOf(prefs.getFloat("banner_rotate", 0f)) }

    // Forms editing states
    var editName by remember { mutableStateOf(profile.name) }
    var editBio by remember { mutableStateOf(profile.bio) }
    var editGenres by remember { mutableStateOf(profile.genres) }
    var editSpotify by remember { mutableStateOf(profile.spotifyUrl) }
    var editYoutube by remember { mutableStateOf(profile.youtubeUrl) }
    var editInstagram by remember { mutableStateOf(profile.instagramUrl) }
    var editTwitter by remember { mutableStateOf(profile.twitterUrl) }
    var editAvatarUrl by remember { mutableStateOf(profile.avatarUrl) }
    var editBannerUrl by remember { mutableStateOf(profile.bannerUrl) }

    LaunchedEffect(profileState) {
        profileState?.let {
            editName = it.name
            editBio = it.bio
            editGenres = it.genres
            editSpotify = it.spotifyUrl
            editYoutube = it.youtubeUrl
            editInstagram = it.instagramUrl
            editTwitter = it.twitterUrl
            editAvatarUrl = it.avatarUrl
            editBannerUrl = it.bannerUrl
        }
    }

    // Media launchers with internal copying
    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val copiedUri = copyUriToInternalStorage(context, it, "avatar_${System.currentTimeMillis()}.jpg")
            if (copiedUri != null) {
                editAvatarUrl = copiedUri.toString()
                Toast.makeText(context, "Local avatar image set!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to copy avatar!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val bannerPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val copiedUri = copyUriToInternalStorage(context, it, "banner_${System.currentTimeMillis()}.jpg")
            if (copiedUri != null) {
                editBannerUrl = copiedUri.toString()
                Toast.makeText(context, "Local banner image set!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to copy banner!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Creator desk song register states & distinct pickers
    var songTitle by remember { mutableStateOf("") }
    var songAlbum by remember { mutableStateOf("") }
    var songAudioUrl by remember { mutableStateOf("") }
    var songCoverUrl by remember { mutableStateOf("") }
    var songLyrics by remember { mutableStateOf("") }
    var songYoutubeUrl by remember { mutableStateOf("") }

    val songAudioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val copiedUri = copyUriToInternalStorage(context, it, "song_audio_${System.currentTimeMillis()}.mp3")
            if (copiedUri != null) {
                songAudioUrl = copiedUri.toString()
                Toast.makeText(context, "Local song audio copied & set successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to copy song audio!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val songCoverPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val copiedUri = copyUriToInternalStorage(context, it, "song_cover_${System.currentTimeMillis()}.jpg")
            if (copiedUri != null) {
                songCoverUrl = copiedUri.toString()
                Toast.makeText(context, "Local cover art copied & set successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to copy cover art!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var songZoom by remember { mutableStateOf(prefs.getFloat("song_zoom", 1.0f)) }
    var songPanX by remember { mutableStateOf(prefs.getFloat("song_pan_x", 0f)) }
    var songPanY by remember { mutableStateOf(prefs.getFloat("song_pan_y", 0f)) }
    var songRotate by remember { mutableStateOf(prefs.getFloat("song_rotate", 0f)) }

    var showCreatorDesk by remember { mutableStateOf(false) }
    var newPostContent by remember { mutableStateOf("") }

    // Admin Secure Password Prompt Modal States
    var showLoginDialog by remember { mutableStateOf(false) }
    var adminPasswordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf(false) }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(600, easing = FastOutSlowInEasing)) +
                slideInVertically(
                    initialOffsetY = { it / 6 },
                    animationSpec = tween(600, easing = FastOutSlowInEasing)
                )
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = DMSBackground,
            topBar = {
                TopAppBar(
                title = { Text("ARTIST MODULE", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = DMSTextPrimary)
                    }
                },
                actions = {
                    if (isAdminLoggedIn) {
                        IconButton(
                            onClick = { showCreatorDesk = !showCreatorDesk },
                            modifier = Modifier.testTag("btn_creator_desk_toggle")
                        ) {
                            Icon(
                                imageVector = if (showCreatorDesk) Icons.Default.Close else Icons.Default.AdminPanelSettings,
                                contentDescription = "Console",
                                tint = DMSSecondary
                            )
                        }
                        IconButton(
                            onClick = {
                                viewModel.adminLogout()
                                showCreatorDesk = false
                                Toast.makeText(context, "Console session terminated safely.", Toast.LENGTH_SHORT).show()
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Terminal exit",
                                tint = DMSError
                            )
                        }
                    } else {
                        IconButton(
                            onClick = { showLoginDialog = true },
                            modifier = Modifier.testTag("btn_creator_desk_toggle")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Lock",
                                tint = DMSTextSecondary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DMSBackground)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Profile Banner & Avatar Preview Section
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    // Banner Image with Crops Applied
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .background(DMSSurfaceVariant)
                    ) {
                        AsyncImage(
                            model = if (editBannerUrl.isNotBlank()) getCoilModel(editBannerUrl) else R.drawable.img_deb_banner,
                            contentDescription = "Artist banner",
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = bannerZoom
                                    scaleY = bannerZoom
                                    translationX = bannerPanX
                                    translationY = bannerPanY
                                    rotationZ = bannerRotate
                                    clip = true
                                },
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Avatar Circle overlapping banner with Crops Applied
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 24.dp)
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(DMSSurface)
                            .border(2.dp, DMSPrimary, CircleShape)
                    ) {
                        AsyncImage(
                            model = if (editAvatarUrl.isNotBlank()) getCoilModel(editAvatarUrl) else R.drawable.img_deb_avatar,
                            contentDescription = "Artist avatar",
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = avatarZoom
                                    scaleY = avatarZoom
                                    translationX = avatarPanX
                                    translationY = avatarPanY
                                    rotationZ = avatarRotate
                                    clip = true
                                },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            // High-End Creator settings desk (Form Fields)
            item {
                AnimatedVisibility(
                    visible = showCreatorDesk,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(DMSSurface)
                            .border(1.dp, DMSPrimary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            "DEVELOPER CORE: PROFILE & GEOMETRY DECK",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            color = DMSPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Text Field Inputs
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Artist Brand Name", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("edit_artist_name"),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DMSPrimary, unfocusedBorderColor = DMSBorder)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = editBio,
                            onValueChange = { editBio = it },
                            label = { Text("Professional Biography", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth().testTag("edit_artist_bio"),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DMSPrimary, unfocusedBorderColor = DMSBorder)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = editGenres,
                            onValueChange = { editGenres = it },
                            label = { Text("Genres (Separated)", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DMSPrimary, unfocusedBorderColor = DMSBorder)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = editSpotify,
                            onValueChange = { editSpotify = it },
                            label = { Text("Spotify Artist Page Link", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DMSPrimary, unfocusedBorderColor = DMSBorder)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = editYoutube,
                            onValueChange = { editYoutube = it },
                            label = { Text("YouTube Channel Link", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DMSPrimary, unfocusedBorderColor = DMSBorder)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = editInstagram,
                            onValueChange = { editInstagram = it },
                            label = { Text("Instagram Profile Link", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DMSPrimary, unfocusedBorderColor = DMSBorder)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = editTwitter,
                            onValueChange = { editTwitter = it },
                            label = { Text("Studio Support Email Address", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DMSPrimary, unfocusedBorderColor = DMSBorder)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { avatarPicker.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = DMSSurfaceVariant),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("AVATAR PIC", fontSize = 10.sp, color = DMSPrimary)
                            }
                            Button(
                                onClick = { bannerPicker.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = DMSSurfaceVariant),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("BANNER PIC", fontSize = 10.sp, color = DMSSecondary)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Image Crop controls
                        ImageGeometrySection(
                            title = "AVATAR POSITION GEOMETRY",
                            zoom = avatarZoom,
                            onZoomChange = { avatarZoom = it },
                            panX = avatarPanX,
                            onPanXChange = { avatarPanX = it },
                            panY = avatarPanY,
                            onPanYChange = { avatarPanY = it },
                            rotate = avatarRotate,
                            onRotateChange = { avatarRotate = it }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        ImageGeometrySection(
                            title = "BANNER POSITION GEOMETRY",
                            zoom = bannerZoom,
                            onZoomChange = { bannerZoom = it },
                            panX = bannerPanX,
                            onPanXChange = { bannerPanX = it },
                            panY = bannerPanY,
                            onPanYChange = { bannerPanY = it },
                            rotate = bannerRotate,
                            onRotateChange = { bannerRotate = it }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Trigger DB persistence update
                        Button(
                            onClick = {
                                val updated = profile.copy(
                                    name = editName.ifBlank { "Souvik Deb" },
                                    bio = editBio.ifBlank { "" },
                                    genres = editGenres.ifBlank { "" },
                                    avatarUrl = editAvatarUrl,
                                    bannerUrl = editBannerUrl,
                                    spotifyUrl = editSpotify,
                                    youtubeUrl = editYoutube,
                                    instagramUrl = editInstagram,
                                    twitterUrl = editTwitter
                                )
                                viewModel.updateArtistProfile(updated)

                                // Persist scaling geometry inside prefs
                                prefs.edit()
                                    .putFloat("avatar_zoom", avatarZoom)
                                    .putFloat("avatar_pan_x", avatarPanX)
                                    .putFloat("avatar_pan_y", avatarPanY)
                                    .putFloat("avatar_rotate", avatarRotate)
                                    .putFloat("banner_zoom", bannerZoom)
                                    .putFloat("banner_pan_x", bannerPanX)
                                    .putFloat("banner_pan_y", bannerPanY)
                                    .putFloat("banner_rotate", bannerRotate)
                                    .apply()

                                Toast.makeText(context, "Artist specifications committed!", Toast.LENGTH_SHORT).show()
                                showCreatorDesk = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DMSPrimary),
                            modifier = Modifier.fillMaxWidth().testTag("btn_save_artist_profile")
                        ) {
                            Text("SAVE SPECIFICATIONS", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = DMSBorder)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Register track sub-form
                        Text(
                            "CREATOR DESK: AUDIO DEMO COVERS",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            color = DMSSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = songTitle,
                            onValueChange = { songTitle = it },
                            label = { Text("Audio Title", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_song_title"),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DMSSecondary, unfocusedBorderColor = DMSBorder)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = songAlbum,
                            onValueChange = { songAlbum = it },
                            label = { Text("Album Grouping", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DMSSecondary, unfocusedBorderColor = DMSBorder)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Audio Selection Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { songAudioPicker.launch("audio/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = DMSSurfaceVariant),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.AudioFile, "Audio", modifier = Modifier.size(14.dp), tint = DMSSecondary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("SELECT AUDIO", fontSize = 10.sp, color = DMSSecondary)
                            }
                            Button(
                                onClick = { songCoverPicker.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = DMSSurfaceVariant),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Image, "Cover", modifier = Modifier.size(14.dp), tint = DMSTertiary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("SELECT COVER", fontSize = 10.sp, color = DMSTertiary)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))

                        // Visual feedback of what files are selected
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DMSSurfaceVariant)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = if (songAudioUrl.isNotBlank()) "🟢 Audio: ${songAudioUrl.takeLast(35)}" else "🔴 Audio: No local file loaded (paste link instead)",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (songAudioUrl.isNotBlank()) DMSSecondary else DMSTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (songCoverUrl.isNotBlank()) "🟢 Cover: ${songCoverUrl.takeLast(35)}" else "🔴 Cover: No image file loaded (paste link instead)",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (songCoverUrl.isNotBlank()) DMSTertiary else DMSTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        // Backup paste URL fields in case user prefers typing links
                        OutlinedTextField(
                            value = songAudioUrl,
                            onValueChange = { songAudioUrl = it },
                            label = { Text("Audio Stream URL / File Path", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DMSSecondary, unfocusedBorderColor = DMSBorder)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = songCoverUrl,
                            onValueChange = { songCoverUrl = it },
                            label = { Text("Cover Art Image URL / File Path", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DMSSecondary, unfocusedBorderColor = DMSBorder)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (songTitle.isBlank() || songAlbum.isBlank()) {
                                    Toast.makeText(context, "Title and Album are required!", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.insertTrack(
                                        title = songTitle,
                                        album = songAlbum,
                                        audioUrl = songAudioUrl,
                                        coverUrl = songCoverUrl,
                                        lyrics = songLyrics,
                                        youtubeUrl = songYoutubeUrl
                                    )
                                    // Save geometry settings for cover
                                    prefs.edit()
                                        .putFloat("song_zoom", songZoom)
                                        .putFloat("song_pan_x", songPanX)
                                        .putFloat("song_pan_y", songPanY)
                                        .putFloat("song_rotate", songRotate)
                                        .apply()

                                    songTitle = ""
                                    songAlbum = ""
                                    songAudioUrl = ""
                                    songCoverUrl = ""
                                    Toast.makeText(context, "High-Fidelity Track Registered!", Toast.LENGTH_SHORT).show()
                                    showCreatorDesk = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DMSSecondary),
                            modifier = Modifier.fillMaxWidth().testTag("btn_add_track")
                        ) {
                            Text("REGISTER AUDIO DEMO WITH COVER", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Clean Artist Details (Static Bio Display)
            item {
                if (isProfileLoading) {
                    ProfileSkeletonLoader()
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = profile.name,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = DMSTextPrimary
                            )
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified Artist",
                                tint = Color(0xFF29B6F6),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "VERIFIED DMS PRODUCER",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF29B6F6),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF29B6F6).copy(alpha = 0.12f))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = profile.genres,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = DMSPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = profile.bio,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = DMSTextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        SocialMediaRow(profile = profile, onUrlClick = intentLauncher)
                    }
                }
            }

            // Fan Interactive Social Connect Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text("STUDIO CONNECT FEED", style = MaterialTheme.typography.titleMedium, color = DMSTextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (googleUser == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DMSSurface)
                                .border(1.dp, DMSBorder, RoundedCornerShape(12.dp))
                                .clickable { showGoogleSimDialog = true }
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("G", fontWeight = FontWeight.Black, color = Color(0xFF4285F4), fontSize = 14.sp)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("SIGN IN WITH GOOGLE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DMSTextPrimary)
                                    Text("Sync credentials, save comments and claim verified badges.", fontSize = 9.sp, color = DMSTextSecondary)
                                }
                                Icon(Icons.Default.ChevronRight, "Go", tint = DMSTextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DMSPrimary.copy(alpha = 0.08f))
                                .border(1.dp, DMSPrimary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(DMSPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = googleUser!!.name.take(1).uppercase(),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                                Text("Signed in as ${googleUser!!.name} (${googleUser!!.email})", fontSize = 9.sp, color = DMSTextPrimary, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = "SIGN OUT",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = DMSError,
                                modifier = Modifier
                                    .clickable { viewModel.logoutGoogle() }
                                    .padding(4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Connect Form Input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newPostContent,
                            onValueChange = { newPostContent = it },
                            placeholder = { Text(if (googleUser == null) "🔒 Please sign in with Google to post..." else "Broadcast feedback to fan forum...", fontSize = 11.sp, color = DMSTextSecondary) },
                            enabled = googleUser != null,
                            modifier = Modifier.weight(1f).testTag("input_fan_post"),
                            maxLines = 2,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DMSPrimary, 
                                unfocusedBorderColor = DMSBorder,
                                disabledBorderColor = DMSBorder.copy(alpha = 0.4f)
                            )
                        )
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (googleUser != null && newPostContent.isNotBlank()) DMSPrimary else DMSBorder)
                                .clickable(enabled = googleUser != null) {
                                    if (newPostContent.isNotBlank()) {
                                        viewModel.addPost(googleUser?.name ?: "Fan", newPostContent)
                                        newPostContent = ""
                                        Toast.makeText(context, "Feedback broadcasted!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .testTag("btn_submit_post"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Send, "Send", tint = if (googleUser != null) Color.Black else DMSTextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Feed posts listing
            if (isProfileLoading) {
                items(3) {
                    PostSkeletonCard()
                }
            } else if (allPosts.isEmpty()) {
                item {
                    Text(
                        "No discussions logged. Be the first to start the feed!",
                        fontSize = 11.sp,
                        color = DMSTextSecondary,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            } else {
                items(allPosts, key = { it.id }) { post ->
                    val representsMe = (googleUser != null && post.author == googleUser?.name)
                    val isSouvikPost = (post.author == "Souvik Deb")
                    val canEdit = isAdminLoggedIn || (representsMe && !isSouvikPost)
                    val canDelete = isAdminLoggedIn || (representsMe && !isSouvikPost)

                    SocialCommentCard(
                        post = post,
                        canEdit = canEdit,
                        canDelete = canDelete,
                        onLike = { viewModel.likePost(post) },
                        onDelete = { viewModel.deletePost(post.id) },
                        onEdit = { revised -> viewModel.editPost(post.id, revised) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

    if (showGoogleSimDialog) {
        AlertDialog(
            onDismissRequest = { showGoogleSimDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("G", fontWeight = FontWeight.Black, color = Color(0xFF4285F4), fontSize = 11.sp)
                    }
                    Text("GOOGLE CONNECT", fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = DMSTextPrimary)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Select a simulated account or register custom details to sign-in instantly.", fontSize = 11.sp, color = DMSTextSecondary)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Button(
                                onClick = {
                                    viewModel.loginWithGoogle("debs4807@gmail.com", "Deb Music Fan", "")
                                    showGoogleSimDialog = false
                                    Toast.makeText(context, "Logged in as Deb Music Fan", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DMSSurfaceVariant),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("DEBS4807", fontSize = 8.sp, color = DMSPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Button(
                                onClick = {
                                    viewModel.loginWithGoogle("synthwave@gmail.com", "Synth Waver", "")
                                    showGoogleSimDialog = false
                                    Toast.makeText(context, "Logged in as Synth Waver", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DMSSurfaceVariant),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("SYNTH WAVE", fontSize = 8.sp, color = DMSSecondary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Divider(color = DMSBorder)
                    
                    OutlinedTextField(
                        value = simNameInput,
                        onValueChange = { simNameInput = it },
                        label = { Text("Display Name", fontSize = 10.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DMSPrimary)
                    )
                    OutlinedTextField(
                        value = simEmailInput,
                        onValueChange = { simEmailInput = it },
                        label = { Text("Email Address", fontSize = 10.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DMSPrimary)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalName = simNameInput.ifBlank { "Custom Studio User" }
                        val finalEmail = simEmailInput.ifBlank { "user@gmail.com" }
                        viewModel.loginWithGoogle(finalEmail, finalName, "")
                        showGoogleSimDialog = false
                        Toast.makeText(context, "Logged in as $finalName", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DMSPrimary)
                ) {
                    Text("SIGN IN", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoogleSimDialog = false }) {
                    Text("CLOSE", color = DMSTextSecondary)
                }
            }
        )
    }

    if (showLoginDialog) {
        AlertDialog(
            onDismissRequest = { showLoginDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.VerifiedUser, "Auth", tint = DMSSecondary)
                    Text("SECURE CONSOLE GATEWAY", fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = DMSSecondary)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Enter the master security credential key to access high-fidelity recording registers and developer decks.",
                        fontSize = 11.sp,
                        color = DMSTextSecondary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = adminPasswordInput,
                        onValueChange = {
                            adminPasswordInput = it
                            loginError = false
                        },
                        label = { Text("Console Authorization Password", fontSize = 11.sp) },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle master key visibility",
                                    tint = DMSTextSecondary
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DMSSecondary),
                        modifier = Modifier.fillMaxWidth().testTag("password_input")
                    )
                    if (loginError) {
                        Text(
                            text = "INVALID CONSOLE KEY. AUTHENTICATION FAILURE.",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = DMSError,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val authenticated = viewModel.adminLogin(adminPasswordInput)
                        if (authenticated) {
                            showLoginDialog = false
                            adminPasswordInput = ""
                            loginError = false
                            showCreatorDesk = true
                            Toast.makeText(context, "AUTHENTICATION GRANTED.", Toast.LENGTH_SHORT).show()
                        } else {
                            loginError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DMSSecondary)
                ) {
                    Text("AUTHORIZE", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLoginDialog = false }) {
                    Text("CANCEL", color = DMSTextSecondary)
                }
            },
            containerColor = DMSSurface
        )
    }
}

@Composable
fun ImageGeometrySection(
    title: String,
    zoom: Float,
    onZoomChange: (Float) -> Unit,
    panX: Float,
    onPanXChange: (Float) -> Unit,
    panY: Float,
    onPanYChange: (Float) -> Unit,
    rotate: Float,
    onRotateChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DMSSurfaceVariant)
            .padding(10.dp)
    ) {
        Text(title, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = DMSTextSecondary)
        Spacer(modifier = Modifier.height(6.dp))

        // Zoom Slider
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Zoom (${String.format("%.1f", zoom)}x)", fontSize = 9.sp, modifier = Modifier.width(75.dp), color = DMSTextPrimary)
            Slider(
                value = zoom,
                onValueChange = onZoomChange,
                valueRange = 0.5f..3.0f,
                colors = SliderDefaults.colors(thumbColor = DMSPrimary, activeTrackColor = DMSPrimary),
                modifier = Modifier.weight(1f)
            )
        }

        // Pan X
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Pan X (${zoom.toInt()}px)", fontSize = 9.sp, modifier = Modifier.width(75.dp), color = DMSTextPrimary)
            Slider(
                value = panX,
                onValueChange = onPanXChange,
                valueRange = -200f..200f,
                colors = SliderDefaults.colors(thumbColor = DMSSecondary, activeTrackColor = DMSSecondary),
                modifier = Modifier.weight(1f)
            )
        }

        // Pan Y
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Pan Y (${zoom.toInt()}px)", fontSize = 9.sp, modifier = Modifier.width(75.dp), color = DMSTextPrimary)
            Slider(
                value = panY,
                onValueChange = onPanYChange,
                valueRange = -200f..200f,
                colors = SliderDefaults.colors(thumbColor = DMSSecondary, activeTrackColor = DMSSecondary),
                modifier = Modifier.weight(1f)
            )
        }

        // Rotate
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Rotate (${rotate.toInt()}°)", fontSize = 9.sp, modifier = Modifier.width(75.dp), color = DMSTextPrimary)
            Slider(
                value = rotate,
                onValueChange = onRotateChange,
                valueRange = -180f..180f,
                colors = SliderDefaults.colors(thumbColor = DMSTertiary, activeTrackColor = DMSTertiary),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun SocialCommentCard(
    post: SocialPost,
    canEdit: Boolean,
    canDelete: Boolean,
    onLike: () -> Unit,
    onDelete: () -> Unit,
    onEdit: (String) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editContentText by remember { mutableStateOf(post.content) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(DMSSurface)
            .border(1.dp, DMSBorder, RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Simple avatar bullet
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (post.author == "Souvik Deb") DMSPrimary else DMSBorder),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = post.author.take(2).uppercase(),
                fontSize = 10.sp,
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
 
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = post.author,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (post.author == "Souvik Deb") DMSPrimary else DMSTextPrimary
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Like interaction
                    Row(
                        modifier = Modifier.clickable { onLike() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (post.hasLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (post.hasLiked) DMSTertiary else DMSTextSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(post.likes.toString(), fontSize = 9.sp, color = DMSTextSecondary)
                    }
 
                    // Edit option
                    if (canEdit && !isEditing) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Post",
                            tint = DMSSecondary,
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { isEditing = true }
                        )
                    }

                    // Delete button
                    if (canDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = DMSError.copy(alpha = 0.5f),
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { onDelete() }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))

            if (isEditing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = editContentText,
                        onValueChange = { editContentText = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DMSSecondary,
                            unfocusedBorderColor = DMSBorder
                        )
                    )
                    IconButton(
                        onClick = {
                            if (editContentText.isNotBlank()) {
                                onEdit(editContentText)
                                isEditing = false
                            }
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Check, "Save", tint = DMSSecondary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = {
                            editContentText = post.content
                            isEditing = false
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, "Cancel", tint = DMSTextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            } else {
                Text(
                    text = post.content,
                    fontSize = 11.sp,
                    color = DMSTextSecondary,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
fun SocialMediaRow(
    profile: ArtistProfile,
    onUrlClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (profile.spotifyUrl.isNotBlank()) {
            SocialIconPill(
                brandName = "Spotify",
                iconColor = Color(0xFF1DB954),
                url = profile.spotifyUrl,
                onClick = onUrlClick
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(16.dp)) {
                    drawCircle(color = Color(0xFF1DB954))
                    val path1 = androidx.compose.ui.graphics.Path().apply {
                        moveTo(size.width * 0.25f, size.height * 0.65f)
                        quadraticBezierTo(size.width * 0.5f, size.height * 0.55f, size.width * 0.75f, size.height * 0.65f)
                    }
                    val path2 = androidx.compose.ui.graphics.Path().apply {
                        moveTo(size.width * 0.22f, size.height * 0.50f)
                        quadraticBezierTo(size.width * 0.5f, size.height * 0.38f, size.width * 0.78f, size.height * 0.50f)
                    }
                    val path3 = androidx.compose.ui.graphics.Path().apply {
                        moveTo(size.width * 0.20f, size.height * 0.35f)
                        quadraticBezierTo(size.width * 0.5f, size.height * 0.22f, size.width * 0.80f, size.height * 0.35f)
                    }
                    drawPath(
                        path = path1,
                        color = Color.Black,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.3f.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                    drawPath(
                        path = path2,
                        color = Color.Black,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.6f.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                    drawPath(
                        path = path3,
                        color = Color.Black,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                }
            }
        }

        if (profile.youtubeUrl.isNotBlank()) {
            SocialIconPill(
                brandName = "YouTube",
                iconColor = Color(0xFFFF0000),
                url = profile.youtubeUrl,
                onClick = onUrlClick
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(16.dp)) {
                    val rectPath = androidx.compose.ui.graphics.Path().apply {
                        addRoundRect(
                            androidx.compose.ui.geometry.RoundRect(
                                rect = androidx.compose.ui.geometry.Rect(0f, size.height * 0.15f, size.width, size.height * 0.85f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                        )
                    }
                    drawPath(rectPath, color = Color(0xFFFF0000))
                    val triPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(size.width * 0.40f, size.height * 0.35f)
                        lineTo(size.width * 0.65f, size.height * 0.50f)
                        lineTo(size.width * 0.40f, size.height * 0.65f)
                        close()
                    }
                    drawPath(triPath, color = Color.White)
                }
            }
        }

        if (profile.instagramUrl.isNotBlank()) {
            SocialIconPill(
                brandName = "Instagram",
                iconColor = Color(0xFFE1306C),
                url = profile.instagramUrl,
                onClick = onUrlClick
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(16.dp)) {
                    val gradientBrush = androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(Color(0xFF833AB4), Color(0xFFF77737), Color(0xFFE1306C))
                    )
                    drawRoundRect(
                        brush = gradientBrush,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                    val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f.dp.toPx())
                    drawRoundRect(
                        color = Color.White,
                        topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.22f, size.height * 0.22f),
                        size = androidx.compose.ui.geometry.Size(size.width * 0.56f, size.height * 0.56f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                        style = stroke
                    )
                    drawCircle(
                        color = Color.White,
                        radius = size.width * 0.14f,
                        center = center,
                        style = stroke
                    )
                    drawCircle(
                        color = Color.White,
                        radius = size.width * 0.03f,
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.65f, size.height * 0.35f)
                    )
                }
            }
        }

        if (profile.twitterUrl.isNotBlank()) {
            SocialIconPill(
                brandName = "Email",
                iconColor = Color(0xFFEA4335),
                url = profile.twitterUrl,
                onClick = onUrlClick
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(16.dp)) {
                    drawRoundRect(
                        color = Color(0xFFEA4335),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                    val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.25f.dp.toPx())
                    drawRect(
                        color = Color.White,
                        topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height * 0.25f),
                        size = androidx.compose.ui.geometry.Size(size.width * 0.70f, size.height * 0.50f),
                        style = stroke
                    )
                    drawLine(
                        color = Color.White,
                        start = androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height * 0.25f),
                        end = androidx.compose.ui.geometry.Offset(size.width * 0.50f, size.height * 0.52f),
                        strokeWidth = 1.25f.dp.toPx()
                    )
                    drawLine(
                        color = Color.White,
                        start = androidx.compose.ui.geometry.Offset(size.width * 0.85f, size.height * 0.25f),
                        end = androidx.compose.ui.geometry.Offset(size.width * 0.50f, size.height * 0.52f),
                        strokeWidth = 1.25f.dp.toPx()
                    )
                }
            }
        }
    }
}

@Composable
fun SocialIconPill(
    brandName: String,
    iconColor: Color,
    url: String,
    onClick: (String) -> Unit,
    iconContent: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .clickable { onClick(url) }
            .clip(RoundedCornerShape(24.dp)),
        color = DMSSurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, DMSBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            iconContent()
            Text(
                text = brandName.uppercase(),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = DMSTextPrimary
            )
        }
    }
}


@Composable
fun ProfileSkeletonLoader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .width(200.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .shimmer()
                .background(DMSSurfaceVariant)
        )
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .shimmer()
                .background(DMSSurfaceVariant)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .shimmer()
                .background(DMSSurfaceVariant)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .shimmer()
                .background(DMSSurfaceVariant)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .shimmer()
                .background(DMSSurfaceVariant)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .shimmer()
                        .background(DMSSurfaceVariant)
                )
            }
        }
    }
}

@Composable
fun PostSkeletonCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DMSSurface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .shimmer()
                .background(DMSSurfaceVariant)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(110.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .shimmer()
                    .background(DMSSurfaceVariant)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .shimmer()
                    .background(DMSSurfaceVariant)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .shimmer()
                    .background(DMSSurfaceVariant)
            )
        }
    }
}
