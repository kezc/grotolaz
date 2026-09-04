# How the update flow works (and how to verify it)

Read this when you need to explain, debug, or tune the behavior. It is not
required to apply the skill.

## The behavior model

| Situation | What the user sees |
|---|---|
| App up to date on launch | Brief splash → app starts. |
| New version on server at launch / tab reload | Splash stays through install + reload → **new** version starts. The old version never appears. |
| New version appears **while the app is running** | Nothing interrupts. A pulsating "New Software" button appears top-right. |
| User clicks the button | Dialog with **Cancel** (keep working, button stays) / **Update** (reload into the new version). |
| After an update is applied | Button gone (no pending update). |

## The three moving parts

1. **Workbox config** (`workbox-config-for-wasm.js`) precaches all assets with a
   revision manifest, so `serviceWorker.js` bytes change every deploy → the
   browser reliably detects a new worker. `skipWaiting` + `clientsClaim` make the
   new worker activate and claim the page, which fires `controllerchange`.

2. **`registerServiceWorker.js`** turns that single `controllerchange` signal into
   the right behavior by tracking *when* the update was found:
   - Intent is captured at `updatefound` (install start), not at activation, so
     the decision is independent of how long precaching the (large) wasm takes.
   - An update whose install begins within the first ~3s (the initial-load
     window) → reload immediately (but see the startup gate — the reload happens
     under the splash, so nothing old is shown).
   - An update whose install begins later (from the 10s periodic check or the
     visibility check) → `deferActivation`: set `window.pwaUpdateAvailable = true`
     instead of reloading.
   - `window.pwaStartupGate` is a Promise resolved only when the running code is
     confirmed current; the wasm `main()` awaits it before mounting Compose.

3. **`PwaUpdate` (Kotlin) + the UI**: `rememberUpdateAvailable()` polls the JS
   flag once a second; the button/dialog call `PwaUpdate.applyUpdate()` which
   reloads into the already-active new worker.

## Why capture intent at `updatefound`, not `controllerchange`

`controllerchange` fires only after the new worker finishes installing (which
includes downloading the whole precache — potentially many seconds on a slow
link). By then a time-based "are we still in initial load?" check is unreliable.
Recording `willReload` / `deferActivation` the moment the install *starts* keeps
the decision correct regardless of download time.

## Why a startup gate at all

Without it, the wasm app mounts immediately, the user starts interacting, and a
few seconds later the periodic/initial update check reloads the page — losing
their work-in-progress. Gating startup on `pwaStartupGate` means: on launch we
first confirm we are current (or reload to become current) and only then show UI.

## Migrating from an older service worker

Everything above assumes the browser will load your new `index.html`. When a
previous build shipped a **cache-first** hand-written worker, that assumption
fails for exactly the clients you most need to reach.

The trap, in order:

1. The old worker precached the app shell and serves it cache-first.
2. So a returning browser gets the **old** `index.html` out of Cache Storage.
3. That shell loads the **old** entry script — not `registerServiceWorker.js`.
4. Therefore none of the new code runs. Not the registration, not the startup
   gate, not `retireForeignWorkers()`. The client is sealed in.

Nothing you add to the new build changes this. The client is not choosing the old
version; it never sees that there is a new one.

**The one remaining channel** is the old worker's own update check: the browser
periodically re-fetches the script URL the old worker was registered from. That
request is not answered by the old worker's cache — service-worker scripts are
fetched through the ordinary HTTP path — so whatever you serve at that URL will
be seen. Serving a valid script there replaces the old worker with yours.

`assets/web/legacy-sw-tombstone.js` is that script. It:

- `skipWaiting()`s so it does not wait for the old clients to close;
- deletes the retired precaches (freeing what can be many megabytes);
- `clients.claim()`s, then **unregisters itself**;
- `client.navigate(client.url)`s every open tab, which — with no controlling
  worker left — fetches the real `index.html` from the network.

It deliberately has **no `fetch` handler**: a worker without one is bypassed
entirely by the browser, so requests go to the network rather than the stale
precache even before the reload lands.

### Why not just delete the old file?

A 404 also clears the registration in Chrome eventually, via a failed update
check. But it is browser-dependent, undated, and it leaves the old precache on
the device forever. The tombstone is the deterministic version, and it is the
only one that reclaims the storage.

### Two things to get right

**The cache-deletion pattern.** Write it as a regex anchored on something only
the retired names carry (the version segment). The obvious `startsWith(prefix)`
is how you delete your users' data: a data cache named alongside the software
cache usually shares its prefix — `MyApp-1.2.3` and `MyAppData__` both start with
`MyApp`. Read the real names out of DevTools → Application → Cache Storage first.

**The removal date.** The tombstone is a deployment obligation with a lifetime,
not a file you land and forget. Pick the date from how long a user might
plausibly stay away — for an app tied to a yearly event, that is more than a
year — and write the reasoning into the file header. Delete it early and you
strand precisely the users it existed for: their browser goes back to failing its
update check against a 404 and keeps serving the old shell in the meantime. There
is no telemetry for "the last old client came back", so when in doubt, keep it.

`retireForeignWorkers()` in `registerServiceWorker.js` covers the easier half of
the problem — a stale registration on a client that *did* load the new page —
and is scoped to the page's own directory so it cannot unregister a sibling app's
worker on the same origin.

## Verifying a build

The generated
`<webAppModule>/build/dist/wasmJs/productionExecutable/serviceWorker.js` must
contain a precache manifest:

```js
precacheAndRoute([
  { url: "index.html", revision: "…" },
  …
]);
```

and `skipWaiting()` / `clientsClaim()`. If the build says `precache 0 URLs` or
there is no `precacheAndRoute`, `globPatterns` matched nothing — the fix is not
active and updates will silently never install.

## Testing the real behavior

- Serve over http(s), **not** `file://`.
- In DevTools, leave **"Update on reload"** and **"Bypass for network"** *off* —
  they mask real service-worker behavior.
- Launch path: load build A, deploy build B, reload once → you should see the
  splash persist until B is running (A never flashes).
- While-running path: load build A, keep the tab open, deploy build B → within
  ~10s the pulsating "New Software" button appears instead of an auto-reload.

## Build gotcha: `Cannot run program "npx"`

The ComposePWA plugin shells out to a bare `npx` (Workbox CLI), resolved against
the **Gradle daemon's** `PATH` — not your shell's.

A daemon captures its environment once, when it is spawned, and keeps it for its
whole life. If it was born without Node on the `PATH`, every `buildWasmAsPwa` it
serves fails with `error=2 (No such file or directory)`, and prefixing
`PATH=... ./gradlew` will not help — that only affects the client, not the
running daemon.

The cure is to get rid of that daemon:

```sh
./gradlew --stop   # then rebuild; the fresh daemon inherits a sane PATH
```

It looks maddeningly intermittent for two reasons: the task is skipped while
`UP-TO-DATE`, so a bad daemon stays invisible until a source change makes it
actually run — and whether any given build succeeds depends on which daemon
happened to serve it. IDEs normally inject the imported login-shell environment
into the daemons they spawn, so this is not an "IDE vs terminal" problem; it is
one unlucky daemon.

To confirm rather than guess:

```sh
for pid in $(pgrep -f GradleDaemon); do
  ps eww -o command= -p $pid | tr ' ' '\n' | grep ^PATH= | head -1
done
```

Prerequisite either way: Node.js must be installed and reachable.

## Customization knobs

- **Update check frequency**: `UPDATE_INTERVAL_MS` in `registerServiceWorker.js`
  (10s is a testing value; use e.g. `30 * 60 * 1000` in production).
- **Initial-load window**: `INITIAL_LOAD_WINDOW_MS` (default 3s). Rarely needs
  changing.
- **Startup-gate safety timeout**: the `setTimeout(allowStart, 20 * 1000)` — the
  longest the splash can ever stay up if a check hangs.
- **Pulse intensity/speed**: `targetValue` (bigger = larger pulse) and
  `durationMillis` (smaller = faster) in the update UI snippet.
- **Wording**: the button label and dialog strings in the UI snippet.
