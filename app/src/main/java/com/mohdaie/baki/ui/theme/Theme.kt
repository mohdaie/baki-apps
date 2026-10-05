package com.mohdaie.baki.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ink = Color(0xFF15171A)
val Paper = Color(0xFFF4F3EE)
val GridLine = Color(0xFFE6E4DC)
val Muted = Color(0xFF4B505A)
val Faint = Color(0xFF8A8E97)
val Accent = Color(0xFF1F7A4D)
val AccentSoft = Color(0xFFCFEAD9)
val Deep = Color(0xFF1C3B2A)
val Red = Color(0xFFB8322A)
val RedSoft = Color(0xFFF6D6D3)
val RedText = Color(0xFF9C2A23)
val Green = Color(0xFF17603C)
val Amber = Color(0xFFFBEFC6)
val AmberText = Color(0xFF6E4E00)
val Hairline = Color(0xFFE3E1D8)
val Track = Color(0xFFECEBE5)
val Disabled = Color(0xFFB5B3AA)
val SlateTile = Color(0xFFD9DBE0)
val IncomeTile = Color(0xFFBFE3CC)

@Composable
fun BakiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Accent,
            onPrimary = Color.White,
            background = Paper,
            onBackground = Ink,
            surface = Color.White,
            onSurface = Ink,
        ),
        content = content,
    )
}
