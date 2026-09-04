// =============================================================================
//  TOMBSTONE for a retired hand-written service worker.
//  Not a service worker for this app — this file exists only to switch the OLD
//  one off. Do not extend it; the real worker is the Workbox-generated
//  serviceWorker.js.
//
//  Copy this to the OLD worker's filename and URL — whatever the previous build
//  passed to register() (commonly `sw.js`). It must sit at exactly that path, or
//  the clients you are trying to reach will never fetch it.
// =============================================================================
//
//  WHY THIS FILE EXISTS
//
//  Before the Workbox rework this app registered a hand-written worker that
//  precached the app shell and served it CACHE-FIRST. A browser that visited the
//  old build therefore keeps serving the OLD index.html — which loads the old
//  entry script, not registerServiceWorker.js. The new registration code never
//  gets a chance to run, so no amount of cleanup logic in the new build can
//  reach those clients. They are stuck, and they stay stuck.
//
//  What those clients DO still do is periodically re-fetch this URL to check the
//  old worker for updates. That is the only channel back to them, so we use it:
//  serving a valid script here replaces the old worker with this one, which
//  immediately unregisters itself and reloads the page. The next load has no
//  controlling worker, fetches the real index.html from the network, and the
//  Workbox worker takes over from there.
//
//  Deleting the old file instead (404) also eventually clears the registration
//  in Chrome, but only via a failed update check, and it leaves the old precache
//  behind forever. This is the deterministic version.
//
//  WHEN CAN IT BE REMOVED?
//
//      Added {{ADDED_DATE}}. DO NOT REMOVE BEFORE {{REMOVE_AFTER}}.
//
//  Pick that date from how long a user might plausibly stay away between visits,
//  and write down the reasoning. A client that does not come back before you
//  delete the file is stranded permanently: its browser goes back to failing its
//  update check against a 404 and keeps serving the old cached shell meanwhile.
//  The file costs a few kilobytes and is excluded from the precache, so leaving
//  it in place longer than necessary costs nothing. There is no telemetry for
//  this — if in doubt, keep it.
// =============================================================================

// Delete the retired precaches, so the old (often multi-megabyte) copy of the
// software does not sit in Cache Storage forever.
//
// CAREFUL: match the old precache names EXACTLY. A startsWith() check is the
// obvious way to write this and it is how you delete your users' data — app data
// caches very often share a prefix with the software cache they were named
// alongside ("MyApp-1.2.3" vs "MyAppData__"). Anchor the pattern on something
// only the retired precache names have, such as the version segment, and leave
// everything else — the Workbox precache, compose_web_resources_cache, the app's
// own data cache — strictly alone.
//
// Set this to a pattern matching the OLD names; verify it in DevTools →
// Application → Cache Storage against the real list before deploying.
const RETIRED_PRECACHE = {{RETIRED_PRECACHE_PATTERN}};

self.addEventListener("install", () => {
    // Do not wait for the old worker's clients to go away.
    self.skipWaiting();
});

self.addEventListener("activate", (event) => {
    event.waitUntil((async () => {
        const names = await caches.keys();
        await Promise.all(
            names.filter((name) => RETIRED_PRECACHE.test(name)).map((name) => caches.delete(name))
        );

        // Take control of the pages still running the old shell, so that
        // navigate() below is allowed to act on them.
        await self.clients.claim();

        // Remove this registration. Clients stay controlled until they navigate,
        // which is exactly what happens next.
        await self.registration.unregister();

        const clients = await self.clients.matchAll({ type: "window" });
        for (const client of clients) {
            // Reload each open tab. With no controlling worker left, this fetches
            // the current index.html from the network.
            client.navigate(client.url);
        }
    })());
});

// Deliberately NO fetch handler. Without one the browser bypasses this worker
// entirely, so even before the reload above, requests go to the network instead
// of the stale precache.
