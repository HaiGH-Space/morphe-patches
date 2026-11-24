package com.nth.patches.nova.patches

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.patch
import com.nth.patches.nova.fingerprints.NovaPaywallFingerprint
import com.nth.patches.nova.shared.NovaConstants.COMPATIBILITY_NOVA

@Suppress("unused")
val novaPaywallPatch = patch(
    name = "Hide Paywall",
    description = "Instantly finishes and hides NovaPaywallActivity.",
    default = true
) {
    compatibleWith(COMPATIBILITY_NOVA)

    execute {
        // Immediately finish the activity and return upon entry to prevent the paywall from showing
        NovaPaywallFingerprint.method.addInstructions(
            0,
            """
                invoke-virtual {p0}, Landroid/app/Activity;->finish()V
                return-void
            """
        )
    }
}
