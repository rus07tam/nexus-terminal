package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionProfile
import com.example.model.TransportType
import com.example.terminal.LocalShellSession
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.RoseNeon
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.viewmodel.TerminalViewModel

@Composable
fun TerminalScreen(
    viewModel: TerminalViewModel,
    modifier: Modifier = Modifier
) {
    val sessions by viewModel.sessions.collectAsState()
    val activeSessionId by viewModel.activeSessionId.collectAsState()
    val isLocked by viewModel.isLocked.collectAsState()
    val autoScroll by viewModel.autoScroll.collectAsState()
    val keyboardLayout by viewModel.keyboardLayout.collectAsState()
    val isCtrlActive by viewModel.isCtrlActive.collectAsState()
    val isAltActive by viewModel.isAltActive.collectAsState()
    val renderVersion by viewModel.renderVersion.collectAsState()
    val showSoftKeyboard by viewModel.showSoftKeyboard.collectAsState()

    val activeSession = viewModel.getActiveSession()
    val focusRequester = remember { FocusRequester() }
    var inputText by remember { mutableStateOf(TextFieldValue("")) }

    LaunchedEffect(showSoftKeyboard) {
        if (showSoftKeyboard) {
            focusRequester.requestFocus()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
            .imePadding()
    ) {
        // TOP BAR: Session Tabs & Lock/AutoScroll buttons
        TerminalTopBar(
            sessions = sessions,
            activeSessionId = activeSessionId,
            isLocked = isLocked,
            autoScroll = autoScroll,
            onSessionSelect = { viewModel.switchSession(it) },
            onSessionClose = { viewModel.closeSession(it) },
            onNewSession = {
                val profile = viewModel.profiles.value.firstOrNull() ?: ConnectionProfile(
                    id = "local_shell_${System.currentTimeMillis()}",
                    name = "Local Shell",
                    colorHex = "#22C55E",
                    transport = TransportType.LOCAL_SHELL
                )
                viewModel.createSession(profile)
            },
            onToggleLock = { viewModel.toggleLock() },
            onToggleAutoScroll = { viewModel.toggleAutoScroll() },
            onClearScreen = { viewModel.clearScreen() },
            onCopyOutput = { viewModel.copyTerminalOutput() },
            onToggleSoftKeyboard = { viewModel.toggleSoftKeyboard() },
            onRunDemo = { activeSession?.runProtocolDemo() }
        )

        // MAIN TERMINAL VIEW
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (activeSession != null) {
                TerminalCanvasView(
                    session = activeSession,
                    isLocked = isLocked,
                    autoScroll = autoScroll,
                    renderVersion = renderVersion,
                    onMouseClick = { row, col ->
                        activeSession.sendMouseClick(row, col)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No active session. Click '+' to start a shell.",
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Hidden text input field for IME / Soft Keyboard capture
            BasicTextField(
                value = inputText,
                onValueChange = { newVal ->
                    if (newVal.text.length > inputText.text.length) {
                        // User typed new characters
                        val added = newVal.text.substring(inputText.text.length)
                        viewModel.sendInput(added)
                    } else if (newVal.text.length < inputText.text.length) {
                        // Backspace pressed
                        viewModel.sendInput("\b")
                    }
                    inputText = newVal
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send,
                    keyboardType = KeyboardType.Ascii
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        viewModel.sendInput("\n")
                        inputText = TextFieldValue("")
                    }
                ),
                modifier = Modifier
                    .size(1.dp)
                    .focusRequester(focusRequester)
            )
        }

        // BOTTOM KEYBOARD TOOLBAR
        KeyboardToolbar(
            layout = keyboardLayout,
            isCtrlActive = isCtrlActive,
            isAltActive = isAltActive,
            onKeyClick = { key ->
                viewModel.sendKey(key)
            }
        )
    }
}

@Composable
fun TerminalTopBar(
    sessions: List<LocalShellSession>,
    activeSessionId: String?,
    isLocked: Boolean,
    autoScroll: Boolean,
    onSessionSelect: (String) -> Unit,
    onSessionClose: (String) -> Unit,
    onNewSession: () -> Unit,
    onToggleLock: () -> Unit,
    onToggleAutoScroll: () -> Unit,
    onClearScreen: () -> Unit,
    onCopyOutput: () -> Unit,
    onToggleSoftKeyboard: () -> Unit,
    onRunDemo: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Slate900,
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Row 1: Session Tabs
            val tabsScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(tabsScrollState)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                sessions.forEach { session ->
                    val isSelected = session.id == activeSessionId
                    val profileColor = try {
                        Color(android.graphics.Color.parseColor(session.profile.colorHex))
                    } catch (e: Exception) {
                        EmeraldNeon
                    }

                    Row(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) Slate800 else Slate900)
                            .border(1.dp, if (isSelected) CyanNeon else Slate700, RoundedCornerShape(6.dp))
                            .clickable { onSessionSelect(session.id) }
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(profileColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = session.buffer.windowTitle.takeIf { it.isNotBlank() && it != "Terminal" }
                                ?: "${session.profile.name} (${session.pid})",
                            color = if (isSelected) Color.White else Color.LightGray,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                        if (sessions.size > 1) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close session",
                                tint = Color.Gray,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { onSessionClose(session.id) }
                            )
                        }
                    }
                }

                // New Session '+' Button
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Slate800)
                        .border(1.dp, Slate700, RoundedCornerShape(6.dp))
                        .clickable(onClick = onNewSession)
                        .testTag("btn_new_session"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New session",
                        tint = CyanNeon,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Row 2: Lock, AutoScroll and quick actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Top-left controls: Lock & AutoScroll buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Lock button: stops rendering updates
                    Box(
                        modifier = Modifier
                            .height(30.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isLocked) AmberNeon else Slate800)
                            .border(1.dp, if (isLocked) AmberNeon else Slate700, RoundedCornerShape(6.dp))
                            .clickable(onClick = onToggleLock)
                            .padding(horizontal = 8.dp)
                            .testTag("btn_lock_terminal"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = "Toggle Lock",
                                tint = if (isLocked) Slate950 else Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isLocked) "LOCKED" else "LOCK",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isLocked) Slate950 else Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Auto-scroll button: toggle snapping to bottom
                    Box(
                        modifier = Modifier
                            .height(30.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (autoScroll) CyanNeon else Slate800)
                            .border(1.dp, if (autoScroll) CyanNeon else Slate700, RoundedCornerShape(6.dp))
                            .clickable(onClick = onToggleAutoScroll)
                            .padding(horizontal = 8.dp)
                            .testTag("btn_auto_scroll"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Auto Scroll",
                                tint = if (autoScroll) Slate950 else Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (autoScroll) "AUTO-SCROLL ON" else "SCROLL PAUSED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (autoScroll) Slate950 else Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Action buttons on the right: Demo, Copy, Clear, Soft Keyboard
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Protocol demo button
                    Box(
                        modifier = Modifier
                            .height(30.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Slate800)
                            .border(1.dp, EmeraldNeon, RoundedCornerShape(6.dp))
                            .clickable(onClick = onRunDemo)
                            .padding(horizontal = 6.dp)
                            .testTag("btn_demo"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "DEMO",
                            color = EmeraldNeon,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    IconButton(
                        onClick = onCopyOutput,
                        modifier = Modifier.size(30.dp).testTag("btn_copy_output")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy all output",
                            tint = Color.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onClearScreen,
                        modifier = Modifier.size(30.dp).testTag("btn_clear_screen")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear screen",
                            tint = Color.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleSoftKeyboard,
                        modifier = Modifier.size(30.dp).testTag("btn_toggle_soft_keyboard")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = "Soft keyboard",
                            tint = CyanNeon,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
