package com.example.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.TerminalRepository
import com.example.model.ConnectionProfile
import com.example.model.KeyType
import com.example.model.KeyboardLayout
import com.example.model.KeyboardRow
import com.example.model.ProtocolSettings
import com.example.model.ToolbarKey
import com.example.model.TransportType
import com.example.terminal.LocalShellSession
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppTab(val title: String) {
    CONNECTIONS("Connections"),
    SESSIONS("Sessions"),
    TERMINAL("Terminal"),
    SETTINGS("Settings")
}

enum class SettingsSection(val title: String) {
    KEYBOARD("Keyboard"),
    PROTOCOLS("Protocols")
}

class TerminalViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TerminalRepository(application)
    private val clipboardManager = application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    // Navigation
    private val _currentTab = MutableStateFlow(AppTab.TERMINAL)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _settingsSection = MutableStateFlow(SettingsSection.KEYBOARD)
    val settingsSection: StateFlow<SettingsSection> = _settingsSection.asStateFlow()

    // Connections
    private val _profiles = MutableStateFlow<List<ConnectionProfile>>(emptyList())
    val profiles: StateFlow<List<ConnectionProfile>> = _profiles.asStateFlow()

    // Sessions
    private val _sessions = MutableStateFlow<List<LocalShellSession>>(emptyList())
    val sessions: StateFlow<List<LocalShellSession>> = _sessions.asStateFlow()

    private val _activeSessionId = MutableStateFlow<String?>(null)
    val activeSessionId: StateFlow<String?> = _activeSessionId.asStateFlow()

    // Terminal Controls
    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _autoScroll = MutableStateFlow(true)
    val autoScroll: StateFlow<Boolean> = _autoScroll.asStateFlow()

    // Keyboard toolbar state
    private val _keyboardLayout = MutableStateFlow(KeyboardLayout.defaultTermux())
    val keyboardLayout: StateFlow<KeyboardLayout> = _keyboardLayout.asStateFlow()

    // Modifier switches
    private val _isCtrlActive = MutableStateFlow(false)
    val isCtrlActive: StateFlow<Boolean> = _isCtrlActive.asStateFlow()

    private val _isAltActive = MutableStateFlow(false)
    val isAltActive: StateFlow<Boolean> = _isAltActive.asStateFlow()

    // Protocols
    private val _protocolSettings = MutableStateFlow(ProtocolSettings())
    val protocolSettings: StateFlow<ProtocolSettings> = _protocolSettings.asStateFlow()

    // Feedback messages (e.g. for notifications or clipboard events)
    private val _snackbarMessages = MutableSharedFlow<String>()
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    // Soft keyboard request trigger
    private val _showSoftKeyboard = MutableStateFlow(false)
    val showSoftKeyboard: StateFlow<Boolean> = _showSoftKeyboard.asStateFlow()

    // UI Tick for force recomposition on terminal changes
    private val _renderVersion = MutableStateFlow(0L)
    val renderVersion: StateFlow<Long> = _renderVersion.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val loadedProfiles = repository.loadProfiles()
        _profiles.value = loadedProfiles

        val loadedKeyboard = repository.loadKeyboardLayout()
        _keyboardLayout.value = loadedKeyboard

        val loadedProtocols = repository.loadProtocolSettings()
        _protocolSettings.value = loadedProtocols

        // Automatically start default session if none exists
        if (_sessions.value.isEmpty()) {
            val defaultProfile = loadedProfiles.firstOrNull() ?: ConnectionProfile(
                id = "default",
                name = "Android Local Shell",
                colorHex = "#22C55E",
                transport = TransportType.LOCAL_SHELL
            )
            createSession(defaultProfile)
        }
    }

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun selectSettingsSection(section: SettingsSection) {
        _settingsSection.value = section
    }

    fun toggleSoftKeyboard() {
        _showSoftKeyboard.value = !_showSoftKeyboard.value
    }

    fun triggerRenderUpdate() {
        if (!_isLocked.value) {
            _renderVersion.value = System.currentTimeMillis()
        }
    }

    fun createSession(profile: ConnectionProfile) {
        val session = LocalShellSession(
            profile = profile,
            context = getApplication(),
            scope = viewModelScope,
            settings = _protocolSettings.value,
            onBufferUpdated = {
                triggerRenderUpdate()
            },
            onClipboardCopy = { text ->
                copyToClipboard(text)
            },
            onNotification = { message ->
                showNotification(message)
            },
            onBell = {
                triggerVibrate()
            },
            onSessionExited = {
                triggerRenderUpdate()
            }
        )
        session.start()
        _sessions.value = _sessions.value + session
        _activeSessionId.value = session.id
        _currentTab.value = AppTab.TERMINAL
    }

    fun switchSession(sessionId: String) {
        _activeSessionId.value = sessionId
        _currentTab.value = AppTab.TERMINAL
        triggerRenderUpdate()
    }

    fun closeSession(sessionId: String) {
        val target = _sessions.value.find { it.id == sessionId }
        target?.terminate()
        val remaining = _sessions.value.filter { it.id != sessionId }
        _sessions.value = remaining
        if (_activeSessionId.value == sessionId) {
            _activeSessionId.value = remaining.lastOrNull()?.id
        }
        triggerRenderUpdate()
    }

    fun restartSession(sessionId: String) {
        val target = _sessions.value.find { it.id == sessionId } ?: return
        val profile = target.profile
        closeSession(sessionId)
        createSession(profile)
    }

    fun getActiveSession(): LocalShellSession? {
        val id = _activeSessionId.value ?: return _sessions.value.firstOrNull()
        return _sessions.value.find { it.id == id } ?: _sessions.value.firstOrNull()
    }

    fun toggleLock() {
        _isLocked.value = !_isLocked.value
        getActiveSession()?.buffer?.isLocked = _isLocked.value
        if (!_isLocked.value) {
            triggerRenderUpdate()
        }
    }

    fun toggleAutoScroll() {
        _autoScroll.value = !_autoScroll.value
        getActiveSession()?.buffer?.autoScroll = _autoScroll.value
    }

    fun sendInput(input: String) {
        getActiveSession()?.sendInput(input)
    }

    fun sendKey(key: ToolbarKey) {
        if (key.type == KeyType.SWITCH) {
            when (key.sequence) {
                "CTRL" -> _isCtrlActive.value = !_isCtrlActive.value
                "ALT" -> _isAltActive.value = !_isAltActive.value
                else -> {
                    // Custom switch key
                    sendInput(key.sequence)
                }
            }
            return
        }

        var sequence = key.sequence

        // Check active modifiers
        if (_isCtrlActive.value) {
            if (sequence.length == 1) {
                val c = sequence[0].uppercaseChar()
                if (c in 'A'..'Z') {
                    sequence = (c.code - 64).toChar().toString()
                }
            }
            _isCtrlActive.value = false
        } else if (_isAltActive.value) {
            sequence = "\u001b$sequence"
            _isAltActive.value = false
        }

        sendInput(sequence)
    }

    fun handleRawInput(char: Char) {
        var str = char.toString()
        if (_isCtrlActive.value) {
            val c = char.uppercaseChar()
            if (c in 'A'..'Z') {
                str = (c.code - 64).toChar().toString()
            }
            _isCtrlActive.value = false
        } else if (_isAltActive.value) {
            str = "\u001b$str"
            _isAltActive.value = false
        }
        sendInput(str)
    }

    fun clearScreen() {
        sendInput("\u000c") // CTRL-L
    }

    fun copyTerminalOutput() {
        val session = getActiveSession() ?: return
        val text = session.buffer.getPlainText()
        copyToClipboard(text)
        viewModelScope.launch {
            _snackbarMessages.emit("Terminal output copied to clipboard")
        }
    }

    fun copyToClipboard(text: String) {
        try {
            val clip = ClipData.newPlainText("Terminal", text)
            clipboardManager.setPrimaryClip(clip)
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun showNotification(message: String) {
        viewModelScope.launch {
            _snackbarMessages.emit("OSC 9: $message")
        }
    }

    private fun triggerVibrate() {
        try {
            val context = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(50)
                }
            }
        } catch (e: Exception) {
            // ignore vibration errors
        }
    }

    // Connections management
    fun saveProfile(profile: ConnectionProfile) {
        val current = _profiles.value.toMutableList()
        val index = current.indexOfFirst { it.id == profile.id }
        if (index >= 0) {
            current[index] = profile
        } else {
            current.add(profile)
        }
        _profiles.value = current
        repository.saveProfiles(current)
    }

    fun deleteProfile(profileId: String) {
        val updated = _profiles.value.filter { it.id != profileId }
        _profiles.value = updated
        repository.saveProfiles(updated)
    }

    // Keyboard toolbar configuration
    fun updateKeyboardLayout(layout: KeyboardLayout) {
        _keyboardLayout.value = layout
        repository.saveKeyboardLayout(layout)
    }

    fun applyKeyboardPreset(presetName: String) {
        val layout = when (presetName) {
            "Vim" -> KeyboardLayout.vimPreset()
            "Tmux" -> KeyboardLayout.tmuxPreset()
            else -> KeyboardLayout.defaultTermux()
        }
        updateKeyboardLayout(layout)
    }

    fun addKeyToRow(rowIndex: Int, key: ToolbarKey) {
        val currentRows = _keyboardLayout.value.rows.toMutableList()
        if (rowIndex in currentRows.indices) {
            val row = currentRows[rowIndex]
            val updatedKeys = row.keys + key
            currentRows[rowIndex] = row.copy(keys = updatedKeys)
            updateKeyboardLayout(KeyboardLayout(rows = currentRows))
        }
    }

    fun deleteKeyFromRow(rowIndex: Int, keyId: String) {
        val currentRows = _keyboardLayout.value.rows.toMutableList()
        if (rowIndex in currentRows.indices) {
            val row = currentRows[rowIndex]
            val updatedKeys = row.keys.filter { it.id != keyId }
            currentRows[rowIndex] = row.copy(keys = updatedKeys)
            updateKeyboardLayout(KeyboardLayout(rows = currentRows))
        }
    }

    fun addKeyboardRow() {
        val currentRows = _keyboardLayout.value.rows.toMutableList()
        currentRows.add(KeyboardRow(id = UUID.randomUUID().toString(), keys = emptyList()))
        updateKeyboardLayout(KeyboardLayout(rows = currentRows))
    }

    fun deleteKeyboardRow(rowIndex: Int) {
        val currentRows = _keyboardLayout.value.rows.toMutableList()
        if (rowIndex in currentRows.indices && currentRows.size > 1) {
            currentRows.removeAt(rowIndex)
            updateKeyboardLayout(KeyboardLayout(rows = currentRows))
        }
    }

    // Protocol settings management
    fun toggleProtocol(protocolId: String) {
        val updated = _protocolSettings.value.toggle(protocolId)
        _protocolSettings.value = updated
        repository.saveProtocolSettings(updated)

        // Propagate to all running sessions
        for (session in _sessions.value) {
            session.updateSettings(updated)
        }
    }
}
