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
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
            .background(MaterialTheme.colorScheme.background)
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
            onToggleSoftKeyboard = { viewModel.toggleSoftKeyboard() }
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
    onToggleSoftKeyboard: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            // Row 1: Session Tabs
            val tabsScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(tabsScrollState),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                sessions.forEach { session ->
                    val isSelected = session.id == activeSessionId
                    val profileColor = try {
                        Color(android.graphics.Color.parseColor(session.profile.colorHex))
                    } catch (e: Exception) {
                        MaterialTheme.colorScheme.primary
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = { onSessionSelect(session.id) },
                        label = {
                            Text(
                                text = session.buffer.windowTitle.takeIf { it.isNotBlank() && it != "Terminal" }
                                    ?: "${session.profile.name} (${session.pid})",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1
                            )
                        },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(profileColor)
                            )
                        },
                        trailingIcon = if (sessions.size > 1) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close session",
                                    modifier = Modifier
                                        .size(13.dp)
                                        .clickable { onSessionClose(session.id) }
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                }

                // New Session '+' Button
                IconButton(
                    onClick = onNewSession,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("btn_new_session")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New session",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Row 2: Lock, AutoScroll and quick actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Top-left controls: Lock & AutoScroll chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = isLocked,
                        onClick = onToggleLock,
                        label = {
                            Text(
                                text = if (isLocked) "LOCKED" else "LOCK",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = "Toggle Lock",
                                modifier = Modifier.size(12.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ),
                        modifier = Modifier
                            .height(26.dp)
                            .testTag("btn_lock_terminal")
                    )

                    FilterChip(
                        selected = autoScroll,
                        onClick = onToggleAutoScroll,
                        label = {
                            Text(
                                text = if (autoScroll) "AUTO" else "PAUSED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Auto Scroll",
                                modifier = Modifier.size(12.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .height(26.dp)
                            .testTag("btn_auto_scroll")
                    )
                }

                // Action buttons on the right: Copy, Clear, Soft Keyboard
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onCopyOutput,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("btn_copy_output")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy all output",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    IconButton(
                        onClick = onClearScreen,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("btn_clear_screen")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear screen",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleSoftKeyboard,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("btn_toggle_soft_keyboard")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = "Soft keyboard",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
