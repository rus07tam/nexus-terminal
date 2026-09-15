package com.example.model

data class ProtocolItem(
    val id: String,
    val title: String,
    val spec: String,
    val description: String,
    val isEnabled: Boolean = true
)

data class ProtocolCategory(
    val id: String,
    val title: String,
    val items: List<ProtocolItem>
)

data class ProtocolSettings(
    // 1. Control Sequence Layer
    val ecma48: Boolean = true,
    val vtSequences: Boolean = true,
    val xtermSequences: Boolean = true,
    val deviceAttributes: Boolean = true,
    val xtVersion: Boolean = true,
    val decrqm: Boolean = true,
    val rep: Boolean = true,
    val xtGetTcap: Boolean = true,

    // 2. Color
    val trueColor: Boolean = true,
    val oscPalette: Boolean = true,
    val underlineColor: Boolean = true,

    // 3. Text, Unicode, Width
    val graphemeSegmentation: Boolean = true,
    val eastAsianWidth: Boolean = true,
    val emojiZwj: Boolean = true,
    val styledUnderlines: Boolean = true,

    // 4. Graphics
    val kittyGraphics: Boolean = true,
    val sixel: Boolean = true,
    val iterm2Images: Boolean = true,
    val xtSmGraphics: Boolean = true,

    // 5. Keyboard Input
    val kittyKeyboard: Boolean = true,
    val bracketedPaste: Boolean = true,
    val focusReporting: Boolean = true,

    // 6. Mouse
    val mouseTracking: Boolean = true,

    // 7. Screen modes / buffers / cursor
    val alternateScreen: Boolean = true,
    val synchronizedOutput: Boolean = true,
    val decscusr: Boolean = true,
    val decModes: Boolean = true, // DECAWM, DECOM, DECCOLM, DECSTBM, XTWINOPS

    // 8. OSC Extensions
    val oscTitle: Boolean = true,
    val oscHyperlinks: Boolean = true,
    val oscClipboard: Boolean = true,
    val oscNotifications: Boolean = true,

    // 9. Shell Integration
    val osc133SemanticPrompts: Boolean = true,
    val osc7Cwd: Boolean = true,

    // 10. Android Specific Layer
    val imeComposition: Boolean = true,
    val vsyncRendering: Boolean = true,

    // 11. Terminfo
    val terminfoAdvertising: Boolean = true
) {
    fun getCategories(): List<ProtocolCategory> {
        return listOf(
            ProtocolCategory(
                id = "control",
                title = "1. Base Control Sequences",
                items = listOf(
                    ProtocolItem("ecma48", "ECMA-48 Framing", "ECMA-48", "C0/C1 control codes, CSI/DCS/OSC/APC/PM parser framing", ecma48),
                    ProtocolItem("vtSequences", "VT100–VT520", "vt100.net docs", "Classic DEC terminal cursor movements, margins, scrolling", vtSequences),
                    ProtocolItem("xtermSequences", "xterm Sequences", "xterm ctlseqs", "Standard modern xterm escape sequence extensions", xtermSequences),
                    ProtocolItem("deviceAttributes", "DA1 / DA2 / DA3", "VT100 / VT220 DA2", "Primary, secondary & tertiary device attribute identification", deviceAttributes),
                    ProtocolItem("xtVersion", "XTVERSION", "CSI > 0 q", "Reports terminal name and version string back to programs", xtVersion),
                    ProtocolItem("decrqm", "DECRQM", "ANSI + DEC mode", "Request ANSI or DEC private mode state and report via DECRPM", decrqm),
                    ProtocolItem("rep", "REP (Repeat Char)", "CSI Ps b", "Repeats the last emitted graphical character Ps times", rep),
                    ProtocolItem("xtGetTcap", "XTGETTCAP", "DCS + q ... ST", "Capability and terminfo query support for tmux/vim", xtGetTcap)
                )
            ),
            ProtocolCategory(
                id = "color",
                title = "2. Color & Styling",
                items = listOf(
                    ProtocolItem("trueColor", "True Color (24-bit)", "SGR 38;2 / 48;2", "Direct 16.7M RGB color palette rendering in terminal", trueColor),
                    ProtocolItem("oscPalette", "OSC 4 / 10 / 11 / 12", "xterm OSC", "Dynamic 256-color palette query/set and cursor/fg/bg overrides", oscPalette),
                    ProtocolItem("underlineColor", "SGR 58/59 Underline Color", "kitty/vte spec", "Separate underline color independent of character text color", underlineColor)
                )
            ),
            ProtocolCategory(
                id = "unicode",
                title = "3. Text & Unicode",
                items = listOf(
                    ProtocolItem("graphemeSegmentation", "Grapheme Segmentation", "UAX #29", "Correct grapheme cluster boundary detection rather than raw char width", graphemeSegmentation),
                    ProtocolItem("eastAsianWidth", "East Asian Width", "UAX #11", "Proper 2-cell rendering for fullwidth and CJK characters", eastAsianWidth),
                    ProtocolItem("emojiZwj", "Emoji & ZWJ Sequences", "UTS #51", "Zero-width joiner sequences, flags and multi-codepoint emoji", emojiZwj),
                    ProtocolItem("styledUnderlines", "Curly / Dotted Underlines", "SGR 4:3 / 4:4 / 4:5", "LSP diagnostic underlines (wavy, dotted, dashed, double)", styledUnderlines)
                )
            ),
            ProtocolCategory(
                id = "graphics",
                title = "4. Graphics Protocols",
                items = listOf(
                    ProtocolItem("kittyGraphics", "Kitty Graphics Protocol", "APC _G...", "Modern base64 chunked image display with placement IDs and z-index", kittyGraphics),
                    ProtocolItem("sixel", "Sixel Graphics", "DEC STD 070", "Classic DEC Sixel raster graphics decoder for terminal art and charts", sixel),
                    ProtocolItem("iterm2Images", "iTerm2 Inline Images", "OSC 1337 File=", "Base64 inline image embedding supported by CLI utilities", iterm2Images),
                    ProtocolItem("xtSmGraphics", "XTSMGRAPHICS", "CSI ? Pi ; Pa ; Pv S", "Replies graphics subsystem geometry and resolution limits", xtSmGraphics)
                )
            ),
            ProtocolCategory(
                id = "keyboard",
                title = "5. Keyboard Input",
                items = listOf(
                    ProtocolItem("kittyKeyboard", "Kitty Keyboard Protocol", "CSI > u / CSI ? u", "Progressive enhancement, disambiguation and press/release events", kittyKeyboard),
                    ProtocolItem("bracketedPaste", "Bracketed Paste Mode", "DECSET 2004", "Wraps pasted text in escape codes to protect shell from auto-execution", bracketedPaste),
                    ProtocolItem("focusReporting", "Focus Reporting", "DECSET 1004", "Sends CSI I when terminal gains focus and CSI O on focus loss", focusReporting)
                )
            ),
            ProtocolCategory(
                id = "mouse",
                title = "6. Mouse Tracking",
                items = listOf(
                    ProtocolItem("mouseTracking", "Mouse Tracking + SGR 1006", "Mode 1000 / 1002 / 1006", "Reports touch taps and drags as mouse click events for tmux/htop/vim", mouseTracking)
                )
            ),
            ProtocolCategory(
                id = "screen",
                title = "7. Screen Modes & Buffers",
                items = listOf(
                    ProtocolItem("alternateScreen", "Alternate Screen Buffer", "DECSET 1049", "Dedicated full-screen buffer for vim/nano/less restoring history on exit", alternateScreen),
                    ProtocolItem("synchronizedOutput", "Synchronized Output", "DECSET 2026", "Prevents screen tearing during high-throughput CLI animations", synchronizedOutput),
                    ProtocolItem("decscusr", "DECSCUSR Cursor Shape", "VT520 / xterm", "Configurable cursor geometry: block, underline, or bar with blink", decscusr),
                    ProtocolItem("decModes", "DEC Terminal Modes", "DECAWM / DECOM / DECSTBM", "Margins, origin mode, auto-wrap and window operations", decModes)
                )
            ),
            ProtocolCategory(
                id = "osc",
                title = "8. OSC Extensions",
                items = listOf(
                    ProtocolItem("oscTitle", "OSC 0/1/2 Window Title", "xterm OSC 0", "Dynamic window and tab title updates from shell or scripts", oscTitle),
                    ProtocolItem("oscHyperlinks", "OSC 8 Hyperlinks", "egmontkob spec", "Clickable embedded hyperlinks in terminal text", oscHyperlinks),
                    ProtocolItem("oscClipboard", "OSC 52 Clipboard Bridge", "xterm OSC 52", "Allows remote/local programs to copy and paste via system clipboard", oscClipboard),
                    ProtocolItem("oscNotifications", "OSC 9 Notifications", "iTerm2 / Ghostty", "Desktop push notification bridge for long-running commands", oscNotifications)
                )
            ),
            ProtocolCategory(
                id = "shell",
                title = "9. Shell Integration",
                items = listOf(
                    ProtocolItem("osc133SemanticPrompts", "OSC 133 Semantic Prompts", "FTCS / VS Code", "Semantic markers: Prompt (A), Input (B), Output (C), Exit code (D)", osc133SemanticPrompts),
                    ProtocolItem("osc7Cwd", "OSC 7 Working Directory", "file:// URI", "Tracks and displays current working directory in the status header", osc7Cwd)
                )
            ),
            ProtocolCategory(
                id = "android",
                title = "10. Android Runtime Optimizations",
                items = listOf(
                    ProtocolItem("imeComposition", "IME Composition Support", "InputConnection", "Maps soft keyboard composition spans directly to terminal input", imeComposition),
                    ProtocolItem("vsyncRendering", "Vsync Battery Throttling", "Choreographer", "Batches redraws to 60fps frame intervals to conserve battery/thermals", vsyncRendering)
                )
            ),
            ProtocolCategory(
                id = "terminfo",
                title = "11. Terminfo Advertising",
                items = listOf(
                    ProtocolItem("terminfoAdvertising", "Terminfo TERM Variable", "TERM=xterm-256color", "Advertises proper terminal capabilities to curses/ncurses programs", terminfoAdvertising)
                )
            )
        )
    }

    fun toggle(itemId: String): ProtocolSettings {
        return when (itemId) {
            "ecma48" -> copy(ecma48 = !ecma48)
            "vtSequences" -> copy(vtSequences = !vtSequences)
            "xtermSequences" -> copy(xtermSequences = !xtermSequences)
            "deviceAttributes" -> copy(deviceAttributes = !deviceAttributes)
            "xtVersion" -> copy(xtVersion = !xtVersion)
            "decrqm" -> copy(decrqm = !decrqm)
            "rep" -> copy(rep = !rep)
            "xtGetTcap" -> copy(xtGetTcap = !xtGetTcap)

            "trueColor" -> copy(trueColor = !trueColor)
            "oscPalette" -> copy(oscPalette = !oscPalette)
            "underlineColor" -> copy(underlineColor = !underlineColor)

            "graphemeSegmentation" -> copy(graphemeSegmentation = !graphemeSegmentation)
            "eastAsianWidth" -> copy(eastAsianWidth = !eastAsianWidth)
            "emojiZwj" -> copy(emojiZwj = !emojiZwj)
            "styledUnderlines" -> copy(styledUnderlines = !styledUnderlines)

            "kittyGraphics" -> copy(kittyGraphics = !kittyGraphics)
            "sixel" -> copy(sixel = !sixel)
            "iterm2Images" -> copy(iterm2Images = !iterm2Images)
            "xtSmGraphics" -> copy(xtSmGraphics = !xtSmGraphics)

            "kittyKeyboard" -> copy(kittyKeyboard = !kittyKeyboard)
            "bracketedPaste" -> copy(bracketedPaste = !bracketedPaste)
            "focusReporting" -> copy(focusReporting = !focusReporting)

            "mouseTracking" -> copy(mouseTracking = !mouseTracking)

            "alternateScreen" -> copy(alternateScreen = !alternateScreen)
            "synchronizedOutput" -> copy(synchronizedOutput = !synchronizedOutput)
            "decscusr" -> copy(decscusr = !decscusr)
            "decModes" -> copy(decModes = !decModes)

            "oscTitle" -> copy(oscTitle = !oscTitle)
            "oscHyperlinks" -> copy(oscHyperlinks = !oscHyperlinks)
            "oscClipboard" -> copy(oscClipboard = !oscClipboard)
            "oscNotifications" -> copy(oscNotifications = !oscNotifications)

            "osc133SemanticPrompts" -> copy(osc133SemanticPrompts = !osc133SemanticPrompts)
            "osc7Cwd" -> copy(osc7Cwd = !osc7Cwd)

            "imeComposition" -> copy(imeComposition = !imeComposition)
            "vsyncRendering" -> copy(vsyncRendering = !vsyncRendering)

            "terminfoAdvertising" -> copy(terminfoAdvertising = !terminfoAdvertising)
            else -> this
        }
    }
}
