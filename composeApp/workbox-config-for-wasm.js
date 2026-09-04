module.exports = {
  globDirectory: "build/dist/wasmJs/productionExecutable/",

  // Precache the actual app assets. This is the key line for correct updates:
  // it makes Workbox embed a per-file revision manifest into serviceWorker.js,
  // so the SW's bytes change on every deploy. The browser then detects the new
  // worker and (with skipWaiting + clientsClaim below) activates it and takes
  // control on the next launch instead of serving a stale copy first.
  globPatterns: [
    "**/*.{js,wasm,html,json,ico,png,svg,webp,css,woff,woff2,ttf,txt}",
  ],

  // wasm bundles are large; raise this so the .wasm and webp images actually get precached.
  maximumFileSizeToCacheInBytes: 20 * 1024 * 1024,

  // NO runtimeCaching on purpose. The service worker caches ONLY the static
  // software (the precache above). Everything else falls straight through to the network.
  swDest: "build/dist/wasmJs/productionExecutable/serviceWorker.js",

  // Instant activation of the new worker + controllerchange (which the client in
  // registerServiceWorker.js turns into a gated reload or the "New Software"
  // button, depending on whether the app was already running).
  skipWaiting: true,
  clientsClaim: true,

  // Drop precaches from previous versions once the new SW activates.
  cleanupOutdatedCaches: true,
};
