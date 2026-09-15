package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
fun SessionsScreen(
    viewModel: TerminalViewModel,
    modifier: Modifier = Modifier
) {
    val sessions by viewModel.sessions.collectAsState()
    val activeSessionId by viewModel.activeSessionId.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val profile = viewModel.profiles.value.firstOrNull() ?: ConnectionProfile(
                        id = "local_shell_${System.currentTimeMillis()}",
                        name = "Local Shell",
                        colorHex = "#22C55E",
                        transport = TransportType.LOCAL_SHELL
                    )
                    viewModel.createSession(profile)
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.testTag("fab_new_session")
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Session")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "Active Sessions",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Manage running processes and terminal instances (${sessions.size} total)",
                color = Color.Gray,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            if (sessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No active terminal sessions",
                            color = Color.LightGray,
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val profile = viewModel.profiles.value.firstOrNull() ?: ConnectionProfile(
                                    id = "local_shell_${System.currentTimeMillis()}",
                                    name = "Local Shell",
                                    colorHex = "#22C55E",
                                    transport = TransportType.LOCAL_SHELL
                                )
                                viewModel.createSession(profile)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon, contentColor = Slate950)
                        ) {
                            Text("Launch Shell", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(sessions) { session ->
                        SessionCard(
                            session = session,
                            isActive = (session.id == activeSessionId),
                            onSwitchTo = { viewModel.switchSession(session.id) },
                            onRestart = { viewModel.restartSession(session.id) },
                            onClose = { viewModel.closeSession(session.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SessionCard(
    session: LocalShellSession,
    isActive: Boolean,
    onSwitchTo: () -> Unit,
    onRestart: () -> Unit,
    onClose: () -> Unit
) {
    val profileColor = try {
        Color(android.graphics.Color.parseColor(session.profile.colorHex))
    } catch (e: Exception) {
        EmeraldNeon
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSwitchTo)
            .testTag("card_session_${session.id}"),
        colors = CardDefaults.cardColors(containerColor = if (isActive) Slate800 else Slate900),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isActive) CyanNeon else Slate800
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(profileColor)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = session.buffer.windowTitle.takeIf { it.isNotBlank() && it != "Terminal" }
                            ?: session.profile.name,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Status chip
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (session.isRunning) EmeraldNeon else RoseNeon)
                    )
                    Text(
                        text = if (session.isRunning) "RUNNING" else "EXITED (${session.exitCode ?: 0})",
                        color = if (session.isRunning) EmeraldNeon else RoseNeon,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Details info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "PID: ${session.pid}",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "CWD: ${session.buffer.cwd}",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onSwitchTo,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isActive) CyanNeon else Slate700,
                        contentColor = if (isActive) Slate950 else Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isActive) "Active Terminal" else "Switch Here",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                IconButton(
                    onClick = onRestart,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Slate800)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Restart", tint = AmberNeon)
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Slate800)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = RoseNeon)
                }
            }
        }
    }
}
