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

            // Max dimensions for safety
            val maxW = 1200
            val maxH = 1200

            val pixels = mutableMapOf<Int, Int>() // key = (y * maxW + x)
            val colorTable = mutableMapOf<Int, Int>()

            // Default basic 16 colors
            colorTable[0] = android.graphics.Color.BLACK
            colorTable[1] = android.graphics.Color.BLUE
            colorTable[2] = android.graphics.Color.RED
            colorTable[3] = android.graphics.Color.GREEN
            colorTable[4] = android.graphics.Color.MAGENTA
            colorTable[5] = android.graphics.Color.CYAN
            colorTable[6] = android.graphics.Color.YELLOW
            colorTable[7] = android.graphics.Color.WHITE
            colorTable[8] = android.graphics.Color.DKGRAY
            colorTable[9] = android.graphics.Color.rgb(0, 0, 180)
            colorTable[10] = android.graphics.Color.rgb(180, 0, 0)
            colorTable[11] = android.graphics.Color.rgb(0, 180, 0)
            colorTable[12] = android.graphics.Color.rgb(180, 0, 180)
            colorTable[13] = android.graphics.Color.rgb(0, 180, 180)
            colorTable[14] = android.graphics.Color.rgb(180, 180, 0)
            colorTable[15] = android.graphics.Color.LTGRAY

            var currentColor = colorTable[7] ?: android.graphics.Color.WHITE
            var i = 0
            val len = sixelData.length

            while (i < len) {
                val c = sixelData[i]
                when {
                    c == '"' -> {
                        // Raster attributes: "Pan;Pad;Ph;Pv
                        i++
                        val sb = StringBuilder()
                        while (i < len && (sixelData[i].isDigit() || sixelData[i] == ';')) {
                            sb.append(sixelData[i])
                            i++
                        }
                        val parts = sb.toString().split(';')
                        if (parts.size >= 4) {
                            val rasterW = parts[2].toIntOrNull() ?: 0
                            val rasterH = parts[3].toIntOrNull() ?: 0
                            if (rasterW > width) width = rasterW
                            if (rasterH > height) height = rasterH
                        }
                        continue
                    }
                    c == '#' -> {
                        // Color introduction #Pn or #Pn;format;c1;c2;c3
                        i++
                        val sb = StringBuilder()
                        while (i < len && (sixelData[i].isDigit() || sixelData[i] == ';')) {
                            sb.append(sixelData[i])
                            i++
                        }
                        val parts = sb.toString().split(';')
                        if (parts.isNotEmpty()) {
                            val colorIdx = parts[0].toIntOrNull() ?: 0
                            if (parts.size >= 5) {
                                val format = parts[1]
                                val c1 = parts[2].toIntOrNull() ?: 0
                                val c2 = parts[3].toIntOrNull() ?: 0
                                val c3 = parts[4].toIntOrNull() ?: 0
                                if (format == "2") {
                                    // RGB: If values > 100, assume 0..255, else percentage 0..100
                                    val r = if (c1 > 100) c1.coerceIn(0, 255) else (c1 * 255 / 100).coerceIn(0, 255)
                                    val g = if (c2 > 100) c2.coerceIn(0, 255) else (c2 * 255 / 100).coerceIn(0, 255)
                                    val b = if (c3 > 100) c3.coerceIn(0, 255) else (c3 * 255 / 100).coerceIn(0, 255)
                                    val rgb = android.graphics.Color.rgb(r, g, b)
                                    colorTable[colorIdx] = rgb
                                    currentColor = rgb
                                } else {
                                    currentColor = colorTable[colorIdx] ?: android.graphics.Color.WHITE
                                }
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
                                val repeat = count.coerceIn(1, 2000)
                                for (r in 0 until repeat) {
                                    if (currentX < maxW) {
                                        for (bit in 0 until 6) {
                                            if ((sixelVal and (1 shl bit)) != 0 && (currentY + bit) < maxH) {
                                                pixels[(currentY + bit) * maxW + currentX] = currentColor
                                            }
                                        }
                                        currentX++
                                    }
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
                        // Newline (move down 6 pixels to next band and return to left margin)
                        currentX = 0
                        currentY += 6
                        if (currentY + 6 > height) height = currentY + 6
                        i++
                    }
                    c in '?'..'~' -> {
                        // Single sixel character (6 vertical pixels)
                        val sixelVal = c.code - 63
                        if (currentX < maxW) {
                            for (bit in 0 until 6) {
                                if ((sixelVal and (1 shl bit)) != 0 && (currentY + bit) < maxH) {
                                    pixels[(currentY + bit) * maxW + currentX] = currentColor
                                }
                            }
                            currentX++
                        }
                        if (currentX > width) width = currentX
                        if (currentY + 6 > height) height = currentY + 6
                        i++
                    }
                    else -> {
                        i++
                    }
                }
            }

            if (width <= 0 || height <= 0 || pixels.isEmpty()) return null
            val clampedW = width.coerceIn(1, maxW)
            val clampedH = height.coerceIn(1, maxH)
            val bitmap = Bitmap.createBitmap(clampedW, clampedH, Bitmap.Config.ARGB_8888)

            for ((key, color) in pixels) {
                val py = key / maxW
                val px = key % maxW
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
