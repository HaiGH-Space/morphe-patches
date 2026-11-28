package com.nth.patches.wps.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object WpsConstants {
    val COMPATIBILITY_WPS = Compatibility(
        name = "WPS Office",
        packageName = "cn.wps.moffice_eng",
        apkFileType = ApkFileType.APK,
        appIconColor = 0xD32F2F,
        targets = listOf(
            AppTarget(
                version = "26.11.2",
                versionCode = 1653
            )
        )
    )
}
