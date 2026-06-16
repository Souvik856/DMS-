package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Static Dark Colors (Apple Music Dark Theme Overhaul)
val DarkPrimary = Color(0xFFFD2C55)         // Signature Apple Music Red-Pink
val DarkSecondary = Color(0xFF9D4EDD)       // Vibrant Purple Aura
val DarkTertiary = Color(0xFF00B4D8)        // Vibrant Cyan/Azure Pulse
val DarkBackground = Color(0xFF050508)      // Pitch black velvet
val DarkSurface = Color(0xFF14141B)         // Matte space gray card
val DarkSurfaceVariant = Color(0xFF1E1E28)  // Glass-like elevation gray
val DarkBorder = Color(0xFF2C2C39)           // Fine metallic border accent
val DarkTextPrimary = Color(0xFFFFFFFF)      // Pure white readable text
val DarkTextSecondary = Color(0xFF8E8E9F)    // Soft Apple Music Gray
val DarkError = Color(0xFFFF2A5F)            // Modern system red

// Static Light Colors (Apple Music Light Theme Overhaul)
val LightPrimary = Color(0xFFFD2C55)        // Signature Apple Music Red-Pink
val LightSecondary = Color(0xFF9D4EDD)      // Vibrant Purple Aura
val LightTertiary = Color(0xFF00B4D8)       // Vibrant Cyan/Azure Pulse
val LightBackground = Color(0xFFFAFAFC)     // Crisp light cream-gray
val LightSurface = Color(0xFFFFFFFF)        // High contrast card white
val LightSurfaceVariant = Color(0xFFF2F2F7) // System light grey
val LightBorder = Color(0xFFE5E5EA)         // Soft gray borders
val LightTextPrimary = Color(0xFF000000)     // Jet black readability text
val LightTextSecondary = Color(0xFF8E8E93)   // System neutral gray
val LightError = Color(0xFFFF3B30)           // Standard Apple Red

// Dynamic Theme Mappings (Used directly across screens)
val DMSPrimary: Color @Composable get() = MaterialTheme.colorScheme.primary
val DMSSecondary: Color @Composable get() = MaterialTheme.colorScheme.secondary
val DMSTertiary: Color @Composable get() = MaterialTheme.colorScheme.tertiary
val DMSBackground: Color @Composable get() = MaterialTheme.colorScheme.background
val DMSSurface: Color @Composable get() = MaterialTheme.colorScheme.surface
val DMSSurfaceVariant: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
val DMSBorder: Color @Composable get() = MaterialTheme.colorScheme.outline
val DMSTextPrimary: Color @Composable get() = MaterialTheme.colorScheme.onBackground
val DMSTextSecondary: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
val DMSError: Color @Composable get() = MaterialTheme.colorScheme.error
val DMSAccentGreen: Color @Composable get() = MaterialTheme.colorScheme.primary
