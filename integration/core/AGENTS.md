# Glympse Core SDK: Android integration guide for coding agents

Use this when adding the Glympse Core SDK to an Android app (Kotlin or Java). The SDK shares a user's location through
time-limited "Glympses" (tickets) delivered by invites, and views Glympses received from others. There is no login: an
anonymous Glympse user is created the first time the SDK starts and is persisted on the device. The API lives in
`com.glympse.android.api.*` (`GGlympse`, `GlympseFactory`, `GC` constants, `GE` events, models) and
`com.glympse.android.core.*` (`GArray`, `GPrimitive`, `CoreFactory`). Full API outline: `llms-full.txt`.
This guide covers the core platform. Groups, cards, triggers, places, chat, pickups and the map/controls artifacts exist
but are not covered. Code below is Kotlin; every name is identical in Java.

## 1. Install (Maven)

Add the repo to `settings.gradle.kts` (`dependencyResolutionManagement { repositories { ... } }`):
```kotlin
maven { setUrl("https://raw.github.com/Glympse/glympse-android-sdk-release/master") }
```
`app/build.gradle.kts`:
```kotlin
implementation("com.glympse:glympseapi:<version>")        // core platform
implementation("com.glympse:glympseapi-push:<version>")   // optional: FCM push (also add firebase-messaging)
```
Use the latest version: the `<release>` element of
`https://raw.github.com/Glympse/glympse-android-sdk-release/master/com/glympse/glympseapi/maven-metadata.xml`.
You also need a Glympse **API key** (request one from Glympse). Keep it out of source control (build config or secrets).
Optionally add `com.google.android.gms:play-services-location`: the SDK reaches Fused Location reflectively and falls
back to the platform location manager when it is absent.

## 2. Requirements and permissions

- SDK AAR minSdk is 19. Compile against a current API level.
- The AAR manifest merges `INTERNET`, `WAKE_LOCK`, `ACCESS_COARSE/FINE_LOCATION`, `FOREGROUND_SERVICE`,
  `FOREGROUND_SERVICE_LOCATION`, the `GlympseService` (location foreground service) and receivers.
- Optional additions: `ACCESS_NETWORK_STATE` (recommended), `POST_NOTIFICATIONS` (API 33+, the SDK shows an ongoing
  notification while a Glympse is active), `SEND_SMS` (device-sent SMS invites), `READ_CONTACTS` (imports the user's
  nickname and avatar, and must be granted BEFORE the first `start()` or the new account cannot read it).
- The SDK never prompts for runtime permissions. Request location (COARSE+FINE together) before the user sends a
  Glympse. When it is missing the SDK re-checks every 15 seconds, so granting it later works without a restart.
  `locationManager.locationPermission` and `locationState` (`CoreConstants.LOCATION_*`) report the status.
- `glympse.verify()` only checks the `INTERNET` permission. Starting with other permissions missing does not throw.
- Sharing text in the ongoing notification: `config.setActiveSharingNotificationMessage(String)`.

## 3. Start-up: exact call order

Create ONE platform per process on the main thread (hold it in an `Application`-scoped singleton).
```kotlin
val g = GlympseFactory.createGlympse(applicationContext, "api.glympse.com", API_KEY) // 1. create
g.addListener(listener)                       // 2. BEFORE start: PLATFORM_STARTED fires synchronously inside start()
g.consentManager.exemptFromConsent(true)      // 3. or collect consent (below). Not persisted: set it every launch
//   Also before start (none are persisted): setRestoreHistory, setHistoryLookback, allowSiblingTickets, setEtaMode
g.start()                                     // 4. silently does nothing if server or API key is empty
g.setActive(true)                             // 5. if the app is in the foreground (see section 4)
```
- Events after `start()`: `PLATFORM_STARTED` (plus `PLATFORM_FIRST_LAUNCH` the first time) -> `PLATFORM_ACCOUNT_CREATED`
  (only when a new anonymous account is made) -> `PLATFORM_LOGIN_SUCCEEDED` (every start) ->
  `PLATFORM_SYNCED_WITH_SERVER` (once per instance: history is synced and the account is usable). Gate UI that needs
  the account (nickname, avatar, sending) on `SYNCED`. `PLATFORM_SESSION_FAILED` means the token expired and the SDK is
  re-authenticating by itself.
- `PLATFORM_ACCOUNT_CREATE_FAILED` and `PLATFORM_LOGIN_FAILED` are fatal: the platform stops itself
  (`PLATFORM_STOPPED`) and cannot restart. Create a new instance and retry later with backoff.
- A started platform cannot be restarted. After `stop()` (or a fatal stop) create a new instance with
  `createGlympse`. While one is running, `createGlympse` returns that running instance and ignores its arguments.
- Account persistence: the anonymous account survives restarts until the app is uninstalled or `logout()` is called.
  `logout()` only works while stopped (returns `false` otherwise). It wipes the account, nickname, avatar, history and
  consent, and the next `start()` creates a fresh account and fires `PLATFORM_FIRST_LAUNCH` again.
  `isFirstLaunch()` is valid right after creation; `hasUserAccount()` says whether an account exists.

### Consent: the account is not created without it

On a clean install the SDK does NOT create the account until the user has consented or the app is exempt. It fires
`LISTENER_CONSENT` / `CONSENT_REQUIRED` and waits; `sendTicket` returns `false` meanwhile. Choose one:
- **Exempt** (only if consent does not apply to your app): `consentManager.exemptFromConsent(true)` before `start()`.
- **Collect consent**: show the agreement (string resource `R.string.glympse_gdpr_message`, links
  `GC.PRIVACY_POLICY_URL()` and `GC.TERMS_AND_CONDITIONS_URL()`), then
  `consentManager.grantConsent(GC.CONSENT_TYPE_SUBJECT(), partnerId)` (`CONSENT_TYPE_GUARDIAN()` for a guardian). This
  starts account creation. On decline call `revokeConsent(partnerId)` and disable Glympse features. `partnerId` is the
  numeric id Glympse issues with your API key. Results: `CONSENT_SUCCEEDED`/`CONSENT_FAILED`. The grant is cached
  locally, so later launches skip the prompt; `CONSENT_MANAGER_SYNCED` follows once consent syncs with the server.

## 4. Lifecycle, threading, state

- The API is not thread safe. Call it only from the thread that created the platform, which must have a Looper
  (normally main). Callbacks arrive on that thread. There are no blocking calls. Never call `stop()` from inside
  `eventsOccurred`; post it.
- `userManager`, `historyManager` and most other managers are null until `start()`. `config` and `consentManager` are
  available earlier. Do not hold managers; read them from the platform while it runs. User, ticket, invite and place
  objects can be kept for UI.
- `setActive(true/false)` is reference counted and MUST be reciprocal: `true` in `onResume`/`onStart`, `false` in
  `onPause`/`onStop`. It is required even if you do not run in the background. `start()` resets the count to 0, so call
  `setActive(true)` right after it when the app is in the foreground. Deactivation is delayed briefly.
- Location runs only while a Glympse of yours is active (foreground and background); in the foreground without one it
  stays off. Override that with `locationManager.startLocation()` / `stopLocation(forced)`.
- If the system restarts the service it broadcasts `com.glympse.android.hal.service.STARTED` to your package. Register
  a manifest receiver for it and recreate and start the platform there; the SDK never restarts itself.
- Call `stop()` when the app terminates or Glympse is no longer needed. Then drop your reference.
- Events: `eventsOccurred(glympse: GGlympse, listener: Int, events: Int, obj: Any?)`. `listener` is a single value, `events`
  is a bitmask, so test `(events and GE.X) != 0`. Add the same listener to the platform and to any sink you care about
  (`ticket`, `user`, the `decodeInvite` sink) and `removeListener` when done. The SDK clears listeners on removed
  objects and on `stop()`. Low-level events (ticket, user) are duplicated on the platform, so subscribing to the
  platform alone is enough for list-level changes.
- Time: all timestamps are server time in milliseconds. Use `glympse.getTime()`, for example
  `ticket.expireTime - glympse.getTime()` is the remaining time (negative when expired).
- Do not implement SDK model interfaces (`GTicket`, `GInvite`, ...) yourself; get them from `GlympseFactory`. The
  only interfaces you implement are `GEventListener` and optional hooks. Do not mutate a `GArray` while iterating it.
- Your platform properties (`setRestoreHistory`, ETA mode, siblings) are not persisted. `config` properties
  (expire-on-arrival, speed sharing) are persisted and need setting once, for example on `PLATFORM_FIRST_LAUNCH`.

## 5. Main flows

**Send a Glympse**
1. Wait for `PLATFORM_SYNCED_WITH_SERVER`, consent granted or exempt, and location permission granted.
2. `val t = GlympseFactory.createTicket(durationMs, message, destination)` (`destination` from
   `GlympseFactory.createPlace(lat, lng, name)`, optional). Minimum 10 s; the default maximum is 4 hours
   (`config.maximumTicketDuration`).
3. `val inv = GlympseFactory.createInvite(GC.INVITE_TYPE_LINK, name, address)`; `t.addInvite(inv)`. Types: `LINK` (your app
   delivers the URL), `EMAIL`, `SMS`, `ACCOUNT`, `GROUP`, ...; name/address meaning per type is in `llms-full.txt`.
4. `t.addListener(l)` (or react to `PLATFORM_TICKET_ADDED`), then `glympse.sendTicket(t)`. It returns `false` when
   not started, without consent, already sent, or null; otherwise creation continues in the background.
5. Events on the ticket: `TICKET_CREATED` (created on the server; invite codes and URLs are NOT ready yet) ->
   `TICKET_INVITE_CREATED` (client-delivered invites such as `LINK`: state `INVITE_STATE_NEEDTOSEND`, read `invite.url`)
   or `TICKET_INVITE_SENT` (server-delivered) -> `TICKET_INVITE_UPDATED` (viewers) -> `TICKET_EXPIRED` ->
   `TICKET_COMPLETED` (data fully uploaded). Failures: `TICKET_FAILED` (discard and retry with a new ticket),
   `TICKET_INVITE_FAILED` (add a NEW invite object).
6. For a `LINK` invite share `invite.url` yourself, then `invite.initiateClientSideSend()` (optional) and
   `invite.completeClientSideSend(true/false)` to report the outcome.
7. Change a sent ticket: `modify(duration, message, destination)`, `clear(message, destination)`, `extend(ms)`,
   `addInvite`/`deleteInvite`, `expire()`. After expiry the ticket is immutable.
8. After a restart the platform restores your active tickets from the server (`historyManager.tickets`, with
   `PLATFORM_TICKET_ADDED`). Expired history only comes back with `setRestoreHistory(true)` (look-back via
   `setHistoryLookback`, server default about 30 days). Tickets from your other apps/devices ("siblings") are excluded
   unless `allowSiblingTickets(true)`.

**View a Glympse**
1. Pass the invite code to `glympse.decodeInvite(code, mode)` or, for free-form text containing a Glympse URL, to
   `glympse.openUrl(message, mode, null)`. Use `extractInviteCodes(message)` from any thread, but decode on the main thread.
2. `mode = GC.INVITE_MODE_DEFAULT or GC.INVITE_MODE_ACTIVATE_USER` views the ticket automatically AND starts pulling
   updates. `INVITE_MODE_DEFAULT` alone views it but does not track it. `INVITE_MODE_PROMPT_BEFORE_VIEWING` sends
   `PLATFORM_INVITE_TICKET` with a `GUserTicket`; show a confirmation, then call `glympse.viewTicket(userTicket)`.
3. Live updates come only for tracked users, and only while the platform is active (foreground). If you did not use
   `ACTIVATE_USER`, call `userManager.startTracking(user)` (reference counted; pair with `stopTracking`, or use
   `startTrackingAll()`).
4. Events: `PLATFORM_USER_ADDED` (a `GUser`), `USER_ACTIVE_TICKET_CHANGED`, `USER_TICKET_ADDED/REMOVED`, then ticket events
   (`TICKET_TRACK_CHANGED`, `TICKET_ETA_CHANGED`, `TICKET_EXPIRED`, `TICKET_ARRIVED`). Subscribe to a user and its
   active ticket on `PLATFORM_USER_ADDED`; drop the old ticket on `USER_ACTIVE_TICKET_RESIGNING`; unsubscribe on
   `USER_TICKET_REMOVED` and `PLATFORM_USER_REMOVED`. No events fire for expired or sibling tickets.
5. Read `user.nickname`, `user.avatar` (an image: call `load()`, wait for `IMAGE_CHANGED`), `user.location`,
   `ticket.track`, `ticket.eta`, `ticket.destination`, `ticket.expireTime`, `ticket.state`.
6. A request invite never auto-replies: `PLATFORM_INVITE_REQUEST` carries a `GUserTicket`; ask the user first.
   Stop watching with `user.stopWatching()`.

**Profile**: read `userManager.self`; set `setNickname(String)` / `setAvatar(GDrawable)` only after `SYNCED`, after checking
the current value (they are persisted, so set them once). Updating them needs network access.

**Push (optional)**: add `glympseapi-push` and `firebase-messaging`, then
`<meta-data android:name="glympse_fcm_application_id" android:value="..."/>` in `<application>`. Send Glympse your app's
package name to receive the value; it is per-app. The push module registers its token and handles Glympse messages. If
you have your own `FirebaseMessagingService`, call `GlympseFCM.isGlympseMessage(msg)` and
`GlympseFCM.handleMessage(this, msg)` for Glympse messages. Manual path: `registerDeviceToken(token)` and
`handleRemoteNotification(payload)` (ignored until the platform has started). When a push arrives while the platform is
not running, the SDK broadcasts `NotificationListener.INTENT_INVITE` / `INTENT_DATA` so you can start it.

## 6. Pitfalls

- Fresh install, nothing happens: you have neither consent nor exemption, or the server or API key is empty.
- Adding the listener after `start()` misses `PLATFORM_STARTED` and `PLATFORM_FIRST_LAUNCH`.
- `TICKET_CREATED` does not mean the invite is ready. Wait for `TICKET_INVITE_CREATED` or `TICKET_INVITE_SENT`.
- Viewing a ticket with the default mode does not give live updates (see "View a Glympse", step 2 and 3).
- `PLATFORM_SYNCED_WITH_SERVER` fires once per instance; `PLATFORM_TICKET_SYNC_COMPLETED` fires on every later sync.
- The server can reject requests as rate limited. The SDK then fires `PLATFORM_RATE_LIMITED` and does not retry those
  requests. Treat it as a signal to back off. The limits are server-side; the SDK's own data-rate
  optimizations are separate and send no such event.
- Do not default the nickname or avatar before `SYNCED`; you may overwrite the user's saved values.
- Request location permission before sending, but let the user decide when; the SDK will not ask.

## 7. Testing against Sandbox

- Create the platform with server `sandbox.glympse.com` and a Sandbox API key (ask Glympse). Production is `api.glympse.com`.
- Every install gets its own anonymous account. For a clean account, stop the platform, call `logout()`, then create
  a new instance (or reinstall).
- Event order on a normal relaunch: `STARTED`, `LOGIN_SUCCEEDED`, `SYNCED`. On the first run: `STARTED` and `FIRST_LAUNCH` (same callback),
  `ACCOUNT_CREATED`, `LOGIN_SUCCEEDED`, `SYNCED`.
