package com.nth.patches.nova.fingerprints

import app.morphe.patcher.Fingerprint

/**
 * Fingerprint matching Nova Launcher's core preference & Prime initialization method.
 * Uses unique string anchors to locate the method across obfuscated builds.
 */
object NovaPrimeUnlockFingerprint : Fingerprint(
    strings = listOf("widget_reset_ids", "ro.razer.internal.api")
)
