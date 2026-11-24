package com.nth.patches.wps.fingerprints

import app.morphe.patcher.Fingerprint

/**
 * Fingerprint for GooglePlayServicesUtilLight.isGooglePlayServicesAvailable
 */
object WpsPlayServicesAvailabilityFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/gms/common/GooglePlayServicesUtilLight;",
    name = "isGooglePlayServicesAvailable"
)

/**
 * Fingerprint for GooglePlayServicesUtilLight.ensurePlayServicesAvailable
 */
object WpsPlayServicesEnsureFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/gms/common/GooglePlayServicesUtilLight;",
    name = "ensurePlayServicesAvailable"
)
