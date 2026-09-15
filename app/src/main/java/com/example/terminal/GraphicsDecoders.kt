package com.example.terminal

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayInputStream

object SixelDecoder {
    /**
     * Decodes Sixel data (starting after 'q') into an Android Bitmap.
     */
    fun decode(sixelData: String): Bitmap? {
        try {
            var width = 0
            var height = 0
            var currentX = 0
            var currentY = 0
            val pixels = mutableMapOf<Pair<Int, Int>, Int>()
            val colorTable = mutableMapOf<Int, Int>()

            // Default basic colors
            colorTable[0] = android.graphics.Color.BLACK
            colorTable[1] = android.graphics.Color.BLUE
            colorTable[2] = android.graphics.Color.RED
            colorTable[3] = android.graphics.Color.GREEN
            colorTable[4] = android.graphics.Color.MAGENTA
            colorTable[5] = android.graphics.Color.CYAN
            colorTable[6] = android.graphics.Color.YELLOW
            colorTable[7] = android.graphics.Color.WHITE

            var currentColor = colorTable[7] ?: android.graphics.Color.WHITE
            var i = 0
            val len = sixelData.length

            while (i < len) {
                val c = sixelData[i]
                when {
                    c == '#' -> {
                        // Color introduction #Pn or #Pn;2;r;g;b
                        i++
                        val sb = StringBuilder()
                        while (i < len && (sixelData[i].isDigit() || sixelData[i] == ';')) {
                            sb.append(sixelData[i])
                            i++
                        }
                        val parts = sb.toString().split(';')
                        if (parts.isNotEmpty()) {
                            val colorIdx = parts[0].toIntOrNull() ?: 0
                            if (parts.size >= 5 && parts[1] == "2") {
                                // 2 = RGB percentages 0..100
                                val r = ((parts[2].toIntOrNull() ?: 0) * 255 / 100).coerceIn(0, 255)
                                val g = ((parts[3].toIntOrNull() ?: 0) * 255 / 100).coerceIn(0, 255)
                                val b = ((parts[4].toIntOrNull() ?: 0) * 255 / 100).coerceIn(0, 255)
                                val rgb = android.graphics.Color.rgb(r, g, b)
                                colorTable[colorIdx] = rgb
                                currentColor = rgb
                            } else {
                                currentColor = colorTable[colorIdx] ?: android.graphics.Color.WHITE
                            }
                        }
                        continue
                    }
                    c == '!' -> {
                        // Repeat count: !count<char>
                        i++
                        var count = 0
                        while (i < len && sixelData[i].isDigit()) {
                            count = count * 10 + (sixelData[i] - '0')
                            i++
                        }
                        if (i < len) {
                            val repeatChar = sixelData[i]
                            if (repeatChar in '?'..'~') {
                                val sixelVal = repeatChar.code - 63
                                for (r in 0 until count.coerceAtMost(2000)) {
                                    for (bit in 0 until 6) {
                                        if ((sixelVal and (1 shl bit)) != 0) {
                                            pixels[Pair(currentX, currentY + bit)] = currentColor
                                        }
                                    }
                                    currentX++
                                }
                                if (currentX > width) width = currentX
                                if (currentY + 6 > height) height = currentY + 6
                            }
                            i++
                        }
                        continue
                    }
                    c == '$' -> {
                        // Carriage return (return to left margin of current 6-pixel band)
                        currentX = 0
                        i++
                    }
                    c == '-' -> {
                        // Newline (move down 6 pixels to next band and return to left)
                        currentX = 0
                        currentY += 6
                        i++
                    }
                    c in '?'..'~' -> {
                        // Single sixel character (6 vertical pixels)
                        val sixelVal = c.code - 63
                        for (bit in 0 until 6) {
                            if ((sixelVal and (1 shl bit)) != 0) {
                                pixels[Pair(currentX, currentY + bit)] = currentColor
                            }
                        }
                        currentX++
                        if (currentX > width) width = currentX
                        if (currentY + 6 > height) height = currentY + 6
                        i++
                    }
                    else -> {
                        i++
                    }
                }
            }

            if (width <= 0 || height <= 0) return null
            val clampedW = width.coerceIn(1, 1024)
            val clampedH = height.coerceIn(1, 1024)
            val bitmap = Bitmap.createBitmap(clampedW, clampedH, Bitmap.Config.ARGB_8888)

            for ((pos, color) in pixels) {
                val (px, py) = pos
                if (px in 0 until clampedW && py in 0 until clampedH) {
                    bitmap.setPixel(px, py, color)
                }
            }
            return bitmap
        } catch (e: Exception) {
            return null
        }
    }
}

object KittyGraphicsDecoder {
    /**
     * Decodes Kitty graphics payload (APC _G...;payload)
     */
    fun decode(paramsStr: String, payload: String): Bitmap? {
        return try {
            val cleanBase64 = payload.replace("\n", "").replace("\r", "").trim()
            val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }
}

object ITerm2Decoder {
    /**
     * Decodes iTerm2 inline image (OSC 1337 File=...:<base64>)
     */
    fun decode(arg: String): Bitmap? {
        return try {
            val colonIdx = arg.indexOf(':')
            if (colonIdx == -1) return null
            val base64Data = arg.substring(colonIdx + 1).replace("\n", "").replace("\r", "").trim()
            val bytes = Base64.decode(base64Data, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }
}
