# Data caching and deployment

Read this when applying Phase B (the app-owned data cache), when the app fetches
anything at runtime, or when updates are detected later than the poll interval
suggests they should be.

## Contents

- [The one rule: the SW owns software, the app owns data](#the-one-rule)
- [Why the service worker must not cache data](#why-the-service-worker-must-not-cache-data)
- [Why the Cache API and not localStorage](#why-the-cache-api-and-not-localstorage)
- [Two caches, one Cache Storage](#two-caches-one-cache-storage)
- [Offline behavior](#offline-behavior)
- [Server cache headers](#server-cache-headers)
- [Verification](#verification)

## The one rule

> The service worker owns the software cache. The app owns the data cache.
> Neither reaches into the other's territory.

**Software** is what the build produces — `*.wasm`, `*.js`, `index.html`, fonts.
Identical for every user until the next deploy. **Data** is whatever the app
fetches at runtime — API responses, documents. Changes independently of deploys,
often per-user.

On desktop and mobile this separation is obvious; the web blurs it because the
browser will happily cache both through the same machinery. Restoring it
deliberately is what makes a deploy able to swap the software while cached data
carries across untouched.

## Why the service worker must not cache data

A service worker intercepts **every** `fetch()` a controlled page makes,
regardless of destination. On wasmJs, Ktor's client engine ultimately calls the
browser's `fetch()`, so Ktor requests pass through the worker too. Ktor has no
idea it is there, and you cannot opt out from the Kotlin side — request
`Cache-Control` headers do not override a Workbox strategy; the strategy decides.

So a catch-all like

```js
runtimeCaching: [{ urlPattern: /.+/, handler: "StaleWhileRevalidate" }]
```

does not merely "cache dynamic assets" — it caches API responses, serves them
one request stale, and quietly accumulates them in Cache Storage.

**The fix is to have no `runtimeCaching` at all.** With no matching route
Workbox's fetch handler never calls `respondWith()`, so the browser handles data
requests as ordinary network fetches. The worker still sees each request (a
negligible routing check) but neither answers nor caches it.

That same catch-all is also why precaching matters for updates: with nothing
precached, the generated `serviceWorker.js` carries no revision manifest, its
bytes never change, the browser's update check sees "no change", and
`skipWaiting`/`clientsClaim` never fire.

## Why the Cache API and not localStorage

This matters only when choosing a mechanism for an app that has none. If the app
already persists data somewhere sensible, leave it alone — see the Phase B audit.

`localStorage` is perfectly adequate for small text payloads: app-owned, survives
updates, untouched by Workbox. Its limits are what rule it out at scale — roughly
5 MB of quota, strings only, and synchronous access that blocks the main thread.
Past that, IndexedDB or the Cache API is the answer.

The distinctive advantage of the Cache API is that it stores a **body plus its
headers**. The server's `Last-Modified` comes back out exactly as sent, so a
conditional GET needs no bookkeeping of your own — the revalidation metadata
*is* the cache entry. With a JSON blob in `localStorage` you must invent,
serialize and hand-maintain that same field.

Two consequences worth designing for:

- **Entries are keyed by request URL**, so app-owned data needs a synthetic key
  (`/app-data/<key>`). It is never fetched; it exists only as a key.
- **Cache Storage is evictable** under storage pressure. A miss must be an
  ordinary code path, never an error.

## Two caches, one Cache Storage

Workbox's precache and the app's data cache live in the same Cache Storage,
partitioned by name. Workbox's own precache is `workbox-precache-<scope>`; give
the data cache a name of its own (`app-data-v1`, `myvault__`, anything).

The rule people repeat — "never prefix it with `workbox`" — is folklore. It keeps
you safe, but for the wrong reason, and it flags names that are perfectly fine.
The real predicate lives in `cleanupOutdatedCaches`, which deletes a cache only
when **all three** hold:

```js
name.includes("-precache-") && name.includes(self.registration.scope) && name !== currentPrecacheName
```

So a cache is at risk only if its name contains **both** the literal `-precache-`
**and** the full registration scope (`https://yourhost.com/`). That is nearly
impossible to hit by accident — you would have to construct it deliberately. Any
ordinary app cache name is safe, whatever it starts with.

Verify rather than trust this text: the predicate is plainly readable in the
generated `workbox-*.js` next to your `serviceWorker.js`, and Workbox could
change it.

## Offline behavior

Pulling the network cable is the test that proves the separation. Three
*independent* mechanisms make it work, and they fail separately:

1. **The app keeps launching** — the Workbox precache serves the software from
   Cache Storage; no network involved. The only one you get for free.
2. **Cached data keeps rendering** — `Store.get()` treats the entry as
   authoritative and never consults the network to decide whether to return it.
   This requires the refresh *failure path* to hand the cached copy to the model.
   Having it in the cache is not enough; an app that only sets `offline = true`
   renders its empty state on top of a full cache.
3. **Polling recovers unaided** — the failed request is caught inside the polling
   loop, so the next tick retries. The loop *is* the recovery; no connectivity
   listener, no backoff. This requires the loop to survive the failure.

Point 3 has a trap: swallowing the error makes a permanently broken endpoint
indistinguishable from a healthy app showing a cached copy. Record the outcome
(`onSuccess`/`onFailure`) rather than discarding it, even if nothing surfaces it
yet.

Both 2 and 3 have a sharper trap underneath, specific to this target: **on
Kotlin/Wasm a failed fetch is not an `Exception`.** `kotlin.js.JsException`
extends `Throwable` directly, and Ktor's JS engine surfaces fetch failures as
`kotlin.Error`. A `catch (e: Exception)` therefore does not catch an offline
browser at all — the failure escapes the retry, escapes the caller, and
terminates the polling coroutine. The symptom triple is distinctive: no offline
indicator ever appears, reconnecting changes nothing, and only a reload fixes it.
The same code is correct on JVM and Android, where Ktor throws `IOException`, so
it survives every non-browser test. Catch `Throwable` and rethrow
`CancellationException`; `runCatching` already does the right thing.

The same applies on the JS side: `registration.update()` **rejects** whenever the
app is offline. Unhandled, that is an unhandled promise rejection on every
interval, which buries genuine service-worker problems in console noise. The
bundled `registerServiceWorker.js` already wraps it in `checkForUpdate()`.

## Server cache headers

Polling is worthless if the browser answers the poll from its own HTTP cache —
the default outcome on a plain static host.

`registration.update()` re-fetches `serviceWorker.js` through the ordinary HTTP
cache. With `Last-Modified`/`ETag` but **no `Cache-Control`** (stock Apache or
nginx), the browser applies *heuristic freshness* — roughly 10% of the
resource's age — and may answer the check without touching the network. Updates
then land late by an interval that grows with time since the last deploy, capped
at 24h by the service-worker spec.

Serve everything with:

```apache
# Always revalidate; never serve a stale version.
<IfModule mod_headers.c>
    Header set Cache-Control "no-cache"
</IfModule>
```

**Prefer `no-cache` over `no-store`.** Both are correct — neither ever serves a
stale version — but `no-store` forbids storing the response, so the browser keeps
no validators and every check becomes a full download instead of a zero-byte
`304`. It pays bandwidth for a guarantee `no-cache` already gives ("may store,
must revalidate before reuse"). `must-revalidate` is redundant alongside
`no-cache`; `Pragma`/`Expires` are HTTP/1.0 fallbacks. `no-store` earns its keep
only for keeping sensitive responses off disk.

Apply the same header on the **data** host, not just the app host — it is a
separate `.htaccess` on a separate machine and is the one people forget. The
exposure there is larger, not smaller: heuristic freshness is a fraction of the
file's *age*, so a manifest last modified three months ago can be considered
fresh for over a week, and a change to it goes unseen for that long. If data is
cross-origin, the same block also needs `Access-Control-Allow-Headers` to include
`If-Modified-Since` (not safelisted, so the conditional GET fails preflight
without it) and benefits from `Access-Control-Max-Age: 86400`, which turns one
preflight per poll into one per day. `Access-Control-Expose-Headers` is not
needed: `Last-Modified` is a safelisted response header, `ETag` is not.

Steady-state cost with `no-cache`: both the software check and a conditional data
request return `304` with a **zero-byte body**; only headers cross the wire, and
HTTP/2 compresses those. Polling is cheap in bytes — it is request *volume* to be
deliberate about. A long `max-age` on big wasm assets is not worth it: once the
worker is installed they come from Cache Storage and the HTTP cache is bypassed.

## Verification

- **Build log**: `The service worker will precache N URLs` with N > 0. The
  generated `serviceWorker.js` contains `precacheAndRoute([...])` and **no**
  `registerRoute` / `StaleWhileRevalidate`.
- **Precache integrity, against the live site**: every URL in the deployed
  manifest must return 200. Workbox fails the *whole* install if one entry 404s,
  so a single missing file stops updates for every client rather than degrading.
  Never delete files from a deployed distribution to tidy it — change the build
  and redeploy. See the script in `SKILL.md` under "After deploying".
- **Build identity**: fetch a small `version.json` and confirm it names the build
  you just shipped, before concluding anything else is wrong.
- **DevTools → Network**: software requests show "(ServiceWorker)" in the Size
  column; data requests do not. Watch `serviceWorker.js` across two poll
  intervals — you want `304` each time, never "(disk cache)".
- **DevTools → Application → Cache Storage**: a `workbox-precache-*` cache with
  the software, and `app-data-v1` with data under `/app-data/…` keys. Neither
  should contain the other's content.
- **Deploy test**: build A open, deploy B; within one poll interval the update
  is signalled, and `app-data-v1` survives the update untouched.
- **Pull the plug** — the test that finds the defects nothing else finds. Do all
  four parts, in order; each one catches a different bug:
  1. With the app **running**, disconnect. The offline indicator must appear
     within about one poll interval. (If it never does, suspect
     `catch (e: Exception)`.)
  2. Reconnect, still running. The indicator must clear by itself. (If only a
     reload fixes it, the polling loop died on the first failure.)
  3. **Relaunch** while still disconnected. The app must start *and show the
     cached data*. (If it shows an empty state, the failure path is not
     publishing the cache.)
  4. Reconnect and confirm fresh data arrives without a reload.
