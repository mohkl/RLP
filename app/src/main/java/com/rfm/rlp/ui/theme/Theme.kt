package com.rfm.rlp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val RlpBlue = Color(0xFF1565C0)
val RlpDarkBlue = Color(0xFF0D47A1)
val RlpAccent = Color(0xFF42A5F5)
val RlpLightBlue = Color(0xFFE3F2FD)
val RlpText = Color(0xFF0D1B2A)

val StatusPass = Color(0xFF2E7D32)
val StatusFail = Color(0xFFC62828)
val StatusReview = Color(0xFFEF6C00)

private val RlpColorScheme = lightColorScheme(
    primary = RlpBlue,
    onPrimary = Color.White,
    primaryContainer = RlpLightBlue,
    onPrimaryContainer = RlpDarkBlue,
    secondary = RlpAccent,
    onSecondary = Color.White,
    background = Color.White,
    onBackground = RlpText,
    surface = Color.White,
    onSurface = RlpText,
    surfaceVariant = RlpLightBlue,
    onSurfaceVariant = RlpDarkBlue,
    outline = Color(0xFF90CAF9)
)

@Composable
fun RlpTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RlpColorScheme,
        typography = Typography(),
        content = content
    )
}
