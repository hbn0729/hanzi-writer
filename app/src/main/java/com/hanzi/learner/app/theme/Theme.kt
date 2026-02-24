package com.hanzi.learner.app.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val ClayPrimary = Color(0xFF3B82F6) // Accessible Blue
val ClaySecondary = Color(0xFF60A5FA)
val ClayTertiary = Color(0xFFF97316) // Accessible Orange
val ClayBackground = Color(0xFFF8FAFC) // Off-white
val ClaySurface = Color(0xFFFFFFFF)
val ClayText = Color(0xFF1E293B) // Dark Slate

private val LightColors = lightColorScheme(
    primary = ClayPrimary,
    onPrimary = Color.White,
    secondary = ClaySecondary,
    onSecondary = ClayText,
    tertiary = ClayTertiary,
    onTertiary = Color.White,
    background = ClayBackground,
    onBackground = ClayText,
    surface = ClaySurface,
    onSurface = ClayText,
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF1E293B), // High contrast
    outline = Color(0xFF94A3B8)
)

private val DarkColors = darkColorScheme(
    primary = ClaySecondary,
    onPrimary = ClayText,
    secondary = ClayPrimary,
    onSecondary = Color.White,
    tertiary = Color(0xFFF97316),
    onTertiary = Color.White,
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF475569)
)

val SeniorTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 64.sp,
        lineHeight = 72.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.5.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.1.sp
    )
)

@Composable
fun HanziLearnerTheme(
    content: @Composable () -> Unit,
) {
    val useDark = !LocalInspectionMode.current && (LocalContext.current.resources.configuration.uiMode and 0x30) == 0x20
    
    MaterialTheme(
        colorScheme = if (useDark) DarkColors else LightColors,
        // using default typography for admin/young users
        content = content,
    )
}

@Composable
fun SeniorTheme(
    content: @Composable () -> Unit,
) {
    val useDark = !LocalInspectionMode.current && (LocalContext.current.resources.configuration.uiMode and 0x30) == 0x20
    
    MaterialTheme(
        colorScheme = if (useDark) DarkColors else LightColors,
        typography = SeniorTypography,
        content = content,
    )
}

