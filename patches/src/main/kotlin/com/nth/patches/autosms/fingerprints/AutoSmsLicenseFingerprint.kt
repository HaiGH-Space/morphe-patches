package com.nth.patches.autosms.fingerprints

import app.morphe.patcher.Fingerprint

/**
 * Fingerprint matching com.trieu.sms.MainActivity.onCreate method.
 */
object AutoSmsLicenseFingerprint : Fingerprint(
    definingClass = "Lcom/trieu/sms/MainActivity;",
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;")
)
