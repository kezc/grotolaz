# Gradle wiring

The PWA build is produced by the third-party **ComposePWA** Gradle plugin
(`dev.yuyuyuyuyu.composepwa`), which shells out to the Workbox CLI (via `npx`) to
generate the service worker. It adds a `buildWasmAsPwa` task to the module it is
applied to.

## 1. Version catalog — `gradle/libs.versions.toml`

Do NOT hardcode a version — resolve it at apply time with the bundled resolver.

**ComposePWA is a deliberate exception to the "latest stable" rule: always take
the newest release, even a prerelease.** This young 0.x plugin ships its current
functionality in `-alpha`/`-beta` builds (the repository even advertises an
`-alpha` as its "release"), and this skill was validated against a prerelease, so
pinning to the older "stable" line would give you a worse, unvalidated setup. Use
`--allow-prerelease`:

```sh
scripts/latest_version.sh --plugin dev.yuyuyuyuyu.composepwa --allow-prerelease
# → prints the newest release, e.g. 0.7.0-alpha02
```

Then write that value:

```toml
[versions]
composePwa = "<resolved-version>"   # newest, from latest_version.sh --allow-prerelease

[plugins]
composePwa = { id = "dev.yuyuyuyuyu.composepwa", version.ref = "composePwa" }
```

> This exception applies ONLY to the composePwa plugin. Any other artifact the
> skill introduces still uses the latest **stable** (no `--allow-prerelease`).

## 2. Apply the plugin in the **wasmJs application module** (the executable)

`<webAppModule>/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.composePwa)          // <-- add
}

kotlin {
    wasmJs {
        browser()
        binaries.executable()
    }
    // ...
}
```

No plugin `{ }` configuration block is needed: the plugin reads
`workbox-config-for-wasm.js` from the module directory by convention. That file
name matters — keep it exactly as-is, next to `build.gradle.kts`.

## 3. Build

```sh
export PATH="/opt/homebrew/bin:$PATH"   # ensure npx is on PATH (see SKILL.md)
./gradlew :<webAppModule>:buildWasmAsPwa
```

A correct build logs something like:

```
The service worker will precache 14 URLs, totaling 12.5 MB.
```

Output distribution (serve THIS directory over http(s), not file://):

```
<webAppModule>/build/dist/wasmJs/productionExecutable/
```

## Where files go

- `workbox-config-for-wasm.js` → module root (next to `build.gradle.kts`).
- `index.html`, `manifest.json`, `registerServiceWorker.js`, icons → the wasmJs
  application module's resources source set. For a single-target wasmJs module
  that keeps sources under `commonMain`, that is `src/commonMain/resources/`;
  otherwise `src/wasmJsMain/resources/`. (Check where an existing `index.html`
  already lives and match it.)
- `PwaUpdate.*.kt` + the update UI → your **shared UI** module (wherever the root
  composable lives).
- `wasmJs-main.kt` content → replace the body of your existing wasmJs `main()`.
