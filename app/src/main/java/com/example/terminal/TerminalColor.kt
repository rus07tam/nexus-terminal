package com.example.terminal

import androidx.compose.ui.graphics.Color

object TerminalColors {
    val DefaultBackground = Color(0xFF0F172A) // Slate-900 obsidian
    val DefaultForeground = Color(0xFFF1F5F9) // Slate-100 crisp white
    val DefaultCursor = Color(0xFF38BDF8)     // Sky-400

    // Standard 16 ANSI colors
    private val Ansi16 = arrayOf(
        Color(0xFF000000), // 0: Black
        Color(0xFFEF4444), // 1: Red
        Color(0xFF22C55E), // 2: Green
        Color(0xFFEAB308), // 3: Yellow
        Color(0xFF3B82F6), // 4: Blue
        Color(0xFFA855F7), // 5: Magenta
        Color(0xFF06B6D4), // 6: Cyan
        Color(0xFFE2E8F0), // 7: White
        // Bright variants
        Color(0xFF64748B), // 8: Bright Black (Gray)
        Color(0xFFF87171), // 9: Bright Red
        Color(0xFF4ADE80), // 10: Bright Green
        Color(0xFFFDE047), // 11: Bright Yellow
        Color(0xFF60A5FA), // 12: Bright Blue
        Color(0xFFC084FC), // 13: Bright Magenta
        Color(0xFF22D3EE), // 14: Bright Cyan
        Color(0xFFFFFFFF)  // 15: Bright White
    )

    // Precomputed 256 color palette
    val Palette256: Array<Color> = Array(256) { index ->
        when {
            index < 16 -> Ansi16[index]
            index in 16..231 -> {
                // 6x6x6 color cube
                val cubeIndex = index - 16
                val r = (cubeIndex / 36)
                val g = ((cubeIndex % 36) / 6)
                val b = (cubeIndex % 6)
                val rVal = if (r > 0) r * 40 + 55 else 0
                val gVal = if (g > 0) g * 40 + 55 else 0
                val bVal = if (b > 0) b * 40 + 55 else 0
                Color(rVal, gVal, bVal)
            }
            else -> {
                // 24 grayscale ramps from index 232 to 255
                val gray = (index - 232) * 10 + 8
                Color(gray, gray, gray)
            }
        }
    }

    fun getAnsiColor(code: Int): Color {
        return if (code in 0..255) Palette256[code] else DefaultForeground
    }

    fun parseRgb(r: Int, g: Int, b: Int): Color {
        val clampedR = r.coerceIn(0, 255)
        val clampedG = g.coerceIn(0, 255)
        val clampedB = b.coerceIn(0, 255)
        return Color(clampedR, clampedG, clampedB)
    }
}
