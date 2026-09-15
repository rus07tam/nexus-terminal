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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.KeyType
import com.example.model.KeyboardLayout
import com.example.model.ProtocolItem
import com.example.model.ToolbarKey
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
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Tab Row: Keyboard Toolbar and Protocols
        TabRow(
            selectedTabIndex = section.ordinal,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[section.ordinal]),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            Tab(
                selected = section == SettingsSection.KEYBOARD,
                onClick = { viewModel.selectSettingsSection(SettingsSection.KEYBOARD) },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Keyboard Toolbar",
                            fontWeight = if (section == SettingsSection.KEYBOARD) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            )
            Tab(
                selected = section == SettingsSection.PROTOCOLS,
                onClick = { viewModel.selectSettingsSection(SettingsSection.PROTOCOLS) },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Protocols & Demo",
                            fontWeight = if (section == SettingsSection.PROTOCOLS) FontWeight.Bold else FontWeight.Normal
                        )
                    }
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
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Live Preview Card
        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "TOOLBAR LIVE PREVIEW",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    KeyboardToolbar(
                        layout = layout,
                        isCtrlActive = false,
                        isAltActive = false,
                        onKeyClick = {}
                    )
                }
            }
        }

        // Quick Presets
        item {
            Text(
                text = "Arrangement Presets",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Termux", "Vim", "Tmux").forEach { preset ->
                    SuggestionChip(
                        onClick = { viewModel.applyKeyboardPreset(preset) },
                        label = { Text(preset, fontWeight = FontWeight.Medium) },
                        modifier = Modifier.weight(1f).testTag("preset_$preset")
                    )
                }
            }
        }

        // Rows Management
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Configured Rows (${layout.rows.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Button(
                    onClick = { viewModel.addKeyboardRow() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Row", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        itemsIndexed(layout.rows) { rowIndex, row ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    // Row Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Row #${rowIndex + 1} • ${row.keys.size} keys",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = { addingKeyToRowIndex = rowIndex },
                                modifier = Modifier.height(30.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Add Key", fontSize = 11.sp)
                            }
                            if (row.keys.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { viewModel.clearRow(rowIndex) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Clear row keys",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            if (layout.rows.size > 1) {
                                Spacer(modifier = Modifier.width(2.dp))
                                IconButton(
                                    onClick = { viewModel.deleteKeyboardRow(rowIndex) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete row",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Keys in Row with reorder buttons and delete
                    if (row.keys.isEmpty()) {
                        Text(
                            text = "No keys in this row yet. Tap '+ Add Key' to pick keys.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        val scrollState = rememberScrollState()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(scrollState),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            row.keys.forEachIndexed { keyIndex, key ->
                                RowKeyItem(
                                    key = key,
                                    isFirst = keyIndex == 0,
                                    isLast = keyIndex == row.keys.size - 1,
                                    onMoveLeft = {
                                        viewModel.moveKeyInRow(rowIndex, keyIndex, keyIndex - 1)
                                    },
                                    onMoveRight = {
                                        viewModel.moveKeyInRow(rowIndex, keyIndex, keyIndex + 1)
                                    },
                                    onDelete = {
                                        viewModel.deleteKeyFromRow(rowIndex, key.id)
                                    }
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
        EnhancedAddKeyDialog(
            onDismiss = { addingKeyToRowIndex = null },
            onAdd = { key ->
                viewModel.addKeyToRow(rowIndex, key)
                addingKeyToRowIndex = null
            }
        )
    }
}

@Composable
fun RowKeyItem(
    key: ToolbarKey,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onDelete: () -> Unit
) {
    val isSwitch = key.type == KeyType.SWITCH
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Reorder left
            if (!isFirst) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Move key left",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(14.dp)
                        .clickable(onClick = onMoveLeft)
                )
            }

            Column(
                modifier = Modifier.padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = key.displayLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isSwitch) "SWITCH" else "PRESS",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSwitch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
            }

            // Reorder right
            if (!isLast) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Move key right",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(14.dp)
                        .clickable(onClick = onMoveRight)
                )
            }

            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Delete key",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .size(14.dp)
                    .clickable(onClick = onDelete)
            )
        }
    }
}

data class KeyCatalogItem(
    val label: String,
    val sequence: String,
    val type: KeyType = KeyType.PRESS
)

@Composable
fun EnhancedAddKeyDialog(
    onDismiss: () -> Unit,
    onAdd: (ToolbarKey) -> Unit
) {
    var selectedCategoryIndex by remember { mutableStateOf(0) }
    val categories = listOf("Controls", "Arrows", "Symbols", "Shortcuts", "Fn Keys", "Custom")

    val controlKeys = listOf(
        KeyCatalogItem("ESC", "\u001b"),
        KeyCatalogItem("TAB", "\t"),
        KeyCatalogItem("CTRL", "CTRL", KeyType.SWITCH),
        KeyCatalogItem("ALT", "ALT", KeyType.SWITCH),
        KeyCatalogItem("HOME", "\u001b[H"),
        KeyCatalogItem("END", "\u001b[F"),
        KeyCatalogItem("PGUP", "\u001b[5~"),
        KeyCatalogItem("PGDN", "\u001b[6~"),
        KeyCatalogItem("DEL", "\u001b[3~"),
        KeyCatalogItem("INS", "\u001b[2~")
    )

    val arrowKeys = listOf(
        KeyCatalogItem("↑", "\u001b[A"),
        KeyCatalogItem("↓", "\u001b[B"),
        KeyCatalogItem("←", "\u001b[D"),
        KeyCatalogItem("→", "\u001b[C")
    )

    val symbolKeys = listOf(
        KeyCatalogItem("|", "|"),
        KeyCatalogItem("~", "~"),
        KeyCatalogItem("-", "-"),
        KeyCatalogItem("_", "_"),
        KeyCatalogItem("/", "/"),
        KeyCatalogItem("\\", "\\"),
        KeyCatalogItem("&", "&"),
        KeyCatalogItem(";", ";"),
        KeyCatalogItem(":", ":"),
        KeyCatalogItem("$", "$"),
        KeyCatalogItem("{", "{"),
        KeyCatalogItem("}", "}"),
        KeyCatalogItem("[", "["),
        KeyCatalogItem("]", "]"),
        KeyCatalogItem("(", "("),
        KeyCatalogItem(")", ")"),
        KeyCatalogItem("\"", "\""),
        KeyCatalogItem("'", "'"),
        KeyCatalogItem("`", "`")
    )

    val shortcutKeys = listOf(
        KeyCatalogItem("Ctrl+C", "\u0003"),
        KeyCatalogItem("Ctrl+D", "\u0004"),
        KeyCatalogItem("Ctrl+Z", "\u001a"),
        KeyCatalogItem("Ctrl+L", "\u000c"),
        KeyCatalogItem("Ctrl+A", "\u0001"),
        KeyCatalogItem("Ctrl+E", "\u0005"),
        KeyCatalogItem("Ctrl+R", "\u0012")
    )

    val fnKeys = (1..12).map { n ->
        val code = when (n) {
            1 -> "\u001bOP"
            2 -> "\u001bOQ"
            3 -> "\u001bOR"
            4 -> "\u001bOS"
            5 -> "\u001b[15~"
            6 -> "\u001b[17~"
            7 -> "\u001b[18~"
            8 -> "\u001b[19~"
            9 -> "\u001b[20~"
            10 -> "\u001b[21~"
            11 -> "\u001b[23~"
            else -> "\u001b[24~"
        }
        KeyCatalogItem("F$n", code)
    }

    var customSequence by remember { mutableStateOf("") }
    var customLabel by remember { mutableStateOf("") }
    var customKeyType by remember { mutableStateOf(KeyType.PRESS) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Toolbar Key",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Category Pills
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(categories) { index, catName ->
                        FilterChip(
                            selected = selectedCategoryIndex == index,
                            onClick = { selectedCategoryIndex = index },
                            label = { Text(catName, fontSize = 11.sp) },
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }

                // Keys grid / palette
                val currentKeys = when (selectedCategoryIndex) {
                    0 -> controlKeys
                    1 -> arrowKeys
                    2 -> symbolKeys
                    3 -> shortcutKeys
                    4 -> fnKeys
                    else -> emptyList()
                }

                if (selectedCategoryIndex < 5) {
                    Text(
                        text = "Tap a key to instantly add it:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(currentKeys.chunked(4)) { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowItems.forEach { item ->
                                    Button(
                                        onClick = {
                                            onAdd(
                                                ToolbarKey(
                                                    id = UUID.randomUUID().toString(),
                                                    type = item.type,
                                                    sequence = item.sequence,
                                                    label = item.label
                                                )
                                            )
                                        },
                                        modifier = Modifier.weight(1f).height(36.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            contentColor = MaterialTheme.colorScheme.onSurface
                                        ),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(2.dp)
                                    ) {
                                        Text(
                                            text = item.label,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1
                                        )
                                    }
                                }
                                repeat(4 - rowItems.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                } else {
                    // Custom key inputs
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customSequence,
                            onValueChange = { customSequence = it },
                            label = { Text("Sequence (e.g. \\u001b, CTRL, -)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = customLabel,
                            onValueChange = { customLabel = it },
                            label = { Text("Display Label (optional)") },
                            placeholder = { Text("e.g. ESC, TAB") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { customKeyType = KeyType.PRESS }
                            ) {
                                RadioButton(
                                    selected = customKeyType == KeyType.PRESS,
                                    onClick = { customKeyType = KeyType.PRESS }
                                )
                                Text("Press", fontSize = 13.sp)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { customKeyType = KeyType.SWITCH }
                            ) {
                                RadioButton(
                                    selected = customKeyType == KeyType.SWITCH,
                                    onClick = { customKeyType = KeyType.SWITCH }
                                )
                                Text("Switch (CTRL/ALT)", fontSize = 13.sp)
                            }
                        }

                        Button(
                            onClick = {
                                if (customSequence.isNotBlank()) {
                                    val finalSeq = customSequence.replace("\\u001b", "\u001b")
                                        .replace("\\r", "\r")
                                        .replace("\\n", "\n")
                                        .replace("\\t", "\t")
                                    onAdd(
                                        ToolbarKey(
                                            id = UUID.randomUUID().toString(),
                                            type = customKeyType,
                                            sequence = finalSeq,
                                            label = customLabel.takeIf { it.isNotBlank() }
                                        )
                                    )
                                }
                            },
                            enabled = customSequence.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Add Custom Key")
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun ProtocolsSettingsTab(viewModel: TerminalViewModel) {
    val settings by viewModel.protocolSettings.collectAsState()
    val categories = remember(settings) { settings.getCategories() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Hero Showcase Session Launcher Card
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_demo_showcase"),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Protocols Showcase Session",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Launches an interactive stub session demonstrating rich terminal features: 24-bit TrueColor, ANSI 256, Sixel graphics, OSC 8 hyperlinks, OSC 9 system notifications, extended underlines (double, curly, dotted, dashed) with separate colors, and interactive shell typing.",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Button(
                        onClick = { viewModel.launchDemoSession() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("btn_launch_demo_session")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Launch Showcase Session", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Emulation Standards & Protocols",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        categories.forEach { category ->
            item {
                Text(
                    text = category.title,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = item.spec,
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Text(
                    text = item.description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = item.isEnabled,
                onCheckedChange = { onToggle() }
            )
        }
    }
}
