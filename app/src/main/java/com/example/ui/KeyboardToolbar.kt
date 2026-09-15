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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.KeyType
import com.example.model.KeyboardLayout
import com.example.model.ToolbarKey
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950

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
        color = Slate950,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            layout.rows.forEachIndexed { rowIndex, row ->
                val scrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
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
    val bg = when {
        key.type == KeyType.SWITCH && isSwitchActive -> EmeraldNeon
        key.type == KeyType.SWITCH -> Slate800
        else -> Slate900
    }
    val contentColor = when {
        key.type == KeyType.SWITCH && isSwitchActive -> Slate950
        key.type == KeyType.SWITCH -> MaterialTheme.colorScheme.primary
        else -> Color(0xFFF1F5F9)
    }
    val borderCol = when {
        key.type == KeyType.SWITCH && isSwitchActive -> EmeraldNeon
        else -> Slate700
    }

    Box(
        modifier = modifier
            .height(38.dp)
            .widthIn(min = 40.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(1.dp, borderCol, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = key.displayLabel,
            color = contentColor,
            fontSize = 12.sp,
            fontWeight = if (key.type == KeyType.SWITCH) FontWeight.Bold else FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            maxLines = 1
        )
    }
}
