package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userSub by viewModel.userSubscription.collectAsState()

    var couponCode by remember { mutableStateOf("") }
    var appliedCouponResult by remember { mutableStateOf("") }

    val activeTier = userSub?.tier ?: "FREE"

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
                title = { Text("MONETIZATION DECKS", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = DMSTextPrimary)
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
            // Screen Header Display
            item {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        text = "DEB MUSIC LICENSING PLANS",
                        style = MaterialTheme.typography.titleMedium,
                        color = DMSTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Access high-performance synthetic processors, loss-free audio rendering and premium cover customizers.",
                        fontSize = 11.sp,
                        color = DMSTextSecondary
                    )
                }
            }

            // Coupon Promo Panel
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DMSSurface)
                        .border(1.2.dp, DMSBorder, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = "OFFICIAL PROMOTION: ENTER COMPLIMENTARY COUPON",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = DMSSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "🔑 Enter coupons (e.g. SOUVIK100 for Master; DMSPRO for Creator) to unlock full tiers at zero fees.",
                            fontSize = 10.sp,
                            color = DMSTextSecondary,
                            lineHeight = 13.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = couponCode,
                                onValueChange = { couponCode = it },
                                placeholder = { Text("Coupon code...", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f).testTag("coupon_input_field"),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = DMSSecondary,
                                    unfocusedBorderColor = DMSBorder
                                )
                            )

                            Button(
                                onClick = {
                                    val cleaned = couponCode.trim().uppercase()
                                    if (cleaned == "DMSPRO") {
                                        viewModel.activateSubscription("CREATOR_VIP", "DMSPRO")
                                        appliedCouponResult = "Coupon DMSPRO validated: CREATOR VIP activated!"
                                        Toast.makeText(context, "Creator VIP Unlocked!", Toast.LENGTH_SHORT).show()
                                        couponCode = ""
                                    } else if (cleaned == "SOUVIK100") {
                                        viewModel.activateSubscription("SOUND_MASTER", "SOUVIK100")
                                        appliedCouponResult = "Coupon SOUVIK100 validated: SOUND MASTER activated!"
                                        Toast.makeText(context, "Sound Master Tier Unlocked!", Toast.LENGTH_SHORT).show()
                                        couponCode = ""
                                    } else {
                                        Toast.makeText(context, "Incorrect coupon entered!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DMSSecondary),
                                modifier = Modifier.testTag("apply_coupon_button")
                            ) {
                                Text("APPLY", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (appliedCouponResult.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Verified, "Coupon Ver", tint = DMSPrimary, modifier = Modifier.size(14.dp))
                                Text(appliedCouponResult, fontSize = 10.sp, color = DMSPrimary)
                            }
                        }
                    }
                }
            }

            // TIER 1: FREE STUDIO
            item {
                TierPriceCard(
                    tierName = "FREE BASIC TIER",
                    price = "$0.00 / Mo",
                    isActive = activeTier == "FREE",
                    isRecommended = false,
                    accent = DMSTextSecondary,
                    benefits = listOf(
                        "96kHz maximum stream rates",
                        "Default 16-bit sound synthesis",
                        "Standard electronic track access"
                    ),
                    onActivate = {
                        viewModel.cancelSubscription()
                        appliedCouponResult = ""
                        Toast.makeText(context, "Reverted back to FREE tier", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // TIER 2: CREATOR VIP
            item {
                TierPriceCard(
                    tierName = "CREATOR VIP",
                    price = "$4.99 / Mo",
                    isActive = activeTier == "CREATOR_VIP",
                    isRecommended = false,
                    accent = DMSSecondary,
                    benefits = listOf(
                        "Full access to sound synthesizer nodes",
                        "High fidelity 24-bit wav exports",
                        "Unlimited track catalog imports",
                        "Full customization of geometry crops"
                    ),
                    onActivate = {
                        viewModel.activateSubscription("CREATOR_VIP", "DIRECT_BUY")
                        appliedCouponResult = ""
                        Toast.makeText(context, "Creator VIP Activated!", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // TIER 3: SOUND MASTER STUDIO
            item {
                TierPriceCard(
                    tierName = "SOUND MASTER",
                    price = "$9.99 / Mo",
                    isActive = activeTier == "SOUND_MASTER",
                    isRecommended = true,
                    accent = DMSPrimary,
                    benefits = listOf(
                        "Professional 32-bit floating point calculations",
                        "Cinematic vocal and echo processors",
                        "Vibrant custom interactive waveforms curves",
                        "Verified studio developer badges"
                    ),
                    onActivate = {
                        viewModel.activateSubscription("SOUND_MASTER", "DIRECT_BUY")
                        appliedCouponResult = ""
                        Toast.makeText(context, "Sound Master Tier Activated!", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
}

@Composable
fun TierPriceCard(
    tierName: String,
    price: String,
    isActive: Boolean,
    isRecommended: Boolean,
    accent: Color,
    benefits: List<String>,
    onActivate: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DMSSurface)
            .border(
                width = if (isActive || isRecommended) 1.5.dp else 1.dp,
                color = if (isActive) accent else if (isRecommended) DMSTertiary else DMSBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(tierName, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = DMSTextPrimary)
                        if (isRecommended) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(DMSTertiary.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("RECOMMENDED", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = DMSTertiary)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(price, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, color = accent)
                }

                if (isActive) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Verified, "Check", tint = accent, modifier = Modifier.size(12.dp))
                            Text("ACTIVE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = accent)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = DMSBorder)
            Spacer(modifier = Modifier.height(12.dp))

            // benefits list
            benefits.forEach { ben ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, "bullet", tint = accent.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
                    Text(ben, fontSize = 11.sp, color = DMSTextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (!isActive) {
                Button(
                    onClick = onActivate,
                    colors = ButtonDefaults.buttonColors(containerColor = if (isRecommended) DMSTertiary else DMSSurfaceVariant),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "DEPLOY PLAN",
                        fontWeight = FontWeight.Bold,
                        color = if (isRecommended) Color.White else DMSTextPrimary
                    )
                }
            }
        }
    }
}
