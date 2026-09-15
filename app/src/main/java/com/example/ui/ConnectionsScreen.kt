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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.viewmodel.TerminalViewModel
import java.util.UUID

@Composable
fun ConnectionsScreen(
    viewModel: TerminalViewModel,
    modifier: Modifier = Modifier
) {
    val profiles by viewModel.profiles.collectAsState()
    var editingProfile by remember { mutableStateOf<ConnectionProfile?>(null) }
    var isAddingNew by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Slate950,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isAddingNew = true },
                containerColor = CyanNeon,
                contentColor = Slate950,
                modifier = Modifier.testTag("fab_add_connection")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Connection")
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
                text = "Connection Profiles",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Configure terminal transports and shell environments",
                color = Color.Gray,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(profiles) { profile ->
                    ConnectionProfileCard(
                        profile = profile,
                        onConnect = { viewModel.createSession(profile) },
                        onEdit = { editingProfile = profile },
                        onDelete = { viewModel.deleteProfile(profile.id) }
                    )
                }
            }
        }

        // Add / Edit Dialog
        if (isAddingNew || editingProfile != null) {
            val initial = editingProfile ?: ConnectionProfile(
                id = UUID.randomUUID().toString(),
                name = "New Connection",
                colorHex = "#06B6D4",
                transport = TransportType.LOCAL_SHELL
            )
            ConnectionEditDialog(
                profile = initial,
                isNew = isAddingNew,
                onDismiss = {
                    isAddingNew = false
                    editingProfile = null
                },
                onSave = { updated ->
                    viewModel.saveProfile(updated)
                    isAddingNew = false
                    editingProfile = null
                }
            )
        }
    }
}

@Composable
fun ConnectionProfileCard(
    profile: ConnectionProfile,
    onConnect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val markerColor = try {
        Color(android.graphics.Color.parseColor(profile.colorHex))
    } catch (e: Exception) {
        EmeraldNeon
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_connection_${profile.id}"),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
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
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(markerColor)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = profile.name,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.LightGray)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Transport badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (profile.transport.isAvailable) Slate800 else Slate800.copy(alpha = 0.5f))
                        .border(
                            1.dp,
                            if (profile.transport.isAvailable) CyanNeon else Slate700,
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = profile.transport.displayName,
                        color = if (profile.transport.isAvailable) CyanNeon else Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (!profile.transport.isAvailable) {
                    Text(
                        text = "(Planned expansion)",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }

            if (profile.initialCommand.isNotBlank() || profile.workingDir.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                if (profile.initialCommand.isNotBlank()) {
                    Text(
                        text = "Cmd: ${profile.initialCommand}",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                if (profile.workingDir.isNotBlank()) {
                    Text(
                        text = "Dir: ${profile.workingDir}",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onConnect,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon, contentColor = Slate950),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_connect_${profile.id}")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Launch Session",
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionEditDialog(
    profile: ConnectionProfile,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (ConnectionProfile) -> Unit
) {
    var name by remember { mutableStateOf(profile.name) }
    var colorHex by remember { mutableStateOf(profile.colorHex) }
    var selectedTransport by remember { mutableStateOf(profile.transport) }
    var initialCommand by remember { mutableStateOf(profile.initialCommand) }
    var workingDir by remember { mutableStateOf(profile.workingDir) }
    var transportDropdownExpanded by remember { mutableStateOf(false) }

    val presetColors = listOf(
        "#22C55E", // Emerald
        "#06B6D4", // Cyan
        "#3B82F6", // Blue
        "#A855F7", // Purple
        "#F59E0B", // Amber
        "#EF4444", // Red
        "#EC4899"  // Pink
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isNew) "New Connection" else "Edit Connection",
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Connection Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = Slate700
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Color Marker Selection
                Column {
                    Text(
                        text = "Color Marker:",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        presetColors.forEach { hex ->
                            val isSelected = colorHex.equals(hex, ignoreCase = true)
                            val col = Color(android.graphics.Color.parseColor(hex))
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(col)
                                    .border(
                                        2.dp,
                                        if (isSelected) Color.White else Color.Transparent,
                                        CircleShape
                                    )
                                    .clickable { colorHex = hex }
                            )
                        }
                    }
                }

                // Transport Selector
                ExposedDropdownMenuBox(
                    expanded = transportDropdownExpanded,
                    onExpandedChange = { transportDropdownExpanded = !transportDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedTransport.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Transport") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = transportDropdownExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = Slate700
                        ),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = transportDropdownExpanded,
                        onDismissRequest = { transportDropdownExpanded = false }
                    ) {
                        TransportType.values().forEach { transport ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = transport.displayName,
                                            fontWeight = if (transport == selectedTransport) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Text(
                                            text = transport.badgeDescription,
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                },
                                onClick = {
                                    selectedTransport = transport
                                    transportDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = initialCommand,
                    onValueChange = { initialCommand = it },
                    label = { Text("Initial Command (optional)") },
                    placeholder = { Text("e.g. uname -a") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = Slate700
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = workingDir,
                    onValueChange = { workingDir = it },
                    label = { Text("Working Directory (optional)") },
                    placeholder = { Text("e.g. /data/data/...") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = Slate700
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        profile.copy(
                            name = name.takeIf { it.isNotBlank() } ?: "Terminal",
                            colorHex = colorHex,
                            transport = selectedTransport,
                            initialCommand = initialCommand,
                            workingDir = workingDir
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Slate950)
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        },
        containerColor = Slate900
    )
}
