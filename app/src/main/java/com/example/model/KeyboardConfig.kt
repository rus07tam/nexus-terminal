package com.example.model

import java.util.UUID

enum class KeyType {
    PRESS,
    SWITCH
}

/**
 * Equivalent to: type Key = { type: 'press' | 'switch', sequence: str, label?: str }
 */
data class ToolbarKey(
    val id: String = UUID.randomUUID().toString(),
    val type: KeyType = KeyType.PRESS,
    val sequence: String,
    val label: String? = null,
    val isActive: Boolean = false // relevant for switch keys like CTRL, ALT
) {
    val displayLabel: String
        get() = label?.takeIf { it.isNotBlank() } ?: sequence
}

data class KeyboardRow(
    val id: String = UUID.randomUUID().toString(),
    val keys: List<ToolbarKey>
)

data class KeyboardLayout(
    val rows: List<KeyboardRow>
) {
    companion object {
        fun defaultTermux(): KeyboardLayout {
            return KeyboardLayout(
                rows = listOf(
                    KeyboardRow(
                        keys = listOf(
                            ToolbarKey(type = KeyType.PRESS, sequence = "\u001b", label = "ESC"),
                            ToolbarKey(type = KeyType.PRESS, sequence = "\t", label = "TAB"),
                            ToolbarKey(type = KeyType.SWITCH, sequence = "CTRL", label = "CTRL"),
                            ToolbarKey(type = KeyType.SWITCH, sequence = "ALT", label = "ALT"),
                            ToolbarKey(type = KeyType.PRESS, sequence = "-", label = "-"),
                            ToolbarKey(type = KeyType.PRESS, sequence = "|", label = "|"),
                            ToolbarKey(type = KeyType.PRESS, sequence = "/", label = "/"),
                            ToolbarKey(type = KeyType.PRESS, sequence = "\u001b[A", label = "▲"),
                            ToolbarKey(type = KeyType.PRESS, sequence = "\u001b[H", label = "HOME"),
                            ToolbarKey(type = KeyType.PRESS, sequence = "\u001b[F", label = "END")
                        )
                    ),
                    KeyboardRow(
                        keys = listOf(
                            ToolbarKey(type = KeyType.PRESS, sequence = "\u001b[5~", label = "PGUP"),
                            ToolbarKey(type = KeyType.PRESS, sequence = "\u001b[6~", label = "PGDN"),
                            ToolbarKey(type = KeyType.PRESS, sequence = "\u001b[D", label = "◀"),
                            ToolbarKey(type = KeyType.PRESS, sequence = "\u001b[B", label = "▼"),
                            ToolbarKey(type = KeyType.PRESS, sequence = "\u001b[C", label = "▶"),
                            ToolbarKey(type = KeyType.PRESS, sequence = "\u0003", label = "C-c"),
                            ToolbarKey(type = KeyType.PRESS, sequence = "\u0004", label = "C-d"),
                            ToolbarKey(type = KeyType.PRESS, sequence = "\u001a", label = "C-z"),
                            ToolbarKey(type = KeyType.PRESS, sequence = "~", label = "~"),
                            ToolbarKey(type = KeyType.PRESS, sequence = "\n", label = "↵")
                        )
                    )
                )
            )
        }

        fun vimPreset(): KeyboardLayout {
            return KeyboardLayout(
                rows = listOf(
                    KeyboardRow(
                        keys = listOf(
                            ToolbarKey(type = KeyType.PRESS, sequence = "\u001b", label = "ESC"),
                            ToolbarKey(type = KeyType.PRESS, sequence = ":", label = ":"),
                            ToolbarKey(type = KeyType.PRESS, sequence = "/", label = "/"),
                            ToolbarKey(type = KeyType.PRESS, sequence = "i", label = "i"),
                            ToolbarKey(type = KeyType.PRESS, sequence = "v", label = "v"),
                            ToolbarKey(type = KeyType.PRESS, sequence = "y", label = "y"),
                            ToolbarKey(type = KeyType.PRESS, sequence = "p", label = "p"),
                            ToolbarKey(type = KeyType.PRESS, sequence = "u", label = "u"),
                            ToolbarKey(type = KeyType.PRESS, sequence = "0", label = "^"),
                            ToolbarKey(type = KeyType.PRESS, sequence = "$", label = "$")
                        )
                    ),
                    KeyboardRow(
                        keys = listOf(
                            ToolbarKey(type = KeyType.SWITCH, sequence = "CTRL", label = "CTRL"),
                            ToolbarKey(type = KeyType.PRESS, sequence = "h", label = "h"),
                            ToolbarKey(type = KeyType.PRESS, sequence = "j", label = "j"),
                            ToolbarKey(type = KeyType.PRESS, sequence = "k", label = "k"),
                            ToolbarKey(type = KeyType.PRESS, sequence = "l", label = "l"),
                            ToolbarKey(type = KeyType.PRESS, sequence = ":w\n", label = ":w"),
                            ToolbarKey(type = KeyType.PRESS, sequence = ":q\n", label = ":q"),
                            ToolbarKey(type = KeyType.PRESS, sequence = ":q!\n", label = ":q!"),
                            ToolbarKey(type = KeyType.PRESS, sequence = ":wq\n", label = ":wq")
                        )
                    )
                )
            )
        }

        fun tmuxPreset(): KeyboardLayout {
            return KeyboardLayout(
                rows = listOf(
                    KeyboardRow(
                        keys = listOf(
                            ToolbarKey(type = KeyType.PRESS, sequence = "\u0002", label = "C-b"),
                            ToolbarKey(type = KeyType.PRESS, sequence = "\u0002\"", label = "split-h"),
                            ToolbarKey(type = KeyType.PRESS, sequence = "\u0002%", label = "split-v"),
                            ToolbarKey(type = KeyType.PRESS, sequence = "\u0002c", label = "new-win"),
                            ToolbarKey(type = KeyType.PRESS, sequence = "\u0002n", label = "next"),
                            ToolbarKey(type = KeyType.PRESS, sequence = "\u0002p", label = "prev"),
                            ToolbarKey(type = KeyType.PRESS, sequence = "\u0002z", label = "zoom"),
                            ToolbarKey(type = KeyType.PRESS, sequence = "\u0002[", label = "copy"),
                            ToolbarKey(type = KeyType.PRESS, sequence = "\u0002d", label = "detach")
                        )
                    )
                )
            )
        }
    }
}
