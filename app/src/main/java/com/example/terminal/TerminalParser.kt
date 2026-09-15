package com.example.terminal

import android.graphics.Bitmap
import android.util.Base64
import androidx.compose.ui.graphics.Color
import com.example.model.ProtocolSettings
import java.text.BreakIterator

class TerminalParser(
    val buffer: TerminalBuffer,
    var settings: ProtocolSettings = ProtocolSettings(),
    val onPtyWrite: (String) -> Unit = {},
    val onClipboardCopy: (String) -> Unit = {},
    val onNotification: (String) -> Unit = {},
    val onBell: () -> Unit = {}
) {
    private enum class State {
        GROUND,
        ESCAPE,
        CSI_PARAM,
        CSI_INTERMEDIATE,
        OSC_STRING,
        DCS_PASSTHROUGH,
        APC_STRING
    }

    private var state = State.GROUND
    private val paramBuffer = StringBuilder()
    private val stringBuffer = StringBuilder()
    private var intermediateChar: Char? = null

    // Current pen attributes
    private var currentFg: Color = TerminalColors.DefaultForeground
    private var currentBg: Color = Color.Transparent
    private var currentUnderlineColor: Color? = null
    private var currentUnderlineStyle: UnderlineStyle = UnderlineStyle.NONE
    private var isBold = false
    private var isDim = false
    private var isItalic = false
    private var isStrikethrough = false
    private var isInverse = false
    private var isBlink = false
    private var currentHyperlinkUrl: String? = null

    // For REP (CSI Ps b - repeat last character)
    private var lastGraphicChar: TerminalChar? = null

    // Synchronized update buffer (DECSET 2026)
    private var isSyncActive = false
    private val syncBuffer = StringBuilder()

    fun parse(input: String) {
        if (settings.synchronizedOutput && isSyncActive) {
            // In synchronized mode, collect until synchronized end
            val endIdx = input.indexOf("\u001b[?2026l")
            if (endIdx != -1) {
                syncBuffer.append(input.substring(0, endIdx))
                isSyncActive = false
                processDirect(syncBuffer.toString())
                syncBuffer.clear()
                val remaining = input.substring(endIdx + 8)
                if (remaining.isNotEmpty()) processDirect(remaining)
                return
            } else {
                syncBuffer.append(input)
                return
            }
        }
        processDirect(input)
    }

    private fun processDirect(input: String) {
        if (input.isEmpty()) return

        // UAX #29 Grapheme Cluster Segmentation
        if (settings.graphemeSegmentation && state == State.GROUND && !input.contains('\u001b')) {
            val iterator = BreakIterator.getCharacterInstance()
            iterator.setText(input)
            var start = iterator.first()
            var end = iterator.next()
            while (end != BreakIterator.DONE) {
                val cluster = input.substring(start, end)
                if (cluster.length == 1 && cluster[0].code < 32) {
                    handleControlChar(cluster[0])
                } else {
                    emitGrapheme(cluster)
                }
                start = end
                end = iterator.next()
            }
            return
        }

        var i = 0
        val len = input.length
        while (i < len) {
            val ch = input[i]
            when (state) {
                State.GROUND -> {
                    when (ch) {
                        '\u001b' -> {
                            state = State.ESCAPE
                        }
                        '\u0007' -> { // BEL
                            onBell()
                        }
                        '\b' -> { // BS
                            buffer.backspace()
                        }
                        '\t' -> { // HT
                            buffer.tab()
                        }
                        '\n' -> { // LF
                            buffer.lineFeed()
                        }
                        '\r' -> { // CR
                            buffer.carriageReturn()
                        }
                        else -> {
                            if (ch.code >= 32) {
                                // Check if surrogate pair
                                if (Character.isHighSurrogate(ch) && i + 1 < len && Character.isLowSurrogate(input[i + 1])) {
                                    val cluster = "${ch}${input[i + 1]}"
                                    emitGrapheme(cluster)
                                    i++
                                } else {
                                    emitGrapheme(ch.toString())
                                }
                            }
                        }
                    }
                }
                State.ESCAPE -> {
                    when (ch) {
                        '[' -> {
                            state = State.CSI_PARAM
                            paramBuffer.clear()
                            intermediateChar = null
                        }
                        ']' -> {
                            state = State.OSC_STRING
                            stringBuffer.clear()
                        }
                        '_' -> { // APC (Kitty Graphics Protocol)
                            state = State.APC_STRING
                            stringBuffer.clear()
                        }
                        'P' -> { // DCS (Sixel / XTGETTCAP)
                            state = State.DCS_PASSTHROUGH
                            stringBuffer.clear()
                        }
                        '7' -> { // DECSC Save cursor
                            buffer.saveCursor()
                            state = State.GROUND
                        }
                        '8' -> { // DECRC Restore cursor
                            buffer.restoreCursor()
                            state = State.GROUND
                        }
                        'c' -> { // RIS Full Reset
                            reset()
                            state = State.GROUND
                        }
                        '=' -> { // Application keypad mode
                            state = State.GROUND
                        }
                        '>' -> { // Normal keypad mode
                            state = State.GROUND
                        }
                        '\\' -> { // ST (String Terminator)
                            state = State.GROUND
                        }
                        else -> {
                            state = State.GROUND
                        }
                    }
                }
                State.CSI_PARAM -> {
                    when {
                        ch in '0'..'9' || ch == ';' || ch == '?' || ch == '>' || ch == '=' || ch == ':' -> {
                            paramBuffer.append(ch)
                        }
                        ch in ' '..'/' -> { // Intermediate characters (e.g. ' ', '$', '\'')
                            intermediateChar = ch
                            state = State.CSI_INTERMEDIATE
                        }
                        ch in '@'..'~' -> {
                            handleCsi(ch)
                            state = State.GROUND
                        }
                        ch == '\u001b' -> {
                            state = State.ESCAPE
                        }
                        else -> {
                            state = State.GROUND
                        }
                    }
                }
                State.CSI_INTERMEDIATE -> {
                    if (ch in '@'..'~') {
                        handleCsi(ch)
                        state = State.GROUND
                    } else if (ch == '\u001b') {
                        state = State.ESCAPE
                    }
                }
                State.OSC_STRING -> {
                    if (ch == '\u0007' || (ch == '\\' && stringBuffer.endsWith("\u001b"))) {
                        if (ch == '\\' && stringBuffer.endsWith("\u001b")) {
                            stringBuffer.setLength(stringBuffer.length - 1)
                        }
                        handleOsc(stringBuffer.toString())
                        state = State.GROUND
                    } else {
                        stringBuffer.append(ch)
                    }
                }
                State.APC_STRING -> {
                    // Kitty Graphics APC: ESC _ ... ESC \ or BEL
                    if (ch == '\u0007' || (ch == '\\' && stringBuffer.endsWith("\u001b"))) {
                        if (ch == '\\' && stringBuffer.endsWith("\u001b")) {
                            stringBuffer.setLength(stringBuffer.length - 1)
                        }
                        if (settings.kittyGraphics) {
                            handleApc(stringBuffer.toString())
                        }
                        state = State.GROUND
                    } else {
                        stringBuffer.append(ch)
                    }
                }
                State.DCS_PASSTHROUGH -> {
                    // DCS: ESC P ... ESC \ or BEL
                    if (ch == '\u0007' || (ch == '\\' && stringBuffer.endsWith("\u001b"))) {
                        if (ch == '\\' && stringBuffer.endsWith("\u001b")) {
                            stringBuffer.setLength(stringBuffer.length - 1)
                        }
                        handleDcs(stringBuffer.toString())
                        state = State.GROUND
                    } else {
                        stringBuffer.append(ch)
                    }
                }
            }
            i++
        }
    }

    private fun handleControlChar(ch: Char) {
        when (ch) {
            '\u0007' -> onBell()
            '\b' -> buffer.backspace()
            '\t' -> buffer.tab()
            '\n' -> buffer.lineFeed()
            '\r' -> buffer.carriageReturn()
        }
    }

    private fun emitGrapheme(cluster: String) {
        val isWide = if (settings.eastAsianWidth) isEastAsianWide(cluster) else false
        val charObj = TerminalChar(
            text = cluster,
            fg = currentFg,
            bg = currentBg,
            underlineColor = currentUnderlineColor,
            underlineStyle = currentUnderlineStyle,
            bold = isBold,
            dim = isDim,
            italic = isItalic,
            strikethrough = isStrikethrough,
            inverse = isInverse,
            hyperlinkUrl = currentHyperlinkUrl,
            isWide = isWide
        )
        lastGraphicChar = charObj
        buffer.writeChar(charObj)
    }

    private fun isEastAsianWide(cluster: String): Boolean {
        if (cluster.isEmpty()) return false
        val codePoint = cluster.codePointAt(0)
        // Emoji and broad East Asian Ranges (UAX #11 / UTS #51)
        return when (codePoint) {
            in 0x1100..0x115F,   // Hangul Jamo
            in 0x2E80..0xA4CF,   // CJK Radicals, Ideographs, Yi
            in 0xAC00..0xD7A3,   // Hangul Syllables
            in 0xF900..0xFAFF,   // CJK Compatibility Ideographs
            in 0xFE10..0xFE19,   // Vertical forms
            in 0xFE30..0xFE6F,   // CJK Compatibility Forms
            in 0xFF00..0xFF60,   // Fullwidth Forms
            in 0xFFE0..0xFFE6,   // Fullwidth Symbols
            in 0x1F300..0x1F9FF, // Miscellaneous Symbols, Pictographs & Emoji
            in 0x1FA00..0x1FAFF  // Chess, Extended Pictographs
            -> true
            else -> false
        }
    }

    private fun handleCsi(finalChar: Char) {
        val paramStr = paramBuffer.toString()
        val isDecPrivate = paramStr.startsWith("?")
        val isGt = paramStr.startsWith(">")
        val isEq = paramStr.startsWith("=")

        val cleanParam = paramStr.removePrefix("?").removePrefix(">").removePrefix("=")
        val params = if (cleanParam.isEmpty()) emptyList() else cleanParam.split(';').map { it.toIntOrNull() ?: 0 }

        when (finalChar) {
            'A' -> buffer.cursorUp(params.getOrElse(0) { 1 }.coerceAtLeast(1))
            'B' -> buffer.cursorDown(params.getOrElse(0) { 1 }.coerceAtLeast(1))
            'C' -> buffer.cursorForward(params.getOrElse(0) { 1 }.coerceAtLeast(1))
            'D' -> buffer.cursorBackward(params.getOrElse(0) { 1 }.coerceAtLeast(1))
            'E' -> { // CNL Cursor Next Line
                buffer.cursorDown(params.getOrElse(0) { 1 }.coerceAtLeast(1))
                buffer.carriageReturn()
            }
            'F' -> { // CPL Cursor Preceding Line
                buffer.cursorUp(params.getOrElse(0) { 1 }.coerceAtLeast(1))
                buffer.carriageReturn()
            }
            'G' -> { // CHA Cursor Horizontal Absolute
                val col = (params.getOrElse(0) { 1 } - 1).coerceAtLeast(0)
                buffer.cursorCol = col.coerceAtMost(buffer.cols - 1)
            }
            'H', 'f' -> { // CUP Cursor Position
                val r = (params.getOrElse(0) { 1 } - 1).coerceAtLeast(0)
                val c = (params.getOrElse(1) { 1 } - 1).coerceAtLeast(0)
                buffer.cursorTo(r, c)
            }
            'J' -> { // ED Erase in Display
                buffer.eraseInDisplay(params.getOrElse(0) { 0 })
            }
            'K' -> { // EL Erase in Line
                buffer.eraseInLine(params.getOrElse(0) { 0 })
            }
            'L' -> buffer.insertLines(params.getOrElse(0) { 1 }.coerceAtLeast(1))
            'M' -> buffer.deleteLines(params.getOrElse(0) { 1 }.coerceAtLeast(1))
            'P' -> buffer.deleteCharacters(params.getOrElse(0) { 1 }.coerceAtLeast(1))
            '@' -> buffer.insertCharacters(params.getOrElse(0) { 1 }.coerceAtLeast(1))
            'S' -> {
                if (isDecPrivate && intermediateChar == null) {
                    // XTSMGRAPHICS: CSI ? Pi ; Pa ; Pv S (Graphics geometry limits query)
                    if (settings.xtSmGraphics) {
                        onPtyWrite("\u001b[?1;0;1024;768S")
                    }
                } else {
                    buffer.scrollUp(params.getOrElse(0) { 1 }.coerceAtLeast(1))
                }
            }
            'T' -> buffer.scrollDown(params.getOrElse(0) { 1 }.coerceAtLeast(1))
            'b' -> {
                // REP (CSI Ps b - repeat last character)
                if (settings.rep) {
                    val count = params.getOrElse(0) { 1 }.coerceIn(1, 2000)
                    val last = lastGraphicChar
                    if (last != null) {
                        for (k in 0 until count) buffer.writeChar(last)
                    }
                }
            }
            'c' -> {
                // Device Attributes
                if (settings.deviceAttributes) {
                    if (isGt) {
                        // DA2: Secondary Device Attributes -> \u001b[>1;10;0c (VT220)
                        onPtyWrite("\u001b[>1;10;0c")
                    } else if (isEq) {
                        // DA3: Tertiary DA -> \u001b[=0c
                        onPtyWrite("\u001b[=0c")
                    } else {
                        // DA1: Primary Device Attributes -> \u001b[?62;1;2;6;7;8;9c
                        onPtyWrite("\u001b[?62;1;2;6;7;8;9c")
                    }
                }
            }
            'd' -> { // VPA Line Position Absolute
                val r = (params.getOrElse(0) { 1 } - 1).coerceAtLeast(0)
                buffer.cursorRow = r.coerceAtMost(buffer.rows - 1)
            }
            'h' -> { // Mode Set (DECSET / SM)
                for (p in params) {
                    if (isDecPrivate) {
                        when (p) {
                            1 -> {} // Application cursor keys
                            6 -> buffer.originMode = true
                            7 -> buffer.autoWrap = true
                            25 -> buffer.isCursorVisible = true
                            1000, 1002 -> if (settings.mouseTracking) buffer.mouseReportingMode = p
                            1006 -> if (settings.mouseTracking) buffer.mouseReportingMode = 1006
                            1004 -> if (settings.focusReporting) buffer.focusReportingMode = true
                            1049 -> if (settings.alternateScreen) buffer.switchScreen(true)
                            2004 -> if (settings.bracketedPaste) buffer.bracketedPasteMode = true
                            2026 -> if (settings.synchronizedOutput) isSyncActive = true
                        }
                    }
                }
            }
            'l' -> { // Mode Reset (DECRST / RM)
                for (p in params) {
                    if (isDecPrivate) {
                        when (p) {
                            6 -> buffer.originMode = false
                            7 -> buffer.autoWrap = false
                            25 -> buffer.isCursorVisible = false
                            1000, 1002, 1006 -> buffer.mouseReportingMode = 0
                            1004 -> buffer.focusReportingMode = false
                            1049 -> if (settings.alternateScreen) buffer.switchScreen(false)
                            2004 -> buffer.bracketedPasteMode = false
                            2026 -> isSyncActive = false
                        }
                    }
                }
            }
            'm' -> { // SGR Select Graphic Rendition
                handleSgr(cleanParam)
            }
            'p' -> {
                // DECRQM (CSI ? Pm $ p or CSI Pm $ p)
                if (intermediateChar == '$' && settings.decrqm) {
                    val mode = params.getOrElse(0) { 0 }
                    val reportVal = if (isDecPrivate) {
                        when (mode) {
                            6 -> if (buffer.originMode) 1 else 2
                            7 -> if (buffer.autoWrap) 1 else 2
                            25 -> if (buffer.isCursorVisible) 1 else 2
                            1049 -> if (buffer.isAltScreenActive) 1 else 2
                            2004 -> if (buffer.bracketedPasteMode) 1 else 2
                            else -> 0
                        }
                    } else 0
                    val reply = if (isDecPrivate) "\u001b[?$mode;$reportVal\$y" else "\u001b[$mode;$reportVal\$y"
                    onPtyWrite(reply)
                }
            }
            'q' -> {
                if (isGt && cleanParam == "0") {
                    // XTVERSION (CSI > 0 q)
                    if (settings.xtVersion) {
                        onPtyWrite("\u001bP>|Terminal(1.0)\u001b\\")
                    }
                } else if (intermediateChar == ' ' && settings.decscusr) {
                    // DECSCUSR (CSI Ps SP q) cursor style
                    val style = params.getOrElse(0) { 1 }
                    buffer.cursorStyle = when (style) {
                        1 -> CursorStyle(CursorShape.BLOCK, isBlinking = true)
                        2 -> CursorStyle(CursorShape.BLOCK, isBlinking = false)
                        3 -> CursorStyle(CursorShape.UNDERLINE, isBlinking = true)
                        4 -> CursorStyle(CursorShape.UNDERLINE, isBlinking = false)
                        5 -> CursorStyle(CursorShape.BAR, isBlinking = true)
                        6 -> CursorStyle(CursorShape.BAR, isBlinking = false)
                        else -> CursorStyle()
                    }
                }
            }
            'r' -> { // DECSTBM Set Top and Bottom Margins
                val top = params.getOrElse(0) { 1 }
                val bot = params.getOrElse(1) { buffer.rows }
                buffer.setScrollMargins(top, bot)
            }
            's' -> buffer.saveCursor()
            'u' -> {
                if (isGt || isEq || cleanParam.startsWith("?")) {
                    // Kitty Keyboard Protocol (CSI > u, CSI ? u)
                    if (settings.kittyKeyboard) {
                        onPtyWrite("\u001b[?0u")
                    }
                } else {
                    buffer.restoreCursor()
                }
            }
        }
    }

    private fun handleSgr(paramStr: String) {
        if (paramStr.isEmpty()) {
            resetSgr()
            return
        }

        // Support both semicolon and colon separation (e.g. SGR 38;2;r;g;b or 38:2::r:g:b or 4:3)
        val tokens = paramStr.split(';', ':').filter { it.isNotEmpty() }
        var i = 0
        while (i < tokens.size) {
            when (val code = tokens[i].toIntOrNull() ?: 0) {
                0 -> resetSgr()
                1 -> isBold = true
                2 -> isDim = true
                3 -> isItalic = true
                4 -> {
                    // Check sub-parameters for underline styles: 4:3 curly, 4:4 dotted, 4:5 dashed, 4:2 double
                    if (settings.styledUnderlines && i + 1 < tokens.size) {
                        when (tokens[i + 1].toIntOrNull()) {
                            1 -> { currentUnderlineStyle = UnderlineStyle.STRAIGHT; i++ }
                            2 -> { currentUnderlineStyle = UnderlineStyle.DOUBLE; i++ }
                            3 -> { currentUnderlineStyle = UnderlineStyle.CURLY; i++ }
                            4 -> { currentUnderlineStyle = UnderlineStyle.DOTTED; i++ }
                            5 -> { currentUnderlineStyle = UnderlineStyle.DASHED; i++ }
                            else -> currentUnderlineStyle = UnderlineStyle.STRAIGHT
                        }
                    } else {
                        currentUnderlineStyle = UnderlineStyle.STRAIGHT
                    }
                }
                7 -> isInverse = true
                9 -> isStrikethrough = true
                22 -> { isBold = false; isDim = false }
                23 -> isItalic = false
                24 -> currentUnderlineStyle = UnderlineStyle.NONE
                27 -> isInverse = false
                29 -> isStrikethrough = false
                in 30..37 -> currentFg = TerminalColors.getAnsiColor(code - 30)
                38 -> {
                    // Extended Foreground: 38;5;n or 38;2;r;g;b
                    if (i + 1 < tokens.size) {
                        when (tokens[i + 1].toIntOrNull()) {
                            5 -> { // 256 colors
                                if (i + 2 < tokens.size) {
                                    val idx = tokens[i + 2].toIntOrNull() ?: 0
                                    currentFg = TerminalColors.getAnsiColor(idx)
                                    i += 2
                                }
                            }
                            2 -> { // TrueColor 24-bit
                                if (settings.trueColor && i + 4 < tokens.size) {
                                    val r = tokens[i + 2].toIntOrNull() ?: 0
                                    val g = tokens[i + 3].toIntOrNull() ?: 0
                                    val b = tokens[i + 4].toIntOrNull() ?: 0
                                    currentFg = TerminalColors.parseRgb(r, g, b)
                                    i += 4
                                }
                            }
                        }
                    }
                }
                39 -> currentFg = TerminalColors.DefaultForeground
                in 40..47 -> currentBg = TerminalColors.getAnsiColor(code - 40)
                48 -> {
                    // Extended Background: 48;5;n or 48;2;r;g;b
                    if (i + 1 < tokens.size) {
                        when (tokens[i + 1].toIntOrNull()) {
                            5 -> {
                                if (i + 2 < tokens.size) {
                                    val idx = tokens[i + 2].toIntOrNull() ?: 0
                                    currentBg = TerminalColors.getAnsiColor(idx)
                                    i += 2
                                }
                            }
                            2 -> {
                                if (settings.trueColor && i + 4 < tokens.size) {
                                    val r = tokens[i + 2].toIntOrNull() ?: 0
                                    val g = tokens[i + 3].toIntOrNull() ?: 0
                                    val b = tokens[i + 4].toIntOrNull() ?: 0
                                    currentBg = TerminalColors.parseRgb(r, g, b)
                                    i += 4
                                }
                            }
                        }
                    }
                }
                49 -> currentBg = Color.Transparent
                58 -> {
                    // Underline color (SGR 58;2;r;g;b or 58;5;n)
                    if (settings.underlineColor && i + 1 < tokens.size) {
                        if (tokens[i + 1] == "2" && i + 4 < tokens.size) {
                            val r = tokens[i + 2].toIntOrNull() ?: 0
                            val g = tokens[i + 3].toIntOrNull() ?: 0
                            val b = tokens[i + 4].toIntOrNull() ?: 0
                            currentUnderlineColor = TerminalColors.parseRgb(r, g, b)
                            i += 4
                        } else if (tokens[i + 1] == "5" && i + 2 < tokens.size) {
                            val idx = tokens[i + 2].toIntOrNull() ?: 0
                            currentUnderlineColor = TerminalColors.getAnsiColor(idx)
                            i += 2
                        }
                    }
                }
                59 -> currentUnderlineColor = null
                in 90..97 -> currentFg = TerminalColors.getAnsiColor(code - 90 + 8)
                in 100..107 -> currentBg = TerminalColors.getAnsiColor(code - 100 + 8)
            }
            i++
        }
    }

    private fun resetSgr() {
        currentFg = TerminalColors.DefaultForeground
        currentBg = Color.Transparent
        currentUnderlineColor = null
        currentUnderlineStyle = UnderlineStyle.NONE
        isBold = false
        isDim = false
        isItalic = false
        isStrikethrough = false
        isInverse = false
        isBlink = false
    }

    private fun handleOsc(content: String) {
        val semiIdx = content.indexOf(';')
        if (semiIdx == -1) return
        val oscCode = content.substring(0, semiIdx).toIntOrNull() ?: return
        val arg = content.substring(semiIdx + 1)

        when (oscCode) {
            0, 1, 2 -> { // Window / icon title
                if (settings.oscTitle) {
                    buffer.windowTitle = arg
                }
            }
            4 -> { // Palette get/set: OSC 4;index;rgb:...
                if (settings.oscPalette) {
                    val parts = arg.split(';')
                    if (parts.size >= 2 && parts[1] == "?") {
                        val idx = parts[0].toIntOrNull() ?: 0
                        onPtyWrite("\u001b]4;$idx;rgb:ffff/ffff/ffff\u001b\\")
                    }
                }
            }
            7 -> { // OSC 7 cwd reporting: file://hostname/path
                if (settings.osc7Cwd) {
                    val path = if (arg.startsWith("file://")) {
                        val uriPart = arg.removePrefix("file://")
                        val slashIdx = uriPart.indexOf('/')
                        if (slashIdx != -1) uriPart.substring(slashIdx) else uriPart
                    } else arg
                    buffer.cwd = path
                }
            }
            8 -> { // OSC 8 hyperlinks: 8;params;url
                if (settings.oscHyperlinks) {
                    val nextSemi = arg.indexOf(';')
                    if (nextSemi != -1) {
                        val url = arg.substring(nextSemi + 1)
                        currentHyperlinkUrl = if (url.isNotBlank()) url else null
                    } else {
                        currentHyperlinkUrl = null
                    }
                }
            }
            9 -> { // OSC 9 desktop notification
                if (settings.oscNotifications) {
                    val msg = if (arg.contains(';')) {
                        arg.substringAfter(';')
                    } else {
                        arg
                    }
                    onNotification(msg.trim())
                }
            }
            777 -> { // OSC 777 notification: notify;title;message
                if (settings.oscNotifications) {
                    val parts = arg.split(';')
                    val msg = if (parts.size >= 3) "${parts[1]}: ${parts[2]}" else arg
                    onNotification(msg.trim())
                }
            }
            10, 11, 12 -> { // FG / BG / Cursor color query
                if (settings.oscPalette && arg == "?") {
                    onPtyWrite("\u001b]$oscCode;rgb:ffff/ffff/ffff\u001b\\")
                }
            }
            52 -> { // OSC 52 clipboard: 52;c;base64
                if (settings.oscClipboard) {
                    val parts = arg.split(';', limit = 2)
                    if (parts.size == 2) {
                        val b64 = parts[1]
                        try {
                            val decoded = String(Base64.decode(b64, Base64.DEFAULT))
                            onClipboardCopy(decoded)
                        } catch (e: Exception) {
                            // ignore invalid b64
                        }
                    }
                }
            }
            133 -> { // Shell integration semantic prompts: OSC 133;A / B / C / D
                if (settings.osc133SemanticPrompts) {
                    buffer.promptState = arg
                }
            }
            1337 -> { // iTerm2 inline images: File=...:<base64>
                if (settings.iterm2Images && arg.startsWith("File=")) {
                    val bitmap = ITerm2Decoder.decode(arg)
                    if (bitmap != null) {
                        buffer.addGraphic(
                            TerminalGraphic(
                                id = "iterm2_${System.currentTimeMillis()}",
                                bitmap = bitmap,
                                row = buffer.cursorRow,
                                col = buffer.cursorCol,
                                widthCells = 20,
                                heightCells = 10,
                                protocol = "iterm2"
                            )
                        )
                    }
                }
            }
        }
    }

    private fun handleApc(content: String) {
        // Kitty Graphics Protocol: _Ga=T,f=100,...;payload
        if (content.startsWith("_G")) {
            val body = content.substring(2)
            val semiIdx = body.indexOf(';')
            val paramsStr = if (semiIdx != -1) body.substring(0, semiIdx) else body
            val payload = if (semiIdx != -1) body.substring(semiIdx + 1) else ""
            val bitmap = KittyGraphicsDecoder.decode(paramsStr, payload)
            if (bitmap != null) {
                buffer.addGraphic(
                    TerminalGraphic(
                        id = "kitty_${System.currentTimeMillis()}",
                        bitmap = bitmap,
                        row = buffer.cursorRow,
                        col = buffer.cursorCol,
                        widthCells = 20,
                        heightCells = 10,
                        protocol = "kitty"
                    )
                )
            }
        }
    }

    private fun handleDcs(content: String) {
        // Sixel Graphics starts with ... q (e.g. "q", "0;0;8q", "70;1;0;q")
        if (settings.sixel && !content.startsWith("+q") && content.contains('q')) {
            val qIdx = content.indexOf('q')
            if (qIdx != -1) {
                val sixelPayload = content.substring(qIdx + 1)
                val bitmap = SixelDecoder.decode(sixelPayload)
                if (bitmap != null) {
                    val wCells = (bitmap.width / 10).coerceIn(4, 50)
                    val hCells = (bitmap.height / 20).coerceIn(2, 25)
                    buffer.addGraphic(
                        TerminalGraphic(
                            id = "sixel_${System.currentTimeMillis()}",
                            bitmap = bitmap,
                            row = buffer.cursorRow,
                            col = buffer.cursorCol,
                            widthCells = wCells,
                            heightCells = hCells,
                            protocol = "sixel"
                        )
                    )
                }
            }
        } else if (settings.xtGetTcap && content.startsWith("+q")) {
            // XTGETTCAP DCS + q ... ST -> respond DCS 1 + s ST
            onPtyWrite("\u001bP1+s\u001b\\")
        }
    }

    fun reset() {
        buffer.eraseInDisplay(2)
        buffer.cursorTo(0, 0)
        resetSgr()
        state = State.GROUND
    }
}
