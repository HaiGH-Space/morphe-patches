# AGENT.md — MorpheApp Extension Development Guide

This document defines the architecture, conventions, build pipeline, runtime constraints, and development workflow for building **Extensions** (`.mpe`) within the **MorpheApp** ecosystem. Use this guide as the primary reference when creating, modifying, or debugging Android extension modules and their corresponding patch hooks.

---

## 1. Core Concepts & Architectural Separation

MorpheApp (a hard-fork evolution of the ReVanced architecture) splits APK modification into two distinct layers: **Patches** (patch-time transformation rules) and **Extensions** (runtime Android business logic).

### Terminology Disambiguation
In MorpheApp repositories, **"Extension"** strictly refers to **Android Java/Kotlin modules located under `extensions/` that compile into `.mpe` DEX archives**. Do **not** confuse this with file name extensions (`.mpp`, `.rvp`, split APKs), browser extensions, or Morphe Manager source-discovery feature requests.

### Patch vs. Extension Matrix

| Dimension | Patch (`.mpp`) | Extension (`.mpe`) |
| :--- | :--- | :--- |
| **Technical Nature** | Ruleset for locating and mutating Dalvik bytecode, APK resources, or raw files. | Pre-compiled DEX file containing full Android Java/Kotlin classes and methods. |
| **Language** | Kotlin DSL (`bytecodePatch`, `resourcePatch`) via `morphe-patcher`. | Java or Kotlin targeting Android APIs directly. |
| **Execution Time** | **Patch-time** (during APK patching via Morphe Manager, Morphe Desktop, or CLI). | **Runtime** (inside the patched target app process on the user's Android device). |
| **Output Artifact** | `.mpp` patch bundle (e.g., `patches-*.mpp`, `patches-1.39.1.mpp`). | `.mpe` DEX archive (e.g., `extension.mpe`, `youtube.mpe`, `shared.mpe`). |
| **Directory** | `patches/` | `extensions/` |
| **Linking Mechanism** | Uses fingerprints to locate target methods and injects Smali calls (e.g., `invoke-static`). | Merged into `context.classes` by `morphe-patcher` **before** the patch's `execute` block runs. |

### When to Write an Extension vs. Pure Smali
- **Pure Smali in Patch:** Simple constant returns (`returnEarly(false)`), flag flips, or single-instruction NOPs.
- **Extension + Patch Hook:** Complex state management (`BaseSettings`), UI filtering (`LithoFilterPatch`), network requests (`Requester`, `VisitorIdRequester`), stream spoofing (`SpoofVideoStreamsPatch`), SponsorBlock integration, or audio mixing (`CrossfadeManager`).

---

## 2. Ecosystem Repository Map

Because MorpheApp does not maintain a single standalone "Extension Docs" page, implementation details and API contracts are distributed across the core `MorpheApp` GitHub repositories:

1. **`MorpheApp/morphe-patcher`**
   - *Key Files:* `docs/2_2_patch_anatomy.md`, `docs/3_structure_and_conventions.md`
   - *Role:* Defines `.mpe` semantics, `extendWith`, `extendWithAll`, and how extension classes are merged into `context.classes`.
2. **`MorpheApp/morphe-patches-gradle-plugin`**
   - *Key Files:* `README.md`
   - *Role:* Provides the `settings`, `patches`, and `extension` Gradle plugins; manages D8 compilation and desugaring into `.mpe`. *(Note: Hard fork of ReVanced's plugin; behavior may diverge significantly).*
3. **`MorpheApp/morphe-patches-template`**
   - *Key Files:* `settings.gradle.kts`, `extensions/extension/`, GitHub Actions workflows
   - *Role:* Starter project skeleton for third-party patch/extension repositories.
4. **`MorpheApp/morphe-patches-library`**
   - *Key Files:* `extension-library` (published as `app.morphe:morphe-extensions-library`), `extensions/signature`
   - *Role:* Shared runtime utilities, preference builders, and base settings classes used across extensions.
5. **`MorpheApp/morphe-patches`**
   - *Key Files:* `settings.gradle.kts`, `extensions/proguard-rules.pro`, `extensions/{youtube,shared-youtube,music,reddit,shared}/`
   - *Role:* Canonical production reference implementation.

---

## 3. Lifecycle: Build, Patch-Time Merge, and Android Runtime

### Stage 1: Gradle Build & D8 Compilation
1. **Plugin Auto-Application:** `morphe-patches-gradle-plugin` automatically applies the `extension` plugin to subprojects inside `extensions/`.
2. **Namespace & ProGuard Configuration (`settings.gradle.kts`):**
   - Set `defaultNamespace = "app.morphe.extension"`.
   - **CRITICAL RULE:** Always pass an **absolute path** to `proguard-rules.pro` (e.g., `file("extensions/proguard-rules.pro").absolutePath`). Passing a relative path will fail in nested extension subprojects because Gradle resolves relative paths against the subproject directory.
3. **Module Output Declaration (`extensions/<module>/build.gradle.kts`):**
   - Declare the output `.mpe` path explicitly:
     ```kotlin
     extension {
         name = "extensions/extension.mpe"
     }
     ```
4. **D8 Compilation & Dependencies:**
   - The plugin invokes **D8** (configured with `minApi` and Android SDK classpath/library providers for Java 8+ desugaring) to compile Java/Kotlin classes into DEX format inside the `.mpe` archive.
   - Shared library dependency: `app.morphe:morphe-extensions-library` (e.g., `2.2.0` / `2.3.0-dev.1`), fetched from GitHub Packages (`https://maven.pkg.github.com/MorpheApp/registry`) or substituted locally via Gradle composite builds (`includeBuild`).
5. **Build Command:**
   - Run `./gradlew buildAndroid` to compile extensions and bundle them with patches into `patches/build/libs/patches-*.mpp`.

### Stage 2: Patch-Time Linking (`morphe-patcher`)
Patches bind to extensions using two primary APIs:

1. **Static Extension Loading (`extendWith`):**
   - Used when a patch requires a pre-built `.mpe` file bundled inside the `.mpp`:
     ```kotlin
     val myComplexPatch = bytecodePatch(name = "My Complex Patch") {
         extendWith("extensions/extension.mpe")

         execute {
             // Extension classes are already merged into context.classes here.
             targetFingerprint.method.addInstruction(
                 0,
                 "invoke-static {}, Lapp/morphe/extension/youtube/patches/MyComplexPatch;->doSomething()V"
             )
         }
     }
     ```
2. **Dynamic Extension Loading (`extendWithAll`):**
   - Used when the number or content of DEX files depends on patch-time resources or dynamic generation:
     ```kotlin
     val dynamicPatch = bytecodePatch(name = "Dynamic Extension Patch") {
         dependsOn(prepareExtensionsPatch) // resourcePatch populating Supplier<InputStream> list
         extendWithAll { derivedExtensions }

         execute { /* ... */ }
     }
     ```
3. **Shared Extension Dependencies:**
   - In multi-app repositories (`morphe-patches`), shared utilities (e.g., `CrossfadeManager`) are isolated into shared `.mpe` modules (`shared.mpe`, `shared-youtube.mpe`) and loaded via a common dependency patch (such as `sharedExtensionPatch`) to avoid duplicate class definitions across `context.classes`.

### Stage 3: Runtime Execution & Initialization Pitfalls
Once patched, classes under `app.morphe.extension.*` execute inside the target Android app's process (e.g., YouTube, YouTube Music, Reddit).

- **Standard Runtime Capabilities:**
  - Persistent settings via `BaseSettings` (`BooleanSetting`, `StringSetting`, `IntegerSetting`, etc.).
  - Native preference UI injection (`SwitchPreference`, `ListPreference`).
  - HTTP networking (`Requester`, `VisitorIdRequester`).
  - UI tree interception (`LithoFilterPatch`).
- **CRITICAL RUNTIME CONSTRAINT — `Context` Initialization Order:**
  - `BaseSettings`, resource lookups, and branding patches depend on `Utils.setContext(Context)` being invoked during the earliest possible app startup lifecycle hook (e.g., `Application.onCreate()` or `MainActivity.onCreate()`).
  - **Failure Mode:** If a target app update or server-side A/B test alters startup execution order so that a patched hook calls into an extension class **before** `Utils.setContext(...)` executes, the app will crash immediately on launch with `java.lang.ExceptionInInitializerError` or a `NullPointerException` inside `BaseSettings`.
  - **Agent Rule:** Never access `BaseSettings` or Android `Resources` in static initializers (`static { ... }`) of classes hooked early in the app launch sequence unless `Utils.getContext()` is guaranteed to be initialized first.

---

## 4. Coding Conventions & Best Practices for Agents

1. **Package & Naming Alignment:**
   - Keep extension classes under `app.morphe.extension.<target_app>.patches.<PatchName>` (or `shared` for cross-app utilities).
   - Mirror the patch name between `patches/` and `extensions/` (e.g., `EnableSwipeToDismissMiniplayerPatch.kt` in `patches/` pairs with `EnableSwipeToDismissMiniplayerPatch.java` in `extensions/`).
2. **Clean Hook Signatures:**
   - Expose minimal, explicit `public static` methods in the extension class for Smali hooks to call.
   - Define a constant `EXTENSION_CLASS_DESCRIPTOR = "Lapp/morphe/extension/.../MyPatch;"` in the Kotlin patch file rather than hardcoding class descriptors repeatedly across multiple `addInstruction` / `replaceInstruction` calls.
3. **ProGuard / R8 Safety:**
   - Any class, method, or field invoked from Smali hooks in `patches/` **must** be preserved from obfuscation or dead-code stripping via `extensions/proguard-rules.pro`.
4. **Java Compatibility:**
   - Write extension code in clean Java (or Kotlin if configured in the module) compatible with Android's D8 desugaring pipeline and the target app's `minSdk`. Avoid reflection unless interacting with unexposed target-app internals.

---

## 5. Licensing & Branding Compliance (GPL v3.0 + Section 7)

When generating or modifying project metadata, READMEs, or package names based on `morphe-patches-template`:
- Core Morphe repositories (`morphe-patcher`, `morphe-patches`, `morphe-patches-library`, `morphe-patches-gradle-plugin`) are licensed under **GNU GPL v3.0 with Additional Terms under Section 7 (7c & 7e)**.
- **Strict Naming Restriction:** Derivative works **must not** use the "Morphe" name, logo, or branding as their primary product brand (e.g., *"Morphe Plus"*, *"Morphe Expanded"*, *"Morphe UserXYZ"* are prohibited).
- **Permitted Attribution:** Use descriptive author-attributed naming such as `"<Author> Morphe patches"` so it cannot be mistaken for an official MorpheApp release.

---

## 6. Agent Development & Verification Checklist

When tasked with implementing a new feature requiring an Extension:

1. **Inspect Existing Setup:**
   - Check `settings.gradle.kts` for `defaultNamespace`, `extensions/proguard-rules.pro` absolute path resolution, and GitHub Packages authentication (`gpr.user` / `gpr.key` or `GITHUB_ACTOR` / `GITHUB_TOKEN`).
2. **Implement Extension Logic (`extensions/...`):**
   - Create the Java/Kotlin class under `app.morphe.extension.*`.
   - Register any required settings in the appropriate `Settings` class extending `BaseSettings`.
   - Ensure all entry-point methods called by Smali are `public static` and guarded against uninitialized `Context`.
3. **Verify ProGuard Rules:**
   - Ensure `extensions/proguard-rules.pro` keeps the entry-point classes and methods intact.
4. **Implement Patch Hook (`patches/...`):**
   - Add `extendWith("extensions/<module>.mpe")` (or depend on the shared extension loader patch).
   - Locate target method(s) using `Fingerprint` and inject `invoke-static` / `invoke-static/range` calls targeting `EXTENSION_CLASS_DESCRIPTOR`.
5. **Build & Test Locally:**
   - Build the `.mpp` bundle:
     ```bash
     ./gradlew buildAndroid
     ```
   - Verify output exists at `patches/build/libs/patches-*.mpp`.
   - Test on a target APK via `morphe-desktop` CLI and ADB:
     ```bash
     morphe-desktop patch --patches patches/build/libs/patches-*.mpp --install <target.apk>
     ```
   - Monitor `adb logcat` on app cold start for `ExceptionInInitializerError`, `NoClassDefFoundError`, or `VerifyError` to confirm D8 compilation, DEX merging, and `Context` initialization order are valid.