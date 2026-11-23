package app.template.patches.nova

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.patch
import app.template.patches.shared.Constants.COMPATIBILITY_NOVA
import com.android.tools.smali.dexlib2.AccessFlags

object NovaPrimeUnlockFingerprint : Fingerprint(
    definingClass = "Lak/a;",
    name = "q0",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Z",
    parameters = listOf("Lh20/o0;", "Lp00/e4;")
)

@Suppress("unused")
val novaPrimeUnlockPatch = patch(
    name = "Unlock Nova Prime",
    description = "Unlocks Nova Prime features and premium options.",
    default = true
) {
    compatibleWith(COMPATIBILITY_NOVA)

    execute {
        NovaPrimeUnlockFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """
        )
    }
}
