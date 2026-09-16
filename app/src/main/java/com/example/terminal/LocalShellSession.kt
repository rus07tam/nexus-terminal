package com.example.terminal

import android.content.Context
import android.util.Log
import com.example.model.ConnectionProfile
import com.example.model.ProtocolSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class LocalShellSession(
    val id: String = UUID.randomUUID().toString(),
    val profile: ConnectionProfile,
    val context: Context,
    val scope: CoroutineScope,
    var settings: ProtocolSettings = ProtocolSettings(),
    val onBufferUpdated: () -> Unit = {},
    val onClipboardCopy: (String) -> Unit = {},
    val onNotification: (String) -> Unit = {},
    val onBell: () -> Unit = {},
    val onSessionExited: (Int) -> Unit = {}
) {
    val buffer = TerminalBuffer()
    private var process: Process? = null
    private var processOut: OutputStream? = null
    private var readerJob: Job? = null

    var isRunning: Boolean = false
        private set

    var exitCode: Int? = null
        private set

    var pid: Int = -1
        private set

    val parser = TerminalParser(
        buffer = buffer,
        settings = settings,
        onPtyWrite = { sendInput(it) },
        onClipboardCopy = onClipboardCopy,
        onNotification = onNotification,
        onBell = onBell
    )

    val isDemoSession: Boolean = profile.id.startsWith("demo")
    private val inputLineBuffer = StringBuilder()
    private var isStartupFiltered = false

    fun start() {
        if (isRunning) return

        if (isDemoSession) {
            isRunning = true
            pid = 7777
            runProtocolDemo()
            return
        }

        try {
            val shellPath = findShellPath()
            val pb = ProcessBuilder(shellPath, "-i")
            pb.redirectErrorStream(true)

            // Setup working directory
            val workDir = if (profile.workingDir.isNotBlank()) {
                val f = File(profile.workingDir)
                if (f.exists() && f.isDirectory) f else context.filesDir
            } else {
                context.filesDir
            }
            pb.directory(workDir)

            // Setup Environment
            val env = pb.environment()
            if (settings.terminfoAdvertising) {
                env["TERM"] = "xterm-256color"
            } else {
                env["TERM"] = "vt100"
            }
            if (settings.trueColor) {
                env["COLORTERM"] = "truecolor"
            }
            env["HOME"] = context.filesDir.absolutePath
            env["TMPDIR"] = context.cacheDir.absolutePath
            val defaultPath = "/system/bin:/system/xbin"
            env["PATH"] = "${context.filesDir.absolutePath}/bin:$defaultPath"
            env["LANG"] = "en_US.UTF-8"
            env["PS1"] = "\\u@android:\\w\\$ "

            val proc = pb.start()
            process = proc
            processOut = proc.outputStream
            isRunning = true

            // Extract PID if available on newer Android
            pid = try {
                val f = proc.javaClass.getDeclaredField("pid")
                f.isAccessible = true
                f.getInt(proc)
            } catch (e: Exception) {
                (1000..9999).random()
            }

            // Print welcome banner
            val welcomeBanner = buildWelcomeBanner()
            parser.parse(welcomeBanner)
            onBufferUpdated()

            // If initial command provided, write it
            if (profile.initialCommand.isNotBlank()) {
                sendInput("${profile.initialCommand}\n")
            }

            // Start background reader
            startReading(proc.inputStream)

            // Watch for process exit
            scope.launch(Dispatchers.IO) {
                try {
                    val code = proc.waitFor()
                    exitCode = code
                    isRunning = false
                    parser.parse("\r\n\u001b[33m[Process completed with exit code $code]\u001b[0m\r\n")
                    onBufferUpdated()
                    onSessionExited(code)
                } catch (e: Exception) {
                    isRunning = false
                }
            }
        } catch (e: Exception) {
            Log.e("LocalShellSession", "Failed to spawn shell", e)
            isRunning = false
            parser.parse("\r\n\u001b[31mError launching local shell: ${e.message}\u001b[0m\r\n")
            onBufferUpdated()
        }
    }

    private fun findShellPath(): String {
        val candidates = listOf("/system/bin/sh", "/system/xbin/sh", "/bin/sh")
        for (path in candidates) {
            if (File(path).canExecute()) return path
        }
        return "/system/bin/sh"
    }

    private fun buildWelcomeBanner(): String {
        return buildString {
            append("\u001b[1;36m┌──────────────────────────────────────────────┐\u001b[0m\r\n")
            append("\u001b[1;36m│\u001b[0m \u001b[1;32m●\u001b[0m \u001b[1;37mTretty Terminal Emulator\u001b[0m (PID: $pid)        \u001b[1;36m│\u001b[0m\r\n")
            append("\u001b[1;36m│\u001b[0m \u001b[90mTransport:\u001b[0m \u001b[33m${profile.transport.displayName}\u001b[0m              \u001b[1;36m│\u001b[0m\r\n")
            append("\u001b[1;36m└──────────────────────────────────────────────┘\u001b[0m\r\n\r\n")
        }
    }

    private fun filterStartupNoise(raw: String): String {
        if (isStartupFiltered) return raw
        var cleaned = raw
        if (cleaned.contains("can't find tty fd") || cleaned.contains("won't have full job control")) {
            cleaned = cleaned.lines().filterNot { line ->
                line.contains("can't find tty fd") || line.contains("won't have full job control")
            }.joinToString("\r\n")
        }
        if (cleaned.contains("$") || cleaned.contains("#") || cleaned.contains(">")) {
            isStartupFiltered = true
        }
        return cleaned
    }

    private fun startReading(inputStream: InputStream) {
        readerJob = scope.launch(Dispatchers.IO) {
            val bufferBytes = ByteArray(4096)
            try {
                while (isActive && isRunning) {
                    val read = inputStream.read(bufferBytes)
                    if (read == -1) break
                    val text = String(bufferBytes, 0, read, Charsets.UTF_8)
                    val filtered = filterStartupNoise(text)
                    if (filtered.isNotEmpty()) {
                        parser.parse(filtered)
                        onBufferUpdated()
                    }
                }
            } catch (e: Exception) {
                // stream closed
            }
        }
    }

    fun sendInput(input: String) {
        if (isDemoSession) {
            handleDemoInput(input)
            return
        }

        // Send raw bytes to process in background
        scope.launch(Dispatchers.IO) {
            try {
                val out = processOut ?: return@launch
                val bytes = input.toByteArray(Charsets.UTF_8)
                out.write(bytes)
                out.flush()
            } catch (e: Exception) {
                Log.e("LocalShellSession", "Error writing to process", e)
            }
        }

        // Local echo for pipe-based interactive shell typing
        for (ch in input) {
            when (ch) {
                '\r', '\n' -> {
                    inputLineBuffer.clear()
                    parser.parse("\r\n")
                    onBufferUpdated()
                }
                '\b', '\u007f' -> {
                    if (inputLineBuffer.isNotEmpty()) {
                        inputLineBuffer.setLength(inputLineBuffer.length - 1)
                        parser.parse("\b \b")
                        onBufferUpdated()
                    }
                }
                '\u0003' -> { // Ctrl+C
                    inputLineBuffer.clear()
                    parser.parse("^C\r\n")
                    onBufferUpdated()
                }
                '\u000c' -> { // Ctrl+L
                    parser.parse("\u001b[2J\u001b[H")
                    onBufferUpdated()
                }
                else -> {
                    if (ch >= ' ' && ch != '\u001b') {
                        inputLineBuffer.append(ch)
                        parser.parse(ch.toString())
                        onBufferUpdated()
                    }
                }
            }
        }
    }

    private fun handleDemoInput(input: String) {
        for (ch in input) {
            when (ch) {
                '\r', '\n' -> {
                    val cmd = inputLineBuffer.toString().trim()
                    inputLineBuffer.clear()
                    parser.parse("\r\n")
                    executeDemoCommand(cmd)
                }
                '\b', '\u007f' -> {
                    if (inputLineBuffer.isNotEmpty()) {
                        inputLineBuffer.setLength(inputLineBuffer.length - 1)
                        parser.parse("\b \b")
                        onBufferUpdated()
                    }
                }
                '\u0003' -> { // Ctrl+C
                    inputLineBuffer.clear()
                    parser.parse("^C\r\n\u001b[1;35mdemo@showcase\u001b[0m:\u001b[1;34m~\u001b[0m$ ")
                    onBufferUpdated()
                }
                '\u000c' -> { // Ctrl+L
                    parser.parse("\u001b[2J\u001b[H\u001b[1;35mdemo@showcase\u001b[0m:\u001b[1;34m~\u001b[0m$ ")
                    onBufferUpdated()
                }
                else -> {
                    if (ch >= ' ' && ch != '\u001b') {
                        inputLineBuffer.append(ch)
                        parser.parse(ch.toString())
                        onBufferUpdated()
                    }
                }
            }
        }
    }

    private fun executeDemoCommand(cmd: String) {
        val lower = cmd.lowercase()
        when {
            lower == "help" -> {
                parser.parse(
                    "\u001b[1;36mAvailable showcase commands:\u001b[0m\r\n" +
                    "  \u001b[32mshowcase\u001b[0m   - Run full protocols test suite\r\n" +
                    "  \u001b[32mcolors\u001b[0m     - Test 24-bit TrueColor and 256 colors\r\n" +
                    "  \u001b[32msixel\u001b[0m      - Render Sixel high-resolution graphic\r\n" +
                    "  \u001b[32mnotify\u001b[0m     - Trigger OSC 9 Android system notification\r\n" +
                    "  \u001b[32mstyles\u001b[0m     - Extended underlines & SGR styles\r\n" +
                    "  \u001b[32municode\u001b[0m    - Unicode 15.1, ZWJ emojis & CJK width\r\n" +
                    "  \u001b[32mlinks\u001b[0m      - OSC 8 Clickable hyperlinks\r\n" +
                    "  \u001b[32mclear\u001b[0m      - Clear screen\r\n"
                )
            }
            lower == "colors" -> {
                showcaseColors()
            }
            lower == "sixel" -> {
                showcaseSixel()
            }
            lower == "notify" -> {
                parser.parse("\u001b]9;Android System Notification from Terminal Demo!\u001b\\")
                parser.parse("\u001b[32m✔ Triggered OSC 9 Notification!\u001b[0m\r\n")
            }
            lower == "styles" -> {
                showcaseStyles()
            }
            lower == "unicode" -> {
                showcaseUnicode()
            }
            lower == "links" -> {
                parser.parse("  \u001b]8;;https://github.com\u001b\\GitHub Website (Click to open)\u001b]8;;\u001b\\\r\n")
                parser.parse("  \u001b]8;;https://google.com\u001b\\Google Search (Click to open)\u001b]8;;\u001b\\\r\n")
            }
            lower == "clear" -> {
                parser.parse("\u001b[2J\u001b[H")
            }
            lower == "showcase" || lower.isEmpty() -> {
                runProtocolDemo()
                return
            }
            else -> {
                parser.parse("\u001b[33mCommand '$cmd' simulated in demo stub. Type 'help' for commands.\u001b[0m\r\n")
            }
        }
        parser.parse("\r\n\u001b[1;35mdemo@showcase\u001b[0m:\u001b[1;34m~\u001b[0m$ ")
        onBufferUpdated()
    }

    fun sendPaste(text: String) {
        if (settings.bracketedPaste && buffer.bracketedPasteMode) {
            sendInput("\u001b[200~$text\u001b[201~")
        } else {
            sendInput(text)
        }
    }

    fun sendMouseClick(row: Int, col: Int, button: Int = 0, isRelease: Boolean = false) {
        if (!settings.mouseTracking || buffer.mouseReportingMode == 0) return
        if (buffer.mouseReportingMode == 1006) {
            // SGR 1006 extended coordinates: CSI < Cb ; Cx ; Cy M / m
            val type = if (isRelease) "m" else "M"
            val seq = "\u001b[<$button;${col + 1};${row + 1}$type"
            sendInput(seq)
        }
    }

    fun sendFocus(gained: Boolean) {
        if (settings.focusReporting && buffer.focusReportingMode) {
            sendInput(if (gained) "\u001b[I" else "\u001b[O")
        }
    }

    fun updateSettings(newSettings: ProtocolSettings) {
        this.settings = newSettings
        parser.settings = newSettings
    }

    fun terminate() {
        try {
            readerJob?.cancel()
            process?.destroy()
            isRunning = false
        } catch (e: Exception) {
            // ignore
        }
    }

    fun runProtocolDemo() {
        val demoText = buildString {
            append("\r\n\u001b[1;35m══════════════════════════════════════════════════════════════════\u001b[0m\r\n")
            append("\u001b[1;36m         ⭐ TRETTY PROTOCOLS & STANDARDS SHOWCASE ⭐              \u001b[0m\r\n")
            append("\u001b[1;35m══════════════════════════════════════════════════════════════════\u001b[0m\r\n\r\n")

            // [1] TrueColor
            append("\u001b[1;33m[1] 24-bit TrueColor (RGB SGR 38;2 & 48;2):\u001b[0m\r\n")
            val rainbow = listOf(
                Triple(255, 0, 0), Triple(255, 128, 0), Triple(255, 255, 0),
                Triple(0, 255, 0), Triple(0, 255, 255), Triple(0, 128, 255),
                Triple(128, 0, 255), Triple(255, 0, 128)
            )
            for ((r, g, b) in rainbow) {
                append("\u001b[48;2;$r;$g;${b}m    \u001b[0m")
            }
            append(" \u001b[90m(24-bit TrueColor Gradient)\u001b[0m\r\n\r\n")

            // [2] Underline Styles & Colors
            append("\u001b[1;33m[2] Extended Underlines (SGR 4:x) & Separate Underline Colors (SGR 58):\u001b[0m\r\n")
            append("  \u001b[4:1;58;2;255;255;255mStraight\u001b[0m  │  ")
            append("\u001b[4:2;58;2;59;130;246mDouble\u001b[0m  │  ")
            append("\u001b[4:3;58;2;239;68;68mCurly / Wavy\u001b[0m  │  ")
            append("\u001b[4:4;58;2;34;197;94mDotted\u001b[0m  │  ")
            append("\u001b[4:5;58;2;234;179;8mDashed\u001b[0m\r\n\r\n")

            // [3] Unicode 15.1, Complex Emojis, Powerline & Box Drawing
            append("\u001b[1;33m[3] Unicode 15.1, CJK East Asian Ambiguous Width & Powerline:\u001b[0m\r\n")
            append("  CJK 2-Cell Width: 【终端测试】【こんにちは】【한국어】\r\n")
            append("  ZWJ Graphemes:    🧑‍💻 👨‍👩‍👧‍👦 🏳️‍🌈 👍🏽 🔥 ⚡ 🚀 📦\r\n")
            append("  Powerline Glyphs:        \r\n")
            append("  Box Drawing:      ┌───┬───┐ │ 1 │ 2 │ ├───┼───┤ │ 3 │ 4 │ └───┴───┘\r\n\r\n")

            // [4] Hyperlinks (OSC 8)
            append("\u001b[1;33m[4] OSC 8 Clickable Hyperlinks:\u001b[0m\r\n")
            append("  \u001b]8;;https://github.com\u001b\\🔗 GitHub.com (Tap anywhere on this link to open)\u001b]8;;\u001b\\\r\n\r\n")

            // [5] Notifications (OSC 9 / OSC 777)
            append("\u001b[1;33m[5] OSC 9 Android System Notification:\u001b[0m\r\n")
            append("\u001b]9;Tretty Showcase: OSC 9 Notification received successfully!\u001b\\")
            append("  \u001b[32m✔ Triggered OSC 9 system notification and in-app banner\u001b[0m\r\n\r\n")

            // [6] Clipboard (OSC 52)
            append("\u001b[1;33m[6] OSC 52 System Clipboard Synchronization:\u001b[0m\r\n")
            append("\u001b]52;c;SGVsbG8gZnJvbSBUcmV0dHkgT1NDIDUyIQ==\u001b\\")
            append("  \u001b[32m✔ Copied 'Hello from Tretty OSC 52!' to clipboard\u001b[0m\r\n\r\n")

            // [7] Sixel Graphics
            append("\u001b[1;33m[7] Sixel High-Resolution Graphics Protocol (DCS q):\u001b[0m\r\n")
            val sixelRainbow = buildString {
                append("\u001bPq")
                append("#1;2;100;15;15#1!60~-") // Red band
                append("#2;2;100;60;0#2!60~-")  // Orange band
                append("#3;2;100;90;0#3!60~-")  // Yellow band
                append("#4;2;15;85;30#4!60~-")  // Green band
                append("#5;2;15;60;100#5!60~-") // Cyan band
                append("#6;2;75;25;90#6!60~")   // Purple band
                append("\u001b\\")
            }
            append(sixelRainbow)
            append("\r\n\r\n")

            // [8] Interactive Shell Instructions
            append("\u001b[1;36mInteractive Showcase Shell ready. Try typing:\u001b[0m\r\n")
            append("  \u001b[32mcolors\u001b[0m, \u001b[32msixel\u001b[0m, \u001b[32mnotify\u001b[0m, \u001b[32mstyles\u001b[0m, \u001b[32municode\u001b[0m, \u001b[32mlinks\u001b[0m, \u001b[32mclear\u001b[0m\r\n\r\n")
            append("\u001b[1;35mdemo@showcase\u001b[0m:\u001b[1;34m~\u001b[0m$ ")
        }
        parser.parse(demoText)
        onBufferUpdated()
    }

    private fun showcaseColors() {
        val sb = StringBuilder()
        sb.append("\r\n\u001b[1;36m=== 24-bit TrueColor Palette ===\u001b[0m\r\n")
        for (i in 0 until 16) {
            val r = (i * 16).coerceIn(0, 255)
            val g = ((15 - i) * 16).coerceIn(0, 255)
            val b = 220
            sb.append("\u001b[48;2;$r;$g;${b}m  \u001b[0m")
        }
        sb.append("\r\n\u001b[1;36m=== ANSI 256 Colors ===\u001b[0m\r\n")
        for (col in 16 until 32) {
            sb.append("\u001b[48;5;${col}m  \u001b[0m")
        }
        sb.append("\r\n")
        parser.parse(sb.toString())
    }

    private fun showcaseSixel() {
        parser.parse("\r\n\u001b[1;36m=== Sixel Graphic Rendering ===\u001b[0m\r\n")
        val sixel = buildString {
            append("\u001bPq")
            append("#1;2;100;15;15#1!80~-")
            append("#2;2;100;60;0#2!80~-")
            append("#3;2;100;90;0#3!80~-")
            append("#4;2;15;85;30#4!80~-")
            append("#5;2;15;60;100#5!80~-")
            append("#6;2;75;25;90#6!80~")
            append("\u001b\\")
        }
        parser.parse(sixel)
        parser.parse("\r\n")
    }

    private fun showcaseStyles() {
        parser.parse(
            "\r\n\u001b[1;36m=== Text Styles & Extended Underlines ===\u001b[0m\r\n" +
            "  \u001b[1mBold Text\u001b[0m  \u001b[2mDim Text\u001b[0m  \u001b[3mItalic Text\u001b[0m  \u001b[9mStrikethrough\u001b[0m  \u001b[7mInverted\u001b[0m\r\n" +
            "  \u001b[4:1;58;2;255;255;255mStraight Underline\u001b[0m\r\n" +
            "  \u001b[4:2;58;2;59;130;246mDouble Underline\u001b[0m\r\n" +
            "  \u001b[4:3;58;2;239;68;68mCurly / Wavy Underline\u001b[0m\r\n" +
            "  \u001b[4:4;58;2;34;197;94mDotted Underline\u001b[0m\r\n" +
            "  \u001b[4:5;58;2;234;179;8mDashed Underline\u001b[0m\r\n"
        )
    }

    private fun showcaseUnicode() {
        parser.parse(
            "\r\n\u001b[1;36m=== Unicode 15.1, CJK & Emojis ===\u001b[0m\r\n" +
            "  CJK Fullwidth:   【安卓终端】【プログラミング】【디지털】\r\n" +
            "  Complex ZWJ:     👨‍👩‍👧‍👦 🧑‍💻 🧙‍♂️ 🧚‍♀️ 🦸‍♂️ 🧝‍♀️\r\n" +
            "  Flags & Glyphs:  🏳️‍🌈 🏁 🚩 🚀 🛰️ 🛸 🌍 🌕\r\n"
        )
    }
}
