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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import com.example.model.KeyType
import com.example.model.KeyboardLayout
import com.example.model.ProtocolItem
import com.example.model.ToolbarKey
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.RoseNeon
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.viewmodel.SettingsSection
import com.example.viewmodel.TerminalViewModel
import java.util.UUID

@Composable
fun SettingsScreen(
    viewModel: TerminalViewModel,
    modifier: Modifier = Modifier
) {
    val section by viewModel.settingsSection.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        // Tab Row: Keyboard and Protocols
        TabRow(
            selectedTabIndex = section.ordinal,
            containerColor = Slate900,
            contentColor = CyanNeon,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[section.ordinal]),
                    color = CyanNeon
                )
            }
        ) {
            Tab(
                selected = section == SettingsSection.KEYBOARD,
                onClick = { viewModel.selectSettingsSection(SettingsSection.KEYBOARD) },
                text = {
                    Text(
                        text = "Keyboard Toolbar",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (section == SettingsSection.KEYBOARD) CyanNeon else Color.Gray
                    )
                }
            )
            Tab(
                selected = section == SettingsSection.PROTOCOLS,
                onClick = { viewModel.selectSettingsSection(SettingsSection.PROTOCOLS) },
                text = {
                    Text(
                        text = "Protocols & Standards",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (section == SettingsSection.PROTOCOLS) CyanNeon else Color.Gray
                    )
                }
            )
        }

        when (section) {
            SettingsSection.KEYBOARD -> KeyboardSettingsTab(viewModel)
            SettingsSection.PROTOCOLS -> ProtocolsSettingsTab(viewModel)
        }
    }
}

@Composable
fun KeyboardSettingsTab(viewModel: TerminalViewModel) {
    val layout by viewModel.keyboardLayout.collectAsState()
    var addingKeyToRowIndex by remember { mutableStateOf<Int?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Toolbar Presets",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Quickly load common developer keyboard arrangements",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("Termux", "Vim", "Tmux").forEach { preset ->
                    Button(
                        onClick = { viewModel.applyKeyboardPreset(preset) },
                        colors = ButtonDefaults.buttonColors(containerColor = Slate800, contentColor = CyanNeon),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("preset_$preset")
                    ) {
                        Text(preset, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Configured Rows (${layout.rows.size})",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Button(
                    onClick = { viewModel.addKeyboardRow() },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon, contentColor = Slate950),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Row", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(layout.rows.indices.toList()) { rowIndex ->
            val row = layout.rows[rowIndex]
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Row #${rowIndex + 1} (${row.keys.size} keys)",
                            color = CyanNeon,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Row {
                            Button(
                                onClick = { addingKeyToRowIndex = rowIndex },
                                colors = ButtonDefaults.buttonColors(containerColor = Slate800, contentColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("+ Key", fontSize = 11.sp)
                            }
                            if (layout.rows.size > 1) {
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(onClick = { viewModel.deleteKeyboardRow(rowIndex) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete row", tint = RoseNeon)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Horizontal preview and delete keys
                    val scrollState = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        row.keys.forEach { key ->
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Slate800)
                                    .border(1.dp, Slate700, RoundedCornerShape(6.dp))
                                    .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = key.displayLabel,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = if (key.type == KeyType.SWITCH) "SWITCH" else "PRESS",
                                        color = if (key.type == KeyType.SWITCH) EmeraldNeon else AmberNeon,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Delete key",
                                    tint = Color.Gray,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { viewModel.deleteKeyFromRow(rowIndex, key.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Key Dialog
    addingKeyToRowIndex?.let { rowIndex ->
        AddKeyDialog(
            onDismiss = { addingKeyToRowIndex = null },
            onAdd = { key ->
                viewModel.addKeyToRow(rowIndex, key)
                addingKeyToRowIndex = null
            }
        )
    }
}

@Composable
fun AddKeyDialog(
    onDismiss: () -> Unit,
    onAdd: (ToolbarKey) -> Unit
) {
    var sequence by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var keyType by remember { mutableStateOf(KeyType.PRESS) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Toolbar Key",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
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
                    value = sequence,
                    onValueChange = { sequence = it },
                    label = { Text("Sequence (e.g. \\u001b, CTRL, -)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = Slate700
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Display Label (optional)") },
                    placeholder = { Text("e.g. ESC, TAB") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = Slate700
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Key Type: Press or Switch
                Column {
                    Text(
                        text = "Key Type:",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { keyType = KeyType.PRESS }
                    ) {
                        RadioButton(
                            selected = keyType == KeyType.PRESS,
                            onClick = { keyType = KeyType.PRESS },
                            colors = RadioButtonDefaults.colors(selectedColor = CyanNeon)
                        )
                        Text("Press (sends sequence immediately)", color = Color.White, fontSize = 12.sp)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { keyType = KeyType.SWITCH }
                    ) {
                        RadioButton(
                            selected = keyType == KeyType.SWITCH,
                            onClick = { keyType = KeyType.SWITCH },
                            colors = RadioButtonDefaults.colors(selectedColor = EmeraldNeon)
                        )
                        Text("Switch (toggles state, e.g. CTRL / ALT)", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (sequence.isNotBlank()) {
                        onAdd(
                            ToolbarKey(
                                id = UUID.randomUUID().toString(),
                                type = keyType,
                                sequence = sequence,
                                label = label.takeIf { it.isNotBlank() }
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Slate950)
            ) {
                Text("Add Key", fontWeight = FontWeight.Bold)
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

@Composable
fun ProtocolsSettingsTab(viewModel: TerminalViewModel) {
    val settings by viewModel.protocolSettings.collectAsState()
    val categories = remember(settings) { settings.getCategories() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Terminal Protocols & Standards",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Enable or disable terminal emulation protocols in real time",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
            )
        }

        categories.forEach { category ->
            item {
                Text(
                    text = category.title,
                    color = CyanNeon,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                )
            }
            items(category.items) { item ->
                ProtocolItemRow(
                    item = item,
                    onToggle = { viewModel.toggleProtocol(item.id) }
                )
            }
        }
    }
}

@Composable
fun ProtocolItemRow(
    item: ProtocolItem,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("protocol_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Slate800)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.spec,
                            color = AmberNeon,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Text(
                    text = item.description,
                    color = Color.Gray,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = item.isEnabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Slate950,
                    checkedTrackColor = EmeraldNeon,
                    uncheckedThumbColor = Color.LightGray,
                    uncheckedTrackColor = Slate800
                )
            )
        }
    }
}
