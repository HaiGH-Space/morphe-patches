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
    description = "Pre-populates SharedPreferences, intercepts FlutterSecureStorage calls, and patches native libapp.so HTTP 401 checks to bypass login and activate license.",
    default = true
) {
    compatibleWith(COMPATIBILITY_AUTOSMS)

    execute {
        // 1. Native Patching: Patch HTTP 401 status checks in lib/arm64-v8a/libapp.so
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

            // (B) Patch ALL 15 HTTP 401 status check branches (CMP 401 at cmp_off + B.cond at branch_off)
            val all401Offsets = listOf(
                0x453f84, 0x4ed27c, 0x5594a4, 0x8baa40, 0x8ebb14,
                0x8fb9e4, 0x902ae8, 0x9098c4, 0x90b8c4, 0x91102c,
                0x92bdb0, 0x92c840, 0x94010c, 0x95160c, 0x973078
            )

            for (cmpOff in all401Offsets) {
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

        // 2. Initialize FlutterSharedPreferences on MainActivity launch
        AutoSmsLicenseFingerprint.method.addInstructions(
            0,
            """
                const-string v0, "FlutterSharedPreferences"
                const/4 v1, 0x0
                invoke-virtual {p0, v0, v1}, Lcom/trieu/sms/MainActivity;->getSharedPreferences(Ljava/lang/String;I)Landroid/content/SharedPreferences;
                move-result-object v0
                invoke-interface {v0}, Landroid/content/SharedPreferences;->edit()Landroid/content/SharedPreferences${'$'}Editor;
                move-result-object v0

                const-string v1, "flutter.licenseValid"
                const/4 v2, 0x1
                invoke-interface {v0, v1, v2}, Landroid/content/SharedPreferences${'$'}Editor;->putBoolean(Ljava/lang/String;Z)Landroid/content/SharedPreferences${'$'}Editor;
                move-result-object v0

                const-string v1, "flutter.license"
                const-string v2, "ACTIVATED"
                invoke-interface {v0, v1, v2}, Landroid/content/SharedPreferences${'$'}Editor;->putString(Ljava/lang/String;Ljava/lang/String;)Landroid/content/SharedPreferences${'$'}Editor;
                move-result-object v0

                const-string v1, "flutter.auth_expire_license_v1"
                const-wide v2, 0x16bcc41e900L
                invoke-interface {v0, v1, v2, v3}, Landroid/content/SharedPreferences${'$'}Editor;->putLong(Ljava/lang/String;J)Landroid/content/SharedPreferences${'$'}Editor;
                move-result-object v0

                const-string v1, "flutter.auth_expires_time_v1"
                const-string v2, "253402300799000"
                invoke-interface {v0, v1, v2}, Landroid/content/SharedPreferences${'$'}Editor;->putString(Ljava/lang/String;Ljava/lang/String;)Landroid/content/SharedPreferences${'$'}Editor;
                move-result-object v0

                const-string v1, "flutter.auth_token_v1"
                const-string v2, "bypass_token_12345"
                invoke-interface {v0, v1, v2}, Landroid/content/SharedPreferences${'$'}Editor;->putString(Ljava/lang/String;Ljava/lang/String;)Landroid/content/SharedPreferences${'$'}Editor;
                move-result-object v0

                const-string v1, "flutter.auth_refresh_token_v1"
                const-string v2, "bypass_token_12345"
                invoke-interface {v0, v1, v2}, Landroid/content/SharedPreferences${'$'}Editor;->putString(Ljava/lang/String;Ljava/lang/String;)Landroid/content/SharedPreferences${'$'}Editor;
                move-result-object v0

                const-string v1, "flutter.loginState"
                const/4 v2, 0x1
                invoke-interface {v0, v1, v2}, Landroid/content/SharedPreferences${'$'}Editor;->putBoolean(Ljava/lang/String;Z)Landroid/content/SharedPreferences${'$'}Editor;
                move-result-object v0

                const-string v1, "flutter.auth_keep_login_v1"
                const/4 v2, 0x1
                invoke-interface {v0, v1, v2}, Landroid/content/SharedPreferences${'$'}Editor;->putBoolean(Ljava/lang/String;Z)Landroid/content/SharedPreferences${'$'}Editor;
                move-result-object v0

                const-string v1, "flutter.auth_secure_migrated_v1"
                const/4 v2, 0x1
                invoke-interface {v0, v1, v2}, Landroid/content/SharedPreferences${'$'}Editor;->putBoolean(Ljava/lang/String;Z)Landroid/content/SharedPreferences${'$'}Editor;
                move-result-object v0

                const-string v1, "flutter.auth_last_phone_v1"
                const-string v2, "0900000000"
                invoke-interface {v0, v1, v2}, Landroid/content/SharedPreferences${'$'}Editor;->putString(Ljava/lang/String;Ljava/lang/String;)Landroid/content/SharedPreferences${'$'}Editor;
                move-result-object v0

                invoke-interface {v0}, Landroid/content/SharedPreferences${'$'}Editor;->apply()V
            """
        )

        // 3. Intercept FlutterSecureStorage MethodChannel calls (read, write, readAll, containsKey)
        AutoSmsSecureStorageFingerprint.method.addInstructions(
            0,
            """
                if-eqz p1, :cond_orig
                if-eqz p2, :cond_orig
                iget-object v0, p1, Lio/flutter/plugin/common/MethodCall;->method:Ljava/lang/String;
                if-eqz v0, :cond_orig

                const-string v1, "read"
                invoke-virtual {v1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v1
                if-eqz v1, :handle_read

                const-string v1, "write"
                invoke-virtual {v1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v1
                if-eqz v1, :handle_write

                const-string v1, "readAll"
                invoke-virtual {v1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v1
                if-eqz v1, :handle_read_all

                const-string v1, "containsKey"
                invoke-virtual {v1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v1
                if-eqz v1, :handle_contains_key

                goto :cond_orig

                :handle_read
                const-string v0, "key"
                invoke-virtual {p1, v0}, Lio/flutter/plugin/common/MethodCall;->argument(Ljava/lang/String;)Ljava/lang/Object;
                move-result-object v0
                check-cast v0, Ljava/lang/String;
                if-eqz v0, :cond_orig

                const-string v1, "auth_token_v1"
                invoke-virtual {v1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v1
                if-eqz v1, :return_token

                const-string v1, "auth_refresh_token_v1"
                invoke-virtual {v1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v1
                if-eqz v1, :return_token

                const-string v1, "auth_expires_time_v1"
                invoke-virtual {v1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v1
                if-eqz v1, :return_expires

                const-string v1, "auth_expire_license_v1"
                invoke-virtual {v1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v1
                if-eqz v1, :return_expires

                const-string v1, "auth_last_phone_v1"
                invoke-virtual {v1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v1
                if-eqz v1, :return_phone

                const-string v1, "licenseValid"
                invoke-virtual {v1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v1
                if-eqz v1, :return_bool_true

                const-string v1, "auth_keep_login_v1"
                invoke-virtual {v1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v1
                if-eqz v1, :return_bool_true

                const-string v1, "loginState"
                invoke-virtual {v1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v1
                if-eqz v1, :return_bool_true

                const-string v1, "auth_secure_migrated_v1"
                invoke-virtual {v1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v1
                if-eqz v1, :return_bool_true

                const-string v1, "license"
                invoke-virtual {v1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v1
                if-eqz v1, :return_license

                goto :cond_orig

                :handle_write
                const-string v0, "key"
                invoke-virtual {p1, v0}, Lio/flutter/plugin/common/MethodCall;->argument(Ljava/lang/String;)Ljava/lang/Object;
                move-result-object v0
                check-cast v0, Ljava/lang/String;
                if-eqz v0, :cond_orig

                const-string v1, "licenseValid"
                invoke-virtual {v1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v1
                if-eqz v1, :ignore_write

                const-string v1, "auth_expire_license_v1"
                invoke-virtual {v1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v1
                if-eqz v1, :ignore_write

                const-string v1, "auth_expires_time_v1"
                invoke-virtual {v1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v1
                if-eqz v1, :ignore_write

                const-string v1, "license"
                invoke-virtual {v1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v1
                if-eqz v1, :ignore_write

                const-string v1, "loginState"
                invoke-virtual {v1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v1
                if-eqz v1, :ignore_write

                goto :cond_orig

                :ignore_write
                const/4 v0, 0x0
                invoke-interface {p2, v0}, Lio/flutter/plugin/common/MethodChannel${'$'}Result;->success(Ljava/lang/Object;)V
                return-void

                :return_token
                const-string v0, "bypass_token_12345"
                invoke-interface {p2, v0}, Lio/flutter/plugin/common/MethodChannel${'$'}Result;->success(Ljava/lang/Object;)V
                return-void

                :return_expires
                const-string v0, "253402300799000"
                invoke-interface {p2, v0}, Lio/flutter/plugin/common/MethodChannel${'$'}Result;->success(Ljava/lang/Object;)V
                return-void

                :return_phone
                const-string v0, "0900000000"
                invoke-interface {p2, v0}, Lio/flutter/plugin/common/MethodChannel${'$'}Result;->success(Ljava/lang/Object;)V
                return-void

                :return_bool_true
                const-string v0, "true"
                invoke-interface {p2, v0}, Lio/flutter/plugin/common/MethodChannel${'$'}Result;->success(Ljava/lang/Object;)V
                return-void

                :return_license
                const-string v0, "ACTIVATED"
                invoke-interface {p2, v0}, Lio/flutter/plugin/common/MethodChannel${'$'}Result;->success(Ljava/lang/Object;)V
                return-void

                :handle_read_all
                new-instance v0, Ljava/util/HashMap;
                invoke-direct {v0}, Ljava/util/HashMap;-><init>()V
                const-string v1, "auth_token_v1"
                const-string v2, "bypass_token_12345"
                invoke-virtual {v0, v1, v2}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                const-string v1, "auth_refresh_token_v1"
                const-string v2, "bypass_token_12345"
                invoke-virtual {v0, v1, v2}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                const-string v1, "auth_expires_time_v1"
                const-string v2, "253402300799000"
                invoke-virtual {v0, v1, v2}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                const-string v1, "auth_expire_license_v1"
                const-string v2, "253402300799000"
                invoke-virtual {v0, v1, v2}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                const-string v1, "auth_last_phone_v1"
                const-string v2, "0900000000"
                invoke-virtual {v0, v1, v2}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                const-string v1, "auth_keep_login_v1"
                const-string v2, "true"
                invoke-virtual {v0, v1, v2}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                const-string v1, "licenseValid"
                const-string v2, "true"
                invoke-virtual {v0, v1, v2}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                const-string v1, "license"
                const-string v2, "ACTIVATED"
                invoke-virtual {v0, v1, v2}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                const-string v1, "loginState"
                const-string v2, "true"
                invoke-virtual {v0, v1, v2}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                const-string v1, "auth_secure_migrated_v1"
                const-string v2, "true"
                invoke-virtual {v0, v1, v2}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                invoke-interface {p2, v0}, Lio/flutter/plugin/common/MethodChannel${'$'}Result;->success(Ljava/lang/Object;)V
                return-void

                :handle_contains_key
                const-string v0, "key"
                invoke-virtual {p1, v0}, Lio/flutter/plugin/common/MethodCall;->argument(Ljava/lang/String;)Ljava/lang/Object;
                move-result-object v0
                check-cast v0, Ljava/lang/String;
                if-eqz v0, :cond_orig
                sget-object v0, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;
                invoke-interface {p2, v0}, Lio/flutter/plugin/common/MethodChannel${'$'}Result;->success(Ljava/lang/Object;)V
                return-void

                :cond_orig
            """
        )
    }
}
