package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Track
import com.example.ui.theme.*
import com.example.ui.viewmodel.MusicViewModel

@Composable
fun Modifier.shimmer(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )
    return this.alpha(alpha)
}

@Composable
fun TrackSkeletonRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DMSSurface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
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
                    .width(140.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmer()
                    .background(DMSSurfaceVariant)
            )
            Box(
                modifier = Modifier
                    .width(90.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmer()
                    .background(DMSSurfaceVariant)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .shimmer()
                .background(DMSSurfaceVariant)
        )
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
fun DashboardScreen(
    viewModel: MusicViewModel,
    onNavigateToAudioLab: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSubscription: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allTracks by viewModel.allTracks.collectAsState()
    val playingTrack by viewModel.playingTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playbackPos by viewModel.playbackPosition.collectAsState()
    val subscription by viewModel.userSubscription.collectAsState()
    val isAdmin by viewModel.isAdminLoggedIn.collectAsState()
    val isTracksLoading by viewModel.isTracksLoading.collectAsState()

    val context = LocalContext.current

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
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.horizontalGradient(listOf(DMSPrimary, DMSSecondary))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.GraphicEq, "DMS Logo", modifier = Modifier.size(18.dp), tint = Color.Black)
                        }
                        Text(
                            text = "DEB MUSIC STUDIO",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.SansSerif,
                            color = DMSTextPrimary
                        )
                    }
                },
                actions = {
                    val isDarkMode by viewModel.isDarkMode.collectAsState()
                    IconButton(
                        onClick = { viewModel.toggleDarkMode() },
                        modifier = Modifier.testTag("theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.Nightlight,
                            contentDescription = "Switch Theme",
                            tint = DMSPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(DMSSurfaceVariant)
                            .border(1.dp, DMSBorder, RoundedCornerShape(16.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val plan = subscription?.tier ?: "FREE"
                        Text(
                            text = if (plan == "FREE") "FREE TIER" else plan.replace("_", " "),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (plan == "FREE") DMSTextSecondary else DMSPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DMSBackground)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // High-fidelity Virtual Modular Engine Dashboard
            item {
                StudioStatusMatrix()
            }

            // Central Audio Player Hub (Active Display)
            item {
                NowPlayingCard(
                    track = playingTrack,
                    isPlaying = isPlaying,
                    posMs = playbackPos,
                    onPlayPause = {
                        if (isPlaying) viewModel.pauseTrack() else viewModel.playTrack()
                    },
                    onStop = { viewModel.stopTrack() },
                    onSeek = { viewModel.seekTo(it) }
                )
            }

            // Master Track List Catalogue
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("MASTER TRACKS", style = MaterialTheme.typography.titleMedium, color = DMSTextPrimary)
                    if (!isTracksLoading) {
                        Text(
                            text = "${allTracks.size} AVAILABLE",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = DMSSecondary
                        )
                    }
                }
            }

            if (isTracksLoading) {
                items(3) {
                    TrackSkeletonRow()
                }
            } else if (allTracks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, DMSBorder, RoundedCornerShape(12.dp))
                            .background(DMSSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No custom audios imported. Go to Artist Profile > Creator Desk to import demos.",
                            fontSize = 11.sp,
                            color = DMSTextSecondary,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(allTracks, key = { it.id }) { track ->
                    val isActive = playingTrack?.id == track.id
                    TrackItemRow(
                        track = track,
                        isActive = isActive,
                        isPlaying = isActive && isPlaying,
                        showDeleteButton = isAdmin,
                        onClick = { viewModel.setPlayingTrack(track) },
                        onDelete = { viewModel.deleteTrack(track) }
                    )
                }
            }

            // Moved here to bottom of page as requested
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = DMSBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "STUDIO OPERATIONS", 
                        style = MaterialTheme.typography.titleMedium, 
                        color = DMSTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OperationNavButton(
                            title = "SOUND STATION",
                            desc = "FX Synthesizer & Draw Waveforms",
                            icon = Icons.Default.Tune,
                            color = DMSSecondary,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("nav_audio_lab"),
                            onClick = onNavigateToAudioLab
                        )

                        OperationNavButton(
                            title = "ARTIST PROFILE",
                            desc = "Souvik Deb Profile Setup",
                            icon = Icons.Default.MusicVideo,
                            color = DMSTertiary,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("nav_artist_profile"),
                            onClick = onNavigateToProfile
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                SubscriptionAdPanel(
                    activeTier = subscription?.tier ?: "FREE",
                    onUpgrade = onNavigateToSubscription
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
}

@Composable
fun StudioStatusMatrix() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DMSSurfaceVariant)
            .border(1.dp, DMSBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        StatusIndicatorColumn("SAMPLING", "96.0 kHz", Icons.Default.Speed, DMSPrimary)
        StatusIndicatorColumn("LATENCY", "1.8 ms", Icons.Default.Timeline, DMSSecondary)
        StatusIndicatorColumn("BIT DEPTH", "32-bit Float", Icons.Default.SettingsVoice, DMSTertiary)
    }
}

@Composable
fun RowScope.StatusIndicatorColumn(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color
) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, label, modifier = Modifier.size(11.dp), tint = accent)
            Text(label, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = DMSTextSecondary)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, color = DMSTextPrimary)
    }
}

@Composable
fun NowPlayingCard(
    track: Track?,
    isPlaying: Boolean,
    posMs: Long,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onSeek: (Long) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(DMSSurface, DMSSurfaceVariant)
                )
            )
            .border(1.5.dp, DMSPrimary.copy(alpha = if (track != null) 0.8f else 0.2f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(
                "NOW PLAYING CONSOLE",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                color = DMSPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (track == null) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.LibraryMusic, "Disc", modifier = Modifier.size(36.dp), tint = DMSTextSecondary.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Decoded deck idle", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DMSTextSecondary)
                    Text("Select a master track below to play", fontSize = 10.sp, color = DMSTextSecondary.copy(alpha = 0.7f))
                }
            } else {
                // Audio is loaded
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DMSBorder),
                        contentAlignment = Alignment.Center
                    ) {
                        if (track.coverUrl.isNotBlank()) {
                             AsyncImage(
                                 model = getCoilModel(track.coverUrl),
                                 contentDescription = "Cover Art",
                                 modifier = Modifier.fillMaxSize(),
                                 contentScale = ContentScale.Crop
                             )
                        } else {
                            Icon(Icons.Default.MusicNote, "Notes", modifier = Modifier.size(24.dp), tint = DMSPrimary)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DMSTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track.album,
                            fontSize = 11.sp,
                            color = DMSTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Custom Seek slider
                val progress = if (track.durationMs > 0) posMs.toFloat() / track.durationMs.toFloat() else 0f
                Slider(
                    value = progress,
                    onValueChange = { onSeek((it * track.durationMs).toLong()) },
                    colors = SliderDefaults.colors(
                        thumbColor = DMSPrimary,
                        activeTrackColor = DMSPrimary,
                        inactiveTrackColor = DMSBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .testTag("playback_seek")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration(posMs),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = DMSTextSecondary
                    )
                    Text(
                        text = formatDuration(track.durationMs),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = DMSTextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Control buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onStop,
                        modifier = Modifier
                            .testTag("btn_stop")
                            .size(36.dp)
                    ) {
                        Icon(Icons.Default.Stop, "Stop", tint = DMSError, modifier = Modifier.size(22.dp))
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(DMSPrimary)
                            .clickable { onPlayPause() }
                            .testTag("btn_play_pause"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "PlayPause",
                            tint = Color.Black,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    IconButton(
                        onClick = { onSeek((posMs + 10000).coerceAtMost(track.durationMs)) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.FastForward, "Skip 10s", tint = DMSTextPrimary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun OperationNavButton(
    title: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DMSSurface)
            .border(1.dp, DMSBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, title, modifier = Modifier.size(20.dp), tint = color)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DMSTextPrimary)
        Spacer(modifier = Modifier.height(2.dp))
        Text(desc, fontSize = 9.sp, color = DMSTextSecondary, lineHeight = 12.sp)
    }
}

@Composable
fun SubscriptionAdPanel(
    activeTier: String,
    onUpgrade: () -> Unit
) {
    if (activeTier != "SOUND_MASTER") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(DMSSurfaceVariant, DMSSurface),
                        radius = 400f
                    )
                )
                .border(1.dp, DMSTertiary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .clickable { onUpgrade() }
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(DMSTertiary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Star, "VIP Upgrade", modifier = Modifier.size(16.dp), tint = DMSTertiary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    val targetTier = if (activeTier == "FREE") "STUDIO PRO" else "SOUND MASTER"
                    Text(
                        "UNLOCK $targetTier WITH COUPON",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DMSTextPrimary
                    )
                    Text(
                        "Activate maximum bitrates, cinematic filters and high fidelity exports.",
                        fontSize = 9.sp,
                        color = DMSTextSecondary
                    )
                }
                Icon(Icons.Default.ChevronRight, "Go", modifier = Modifier.size(16.dp), tint = DMSTextSecondary)
            }
        }
    }
}

@Composable
fun TrackItemRow(
    track: Track,
    isActive: Boolean,
    isPlaying: Boolean,
    showDeleteButton: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) DMSSurfaceVariant else DMSSurface)
            .border(
                1.dp,
                if (isActive) DMSPrimary.copy(alpha = 0.6f) else DMSBorder,
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isActive) DMSPrimary.copy(alpha = 0.15f) else DMSBorder),
            contentAlignment = Alignment.Center
        ) {
            if (track.coverUrl.isNotBlank()) {
                AsyncImage(
                    model = getCoilModel(track.coverUrl),
                    contentDescription = "Track Cover Preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Status",
                            tint = DMSPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Status",
                    tint = if (isActive) DMSPrimary else DMSTextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) DMSPrimary else DMSTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.album,
                fontSize = 10.sp,
                color = DMSTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = formatDuration(track.durationMs),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            color = DMSTextSecondary
        )

        Spacer(modifier = Modifier.width(12.dp))

        if (showDeleteButton) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(Icons.Default.DeleteOutline, "Delete Track", tint = DMSError.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

fun formatDuration(ms: Long): String {
    val sec = (ms / 1000) % 60
    val min = (ms / 60000) % 60
    return String.format("%02d:%02d", min, sec)
}
