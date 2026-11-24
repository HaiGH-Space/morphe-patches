package com.nth.patches.nova.patches

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.patch
import com.nth.patches.nova.fingerprints.NovaPrimeUnlockFingerprint
import com.nth.patches.nova.shared.NovaConstants.COMPATIBILITY_NOVA

@Suppress("unused")
val novaPrimeUnlockPatch = patch(
    name = "Unlock Nova Prime",
    description = "Unlocks Nova Prime features and premium options.",
    default = true
) {
    compatibleWith(COMPATIBILITY_NOVA)

    execute {
        // Inject Smali instructions at method entry:
        // 1. Synchronously commit Prime license key "1" = 1 to SharedPreferences
        // 2. Set the static package identifier string to force Prime recognition
        // 3. Set the instance field 'h' (f80355h) to true (1) to activate Prime status permanently
        NovaPrimeUnlockFingerprint.method.addInstructions(
            0,
            """
                invoke-interface {p1}, Landroid/content/SharedPreferences;->edit()Landroid/content/SharedPreferences${'$'}Editor;
                move-result-object v0
                const-string v1, "1"
                const/4 v2, 0x1
                invoke-interface {v0, v1, v2}, Landroid/content/SharedPreferences${'$'}Editor;->putInt(Ljava/lang/String;I)Landroid/content/SharedPreferences${'$'}Editor;
                move-result-object v0
                invoke-interface {v0}, Landroid/content/SharedPreferences${'$'}Editor;->commit()Z

                const-string v0, "com.teslacoilsw.launcher"
                sput-object v0, Lp00/i2;->p:Ljava/lang/String;

                const/4 v0, 0x1
                iput-boolean v0, p0, Lp00/i2;->h:Z
            """
        )
    }
}
