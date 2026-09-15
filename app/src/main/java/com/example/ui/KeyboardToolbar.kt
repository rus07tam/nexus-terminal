package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.model.ToolbarKey

@Composable
fun KeyboardToolbar(
    layout: KeyboardLayout,
    isCtrlActive: Boolean,
    isAltActive: Boolean,
    onKeyClick: (ToolbarKey) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("keyboard_toolbar"),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            layout.rows.forEachIndexed { rowIndex, row ->
                val scrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                        .padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    row.keys.forEach { key ->
                        val isSwitchActive = when {
                            key.type == KeyType.SWITCH && key.sequence == "CTRL" -> isCtrlActive
                            key.type == KeyType.SWITCH && key.sequence == "ALT" -> isAltActive
                            else -> key.isActive
                        }
                        KeyboardKeyButton(
                            key = key,
                            isSwitchActive = isSwitchActive,
                            onClick = { onKeyClick(key) },
                            modifier = Modifier.testTag("key_${key.displayLabel}")
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KeyboardKeyButton(
    key: ToolbarKey,
    isSwitchActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSwitch = key.type == KeyType.SWITCH
    val bg = when {
        isSwitch && isSwitchActive -> MaterialTheme.colorScheme.primary
        isSwitch -> MaterialTheme.colorScheme.surfaceContainerHighest
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = when {
        isSwitch && isSwitchActive -> MaterialTheme.colorScheme.onPrimary
        isSwitch -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val borderCol = when {
        isSwitch && isSwitchActive -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Box(
        modifier = modifier
            .height(32.dp)
            .widthIn(min = 36.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(0.75.dp, borderCol, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = key.displayLabel,
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = if (isSwitch || key.displayLabel.length <= 3) FontWeight.Bold else FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            maxLines = 1
        )
    }
}
