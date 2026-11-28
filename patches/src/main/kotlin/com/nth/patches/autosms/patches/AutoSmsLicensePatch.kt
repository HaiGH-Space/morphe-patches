package com.nth.patches.autosms.patches

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.patch
import app.morphe.patcher.patch.resourceFile
import com.nth.patches.autosms.fingerprints.AutoSmsLicenseFingerprint
import com.nth.patches.autosms.fingerprints.AutoSmsSecureStorageFingerprint
import com.nth.patches.autosms.shared.AutoSmsConstants.COMPATIBILITY_AUTOSMS
import java.nio.ByteBuffer
import java.nio.ByteOrder

@Suppress("unused")
val autoSmsLicensePatch = patch(
    name = "Unlock License & Bypass Auth",
    description = "Pre-populates SharedPreferences, intercepts FlutterSecureStorage calls via AutoSmsHook extension, and patches native libapp.so HTTP 401/403 checks.",
    default = true
) {
    compatibleWith(COMPATIBILITY_AUTOSMS)
    extendWith("extensions/extension.mpe")

    execute {
        // 1. Native Patching: Patch HTTP 401 & 403 status check branches in lib/arm64-v8a/libapp.so
        val soPath = "lib/arm64-v8a/libapp.so"
        val soFile = resourceFile(soPath)

        if (soFile.exists()) {
            val data = soFile.readBytes()
            val nopBytes = byteArrayOf(0x1f.toByte(), 0x20.toByte(), 0x03.toByte(), 0xd5.toByte())
            var patchedNative = false

            // (A) Patch Non-200 Status Branch NOP at 0x0055948c: b.ne 0x5594a4 (c1 00 00 54) -> NOP
            val off1 = 0x0055948c
            if (off1 + 4 <= data.size) {
                if (data[off1] == 0xc1.toByte() && data[off1 + 1] == 0x00.toByte() &&
                    data[off1 + 2] == 0x00.toByte() && data[off1 + 3] == 0x54.toByte()
                ) {
                    System.arraycopy(nopBytes, 0, data, off1, 4)
                    patchedNative = true
                }
            }

            // (B) Patch ALL 25 HTTP 401/403 status check branches (CMP 401/403 at cmp_off + B.cond at branch_off)
            val allStatusCheckOffsets = listOf(
                0x453f84, 0x4ed27c, 0x5594a4, 0x5594ac, 0x8baa40,
                0x8ebb14, 0x8fb9e4, 0x8fb9ec, 0x902ae8, 0x902af0,
                0x9098c4, 0x9098cc, 0x90b8c4, 0x90b99c, 0x90bdf0,
                0x91102c, 0x92bdb0, 0x92bdb8, 0x92c840, 0x92c918,
                0x94010c, 0x940114, 0x95160c, 0x951614, 0x973078
            )

            for (cmpOff in allStatusCheckOffsets) {
                val branchOff = cmpOff + 4
                if (branchOff + 4 <= data.size) {
                    val insn = ByteBuffer.wrap(data, branchOff, 4).order(ByteOrder.LITTLE_ENDIAN).int
                    if ((insn and -0x1000000) == 0x54000000) { // B.cond opcode prefix
                        System.arraycopy(nopBytes, 0, data, branchOff, 4)
                        patchedNative = true
                    }
                }
            }

            if (patchedNative) {
                soFile.writeBytes(data)
            }
        }

        // 2. Pre-populate SharedPreferences via Extension on MainActivity launch
        AutoSmsLicenseFingerprint.method.addInstructions(
            0,
            """
                invoke-static {p0}, Lcom/nth/patches/extension/AutoSmsHook;->initPreferences(Landroid/content/Context;)V
            """
        )

        // 3. Intercept FlutterSecureStorage MethodChannel calls via Extension
        AutoSmsSecureStorageFingerprint.method.addInstructions(
            0,
            """
                invoke-static {p1, p2}, Lcom/nth/patches/extension/AutoSmsHook;->handleMethodCall(Ljava/lang/Object;Ljava/lang/Object;)Z
                move-result v0
                if-eqz v0, :cond_orig
                return-void
                :cond_orig
            """
        )
    }
}
