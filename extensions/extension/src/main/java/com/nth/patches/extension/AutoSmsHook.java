package com.nth.patches.extension;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AutoSmsHook {

    private static final String BYPASS_TOKEN = "bypass_token_12345";
    private static final String YEAR_9999 = "253402300799000";
    private static final String PHONE = "0900000000";
    private static final String VIP_NAME = "Vip";
    private static final String ACTIVATED = "ACTIVATED";
    private static final String TRUE_STR = "true";

    private static final String USER_JSON = "{\"id\":1,\"name\":\"Vip\",\"username\":\"Vip\",\"displayName\":\"Vip\",\"webUsername\":\"Vip\",\"phone\":\"0900000000\",\"licenseValid\":true,\"license\":\"ACTIVATED\",\"expireAt\":253402300799000,\"auth_expire_license_v1\":253402300799000,\"auth_expires_time_v1\":253402300799000,\"zaloWebExpireAt\":253402300799000,\"zaloMultiDeviceExpire\":253402300799000,\"zaloWebEnabled\":true}";

    private static final Set<String> EXPIRE_KEYS = new HashSet<>(Arrays.asList(
        "auth_expires_time_v1", "auth_expire_license_v1", "zaloWebExpireAt",
        "zaloMultiDeviceExpire", "zalo_web_plan_synced_v1_", "expireAt",
        "expiresAt", "expires_at", "expire_at", "expires", "expireLicense", "expiresTime"
    ));

    private static final Set<String> BOOL_TRUE_KEYS = new HashSet<>(Arrays.asList(
        "licenseValid", "zaloWebEnabled", "auth_keep_login_v1", "loginState",
        "auth_secure_migrated_v1", "isLicenseValid", "is_license_valid"
    ));

    private static final Set<String> LICENSE_KEYS = new HashSet<>(Arrays.asList(
        "license", "LicenseStatus", "licenseStatus"
    ));

    private static final Set<String> USER_JSON_KEYS = new HashSet<>(Arrays.asList(
        "user", "profile", "user_data", "user_info", "account"
    ));

    private static final Set<String> NAME_KEYS = new HashSet<>(Arrays.asList(
        "name", "username", "displayName", "webUsername", "customerName", "name_v1"
    ));

    // Pre-populate SharedPreferences on MainActivity launch
    public static void initPreferences(Context context) {
        if (context == null) return;
        try {
            SharedPreferences sp = context.getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE);
            sp.edit()
                .putBoolean("flutter.licenseValid", true)
                .putString("flutter.license", ACTIVATED)
                .putLong("flutter.auth_expire_license_v1", 253402300799000L)
                .putString("flutter.auth_expires_time_v1", YEAR_9999)
                .putString("flutter.zaloWebExpireAt", YEAR_9999)
                .putString("flutter.zaloMultiDeviceExpire", YEAR_9999)
                .putBoolean("flutter.zaloWebEnabled", true)
                .putString("flutter.zalo_web_plan_synced_v1_", YEAR_9999)
                .putString("flutter.expireAt", YEAR_9999)
                .putString("flutter.expiresAt", YEAR_9999)
                .putString("flutter.expires_at", YEAR_9999)
                .putString("flutter.user", USER_JSON)
                .putString("flutter.profile", USER_JSON)
                .putString("flutter.name", VIP_NAME)
                .putString("flutter.username", VIP_NAME)
                .putString("flutter.displayName", VIP_NAME)
                .putString("flutter.webUsername", VIP_NAME)
                .putString("flutter.auth_token_v1", BYPASS_TOKEN)
                .putString("flutter.auth_refresh_token_v1", BYPASS_TOKEN)
                .putBoolean("flutter.loginState", true)
                .putBoolean("flutter.auth_keep_login_v1", true)
                .putBoolean("flutter.auth_secure_migrated_v1", true)
                .putString("flutter.auth_last_phone_v1", PHONE)
                .apply();
        } catch (Exception ignored) {
        }
    }

    // Intercept MethodCall on FlutterSecureStorage
    // Returns true if handled (method execution finished), false to proceed to original handler
    public static boolean handleMethodCall(Object callObj, Object resultObj) {
        if (callObj == null || resultObj == null) return false;

        try {
            String method = (String) callObj.getClass().getField("method").get(callObj);
            if (method == null) return false;

            if ("read".equals(method)) {
                String key = (String) callObj.getClass().getMethod("argument", String.class).invoke(callObj, "key");
                if (key == null) return false;

                if ("auth_token_v1".equals(key) || "auth_refresh_token_v1".equals(key)) {
                    sendSuccess(resultObj, BYPASS_TOKEN);
                    return true;
                }
                if (EXPIRE_KEYS.contains(key)) {
                    sendSuccess(resultObj, YEAR_9999);
                    return true;
                }
                if ("auth_last_phone_v1".equals(key)) {
                    sendSuccess(resultObj, PHONE);
                    return true;
                }
                if (NAME_KEYS.contains(key)) {
                    sendSuccess(resultObj, VIP_NAME);
                    return true;
                }
                if (BOOL_TRUE_KEYS.contains(key)) {
                    sendSuccess(resultObj, TRUE_STR);
                    return true;
                }
                if (LICENSE_KEYS.contains(key)) {
                    sendSuccess(resultObj, ACTIVATED);
                    return true;
                }
                if (USER_JSON_KEYS.contains(key)) {
                    sendSuccess(resultObj, USER_JSON);
                    return true;
                }
            } else if ("write".equals(method)) {
                String key = (String) callObj.getClass().getMethod("argument", String.class).invoke(callObj, "key");
                if (key != null && (
                    EXPIRE_KEYS.contains(key) || BOOL_TRUE_KEYS.contains(key) ||
                    LICENSE_KEYS.contains(key) || USER_JSON_KEYS.contains(key) ||
                    NAME_KEYS.contains(key) || "auth_token_v1".equals(key) ||
                    "auth_refresh_token_v1".equals(key) || "auth_last_phone_v1".equals(key)
                )) {
                    // Block writes/overwrites to session/user/license/token keys
                    sendSuccess(resultObj, null);
                    return true;
                }
            } else if ("delete".equals(method) || "deleteAll".equals(method) || "clear".equals(method)) {
                // Block logout & session wipe calls on FlutterSecureStorage (disables logout feature)
                sendSuccess(resultObj, null);
                return true;
            } else if ("readAll".equals(method)) {
                Map<String, String> map = new HashMap<>();
                map.put("auth_token_v1", BYPASS_TOKEN);
                map.put("auth_refresh_token_v1", BYPASS_TOKEN);
                map.put("auth_expires_time_v1", YEAR_9999);
                map.put("auth_expire_license_v1", YEAR_9999);
                map.put("zaloWebExpireAt", YEAR_9999);
                map.put("zaloMultiDeviceExpire", YEAR_9999);
                map.put("zaloWebEnabled", TRUE_STR);
                map.put("zalo_web_plan_synced_v1_", YEAR_9999);
                map.put("expireAt", YEAR_9999);
                map.put("expiresAt", YEAR_9999);
                map.put("user", USER_JSON);
                map.put("profile", USER_JSON);
                map.put("name", VIP_NAME);
                map.put("username", VIP_NAME);
                map.put("displayName", VIP_NAME);
                map.put("auth_last_phone_v1", PHONE);
                map.put("auth_keep_login_v1", TRUE_STR);
                map.put("licenseValid", TRUE_STR);
                map.put("license", ACTIVATED);
                map.put("loginState", TRUE_STR);
                map.put("auth_secure_migrated_v1", TRUE_STR);
                sendSuccess(resultObj, map);
                return true;
            } else if ("containsKey".equals(method)) {
                String key = (String) callObj.getClass().getMethod("argument", String.class).invoke(callObj, "key");
                if (key != null && (
                    EXPIRE_KEYS.contains(key) || BOOL_TRUE_KEYS.contains(key) ||
                    LICENSE_KEYS.contains(key) || USER_JSON_KEYS.contains(key) ||
                    NAME_KEYS.contains(key) || "auth_token_v1".equals(key) ||
                    "auth_refresh_token_v1".equals(key) || "auth_last_phone_v1".equals(key)
                )) {
                    sendSuccess(resultObj, Boolean.TRUE);
                    return true;
                }
            }
        } catch (Exception ignored) {
        }

        return false;
    }

    private static void sendSuccess(Object resultObj, Object value) {
        try {
            resultObj.getClass().getMethod("success", Object.class).invoke(resultObj, value);
        } catch (Exception ignored) {
        }
    }
}
