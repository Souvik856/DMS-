package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.ArtistProfileScreen
import com.example.ui.screens.AudioLabScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SubscriptionScreen
import com.example.ui.theme.*
import com.example.ui.viewmodel.MusicViewModel
import kotlinx.coroutines.delay

sealed class ScreenTab(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Dashboard : ScreenTab("dashboard", "DMS Home", Icons.Default.Home)
    object AudioLab : ScreenTab("audio_lab", "Sound Lab", Icons.Default.Tune)
    object ArtistProfile : ScreenTab("artist_profile", "Artist Info", Icons.Default.ContactPage)
    object Subscription : ScreenTab("subscription", "VIP Access", Icons.Default.Star)
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: MusicViewModel = viewModel()
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            DMSTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showSplashScreen by remember { mutableStateOf(true) }

                    if (showSplashScreen) {
                        DMSSplashScreen(onSplashFinished = { showSplashScreen = false })
                    } else {
                        var activeTab by remember { mutableStateOf<ScreenTab>(ScreenTab.Dashboard) }

                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            containerColor = DMSBackground,
                            bottomBar = {
                                NavigationBar(
                                    containerColor = DMSSurface,
                                    contentColor = DMSTextPrimary,
                                    tonalElevation = 8.dp,
                                    modifier = Modifier.navigationBarsPadding()
                                ) {
                                    val tabs = listOf(
                                        ScreenTab.Dashboard,
                                        ScreenTab.AudioLab,
                                        ScreenTab.ArtistProfile,
                                        ScreenTab.Subscription
                                    )
                                    tabs.forEach { tab ->
                                        val isSelected = activeTab == tab
                                        NavigationBarItem(
                                            selected = isSelected,
                                            onClick = { activeTab = tab },
                                            icon = {
                                                Icon(
                                                    imageVector = tab.icon,
                                                    contentDescription = tab.title,
                                                    tint = if (isSelected) DMSPrimary else DMSTextSecondary
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = tab.title,
                                                    fontSize = 9.sp,
                                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                                                    color = if (isSelected) DMSPrimary else DMSTextSecondary,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            },
                                            colors = NavigationBarItemDefaults.colors(
                                                indicatorColor = DMSPrimary.copy(alpha = 0.12f)
                                            )
                                        )
                                    }
                                }
                            }
                        ) { innerPadding ->
                            Crossfade(
                                targetState = activeTab,
                                animationSpec = tween(durationMillis = 250),
                                modifier = Modifier.padding(innerPadding)
                            ) { tab ->
                                when (tab) {
                                    ScreenTab.Dashboard -> {
                                        DashboardScreen(
                                            viewModel = viewModel,
                                            onNavigateToAudioLab = { activeTab = ScreenTab.AudioLab },
                                            onNavigateToProfile = { activeTab = ScreenTab.ArtistProfile },
                                            onNavigateToSubscription = { activeTab = ScreenTab.Subscription }
                                        )
                                    }
                                    ScreenTab.AudioLab -> {
                                        AudioLabScreen(
                                            viewModel = viewModel,
                                            onBack = { activeTab = ScreenTab.Dashboard }
                                        )
                                    }
                                    ScreenTab.ArtistProfile -> {
                                        ArtistProfileScreen(
                                            viewModel = viewModel,
                                            onBack = { activeTab = ScreenTab.Dashboard }
                                        )
                                    }
                                    ScreenTab.Subscription -> {
                                        SubscriptionScreen(
                                            viewModel = viewModel,
                                            onBack = { activeTab = ScreenTab.Dashboard }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DMSSplashScreen(onSplashFinished: () -> Unit) {
    var animateStart by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animateStart = true
        delay(2200)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050508)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.deb_logo),
                contentDescription = "DMS Logo",
                modifier = Modifier
                    .size(130.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(2.dp, Color(0xFFFD2C55), RoundedCornerShape(24.dp))
                    .alpha(if (animateStart) 1f else 0f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "DEB MUSIC STUDIO",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                color = Color(0xFFFD2C55),
                letterSpacing = 2.sp
            )

            Text(
                text = "DMS NEURAL SYNTHESIS ENGINE",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8E8E9F),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(12.dp, 24.dp, 36.dp, 16.dp, 28.dp, 10.dp, 22.dp).forEach { height ->
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(height)
                            .clip(CircleShape)
                            .background(Color(0xFFFD2C55))
                    )
                }
            }

            Text(
                text = "BOOTING COGNITIVE CORES...",
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF9D4EDD),
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}
