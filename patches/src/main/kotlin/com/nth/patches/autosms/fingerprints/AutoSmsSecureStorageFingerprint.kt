package com.nth.patches.autosms.fingerprints

import app.morphe.patcher.Fingerprint

/**
 * Fingerprint matching FlutterSecureStoragePlugin.onMethodCall in x9 class.
 */
object AutoSmsSecureStorageFingerprint : Fingerprint(
    definingClass = "Lx9;",
    name = "onMethodCall",
    returnType = "V",
    parameters = listOf(
        "Lio/flutter/plugin/common/MethodCall;",
        "Lio/flutter/plugin/common/MethodChannel${'$'}Result;"
    )
)
