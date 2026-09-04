# Update-notification styles

Which scheme is right depends entirely on the app, which is why the skill asks
rather than assumes. The deciding question is: **can the user lose work if the
page reloads underneath them?**

| Style | Reloads without asking | Good for | Cost |
|---|---|---|---|
| A. Pulsating button + dialog | no | apps holding user state; long-lived tabs | most visually insistent |
| B. Silent auto-reload | **yes** | stateless/read-only apps, kiosks, dashboards | destroys unsaved state |
| C. Banner / snackbar with action | no | most apps; a calmer default | easier to dismiss and forget |
| D. Quiet indicator | no | expert tools, apps updated rarely | easiest to miss entirely |

All four share the same machinery — `registerServiceWorker.js` plus the
`PwaUpdate` bridge. Only two things vary: whether `deferActivation` is honored,
and what the UI does with `rememberUpdateAvailable()`.

---

## A. Pulsating button + dialog (validated default)

Use `assets/kotlin/app-update-ui.kt` verbatim. A pulsating top-right **New
Software** button appears; clicking opens a Cancel/Update dialog; Update calls
`PwaUpdate.applyUpdate()`, which reloads into the new version.

Tunables: `targetValue` (pulse amplitude, 1.18 works for a button), and
`durationMillis` (600ms; smaller is faster and more urgent).

## B. Silent auto-reload

No UI at all. Change `registerServiceWorker.js` so an update found while running
reloads immediately instead of deferring: in the `updatefound` handler, drop the
`deferActivation = true` branch so every update takes the `controllerchange` →
`window.location.reload()` path.

**Confirm with the user that the app truly holds no unsaved state.** This is the
one option that can destroy work, and it does so silently — the failure mode is
invisible in testing and obvious in production. If in doubt, choose C.

## C. Banner / snackbar with action

Keep the deferral machinery unchanged; replace the affordance UI. Simplest
version, dropped in the same place as the pulsating button:

```kotlin
@Composable
fun BoxScope.UpdateBanner() {
    if (!rememberUpdateAvailable()) return
    Surface(
        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("A new version is available.", modifier = Modifier.weight(1f))
            TextButton(onClick = { PwaUpdate.applyUpdate() }) { Text("Reload") }
        }
    }
}
```

If the app already has a `Scaffold` with a `SnackbarHost`, prefer showing a
snackbar with a "Reload" action instead — it will match the app's existing
language better than a hand-rolled bar.

## D. Quiet indicator

Same machinery, minimal signal — a small dot, a menu badge, or a line in an
About/status area. No animation, no dialog; the user reloads when they choose,
either via a normal browser reload or a control that calls
`PwaUpdate.applyUpdate()`.

```kotlin
if (rememberUpdateAvailable()) {
    Text("update pending", style = MaterialTheme.typography.labelSmall)
}
```

Worth saying to the user when they pick this: because nothing draws the eye, a
long-lived tab may run an old version for days. That is a legitimate choice, but
it should be a deliberate one.

---

## The other kind of update prompt: a server-driven minimum version

All four styles above are **advisory** — they report that something newer exists,
and the user can decline forever. Some apps also carry a second, unrelated
mechanism: a minimum version published in the app's own data, compared against
the build's version at runtime, which *withdraws* an old client rather than
nudging it. If the app you are converting has one, do not remove it as redundant
with the service worker. They answer different questions:

| | Service worker | Minimum-version gate |
|---|---|---|
| Question | is there a newer build? | may this build still be trusted with today's data? |
| Driven by | the deploy | a value on the server, no rebuild needed |
| User can decline | yes, indefinitely | no — the app withholds content |
| Works on non-web targets | no | yes |

That last row is usually decisive. On desktop/Android/iOS `PwaUpdate` is a no-op,
so the gate may be the *only* update signal those users ever get. It is also the
only mechanism that can express "this old client can no longer read the current
data format", which is exactly what you want the day the schema moves.

Two things to fix when both exist in the same app:

- **Do not show both at once.** Suppress the advisory affordance while the gate is
  active, or the user is offered an optional-looking version of a decision that
  is not optional.
- **Make the gate's advice platform-correct.** Its wording usually predates the
  PWA and says something like "close the window and start the app again". On the
  web the app can now do better than instructing — offer a button that calls
  `PwaUpdate.applyUpdate()`. On desktop that same advice is simply wrong: a
  restart returns the identical build, because a new version means a new
  installer. Add a capability flag to the `PwaUpdate` bridge rather than testing
  the platform:

  ```kotlin
  /** `true` where applyUpdate() can actually fetch a newer version. */
  val canUpdateInPlace: Boolean   // wasmJs: true, jvm/other: false
  ```

## Wording

Whatever the style, prefer wording that names the consequence over wording that
names the mechanism. "A new version is ready — reload to use it" tells the user
what happens; "Service worker update available" does not. Match the app's
existing voice and localization approach: if it already uses
`stringResource(...)`, put these strings in resources rather than leaving the
literals the assets ship with.
