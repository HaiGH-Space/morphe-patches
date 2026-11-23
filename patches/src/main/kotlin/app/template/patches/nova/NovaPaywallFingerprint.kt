package app.template.patches.nova

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

object NovaPaywallFingerprint : Fingerprint(
    definingClass = "Lcom/teslacoilsw/launcher/onboarding/NovaPaywallActivity;",
    name = "onCreate",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;")
)
