package com.ontime.app.ui.theme

import androidx.compose.ui.graphics.Color

enum class ThemePalette(
    val title: String,
    val primary: Color,
    val secondary: Color,
    val glow: Color
) {
    LILAC("Lilás Principal", Color(0xFFA855F7), Color(0xFFC084FC), Color(0x66A855F7)),
    LIME_GREEN("Verde Lima", Color(0xFF84CC16), Color(0xFFA3E635), Color(0x6684CC16)),
    MOSS_GREEN("Musgo", Color(0xFF22C55E), Color(0xFF4ADE80), Color(0x6622C55E)),
    INDIGO_BLUE("Azul Índigo", Color(0xFF6366F1), Color(0xFF818CF8), Color(0x666366F1)),
    PASTEL_BLUE("Azul Pastel", Color(0xFF38BDF8), Color(0xFF7DD3FC), Color(0x6638BDF8)),
    PASTEL_RED("Vermelho Pastel", Color(0xFFF87171), Color(0xFFFCA5A5), Color(0x66F87171)),
    BLOOD_RED("Sangue", Color(0xFFEF4444), Color(0xFFDC2626), Color(0x66EF4444)),
    AMBER_YELLOW("Amarelo Ouro", Color(0xFFF59E0B), Color(0xFFFBBF24), Color(0x66F59E0B)),
    SUN_YELLOW("Amarelo Sol", Color(0xFFEAB308), Color(0xFFFDE047), Color(0x66EAB308))
}

val DarkBackground = Color(0xFF0A0A0E)
val CardBackground = Color(0xFF14141B)
val CardBackgroundElevated = Color(0xFF1C1C26)
val TextPrimary = Color(0xFFF3F4F6)
val TextSecondary = Color(0xFF9CA3AF)
val TextMuted = Color(0xFF6B7280)
val DangerRed = Color(0xFFEF4444)
val SuccessGreen = Color(0xFF10B981)

