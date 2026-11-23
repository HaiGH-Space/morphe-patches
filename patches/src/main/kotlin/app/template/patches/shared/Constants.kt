package app.template.patches.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.SupportedAbi

object Constants {
    val COMPATIBILITY_NOVA = Compatibility(
        name = "Nova Launcher",
        packageName = "com.teslacoilsw.launcher",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x2A3543,
        targets = listOf(
            AppTarget(
                version = "9.0.1"
            )
        )
    )
}
