package com.nth.patches.wps.fingerprints

import app.morphe.patcher.Fingerprint

/**
 * Target fingerprint for PremiumUtil.e() to bypass premium and VIP checks.
 */
object WpsPremiumFingerprint : Fingerprint(
    definingClass = "Lcn/wps/moffice/common/premium/PremiumUtil;",
    name = "e",
    returnType = "Z",
    parameters = emptyList()
)
