package com.nth.patches.nova.fingerprints

import app.morphe.patcher.Fingerprint

/**
 * Target fingerprint for NovaPaywallActivity.onCreate.
 * Matches the entry point of Nova Launcher's paywall activity.
 */
object NovaPaywallFingerprint : Fingerprint(
    definingClass = "Lcom/teslacoilsw/launcher/onboarding/NovaPaywallActivity;",
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;")
)
