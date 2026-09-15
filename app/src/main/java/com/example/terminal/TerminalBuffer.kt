package com.example.terminal

import androidx.compose.ui.graphics.Color
import java.util.concurrent.CopyOnWriteArrayList

class TerminalBuffer(
    var cols: Int = 80,
    var rows: Int = 24
) {
    var cursorRow = 0
    var cursorCol = 0

    private var savedCursorRow = 0
    private var savedCursorCol = 0

    var scrollTop = 0
    var scrollBottom = rows - 1

    var isAltScreenActive = false
        private set

    var isLocked = false
    var autoScroll = true

    var windowTitle: String = "Terminal"
    var cwd: String = "~"
    var promptState: String? = null // OSC 133 (A, B, C, D)

    var cursorStyle = CursorStyle()
    var isCursorVisible = true

    // Terminal modes
    var originMode = false // DECOM (mode 6)
    var autoWrap = true    // DECAWM (mode 7)
    var bracketedPasteMode = false // DECSET 2004
    var focusReportingMode = false // DECSET 1004
    var mouseReportingMode = 0     // 0=off, 1000=normal, 1002=button-event, 1006=SGR

    // Screen lines
    private var primaryLines: Array<Array<TerminalChar>> = Array(rows) { Array(cols) { TerminalChar.EMPTY } }
    private var altLines: Array<Array<TerminalChar>> = Array(rows) { Array(cols) { TerminalChar.EMPTY } }
    val history = mutableListOf<Array<TerminalChar>>()
    private val maxHistory = 3000

    val graphics = CopyOnWriteArrayList<TerminalGraphic>()

    val activeLines: Array<Array<TerminalChar>>
        get() = if (isAltScreenActive) altLines else primaryLines

    var lastRenderTick = 0L

    fun resize(newCols: Int, newRows: Int) {
        if (newCols == cols && newRows == rows) return
        val validCols = newCols.coerceAtLeast(20)
        val validRows = newRows.coerceAtLeast(10)

        val newPrimary = Array(validRows) { r ->
            Array(validCols) { c ->
                if (r < rows && c < cols) primaryLines[r][c] else TerminalChar.EMPTY
            }
        }
        val newAlt = Array(validRows) { r ->
            Array(validCols) { c ->
                if (r < rows && c < cols) altLines[r][c] else TerminalChar.EMPTY
            }
        }
        primaryLines = newPrimary
        altLines = newAlt
        cols = validCols
        rows = validRows
        scrollTop = 0
        scrollBottom = rows - 1
        cursorRow = cursorRow.coerceIn(0, rows - 1)
        cursorCol = cursorCol.coerceIn(0, cols - 1)
        touch()
    }

    fun touch() {
        lastRenderTick = System.currentTimeMillis()
    }

    fun switchScreen(alt: Boolean) {
        if (isAltScreenActive != alt) {
            isAltScreenActive = alt
            if (alt) {
                // Clear alt screen
                altLines = Array(rows) { Array(cols) { TerminalChar.EMPTY } }
                cursorRow = 0
                cursorCol = 0
            }
            touch()
        }
    }

    fun saveCursor() {
        savedCursorRow = cursorRow
        savedCursorCol = cursorCol
    }

    fun restoreCursor() {
        cursorRow = savedCursorRow.coerceIn(0, rows - 1)
        cursorCol = savedCursorCol.coerceIn(0, cols - 1)
        touch()
    }

    fun setScrollMargins(top: Int, bottom: Int) {
        scrollTop = (top - 1).coerceIn(0, rows - 1)
        scrollBottom = (bottom - 1).coerceIn(scrollTop, rows - 1)
        cursorRow = if (originMode) scrollTop else 0
        cursorCol = 0
        touch()
    }

    fun cursorTo(row: Int, col: Int) {
        val baseRow = if (originMode) scrollTop else 0
        val maxRow = if (originMode) scrollBottom else rows - 1
        cursorRow = (baseRow + row).coerceIn(0, maxRow)
        cursorCol = col.coerceIn(0, cols - 1)
        touch()
    }

    fun cursorUp(n: Int = 1) {
        val minR = if (originMode) scrollTop else 0
        cursorRow = (cursorRow - n).coerceAtLeast(minR)
        touch()
    }

    fun cursorDown(n: Int = 1) {
        val maxR = if (originMode) scrollBottom else rows - 1
        cursorRow = (cursorRow + n).coerceAtMost(maxR)
        touch()
    }

    fun cursorForward(n: Int = 1) {
        cursorCol = (cursorCol + n).coerceAtMost(cols - 1)
        touch()
    }

    fun cursorBackward(n: Int = 1) {
        cursorCol = (cursorCol - n).coerceAtLeast(0)
        touch()
    }

    fun carriageReturn() {
        cursorCol = 0
        touch()
    }

    fun lineFeed() {
        if (cursorRow == scrollBottom) {
            scrollUp(1)
        } else if (cursorRow < rows - 1) {
            cursorRow++
        }
        touch()
    }

    fun backspace() {
        if (cursorCol > 0) {
            cursorCol--
            touch()
        }
    }

    fun tab() {
        val nextStop = ((cursorCol / 8) + 1) * 8
        cursorCol = nextStop.coerceAtMost(cols - 1)
        touch()
    }

    fun scrollUp(n: Int = 1) {
        for (step in 0 until n) {
            // Push top line to history if on primary screen and scrollTop == 0
            if (!isAltScreenActive && scrollTop == 0) {
                if (history.size >= maxHistory) {
                    history.removeAt(0)
                }
                history.add(primaryLines[0].copyOf())
            }

            // Shift rows up in scroll region
            for (r in scrollTop until scrollBottom) {
                activeLines[r] = activeLines[r + 1]
            }
            activeLines[scrollBottom] = Array(cols) { TerminalChar.EMPTY }
        }
        touch()
    }

    fun scrollDown(n: Int = 1) {
        for (step in 0 until n) {
            for (r in scrollBottom downTo scrollTop + 1) {
                activeLines[r] = activeLines[r - 1]
            }
            activeLines[scrollTop] = Array(cols) { TerminalChar.EMPTY }
        }
        touch()
    }

    fun eraseInDisplay(mode: Int) {
        when (mode) {
            0 -> { // From cursor to end of screen
                for (c in cursorCol until cols) activeLines[cursorRow][c] = TerminalChar.EMPTY
                for (r in cursorRow + 1 until rows) {
                    activeLines[r] = Array(cols) { TerminalChar.EMPTY }
                }
            }
            1 -> { // From start to cursor
                for (r in 0 until cursorRow) {
                    activeLines[r] = Array(cols) { TerminalChar.EMPTY }
                }
                for (c in 0..cursorCol) activeLines[cursorRow][c] = TerminalChar.EMPTY
            }
            2, 3 -> { // Entire screen (3 clears scrollback too)
                for (r in 0 until rows) {
                    activeLines[r] = Array(cols) { TerminalChar.EMPTY }
                }
                if (mode == 3 && !isAltScreenActive) {
                    history.clear()
                    graphics.clear()
                }
            }
        }
        touch()
    }

    fun eraseInLine(mode: Int) {
        if (cursorRow !in 0 until rows) return
        when (mode) {
            0 -> { // From cursor to end of line
                for (c in cursorCol until cols) activeLines[cursorRow][c] = TerminalChar.EMPTY
            }
            1 -> { // From start of line to cursor
                for (c in 0..cursorCol) activeLines[cursorRow][c] = TerminalChar.EMPTY
            }
            2 -> { // Entire line
                activeLines[cursorRow] = Array(cols) { TerminalChar.EMPTY }
            }
        }
        touch()
    }

    fun deleteCharacters(count: Int) {
        val n = count.coerceIn(1, cols - cursorCol)
        for (c in cursorCol until cols - n) {
            activeLines[cursorRow][c] = activeLines[cursorRow][c + n]
        }
        for (c in cols - n until cols) {
            activeLines[cursorRow][c] = TerminalChar.EMPTY
        }
        touch()
    }

    fun insertCharacters(count: Int) {
        val n = count.coerceIn(1, cols - cursorCol)
        for (c in cols - 1 downTo cursorCol + n) {
            activeLines[cursorRow][c] = activeLines[cursorRow][c - n]
        }
        for (c in cursorCol until cursorCol + n) {
            activeLines[cursorRow][c] = TerminalChar.EMPTY
        }
        touch()
    }

    fun deleteLines(count: Int) {
        val n = count.coerceAtMost(scrollBottom - cursorRow + 1)
        for (step in 0 until n) {
            for (r in cursorRow until scrollBottom) {
                activeLines[r] = activeLines[r + 1]
            }
            activeLines[scrollBottom] = Array(cols) { TerminalChar.EMPTY }
        }
        touch()
    }

    fun insertLines(count: Int) {
        val n = count.coerceAtMost(scrollBottom - cursorRow + 1)
        for (step in 0 until n) {
            for (r in scrollBottom downTo cursorRow + 1) {
                activeLines[r] = activeLines[r - 1]
            }
            activeLines[cursorRow] = Array(cols) { TerminalChar.EMPTY }
        }
        touch()
    }

    fun writeChar(char: TerminalChar) {
        if (cursorCol >= cols) {
            if (autoWrap) {
                cursorCol = 0
                lineFeed()
            } else {
                cursorCol = cols - 1
            }
        }

        if (cursorRow in 0 until rows && cursorCol in 0 until cols) {
            activeLines[cursorRow][cursorCol] = char
            cursorCol++
            if (char.isWide && cursorCol < cols) {
                // Wide character occupies 2 cells: put a spacer in the next cell
                activeLines[cursorRow][cursorCol] = TerminalChar(
                    text = "",
                    fg = char.fg,
                    bg = char.bg,
                    isWide = false,
                    isSpacer = true
                )
                cursorCol++
            }
        }
        touch()
    }

    fun addGraphic(graphic: TerminalGraphic) {
        graphics.add(graphic)
        touch()
    }

    fun getPlainText(): String {
        val sb = StringBuilder()
        for (line in history) {
            val text = line.joinToString("") { if (it.isSpacer) "" else it.text }.trimEnd()
            sb.append(text).append("\n")
        }
        for (r in 0 until rows) {
            val text = activeLines[r].joinToString("") { if (it.isSpacer) "" else it.text }.trimEnd()
            sb.append(text).append("\n")
        }
        return sb.toString().trimEnd()
    }
}
