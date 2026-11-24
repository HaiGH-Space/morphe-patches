package com.nth.patches.wps.patches

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.patch
import com.nth.patches.wps.fingerprints.WpsPlayServicesAvailabilityFingerprint
import com.nth.patches.wps.fingerprints.WpsPlayServicesEnsureFingerprint
import com.nth.patches.wps.shared.WpsConstants.COMPATIBILITY_WPS

@Suppress("unused")
val wpsMicroGAuthPatch = patch(
    name = "MicroG Auth & Play Services Bypass",
    description = "Bypasses Google Play Services availability checks to allow Google Sign-In via MicroG.",
    default = true
) {
    compatibleWith(COMPATIBILITY_WPS)

    execute {
        // Force isGooglePlayServicesAvailable to return SUCCESS (0)
        WpsPlayServicesAvailabilityFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """
        )

        // Force ensurePlayServicesAvailable to be a safe no-op
        WpsPlayServicesEnsureFingerprint.method.addInstructions(
            0,
            """
                return-void
            """
        )
    }
}
