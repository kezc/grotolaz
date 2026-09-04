---
name: compose-wasmjs-pwa
description: >-
  Turn a Compose Multiplatform (Kotlin/Wasm) web app into an installable PWA with
  a deliberate software-update experience and a correctly separated data cache:
  Workbox precaching, a startup splash gate so the old version never flashes on
  launch, a user-chosen update notification (pulsating button, banner, silent
  auto-reload, or quiet indicator), an audit of the app's existing data caching
  against what a service worker changes, offline handling, and the server cache
  headers that make update detection actually work. Use this whenever the user
  wants to add PWA
  support, offline support, precaching, a service worker, a web app manifest, an
  "installable web app", or any kind of auto-update / update-prompt / "New
  Software" button to a Compose or Kotlin/Wasm (wasmJs) app — even if they only
  mention "workbox", "service worker", "update on reload", "cache my API
  responses", "make it work offline", or "why is my deploy not picked up". Also
  use it when a Compose wasmJs app caches the wrong things, serves stale data, or
  fails with `Cannot run program "npx"`, and when replacing an older hand-written
  service worker whose clients are stuck on a cached shell ("users are still on
  the old version", "the old sw.js won't go away", "how do I unregister the
  previous service worker"). Prefer this skill over hand-rolling service-worker
  or Cache API code for Compose/wasmJs targets.
---

# Compose wasmJs → PWA with controlled updates and a clean data cache

Converts a Compose Multiplatform app that already has a **wasmJs** browser target
into a PWA whose update behavior is deliberate and whose caching is correctly
partitioned. Bundles the exact files to copy and the wiring steps.

> **Validated 2026-07-25** against Kotlin **2.4.10**, Compose Multiplatform
> **1.11.1**, Gradle **9.5.1**, Ktor **3.5.1**, Workbox **7.4.0**, and the
> ComposePWA plugin **0.7.0-alpha03** — by converting a real production app and
> deploying it.
>
> Several claims here are pinned to those versions and should be re-checked
> rather than trusted if you are far from them: the `JsException` / `kotlin.Error`
> behaviour of a failed fetch (B2 row 5), the `cleanupOutdatedCaches` predicate
> (B2 row 2 — read it out of the generated `workbox-*.js`), and anything about the
> ComposePWA plugin, which is a 0.x alpha and may move under you.
>
> Licensed under Apache-2.0 (see `LICENSE`). Copyright 2026 mpMediaSoft GmbH.

The work splits into two phases:

- **Phase A — software.** Precaching, update detection, the startup gate, and the
  update notification. Always applies. Starts with A0: retiring any service
  worker a previous build already put in users' browsers — do that first, because
  clients stuck behind a cache-first old worker cannot be reached by anything you
  ship later.
- **Phase B — data.** An audit of the app's existing data layer, offline
  handling, and the server cache headers. Applies only if the app fetches
  anything at runtime. Most existing data layers *store* things correctly; the
  defects are almost always in the failure path, which nobody exercises.

Read `references/how-it-works.md` for the software-side rationale and tuning
knobs, `references/data-and-deployment.md` for the caching model and deployment,
and `references/notification-styles.md` when implementing the chosen
notification. You do not need any of them to apply the skill — this file is
enough for the common path.

## The idea that makes the rest follow

> The service worker owns the software cache. The app owns the data cache.
> Neither reaches into the other's territory.

Software is what the build emits and is identical for every user until the next
deploy. Data is what the app fetches at runtime. The browser will happily cache
both through the same machinery, which is exactly the problem — a service worker
that caches API responses serves them stale and hides deploys. Everything below
is downstream of keeping those two apart.

## Step 0 — Ask the user before writing any UI

Notification style is a product decision, not a default. An app that can lose
unsaved work must never silently reload; a kiosk dashboard should never nag. Ask,
then implement one scheme — do not build several and leave dead code behind.

Ask these, together, before touching the project:

1. **Update notification** — which of these when a new version appears *while the
   app is running*:
   - **A. Pulsating "New Software" button + Cancel/Update dialog** — the
     validated default. Best when the user may have unsaved state.
   - **B. Silent auto-reload** — no UI; the tab reloads itself. Only for apps
     that genuinely hold no user state. Say so plainly when offering it: this is
     the one option that can destroy work, and it does so invisibly.
   - **C. Banner / snackbar with a Reload action** — calmer than A, still
     actionable.
   - **D. Quiet indicator** — a dot or label; the user reloads when they like.
     Mention the trade-off: nothing draws the eye, so a long-lived tab can run an
     old version for days.
2. **Does the app fetch data at runtime?** (API calls, documents, polled
   content.) If yes, Phase B applies. If no, skip it entirely.
3. **If Phase B applies — data-status indicator**: pulsing dot / static dot /
   text only when offline / none.

If the user has no opinion, recommend **A** plus a **pulsing dot**: the
combination this skill was validated with. Record what they picked; it decides
which asset variants you copy in A6 and B3.

## Before you start: understand the project shape

Compose Multiplatform apps are usually split into two kinds of module. Identify
them:

- The **wasmJs application module** — has `wasmJs { browser(); binaries.executable() }`
  and a `main()` calling `ComposeViewport { ... }`. The PWA plugin, web
  resources, and the `main()` change go here. (Often keeps its sources under
  `commonMain` even though it only targets wasmJs.)
- The **shared UI module** — where the root `@Composable` (often `App`) lives,
  usually in `commonMain` with several targets. The `PwaUpdate` bridge and the
  update UI go here, as does the app's existing data layer if it has one.

Both may be the same module — fine, everything lands there. Confirm: which module
is the executable, which file has `main()`, which composable `ComposeViewport`
mounts, its package, and every target the shared UI compiles for (each needs an
`actual` for `PwaUpdate`).

If anything is ambiguous — module names, package, whether the UI is multiplatform
or wasmJs-only — ask rather than guess.

## Placeholders in the bundled assets

- `{{PACKAGE}}` — package of the shared UI / root composable.
- `{{APP_NAME}}` / `{{APP_SHORT_NAME}}` — display names for manifest and title.
- `{{WASM_JS_FILE}}` — the JS entry file the wasm build emits, referenced by
  `index.html`: `<moduleName>.js` (e.g. `webApp.js`). Match any existing
  `<script src="...">`.
- `{{APP_DIR}}` / `{{ENDPOINT_URL}}` — only for the bundled `Store` and poll
  template, which most projects will not need (see B2a): a hidden dir under
  `$HOME` for the file-backed store (e.g. `.myapp`), and the polled endpoint.
- `{{RETIRED_PRECACHE_PATTERN}}` / `{{ADDED_DATE}}` / `{{REMOVE_AFTER}}` — only
  for the tombstone in A0: a regex literal matching the OLD worker's cache names,
  and the dates bounding how long the tombstone must stay deployed.

---

# Phase A — software updates

Work in order. After each file, confirm it landed in the right source set for
THIS project — mirror where the project already keeps `index.html`, `main()`, and
the root composable rather than assuming paths.

## A0. Is there already a service worker in the wild? (skip if greenfield)

Do this **before** anything else — it decides what the deploy must contain, and
it is the one part of this skill you cannot retrofit later for users who have
already gone stale.

Ask, and check for yourself: has any previously deployed build at this URL
registered a service worker? Look for a `register(...)` call in the old
`index.html` or entry script, a hand-written `sw.js`/`serviceWorker.js` in the
web resources, and the deployed site's own files if you can reach them. If the
answer is no, skip to A1.

If the answer is yes, understand what you are up against:

> A client stuck behind a **cache-first** old worker never loads your new
> `index.html`. It gets the old shell out of Cache Storage, which loads the old
> entry script — not `registerServiceWorker.js`. **No code in the new build can
> reach that client.** Shipping the new version does nothing for them.

The one channel that still works is the old worker's own update check: the
browser periodically re-fetches the old worker's script URL. So serve a valid
script there that switches the old worker off.

1. Copy `assets/web/legacy-sw-tombstone.js` into the web resources **under the
   old worker's exact filename** (commonly `sw.js`). Wrong path, no effect.
2. Set `{{RETIRED_PRECACHE_PATTERN}}` to a regex matching the OLD cache names.
   **Never `startsWith`.** App data caches routinely share a prefix with the
   software cache they were named alongside (`MyApp-1.2.3` vs `MyAppData__`), and
   a prefix check there deletes the user's data. Anchor on something only the
   retired names have — the version segment, e.g.
   `/^MyApp-\d+\.\d+\.\d+-/`. Read the real names out of DevTools → Application →
   Cache Storage before you commit to the pattern.
3. Uncomment `globIgnores` in the Workbox config with the same filename. The
   tombstone must be fetched from the network, so it must not be precached.
4. Agree a removal date with the user and write it, with the reasoning, into the
   file's header. Base it on how long a user might plausibly stay away — for a
   yearly-event app that is over a year. Deleting it early strands exactly the
   users it exists for.
5. Keep `retireForeignWorkers()` in `registerServiceWorker.js` (A3). It handles
   the easier case: a stale registration on a client that *did* manage to load
   the new page.

Tell the user plainly that A0 is a **deployment** obligation, not just a file:
the tombstone has to stay at that URL until the removal date, and there is no
telemetry that will tell them when the last old client came back.

## A1. Add the Gradle plugin

Follow `assets/gradle/gradle-wiring.md`: add the `composePwa` version-catalog
entries and apply `alias(libs.plugins.composePwa)` in the wasmJs application
module. No plugin config block is needed.

Do not hardcode versions — resolve them at apply time with the bundled resolver.

**The composePwa plugin is an exception: always take the newest release, even a
prerelease.** This 0.x plugin ships its current functionality in `-alpha` builds
and the skill was validated against one:

```sh
scripts/latest_version.sh --plugin dev.yuyuyuyuyu.composepwa --allow-prerelease
```

Every OTHER artifact uses the latest **stable** (`--artifact <group:name>`, no
`--allow-prerelease`). If the project pins plugins directly instead of using a
version catalog, match that style.

## A2. Add the Workbox config

Copy `assets/web/workbox-config-for-wasm.js` to the wasmJs application module
root (next to its `build.gradle.kts`). The filename is a plugin convention — keep
it exactly. It targets `build/dist/wasmJs/productionExecutable/`; adjust only if
the project's dist path differs.

It deliberately has **no `runtimeCaching`**. Do not add any: that is what keeps
the worker out of the app's data. See `references/data-and-deployment.md`.

Then look at what the globs actually match. `globPatterns` is a publishing
decision as much as a caching one — every file in the dist gets served to users
and cached on their devices. Build-time-only files that end up in a resources
directory (a `config.json` of endpoints, source maps) are shipped and precached
without anyone deciding to publish them. Report anything suspicious; the fix is
to stop emitting the file into the dist, not to paper over it with `globIgnores`.

## A3. Add the web resources

Into the wasmJs module's resources source set (match the existing `index.html`
location — usually `src/commonMain/resources/` or `src/wasmJsMain/resources/`):

- `assets/web/registerServiceWorker.js` — copy verbatim. It contains a
  `retireForeignWorkers()` safety belt for projects coming from an earlier worker
  (A0); on a greenfield project it is harmless and may be collapsed to a plain
  `register(...)` as the comment there describes.
- `assets/web/index.html` — if one exists, **merge** rather than overwrite: keep
  its `<script src="registerServiceWorker.js">` in `<head>` (synchronous, no
  `defer`), its icon/manifest `<link>`s, add the `#pwa-splash` `<div>` in
  `<body>`, and keep the existing `<script src="{{WASM_JS_FILE}}">`.
- `assets/web/manifest.json` — set names; ensure the icons it lists exist.
- Icons: reuse the project's existing favicons. If none exist, tell the user which
  files are referenced (`android-chrome-192x192.png`, `android-chrome-512x512.png`,
  `apple-touch-icon.png`, `favicon-16x16.png`, `favicon-32x32.png`, `favicon.ico`)
  so they can supply them. Do not fabricate binary icons.

## A4. Add the `PwaUpdate` bridge

Into the shared UI module:

- `assets/kotlin/PwaUpdate.common.kt` → `commonMain`
- `assets/kotlin/PwaUpdate.wasmJs.kt` → `wasmJsMain`
- `assets/kotlin/PwaUpdate.noop.kt` → once into **each** other target source set
  (`jvmMain`, `androidMain`, `iosMain`, …). Every `expect` needs an `actual` per
  target or the module will not compile.

If the shared UI is wasmJs-only, skip the `expect`/`actual` split: keep just the
wasmJs implementation plus `rememberUpdateAvailable` in one file.

Requires `kotlinx.coroutines` on the shared UI's common classpath.

## A5. Gate app startup in `main()`

Replace the wasmJs `main()` body with `assets/kotlin/wasmJs-main.kt` (set
`{{PACKAGE}}`, swap `App()` for the real root composable). The key change:
`main()` awaits `window.pwaStartupGate` before `ComposeViewport`, then removes
`#pwa-splash`. This is what stops the old version flashing up on launch.

## A6. Add the chosen update notification

Implement **only** the scheme picked in step 0.

- **A (pulsating button + dialog)** — follow `assets/kotlin/app-update-ui.kt`:
  wrap the root composable's content in `Box(Modifier.fillMaxSize())` and add
  `UpdateAffordance()` inside it, last so it draws on top.
- **B / C / D** — read `references/notification-styles.md` and follow the section
  for that style. B also requires a change in `registerServiceWorker.js`
  (dropping the deferral), which that reference describes.

The assets use plain string literals and no icons to avoid extra dependencies. If
the app already uses compose-resources, move the strings into resources to match
its conventions.

---

# Phase B — data: audit what exists, change only what is broken

Skip entirely if the app fetches nothing at runtime.

**Any real app already has a data layer.** Phase B is therefore an audit, not an
installation. Adding a parallel caching mechanism next to a working one is worse
than doing nothing: two caches for the same data drift apart, and you have
rewritten code you were not asked to touch. Most existing data layers turn out to
be fine — a service worker changes surprisingly little for them. Your job is to
find the few arrangements that genuinely break, and to leave the rest alone.

## B1. Find out what the app already does

Look for, in the shared/common code:

- the HTTP client and where responses are turned into model objects;
- any persistence — `localStorage`, IndexedDB, the Cache API, a database, files,
  or nothing at all (in-memory);
- **cache names**, if it uses Cache Storage;
- whether requests send validators (`If-Modified-Since` / `If-None-Match`) or
  refetch blindly;
- how a failed request is handled — **read the actual `catch` clauses**, and what
  happens to the model object when a refresh fails;
- whether refreshing happens in a long-lived loop, and what that loop does if the
  work it drives throws.

Report what you found before changing anything.

## B2. Judge it against six questions

Only these actually matter under a service worker:

| # | Question | If the answer is bad |
|---|---|---|
| 1 | Does the **service worker** cache data — any `runtimeCaching` in the Workbox config? | **Broken. Fix.** Remove `runtimeCaching`. The worker intercepts every `fetch()`, including Ktor's, and will serve API responses stale and accumulate them. The app cannot opt out from the Kotlin side. |
| 2 | If the app uses Cache Storage, could `cleanupOutdatedCaches` match its name? | Almost certainly not — see below. Only a name containing **both** `-precache-` **and** the registration scope is at risk. |
| 3 | Does cached data **persist** (not memory-only)? | Not necessarily broken, but the app will show nothing on an offline launch. Raise it; let the user decide. |
| 4 | On a failed refresh, does the app **publish the cached copy**, or leave the model empty? | **Broken. Fix.** Persisting data is not the same as showing it. See below. |
| 5 | Does the failure path `catch (e: Exception)`? | **Broken on wasmJs. Fix.** A browser fetch failure is not an `Exception`. See below. |
| 6 | Is a failed request an **ordinary outcome** — recorded, not swallowed and not fatal? | Fix per B3. |

**Row 4 is the one people get wrong while believing they got it right.** An app
can have a perfectly good persistent cache, read from it at the top of its
refresh routine, and still come up blank offline, because the failure path only
sets `offline = true` and never hands the cached copy to the model. The user then
sees an empty-state screen ("no data yet") sitting on top of a cache full of
data — the exact opposite of the point of the whole exercise. Check what the UI
renders after a failed refresh, not just what the cache contains:

```kotlin
// On failure, fill a MISSING model from the cache; never overwrite a fresher one.
.onFailure { state = state.copy(data = state.data ?: cached, offline = true) }
```

**Row 5 is invisible on every other target.** On Kotlin/Wasm a failed fetch does
not arrive as an `Exception`: `kotlin.js.JsException` extends `Throwable`
directly, and Ktor's JS engine surfaces fetch failures as `kotlin.Error` — also
`Throwable`, not `Exception`. So `catch (e: Exception)` does not catch an offline
browser. The failure sails past the retry loop, past the caller's own catch, and
out of the polling coroutine: the app never learns it is offline, never polls
again, and only a relaunch recovers. The same code is correct on JVM/Android,
where Ktor throws `IOException` — so this survives review and testing on desktop.

Catch `Throwable` and rethrow `CancellationException` (which is not a failure):

```kotlin
try { refresh() }
catch (cancellation: CancellationException) { throw cancellation }
catch (failure: Throwable) { offline = true }
```

`runCatching { }` is already correct here — it catches `Throwable` — which is why
the bundled B3 snippet is safe. Hand-written `try`/`catch` in an existing data
layer is where to look. Grep for it: `catch (e: Exception)`, `catch (e: IOException)`.

**And check the loop itself.** Whatever drives periodic refresh must survive the
work throwing, or the first failure ends refreshing for the life of the page —
reconnecting then fixes nothing and only a restart helps:

```kotlin
while (isActive) {
    try { action() }
    catch (cancellation: CancellationException) { throw cancellation }
    catch (failure: Throwable) { log("poll failed; continuing") }
    delay(intervalMillis)
}
```

On row 2, be precise rather than superstitious. The widespread advice is "never
name your cache `workbox`-anything", but `cleanupOutdatedCaches` actually deletes
a cache only when all three hold:

```js
name.includes("-precache-") && name.includes(self.registration.scope) && name !== currentPrecacheName
```

You would have to build such a name on purpose. Read the predicate out of the
generated `workbox-*.js` if you want to confirm it for the version in play, then
tell the user their name is safe — do not make them rename for folklore.

Everything else is very likely fine. In particular:

- **`localStorage`** — app-owned, survives updates, untouched by Workbox. Fine
  for small text payloads. Only worth changing if the data is large or binary
  (~5 MB quota, strings only, blocks the main thread), and then IndexedDB or the
  Cache API is the answer.
- **IndexedDB or a database** — fine, and already the right tool. Leave it.
- **A Cache API store under the app's own name** — ideal; better than anything
  this skill bundles. A common shape is a "vault" wrapping `caches.open(name)`
  with its own table-of-contents entry alongside the payloads, storing strings
  and byte arrays. Leave it alone. Two things are worth *raising* without
  demanding a change:
  - **Eviction can desynchronise a table of contents from its payloads.** Cache
    Storage evicts individual entries, so a TOC can outlive the file it lists —
    `exists()` then answers from the TOC and says yes while `retrieve()` throws.
    Suggest making retrieval return null and self-heal the TOC, so a miss is an
    ordinary outcome rather than an exception.
  - **A store-time timestamp is not an HTTP validator.** Vaults typically record
    when *they* wrote the file, which cannot drive a conditional GET. Fine for a
    file cache; if the app wants `304` revalidation it needs the server's
    `Last-Modified`/`ETag` kept alongside the payload.
- **No revalidation** (always a full GET) — works; conditional GETs are an
  optimization, not a correctness fix. Mention it, do not impose it.

The reason most *storage* choices pass: a service-worker update swaps the
*precache*, and that is all. It does not touch `localStorage`, IndexedDB, or a
differently-named Cache Storage entry. Data survives updates almost by default —
the storage exceptions are exactly rows 1 and 2.

Rows 4–6 are a different matter, and they are where the real defects are. They
are not about storage at all but about the **failure path**, which is the part
nobody exercises: it only runs when the network is gone, so it is written once,
never tested, and looks fine on desktop. Read it line by line rather than
skimming for the cache name. If you can, pull the plug and watch — offline
behaviour is cheap to test by hand and almost never tested any other way.

## B2a. If the app has no data layer at all

Only then reach for the bundled implementation — a greenfield app, or one that
currently keeps everything in memory and wants persistence:

- `assets/kotlin/Store.common.kt` → `commonMain`
- `assets/kotlin/Store.wasmJs.kt` → `wasmJsMain`
- `assets/kotlin/Store.jvm.kt` → `jvmMain` (set `{{APP_DIR}}`)
- `assets/kotlin/Store.memory.kt` → once per remaining target, to satisfy
  `expect`/`actual`. Say plainly that it does not survive a restart.

wasmJs needs `kotlinx-serialization-json` (boundary marshalling only) and
`kotlinx-coroutines`; JVM needs coroutines for `Dispatchers.IO`.

`assets/kotlin/data-poll-template.kt` shows the request shape: read the cached
entry, send its stored `Last-Modified` back as `If-Modified-Since`, store a new
copy only on `200`. Treat both as a **reference implementation** — worth reading
for the pattern even when the app keeps its own layer.

## B3. Surface offline state

This applies whatever the data layer is — it is the most common real defect, and
the one fix Phase B almost always ends up making. Wherever the app refreshes,
record the outcome instead of discarding it:

```kotlin
runCatching { /* the app's existing refresh call */ }
    .onSuccess { data = it; offline = false }
    .onFailure { offline = true }   // an ordinary outcome, not an exception
```

A bare `runCatching { ... }` that swallows the failure makes a permanently broken
endpoint look identical to a healthy app showing a cached copy — do this even if
the user chose "no indicator".

`runCatching` is deliberate, not stylistic: it catches `Throwable`, which is what
a wasmJs fetch failure actually is (row 5). Keep a comment saying so, or the next
person will "tidy" it into `try`/`catch (e: Exception)` and silently reintroduce
the bug.

Then add the indicator variant chosen in step 0 from
`assets/kotlin/offline-indicator.kt`, and delete the other variants.

Give the indicator a **label**, not just a coloured dot — including when the user
picked the minimal variant. An 8dp dot in the corner of an app bar is genuinely
missable; users report "there was no offline warning" when the dot was there the
whole time. The bundled variants take `hasCachedCopy` so they can say which of
the two offline situations this is ("showing cached copy" vs "no cached copy
yet"), which is the part the user actually needs.

## B4. Tell the user about server headers

The app cannot fix this; the deployment must. Serve everything with
`Cache-Control: no-cache` — **not** `no-store`. Both prevent stale versions, but
`no-store` discards validators, turning every zero-byte `304` into a full
download. Without any `Cache-Control`, heuristic freshness can answer the update
check from the HTTP cache and hide deploys for minutes.

```apache
# Always revalidate; never serve a stale version.
<IfModule mod_headers.c>
    Header set Cache-Control "no-cache"
</IfModule>
```

Report this as a required deployment step rather than silently assuming it — a
correct build on a badly configured host still updates late.

**The data host needs the same line, and it is easier to forget** because it is
often a different machine with its own `.htaccess`. It matters more there than
the arithmetic suggests: heuristic freshness is a fraction of the file's *age*,
so a rarely-changed file — a config or a minimum-version manifest last touched
three months ago — can be treated as fresh for days, and a change to it stays
invisible that long. One added line inside the existing headers block is the
whole fix.

While you are in that file, two CORS points, since data is often cross-origin:

- `Access-Control-Allow-Headers` must include `If-Modified-Since`; it is not
  CORS-safelisted, and without it the conditional GET fails preflight.
- `Access-Control-Max-Age` is worth setting (e.g. `86400`). Because that header
  is non-safelisted, *every* poll otherwise pays an `OPTIONS` round trip before
  the `GET` — browsers cache preflights only seconds by default.
- No `Access-Control-Expose-Headers` is needed for this pattern: `Last-Modified`
  is a safelisted *response* header, so the app can read it cross-origin.
  `ETag` is not — only relevant if you switch to `If-None-Match`.

---

# Verify

```sh
./gradlew :<webAppModule>:buildWasmAsPwa
```

The log must say `precache N URLs` with N > 0. `precache 0 URLs` means
`globPatterns` matched nothing — check `globDirectory`/dist path.

Then compile the shared UI for every target so the `expect`/`actual` sets are
complete:

```sh
./gradlew :<sharedUi>:compileKotlinWasmJs :<sharedUi>:compileKotlinJvm
```

## After deploying: check that every precached URL resolves

Do this against the live site, not the build output. **Workbox fails the entire
precache install if any single precached URL 404s** — one missing file does not
degrade gracefully, it stops the service worker working for every client at once,
and it presents as an inexplicable "updates stopped happening".

```sh
#!/bin/bash
set -euo pipefail
BASE="https://example.com"           # deployment root, no trailing slash
curl -sS "$BASE/serviceWorker.js" \
  | grep -oE '\{url:"[^"]+"' | sed 's/{url:"//;s/"//' \
  | while read -r u; do
      code=$(curl -sS -o /dev/null -w "%{http_code}" "$BASE/$u")
      [ "$code" = "200" ] || echo "MISSING $code $u"
    done
echo "done — anything printed above is a broken precache entry"
```

The way this usually breaks is someone tidying the deployed directory by hand.
Two files invite exactly that, so say so before they ask:

- **`*.js.map`** — safe to remove; `.map` is not in `globPatterns`, so it is not
  precached, and nothing fetches it unless DevTools is open. You lose readable
  production stack traces. Prefer switching source maps off in the webpack config
  over deleting build output.
- **`*.js.LICENSE.txt`** — do **not** hand-delete. It *is* precached, because
  `.txt` matches the glob patterns, so removing it from a live deployment breaks
  the install for everyone per the paragraph above. It also carries the
  third-party attributions webpack extracted out of the bundle (BSD/MIT notices
  that redistribution requires), which the banner at the top of the bundle points
  at. It costs about a kilobyte.

The rule underneath both: **never delete files from a deployed distribution.**
Change what the build emits and redeploy, so the precache manifest and the
directory always agree.

Two more cheap post-deploy checks worth doing every time:

- Ship a tiny `version.json` (name, version, build) and fetch it. It is the
  fastest way to catch the commonest deploy error of all — uploading the previous
  build directory.
- Confirm a config value actually reached the bundle by grepping the minified JS
  for it, remembering that minifiers write `300000` as `3e5`. Expect unrelated
  numeric constants to match too; presence of the right one is the signal, not
  absence of others.

Full runtime checklist (DevTools, deploy test, pull-the-plug) is in
`references/data-and-deployment.md`.

## If the build fails with `Cannot run program "npx"`

The plugin shells out to a bare `npx`, resolved against the **Gradle daemon's**
`PATH`. A daemon captures its environment once at spawn and keeps it for life, so
one born without Node on the `PATH` fails every time — and `PATH=... ./gradlew`
will not help, because that only affects the client.

```sh
./gradlew --stop   # then rebuild; the fresh daemon inherits a sane PATH
```

It looks intermittent because the task is skipped while `UP-TO-DATE`, so a bad
daemon stays invisible until a source change makes it run. This is not an "IDE vs
terminal" problem — IDEs normally inject a good environment into daemons they
spawn. It is one unlucky daemon.

# Gotchas worth remembering

- **`catch (e: Exception)` does not catch a failed fetch on Kotlin/Wasm.**
  `JsException` and `kotlin.Error` extend `Throwable` directly. Catch `Throwable`
  (rethrowing `CancellationException`), or use `runCatching`. Correct-looking code
  that passes every JVM test can leave the browser build permanently offline after
  the first network blip. See B2 row 5.
- **A client behind a cache-first old worker is unreachable from the new build.**
  It never loads your new `index.html`. The only channel is the old worker's own
  script URL — see A0 and `references/how-it-works.md`.
- **Never `startsWith` when deleting old caches.** A data cache often shares a
  prefix with the software cache (`MyApp-1.2.3` vs `MyAppData__`). Anchor on the
  version segment.
- **One 404 in the precache breaks the service worker for everyone.** Workbox
  installs the manifest atomically. Never hand-delete from a deployed
  distribution — `*.js.LICENSE.txt` is precached and is a favourite thing to
  "tidy away".
- **A minimum-version gate in the app is not made redundant by the service
  worker.** It is server-driven, it can withhold content rather than suggest, and
  on non-web targets it is the only update signal there is. See
  `references/notification-styles.md`.
- `js("…")` in Kotlin/Wasm must be the **sole expression of a top-level
  function**, never a member function — that is why both the wasmJs bridge and
  the wasmJs `Store` use private top-level helpers.
- `registerServiceWorker.js` must load **synchronously** in `<head>` (no
  `async`/`defer`) so `window.pwaStartupGate` exists before the wasm `main()`
  runs.
- Test over http(s), not `file://`, and keep DevTools' "Update on reload" and
  "Bypass for network" **off** — both mask real service-worker behavior.
- `If-Modified-Since` is not CORS-safelisted: cross-origin it triggers a preflight
  a static host will not answer. Serve data same-origin, or configure CORS.
- Cache Storage is evictable. A `Store` miss is normal, never an error.
- Cache-name collision is far narrower than the folklore suggests:
  `cleanupOutdatedCaches` deletes a cache only if its name contains **both**
  `-precache-` **and** the registration scope. Ordinary app cache names are safe
  whatever they start with — see `references/data-and-deployment.md`.
