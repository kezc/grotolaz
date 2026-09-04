(() => {
  // Globals the wasm app (PwaUpdate.wasmJs.kt / the wasmJs main) reads/calls.
  // Always defined — even without service-worker support — so the app can call
  // them safely.
  //   window.pwaUpdateAvailable : true once a new version was found *while the
  //                               app was running* (→ the "New Software" button).
  //   window.pwaApplyUpdate()   : activate the pending update and restart.
  //   window.pwaStartupGate     : a Promise that resolves only when it is safe to
  //                               start the app, i.e. once we know the running
  //                               code is the latest version (no reload pending).
  //                               The Compose app awaits this before mounting, so
  //                               the user never sees the old version flash up and
  //                               get replaced a moment later.
  window.pwaUpdateAvailable = false;
  window.pwaApplyUpdate = () => window.location.reload();

  let resolveStartupGate;
  window.pwaStartupGate = new Promise((resolve) => { resolveStartupGate = resolve; });
  let started = false;
  const allowStart = () => {
    if (started) return;
    started = true;
    resolveStartupGate();
  };

  // No service worker → nothing to check, start straight away.
  if (!("serviceWorker" in navigator)) {
    allowStart();
    return;
  }

  // --- Safety belt: retire any service worker that is not ours ---------------
  // Only needed when this URL has ever hosted a different service worker. It
  // complements the tombstone (assets/web/legacy-sw-tombstone.js): the tombstone
  // reaches clients stuck behind an old cache-first worker, which never load this
  // script at all; this catches the easier case where a stale registration exists
  // but the current index.html did load — after a hard reload, or a leftover from
  // an earlier experiment at this URL.
  //
  // Scope discipline matters: a host often serves more than one app, and
  // getRegistrations() returns registrations across the whole origin. Only ever
  // touch registrations at or below this page's own directory, never a parent
  // scope — unregistering a sibling app's worker is a very unpleasant bug to
  // track down from the other side.
  const retireForeignWorkers = async () => {
    const ourScript = new URL("serviceWorker.js", document.baseURI).href;
    const ourDirectory = new URL("./", document.baseURI).href;
    const registrations = await navigator.serviceWorker.getRegistrations();
    for (const registration of registrations) {
      if (!registration.scope.startsWith(ourDirectory)) continue; // not ours to touch
      const worker = registration.active || registration.waiting || registration.installing;
      if (!worker || worker.scriptURL === ourScript) continue;
      console.log(`Unregistering stale service worker: ${worker.scriptURL}`);
      await registration.unregister();
    }
  };

  window.addEventListener("load", () => {
    // Guard against reload loops from controllerchange.
    let refreshing = false;
    // During initial load / a manual reload we still apply updates immediately.
    let initialLoad = true;
    // Set when an update is found *while running*: its activation must NOT reload
    // the page automatically — we show the button and wait for the user instead.
    let deferActivation = false;
    // Set when an update found during initial load is going to reload the page.
    // While true we keep the splash up and do NOT start the (old) app.
    let willReload = false;

    // Absolute safety net: never leave the splash up forever if something goes
    // wrong with the checks below. In the normal update path the reload happens
    // well before this fires.
    setTimeout(allowStart, 20 * 1000);

    navigator.serviceWorker.addEventListener("controllerchange", () => {
      if (refreshing) return;

      if (deferActivation) {
        // A new worker took control while the app was running. Keep running the
        // current version; surface the "New Software" button and let the user
        // decide when to restart (see window.pwaApplyUpdate below).
        window.pwaUpdateAvailable = true;
        return;
      }

      // Initial load, manual reload, or first install → adopt the new version now.
      // The app hasn't started (still gated by the splash), so this reload swaps
      // to the new version before anything is shown.
      refreshing = true;
      window.location.reload();
    });

    // Retire foreign workers first, then register ours — doing both at once
    // would race. Cleanup failure must never block startup, hence the catch.
    // Greenfield projects can collapse this to a plain
    // navigator.serviceWorker.register("serviceWorker.js").then(...).
    retireForeignWorkers()
        .catch((error) => console.log(`Stale worker cleanup skipped: ${error}`))
        .then(() => navigator.serviceWorker.register("serviceWorker.js")) // must match workbox swDest filename
        .then((registration) => {
          console.log("Service Worker registered!");
          console.log(`scope: ${registration.scope}`);

          // Apply the pending update. The new worker already controls the page
          // (skipWaiting + clientsClaim in workbox-config), so precached new
          // assets are served on the next load — a plain reload is enough.
          window.pwaApplyUpdate = () => {
            window.pwaUpdateAvailable = false;
            window.location.reload();
          };

          // Decide immediate-vs-deferred at the moment a new worker starts
          // installing. An install that *begins* after the initial-load window
          // (i.e. triggered by the periodic/visibility checks below) is a
          // "while running" update and must be deferred. Capturing intent here
          // rather than at activation keeps the decision independent of how long
          // precaching the new assets takes.
          registration.addEventListener("updatefound", () => {
            const installing = registration.installing;
            if (!installing) return;
            if (!initialLoad && navigator.serviceWorker.controller) {
              deferActivation = true;
            } else {
              // A newer version found during initial load → we will reload into
              // it. Keep the splash up instead of starting the old version.
              willReload = true;
            }
          });

          // --- Check for updates on page load, and gate startup on the result ---
          // Forces the browser to fetch serviceWorker.js immediately. If it
          // differs, a new worker installs and the controllerchange handler above
          // reloads into it (initialLoad is still true) — we keep the splash. If
          // it is up to date, we start the app.
          registration.update()
              .then(() => {
                if (willReload || registration.installing || registration.waiting) {
                  // An update is on its way in; the reload will start the new
                  // version. Keep the splash up until then.
                  return;
                }
                allowStart(); // already on the latest version
              })
              .catch(() => allowStart());

          // Close the initial-load window. The load-time check's updatefound
          // fires from a tiny serviceWorker.js fetch, well within this delay;
          // the large precache download happens afterwards and does not affect
          // the decision, which was already captured in updatefound above.
          const INITIAL_LOAD_WINDOW_MS = 3 * 1000;
          setTimeout(() => { initialLoad = false; }, INITIAL_LOAD_WINDOW_MS);

          // --- Keep a running app up to date ---
          // Re-check for a new serviceWorker.js while the tab stays open. From
          // now on any update found is treated as "while running": it activates
          // in the background but the page is NOT reloaded automatically — the
          // app shows the "New Software" button instead.
          // NOTE: 10s is a short TESTING interval; raise it for production
          // (e.g. 30 * 60 * 1000 for 30 minutes).
          const UPDATE_INTERVAL_MS = 10 * 1000;
          // registration.update() rejects whenever the app is offline — an expected
          // outcome for a periodic check, not an error. The app keeps running from
          // the precache and the next tick simply retries. Swallow it deliberately:
          // without this you get an unhandled promise rejection on every interval
          // while offline, which buries any genuine service-worker problem in noise.
          const checkForUpdate = () => registration.update().catch(() => {});

          setInterval(checkForUpdate, UPDATE_INTERVAL_MS);

          // Also check as soon as the tab regains focus.
          document.addEventListener("visibilitychange", () => {
            if (document.visibilityState === "visible") {
              checkForUpdate();
            }
          });
        })
        .catch((error) => {
          console.log("Service Worker register failed");
          console.log(`error: ${error}`);
          allowStart(); // don't block the app on a registration failure
        });
  });
})();
