package com.example.model

enum class TransportType(val displayName: String, val isAvailable: Boolean, val badgeDescription: String) {
    LOCAL_SHELL("Android Local Shell", true, "Available (Standard /system/bin/sh)"),
    SSH("SSH", false, "Planned (OpenSSH / libssh2)"),
    PROOT("Proot distro", false, "Planned (User-space chroot/Debian/Alpine)"),
    CHROOT("Chroot distro", false, "Planned (Requires Root/Magisk)"),
    TELNET("Telnet", false, "Planned (RFC 854 Protocol)")
}

data class ConnectionProfile(
    val id: String,
    val name: String,
    val colorHex: String,
    val transport: TransportType = TransportType.LOCAL_SHELL,
    val initialCommand: String = "",
    val workingDir: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
