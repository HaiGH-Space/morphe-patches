package com.nth.patches.wps.patches

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.patch
import com.nth.patches.wps.fingerprints.WpsPremiumFingerprint
import com.nth.patches.wps.shared.WpsConstants.COMPATIBILITY_WPS

@Suppress("unused")
val wpsPremiumPatch = patch(
    name = "Unlock Premium & VIP",
    description = "Bypasses WPS Office premium and VIP checks to unlock all premium features.",
    default = true
) {
    compatibleWith(COMPATIBILITY_WPS)

    execute {
        // Force PremiumUtil.e() to return true (1) immediately upon entry
        WpsPremiumFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """
        )
    }
}
