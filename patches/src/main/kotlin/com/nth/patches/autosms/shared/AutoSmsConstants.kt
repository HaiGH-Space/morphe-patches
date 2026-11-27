package com.nth.patches.autosms.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object AutoSmsConstants {
    val COMPATIBILITY_AUTOSMS = Compatibility(
        name = "Auto SMS",
        packageName = "com.example.auto_sms",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x1E88E5,
        targets = listOf(
            AppTarget(
                version = "0.1.65",
                versionCode = 188
            )
        )
    )
}
