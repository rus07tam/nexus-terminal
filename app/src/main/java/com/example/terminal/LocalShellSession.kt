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

    fun start() {
        if (isRunning) return
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
            append("\u001b[1;36m│\u001b[0m \u001b[1;32m●\u001b[0m \u001b[1;37mAndroid Terminal Emulator\u001b[0m (PID: $pid)        \u001b[1;36m│\u001b[0m\r\n")
            append("\u001b[1;36m│\u001b[0m \u001b[90mTransport:\u001b[0m \u001b[33m${profile.transport.displayName}\u001b[0m              \u001b[1;36m│\u001b[0m\r\n")
            append("\u001b[1;36m│\u001b[0m \u001b[90mType \u001b[1;34m'demo'\u001b[0m \u001b[90mor\u001b[0m \u001b[1;34m'help'\u001b[0m \u001b[90mfor protocol features     \u001b[1;36m│\u001b[0m\r\n")
            append("\u001b[1;36m└──────────────────────────────────────────────┘\u001b[0m\r\n\r\n")
        }
    }

    private fun startReading(inputStream: InputStream) {
        readerJob = scope.launch(Dispatchers.IO) {
            val bufferBytes = ByteArray(4096)
            try {
                while (isActive && isRunning) {
                    val read = inputStream.read(bufferBytes)
                    if (read == -1) break
                    val text = String(bufferBytes, 0, read, Charsets.UTF_8)
                    parser.parse(text)
                    onBufferUpdated()
                }
            } catch (e: Exception) {
                // stream closed
            }
        }
    }

    fun sendInput(input: String) {
        // Intercept built-in commands like "demo" or "test" if executed interactively
        if (input.trim() == "demo" || input.trim() == "term-test") {
            runProtocolDemo()
            return
        }

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
            append("\r\n\u001b[1;35m=== Terminal Protocols & Standards Interactive Demo ===\u001b[0m\r\n")
            append("\u001b[1;32m[1] True Color (24-bit SGR 38;2):\u001b[0m\r\n")
            // True color rainbow bar
            val colors = listOf(
                Triple(239, 68, 68),
                Triple(249, 115, 22),
                Triple(234, 179, 8),
                Triple(34, 197, 94),
                Triple(6, 182, 212),
                Triple(59, 130, 246),
                Triple(168, 85, 247),
                Triple(236, 72, 153)
            )
            for ((r, g, b) in colors) {
                append("\u001b[48;2;$r;$g;${b}m   \u001b[0m")
            }
            append(" \u001b[90m(Smooth 24-bit RGB Palette)\u001b[0m\r\n\r\n")

            append("\u001b[1;32m[2] Underline Styles & Separate Colors (SGR 4:x & SGR 58):\u001b[0m\r\n")
            append("  \u001b[4:1mStraight\u001b[0m | ")
            append("  \u001b[4:2mDouble\u001b[0m | ")
            append("  \u001b[4:3;58;2;239;68;68mCurly (Red)\u001b[0m | ")
            append("  \u001b[4:4;58;2;34;197;94mDotted (Green)\u001b[0m | ")
            append("  \u001b[4:5;58;2;59;130;246mDashed (Blue)\u001b[0m\r\n\r\n")

            append("\u001b[1;32m[3] Unicode 15+, ZWJ & East Asian Width:\u001b[0m\r\n")
            append("  CJK Width: 【终端测试】 (2 cells each)\r\n")
            append("  Emoji/ZWJ: 🚀 👨‍💻 ⚡ 🦊 🤖 🔥 🇷🇺 🌍\r\n\r\n")

            append("\u001b[1;32m[4] OSC 8 Hyperlink:\u001b[0m\r\n")
            append("  \u001b]8;;https://github.com\u001b\\Click here for GitHub (OSC 8 Link)\u001b]8;;\u001b\\\r\n\r\n")

            append("\u001b[1;32m[5] OSC 52 Clipboard & OSC 9 Notification:\u001b[0m\r\n")
            append("  \u001b]52;c;SGVsbG8gZnJvbSBPU0MgNTIh\u001b\\Copied 'Hello from OSC 52!' to Android clipboard\r\n")
            append("  \u001b]9;Terminal notification sent!\u001b\\Sent system notification\r\n\r\n")

            // Mini Sixel graphic test
            append("\u001b[1;32m[6] Sixel Graphic Render:\u001b[0m\r\n")
            val sixelDemo = "\u001bPq#0;2;0;0;0#1;2;100;0;0#2;2;0;100;0#3;2;0;0;100#1!20~#2!20~#3!20~\u001b\\"
            append(sixelDemo)
            append("\r\n\r\n\u001b[1;36m=== End of Demonstration ===\u001b[0m\r\n")
        }
        parser.parse(demoText)
        onBufferUpdated()
    }
}
