package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.terminal.CursorShape
import com.example.terminal.LocalShellSession
import com.example.terminal.TerminalChar
import com.example.terminal.TerminalColors
import com.example.terminal.UnderlineStyle

@Composable
fun TerminalCanvasView(
    session: LocalShellSession,
    isLocked: Boolean,
    autoScroll: Boolean,
    renderVersion: Long,
    onMouseClick: (row: Int, col: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val buffer = session.buffer
    val listState = rememberLazyListState()

    var clickedHyperlink by remember { mutableStateOf<String?>(null) }

    // Cursor blink animation
    val infiniteTransition = rememberInfiniteTransition(label = "cursor_blink")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_alpha"
    )

    // Collect all lines: history + active screen rows
    val allRows = remember(renderVersion, isLocked) {
        val rowsList = mutableListOf<Array<TerminalChar>>()
        rowsList.addAll(buffer.history)
        for (r in 0 until buffer.rows) {
            rowsList.add(buffer.activeLines[r])
        }
        rowsList
    }

    val historyCount = buffer.history.size

    // Auto-scroll to bottom on new output if enabled and not locked
    LaunchedEffect(allRows.size, renderVersion, autoScroll, isLocked) {
        if (autoScroll && !isLocked && allRows.isNotEmpty()) {
            listState.scrollToItem(allRows.size - 1)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalColors.DefaultBackground)
            .testTag("terminal_canvas")
            .pointerInput(session.id) {
                detectTapGestures(
                    onTap = { offset ->
                        val cellH = 20f
                        val cellW = 10f
                        val row = (offset.y / cellH).toInt().coerceIn(0, buffer.rows - 1)
                        val col = (offset.x / cellW).toInt().coerceIn(0, buffer.cols - 1)
                        onMouseClick(row, col)
                    }
                )
            }
    ) {
        SelectionContainer {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                itemsIndexed(allRows) { index, lineChars ->
                    val isCurrentCursorLine = (index == historyCount + buffer.cursorRow)
                    val cursorCol = if (isCurrentCursorLine && buffer.isCursorVisible) buffer.cursorCol else -1

                    TerminalLineRow(
                        chars = lineChars,
                        cursorCol = cursorCol,
                        cursorStyle = buffer.cursorStyle,
                        cursorAlpha = if (buffer.cursorStyle.isBlinking) cursorAlpha else 1f,
                        onHyperlinkClick = { url -> clickedHyperlink = url }
                    )
                }
            }
        }

        // Render inline graphics (Kitty APC, Sixel, iTerm2)
        for (graphic in buffer.graphics) {
            val visualRow = historyCount + graphic.row
            if (visualRow in 0..allRows.size) {
                Box(
                    modifier = Modifier
                        .offset(x = (graphic.col * 8).dp, y = (visualRow * 20).dp)
                        .padding(4.dp)
                ) {
                    Image(
                        bitmap = graphic.bitmap.asImageBitmap(),
                        contentDescription = "Terminal inline graphic (${graphic.protocol})",
                        modifier = Modifier
                            .width((graphic.widthCells * 9).dp)
                            .height((graphic.heightCells * 18).dp)
                    )
                }
            }
        }

        // Hyperlink click dialog
        clickedHyperlink?.let { url ->
            AlertDialog(
                onDismissRequest = { clickedHyperlink = null },
                title = { Text("Open Hyperlink") },
                text = { Text("Do you want to open this link in your browser?\n\n$url") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // invalid URI
                            }
                            clickedHyperlink = null
                        }
                    ) {
                        Text("Open")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { clickedHyperlink = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun TerminalLineRow(
    chars: Array<TerminalChar>,
    cursorCol: Int,
    cursorStyle: com.example.terminal.CursorStyle,
    cursorAlpha: Float,
    onHyperlinkClick: (String) -> Unit
) {
    // Build annotated string for the line
    val annotated = remember(chars) {
        buildAnnotatedString {
            var col = 0
            while (col < chars.size) {
                val c = chars[col]
                if (c.isSpacer) {
                    col++
                    continue
                }

                val text = c.text
                val decoration = when (c.underlineStyle) {
                    UnderlineStyle.STRAIGHT, UnderlineStyle.DOUBLE,
                    UnderlineStyle.CURLY, UnderlineStyle.DOTTED, UnderlineStyle.DASHED -> {
                        if (c.strikethrough) TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
                        else TextDecoration.Underline
                    }
                    UnderlineStyle.NONE -> {
                        if (c.strikethrough) TextDecoration.LineThrough else TextDecoration.None
                    }
                }

                val fg = if (c.inverse) c.bg.takeIf { it != Color.Transparent } ?: TerminalColors.DefaultBackground else c.fg
                val bg = if (c.inverse) c.fg else c.bg

                val spanStyle = SpanStyle(
                    color = if (c.dim) fg.copy(alpha = 0.5f) else fg,
                    background = bg,
                    fontWeight = if (c.bold) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (c.italic) FontStyle.Italic else FontStyle.Normal,
                    textDecoration = decoration,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                )

                if (c.hyperlinkUrl != null) {
                    pushStringAnnotation(tag = "URL", annotation = c.hyperlinkUrl)
                    withStyle(spanStyle.copy(color = Color(0xFF38BDF8), textDecoration = TextDecoration.Underline)) {
                        append(text)
                    }
                    pop()
                } else {
                    withStyle(spanStyle) {
                        append(text)
                    }
                }
                col++
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                // Custom drawing for special underline styles (curly, dotted, dashed)
                var currentX = 0f
                val charWidthApprox = size.width / chars.size.coerceAtLeast(1)
                for (col in chars.indices) {
                    val c = chars[col]
                    val uColor = c.underlineColor ?: c.fg
                    val startX = col * charWidthApprox
                    val endX = (col + 1) * charWidthApprox
                    val y = size.height - 2f

                    when (c.underlineStyle) {
                        UnderlineStyle.CURLY -> {
                            val path = Path().apply {
                                moveTo(startX, y)
                                val mid = (startX + endX) / 2
                                quadraticTo(startX + (endX - startX) * 0.25f, y - 3f, mid, y)
                                quadraticTo(startX + (endX - startX) * 0.75f, y + 3f, endX, y)
                            }
                            drawPath(path, uColor, style = Stroke(width = 1.5f))
                        }
                        UnderlineStyle.DOTTED -> {
                            drawLine(
                                color = uColor,
                                start = Offset(startX, y),
                                end = Offset(endX, y),
                                strokeWidth = 1.5f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f), 0f)
                            )
                        }
                        UnderlineStyle.DASHED -> {
                            drawLine(
                                color = uColor,
                                start = Offset(startX, y),
                                end = Offset(endX, y),
                                strokeWidth = 1.5f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
                            )
                        }
                        else -> {}
                    }
                }

                // Render Cursor if present on this line
                if (cursorCol in 0 until chars.size && cursorAlpha > 0.2f) {
                    val startX = cursorCol * charWidthApprox
                    val cursorColor = TerminalColors.DefaultCursor.copy(alpha = cursorAlpha)
                    when (cursorStyle.shape) {
                        CursorShape.BLOCK -> {
                            drawRect(
                                color = cursorColor,
                                topLeft = Offset(startX, 0f),
                                size = androidx.compose.ui.geometry.Size(charWidthApprox, size.height)
                            )
                        }
                        CursorShape.UNDERLINE -> {
                            drawRect(
                                color = cursorColor,
                                topLeft = Offset(startX, size.height - 3f),
                                size = androidx.compose.ui.geometry.Size(charWidthApprox, 3f)
                            )
                        }
                        CursorShape.BAR -> {
                            drawRect(
                                color = cursorColor,
                                topLeft = Offset(startX, 0f),
                                size = androidx.compose.ui.geometry.Size(2.5f, size.height)
                            )
                        }
                    }
                }
            }
    ) {
        androidx.compose.foundation.text.ClickableText(
            text = annotated,
            onClick = { offset ->
                annotated.getStringAnnotations(tag = "URL", start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        onHyperlinkClick(annotation.item)
                    }
            }
        )
    }
}
