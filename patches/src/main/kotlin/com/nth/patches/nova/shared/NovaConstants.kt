package com.nth.patches.nova.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object NovaConstants {
    val COMPATIBILITY_NOVA = Compatibility(
        name = "Nova Launcher",
        packageName = "com.teslacoilsw.launcher",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x2A3543,
        targets = listOf(
            AppTarget(
                version = "90100 (9.0.1)",
                versionCode = 90100
            )
        )
    )
}
