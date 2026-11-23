package app.template.patches.nova

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.patch
import app.template.patches.shared.Constants.COMPATIBILITY_NOVA

@Suppress("unused")
val novaPaywallPatch = patch(
    name = "Hide Paywall",
    description = "Instantly finishes and hides NovaPaywallActivity.",
    default = true
) {
    compatibleWith(COMPATIBILITY_NOVA)

    execute {
        NovaPaywallFingerprint.method.addInstructions(
            0,
            """
                invoke-virtual {p0}, Landroid/app/Activity;->finish()V
                return-void
            """
        )
    }
}
