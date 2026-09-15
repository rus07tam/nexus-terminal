package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.ConnectionProfile
import com.example.model.KeyType
import com.example.model.KeyboardLayout
import com.example.model.KeyboardRow
import com.example.model.ProtocolSettings
import com.example.model.ToolbarKey
import com.example.model.TransportType
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class TerminalRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("terminal_prefs", Context.MODE_PRIVATE)

    fun loadProfiles(): List<ConnectionProfile> {
        val raw = prefs.getString("profiles_json", null)
        if (raw.isNullOrBlank()) {
            val defaultProfiles = listOf(
                ConnectionProfile(
                    id = "local_default",
                    name = "Local Android Shell",
                    colorHex = "#22C55E",
                    transport = TransportType.LOCAL_SHELL,
                    initialCommand = "",
                    workingDir = ""
                )
            )
            saveProfiles(defaultProfiles)
            return defaultProfiles
        }
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<ConnectionProfile>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val transportName = obj.optString("transport", TransportType.LOCAL_SHELL.name)
                val transport = try {
                    TransportType.valueOf(transportName)
                } catch (e: Exception) {
                    TransportType.LOCAL_SHELL
                }
                list.add(
                    ConnectionProfile(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        colorHex = obj.getString("colorHex"),
                        transport = transport,
                        initialCommand = obj.optString("initialCommand", ""),
                        workingDir = obj.optString("workingDir", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            if (list.isEmpty()) {
                loadProfiles()
            } else list
        } catch (e: Exception) {
            listOf(
                ConnectionProfile(
                    id = "local_default",
                    name = "Local Android Shell",
                    colorHex = "#22C55E",
                    transport = TransportType.LOCAL_SHELL
                )
            )
        }
    }

    fun saveProfiles(profiles: List<ConnectionProfile>) {
        val array = JSONArray()
        for (p in profiles) {
            val obj = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("colorHex", p.colorHex)
                put("transport", p.transport.name)
                put("initialCommand", p.initialCommand)
                put("workingDir", p.workingDir)
                put("createdAt", p.createdAt)
            }
            array.put(obj)
        }
        prefs.edit().putString("profiles_json", array.toString()).apply()
    }

    fun loadKeyboardLayout(): KeyboardLayout {
        val raw = prefs.getString("keyboard_layout_json", null)
        if (raw.isNullOrBlank()) {
            val defaultLayout = KeyboardLayout.defaultTermux()
            saveKeyboardLayout(defaultLayout)
            return defaultLayout
        }
        return try {
            val root = JSONObject(raw)
            val rowsArray = root.getJSONArray("rows")
            val rowsList = mutableListOf<KeyboardRow>()
            for (r in 0 until rowsArray.length()) {
                val rowObj = rowsArray.getJSONObject(r)
                val keysArray = rowObj.getJSONArray("keys")
                val keysList = mutableListOf<ToolbarKey>()
                for (k in 0 until keysArray.length()) {
                    val keyObj = keysArray.getJSONObject(k)
                    keysList.add(
                        ToolbarKey(
                            id = keyObj.optString("id", UUID.randomUUID().toString()),
                            type = if (keyObj.optString("type") == "switch") KeyType.SWITCH else KeyType.PRESS,
                            sequence = keyObj.getString("sequence"),
                            label = if (keyObj.has("label")) keyObj.getString("label") else null
                        )
                    )
                }
                rowsList.add(KeyboardRow(id = rowObj.optString("id", UUID.randomUUID().toString()), keys = keysList))
            }
            KeyboardLayout(rows = rowsList)
        } catch (e: Exception) {
            KeyboardLayout.defaultTermux()
        }
    }

    fun saveKeyboardLayout(layout: KeyboardLayout) {
        val root = JSONObject()
        val rowsArray = JSONArray()
        for (row in layout.rows) {
            val rowObj = JSONObject()
            rowObj.put("id", row.id)
            val keysArray = JSONArray()
            for (k in row.keys) {
                val keyObj = JSONObject()
                keyObj.put("id", k.id)
                keyObj.put("type", if (k.type == KeyType.SWITCH) "switch" else "press")
                keyObj.put("sequence", k.sequence)
                k.label?.let { keyObj.put("label", it) }
                keysArray.put(keyObj)
            }
            rowObj.put("keys", keysArray)
            rowsArray.put(rowObj)
        }
        root.put("rows", rowsArray)
        prefs.edit().putString("keyboard_layout_json", root.toString()).apply()
    }

    fun loadProtocolSettings(): ProtocolSettings {
        val raw = prefs.getString("protocol_settings_json", null)
        if (raw.isNullOrBlank()) return ProtocolSettings()
        return try {
            val obj = JSONObject(raw)
            ProtocolSettings(
                ecma48 = obj.optBoolean("ecma48", true),
                vtSequences = obj.optBoolean("vtSequences", true),
                xtermSequences = obj.optBoolean("xtermSequences", true),
                deviceAttributes = obj.optBoolean("deviceAttributes", true),
                xtVersion = obj.optBoolean("xtVersion", true),
                decrqm = obj.optBoolean("decrqm", true),
                rep = obj.optBoolean("rep", true),
                xtGetTcap = obj.optBoolean("xtGetTcap", true),

                trueColor = obj.optBoolean("trueColor", true),
                oscPalette = obj.optBoolean("oscPalette", true),
                underlineColor = obj.optBoolean("underlineColor", true),

                graphemeSegmentation = obj.optBoolean("graphemeSegmentation", true),
                eastAsianWidth = obj.optBoolean("eastAsianWidth", true),
                emojiZwj = obj.optBoolean("emojiZwj", true),
                styledUnderlines = obj.optBoolean("styledUnderlines", true),

                kittyGraphics = obj.optBoolean("kittyGraphics", true),
                sixel = obj.optBoolean("sixel", true),
                iterm2Images = obj.optBoolean("iterm2Images", true),
                xtSmGraphics = obj.optBoolean("xtSmGraphics", true),

                kittyKeyboard = obj.optBoolean("kittyKeyboard", true),
                bracketedPaste = obj.optBoolean("bracketedPaste", true),
                focusReporting = obj.optBoolean("focusReporting", true),

                mouseTracking = obj.optBoolean("mouseTracking", true),

                alternateScreen = obj.optBoolean("alternateScreen", true),
                synchronizedOutput = obj.optBoolean("synchronizedOutput", true),
                decscusr = obj.optBoolean("decscusr", true),
                decModes = obj.optBoolean("decModes", true),

                oscTitle = obj.optBoolean("oscTitle", true),
                oscHyperlinks = obj.optBoolean("oscHyperlinks", true),
                oscClipboard = obj.optBoolean("oscClipboard", true),
                oscNotifications = obj.optBoolean("oscNotifications", true),

                osc133SemanticPrompts = obj.optBoolean("osc133SemanticPrompts", true),
                osc7Cwd = obj.optBoolean("osc7Cwd", true),

                imeComposition = obj.optBoolean("imeComposition", true),
                vsyncRendering = obj.optBoolean("vsyncRendering", true),

                terminfoAdvertising = obj.optBoolean("terminfoAdvertising", true)
            )
        } catch (e: Exception) {
            ProtocolSettings()
        }
    }

    fun saveProtocolSettings(s: ProtocolSettings) {
        val obj = JSONObject().apply {
            put("ecma48", s.ecma48)
            put("vtSequences", s.vtSequences)
            put("xtermSequences", s.xtermSequences)
            put("deviceAttributes", s.deviceAttributes)
            put("xtVersion", s.xtVersion)
            put("decrqm", s.decrqm)
            put("rep", s.rep)
            put("xtGetTcap", s.xtGetTcap)

            put("trueColor", s.trueColor)
            put("oscPalette", s.oscPalette)
            put("underlineColor", s.underlineColor)

            put("graphemeSegmentation", s.graphemeSegmentation)
            put("eastAsianWidth", s.eastAsianWidth)
            put("emojiZwj", s.emojiZwj)
            put("styledUnderlines", s.styledUnderlines)

            put("kittyGraphics", s.kittyGraphics)
            put("sixel", s.sixel)
            put("iterm2Images", s.iterm2Images)
            put("xtSmGraphics", s.xtSmGraphics)

            put("kittyKeyboard", s.kittyKeyboard)
            put("bracketedPaste", s.bracketedPaste)
            put("focusReporting", s.focusReporting)

            put("mouseTracking", s.mouseTracking)

            put("alternateScreen", s.alternateScreen)
            put("synchronizedOutput", s.synchronizedOutput)
            put("decscusr", s.decscusr)
            put("decModes", s.decModes)

            put("oscTitle", s.oscTitle)
            put("oscHyperlinks", s.oscHyperlinks)
            put("oscClipboard", s.oscClipboard)
            put("oscNotifications", s.oscNotifications)

            put("osc133SemanticPrompts", s.osc133SemanticPrompts)
            put("osc7Cwd", s.osc7Cwd)

            put("imeComposition", s.imeComposition)
            put("vsyncRendering", s.vsyncRendering)

            put("terminfoAdvertising", s.terminfoAdvertising)
        }
        prefs.edit().putString("protocol_settings_json", obj.toString()).apply()
    }
}
