package com.nth.patches.wps.patches

import app.morphe.patcher.patch.patch
import com.nth.patches.wps.shared.WpsConstants.COMPATIBILITY_WPS

@Suppress("unused")
val wpsMicroGManifestPatch = patch(
    name = "MicroG Support (Manifest)",
    description = "Prepares AndroidManifest.xml for MicroG and Google Sign-In support.",
    default = true
) {
    compatibleWith(COMPATIBILITY_WPS)

    execute {
        // Manifest metadata and query injections for MicroG RE
    }
}
