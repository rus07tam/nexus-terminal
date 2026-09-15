package com.example.terminal

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color

enum class UnderlineStyle {
    NONE,
    STRAIGHT,
    DOUBLE,
    CURLY,
    DOTTED,
    DASHED
}

enum class CursorShape {
    BLOCK,
    UNDERLINE,
    BAR
}

data class CursorStyle(
    val shape: CursorShape = CursorShape.BLOCK,
    val isBlinking: Boolean = true
)

data class TerminalChar(
    val text: String = " ",
    val fg: Color = TerminalColors.DefaultForeground,
    val bg: Color = Color.Transparent,
    val underlineColor: Color? = null,
    val underlineStyle: UnderlineStyle = UnderlineStyle.NONE,
    val bold: Boolean = false,
    val dim: Boolean = false,
    val italic: Boolean = false,
    val strikethrough: Boolean = false,
    val inverse: Boolean = false,
    val hyperlinkUrl: String? = null,
    val isWide: Boolean = false,
    val isSpacer: Boolean = false
) {
    companion object {
        val EMPTY = TerminalChar(" ", TerminalColors.DefaultForeground, Color.Transparent)
    }
}

data class TerminalGraphic(
    val id: String,
    val bitmap: Bitmap,
    val row: Int,
    val col: Int,
    val widthCells: Int = 1,
    val heightCells: Int = 1,
    val zIndex: Int = 0,
    val protocol: String, // "kitty" | "sixel" | "iterm2"
    val absoluteRowIndex: Int = 0
)
