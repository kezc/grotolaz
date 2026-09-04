module.exports = {
  globDirectory: "build/dist/wasmJs/productionExecutable/",

  // Precache the actual app assets. This is the key line for correct updates:
  // it makes Workbox embed a per-file revision manifest into serviceWorker.js,
  // so the SW's bytes change on every deploy. The browser then detects the new
  // worker and (with skipWaiting + clientsClaim below) activates it and takes
  // control on the next launch instead of serving a stale copy first.
  //
  // If globPatterns is empty the SW is byte-identical every build, the browser
  // sees "no change", and updates silently never install — a classic footgun.
  // These patterns precache whatever the build happens to have put in the dist,
  // which is also a publishing decision: anything matched is served to every
  // user and cached on their device. Check the dist for build-time-only files
  // that were never meant to ship (config files with endpoints, source maps,
  // internal notes) — the fix is to stop emitting them, not to exclude them here.
  globPatterns: [
    "**/*.{js,wasm,html,json,ico,png,svg,css,woff,woff2,ttf,txt}",
  ],

  // Only when migrating from an older hand-written worker: its tombstone must be
  // fetched from the NETWORK by old clients as their update check, so it must not
  // be precached or revisioned by this worker. Use the old worker's filename.
  // globIgnores: ["sw.js"],

  // wasm bundles are large; raise this so the .wasm actually gets precached.
  maximumFileSizeToCacheInBytes: 10 * 1024 * 1024,

  // NO runtimeCaching on purpose. The service worker caches ONLY the static
  // software (the precache above). Everything else — notably your app's own
  // data/API requests — falls straight through to the network, leaving the app
  // the sole owner of its data caching. A catch-all StaleWhileRevalidate route
  // here would wrongly cache dynamic data responses.

  swDest: "build/dist/wasmJs/productionExecutable/serviceWorker.js",

  // Instant activation of the new worker + controllerchange (which the client in
  // registerServiceWorker.js turns into a gated reload or the "New Software"
  // button, depending on whether the app was already running).
  skipWaiting: true,
  clientsClaim: true,

  // Drop precaches from previous versions once the new SW activates.
  cleanupOutdatedCaches: true,
};
