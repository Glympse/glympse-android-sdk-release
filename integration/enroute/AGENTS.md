# Glympse En Route SDK: Android integration guide for coding agents

Use this when adding the En Route SDK to an Android app (Kotlin or Java). The API lives in
`com.glympse.enroute.android.api.*`, `com.glympse.android.api.*`, `com.glympse.android.core.*` and
`com.glympse.android.toolbox.*`.
Full API outline (managers, events, constants, models): `llms-full.txt`.
Reference app: `example/EnRouteDemoKotlin` in https://github.com/Glympse/glympse-android-sdk-release. See
`EnRouteWrapper.kt` (manager lifecycle), `EventListener.kt` (events), `permissions/` (runtime permission flow),
`TasksScreen.kt` (task actions), and the manifest (permissions, push meta-data). Push setup: `PUSH-README.md` there.

## 1. Install (Maven)

`settings.gradle.kts` (add the Glympse repo to `dependencyResolutionManagement`; this fails if declared per-project
when `FAIL_ON_PROJECT_REPOS` is set):
```kotlin
maven { setUrl("https://raw.github.com/Glympse/glympse-android-sdk-release/master") }
```
`app/build.gradle.kts` (all artifacts share one version and the POMs do not pull each other in, so declare each one):
```kotlin
implementation("com.glympse:enrouteapi:<version>")          // En Route API (this SDK)
implementation("com.glympse:glympseapi:<version>")          // core platform
implementation("com.glympse:glympseapi-toolbox:<version>")  // GListener / GSource
implementation("com.glympse:glympseapi-push:<version>")     // optional: FCM push
```
Use the latest version published in that repo: the `<release>` element of
`https://raw.github.com/Glympse/glympse-android-sdk-release/master/com/glympse/enrouteapi/maven-metadata.xml`.
Push also needs `com.google.firebase:firebase-messaging`.

## 2. Requirements and permissions

- SDK AAR minSdk is 19. Compile against a current API level.
- The AAR manifest already merges: `INTERNET`, `WAKE_LOCK`, `ACCESS_COARSE/FINE_LOCATION`, `FOREGROUND_SERVICE`,
  `FOREGROUND_SERVICE_LOCATION`, plus a location-type foreground service and receivers.
- Add to your manifest: `POST_NOTIFICATIONS` (API 33+, the SDK shows an ongoing foreground notification) and
  `ACCESS_BACKGROUND_LOCATION` (for tracking while the app is not in the foreground; confirm with Glympse whether your
  org configuration needs it).
- Request at runtime, in this order: (1) COARSE+FINE together, (2) BACKGROUND separately, only after (1) is granted
  (API 29+), (3) POST_NOTIFICATIONS on API 33+. Do this before the user starts or tracks a task: start, phase-change
  and complete calls post the device's current location.
- Active sharing notification text customization: `setForegroundNotificationMessage(String)`.

## 3. Init and auth: exact call order

Create ONE manager per process (hold it in an `Application`-scoped singleton) on a thread with a Looper (main).
```kotlin
val m = EnRouteFactory.createEnRouteManager(applicationContext)  // 1. create
m.authenticationMode = EnRouteConstants.AUTH_MODE_CREDENTIALS    // 2. config; MUST precede start()
//   or AUTH_MODE_TOKEN. Also before start(): setBaseUrl (only if you are testing in Sandbox),
//   taskManager.setFetchCompletedTasks, setSessionsGeofencesEnabled, setIncludeRootFields
m.addListener(listener)                                           // 3. GListener (toolbox)
m.start()                                                         // 4. returns false if mode unset and login needed
// 5. wait for STARTED. No stored login: AUTHENTICATION_NEEDED fires (either mode), show login UI. Else it resumes
//    (STARTED then SYNCED, no auth event)
m.loginWithCredentials(user, pass)                                // 6a. credentials mode, only after start()
m.loginWithToken(token, expireTimeEpochMs)                        // 6b. token mode: ABSOLUTE epoch ms, >= 1 hour ahead
m.setActive(true)                                                 // 7. once per foreground transition (see §4)
```
- Mismatched mode/login method, calling login before `start()`, or a second login without logout returns `false`
  and only logs a warning. Check the return value.
- `ENROUTE_MANAGER_LOGIN_COMPLETED` is emitted only for successful logins. Failures come through `LOGGED_OUT` (below).
  In credentials mode it fires at login success, before the profile loads; in token mode it fires after org and
  agent load.
- Readiness: `ENROUTE_MANAGER_SYNCED` means the organization and agent profiles have synced: `organization` and
  `selfAgent` are populated from then on. It does NOT mean tasks are loaded: `taskManager.tasks` is empty at `SYNCED`.
  The initial task list arrives with `TASKS_TASK_LIST_CHANGED`. Add the task-manager listener when `SYNCED` fires and
  re-read `tasks` on every `TASKS_TASK_LIST_CHANGED`, including the first. Show a loading state until then, not "no tasks".
- Login failure arrives as `ENROUTE_MANAGER_LOGGED_OUT` with `param1` (Long) =
  `EnRouteConstants.LOGOUT_REASON_INVALID_CREDENTIALS`.
- Logout reasons the SDK actually emits: `USER_ACTION`, `INVALID_CREDENTIALS`, `INVALID_TOKEN`, `OLD_VERSION`,
  `SERVER_ERROR`. `OLD_VERSION` means the SDK build is older than the org's minimum: upgrade the SDK.
- `INVALID_TOKEN` fires when: credentials mode finds the stored token expired or invalid; token mode fails before its
  first sync; or the agent profile fails to load or belongs to a different agent.
- Token mode, after sync: an expired or invalid token pauses the SDK (including location sharing) and fires
  `ENROUTE_MANAGER_AUTHENTICATION_NEEDED`. Fetch a fresh token and call `loginWithToken`; queued calls are retried.
- `stop()` keeps the stored login, so the next launch resumes. `logout(reason)` clears it. Both end in `STOPPED`;
  `LOGGED_OUT` also precedes it for `logout`. After `STOPPED` the instance is dead, its sub-managers are released,
  and all listeners are dropped. Create a new manager and repeat steps 1-6 to log in again.

## 4. Lifecycle, threading, state

- `setActive(true/false)` is reference counted and must be reciprocal: one `true` in `onResume`/`onStart`, one `false`
  in `onPause`/`onStop`. An extra `true` (for example right after `start()`) keeps the platform active forever.
  Deactivation is delayed a moment so quick pause/resume does not flap.
- Never recreate the manager per Activity (`onCreate` runs again on rotation/process-alive relaunch). Guard creation
  with `if (manager?.isStarted == true) return`. A stopped manager (after logout) must be replaced, and the new one
  starts with an active count of 0: call `setActive(true)` on it if the app is currently in the foreground.
- Track foreground state in your wrapper and call `setActive` only on real transitions, so calls stay paired.
- The SDK schedules its work on the creating thread's Looper, so create it on the main thread. Listener callbacks:
  keep them short and push results into `StateFlow`/`LiveData`; do not block or do I/O in `eventsOccurred`.
- `eventsOccurred(source, listener, events, param1, param2)`: `events` is a bitmask and several bits can arrive in one
  call. Test each with `(events and X) != 0`, dispatch on `listener` first (`LISTENER_ENROUTE_MANAGER`, `LISTENER_TASKS`,
  `LISTENER_AGENTS`, `LISTENER_SESSIONS`, `LISTENER_PICKUPS`). Cast params per the doc in `llms-full.txt`
  (logout reason is `Long`). All constants are `Int`. Exact Kotlin signature:
  ```kotlin
  object EventListener : GListener {   // com.glympse.android.toolbox.listener.GListener / GSource
      override fun eventsOccurred(source: GSource?, listener: Int, events: Int, param1: Any?, param2: Any?) { /* ... */ }
  }
  ```
  Register the same listener object on the manager and on each sub-manager you use; it receives all of them.
- Getter types you will hit: `id` is `Long` on `GTask`, `GOrganization` and `GAgent` (convert with `toString()` for
  display); `GOperation.taskId` is `Long`; `GAgent.roles` is a `GArray<String>` (iterable, `joinToString` works).
- Model objects (`GTask`, `GOperation`, ...) are mutable and keep identity when they change. For Compose/RecyclerView
  diffing, copy the fields you display into an immutable data class on every update (`TASKS_TASK_LIST_CHANGED`,
  `TASKS_TASK_PHASE_CHANGED`), or the UI will show stale phases.
- Each sub-manager (`taskManager`, `sessionManager`, `pickupManager`, `agentManager`) has its own `addListener`.

## 5. Main flows

Tasks (the common flow; `EnRouteConstants.PHASE_PROPERTY_*()` are functions, call them with parens):
1. After `SYNCED`: `taskManager.addListener(l)`; the list is read with `taskManager.tasks` (also `pendingTasks`,
   `activeOperations`, `findTaskById(Long)`) and is empty until the first `TASKS_TASK_LIST_CHANGED`. Re-read on every
   `TASKS_TASK_LIST_CHANGED`. By default completed tasks are not fetched (`setFetchCompletedTasks(false)`), so a
   completed task is gone from the list after the next launch; call `setFetchCompletedTasks(true)` before `start()` to
   keep them (state `TASK_STATE_COMPLETED`).
2. Start: `taskManager.startTask(task)`. It returns `false` unless `task.state == TASK_STATE_CREATED`, the task has
   no operation yet, and the org profile is loaded. Then wait for `TASKS_TASK_STARTED` or `TASKS_TASK_START_FAILED`.
   After `TASK_STARTED` the state is `TASK_STATE_STARTED`. The task's phase is populated by the update that follows
   the start, delivered by push (or by `refresh()` when push is not available). Until it arrives `phase` reads
   `PHASE_PROPERTY_UNKNOWN()` (`"unknown"`). Started is not live: offer Go Live whenever `phase != PHASE_PROPERTY_LIVE()`.
3. Move phase: `taskManager.setTaskPhase(task, EnRouteConstants.PHASE_PROPERTY_LIVE())`, then
   `..._ARRIVED()`. The change is confirmed by `TASKS_TASK_PHASE_CHANGED`. It returns `false` if the phase is unchanged.
   Start the task first: `setTaskPhase` does not reject a task that was never started (it changes the local phase and
   posts it, but the server still has the task as not started).
4. Finish: `taskManager.completeOperation(task.operation)` (or the overload with a `TASK_COMPLETE_REASON_*` code).
   `operation` is null until a task has started, so null-check it. Result events:
   `TASKS_OPERATION_COMPLETED` / `TASKS_OPERATION_COMPLETION_FAILED`. ARRIVED is a separate, optional step;
   `completeOperation` can be called from LIVE. See "Data freshness": the local task can still read STARTED / `live`
   after `OPERATION_COMPLETED`.
5. Other: `createTask(GPrimitive, startAfterCreate)` (returns `false` unless the self agent has the `manager` or
   `admin` role), `addOrUpdateMetadataItem`, `sendMessage`, `setTravelModeForTask`.
6. Metadata: `task.metadata` is a list of `GPrimitive` entries. In each, `getString("n")` is the field name and
   `get("v")` is its value (a `GPrimitive`; check `type()` against `CoreConstants.PRIMITIVE_TYPE_*`).

Phase names and which transitions are valid come from the org: read `organization.config` (`initialPhase`,
`initialTrackingPhase`, `arrivalPhase`, `finalPhase`, `completionPhases`, `pickerDisabledTaskPhases`) rather than
hard-coding a state machine.

Profile: `organization` (`name`, `config`), `selfAgent` (`displayName`, `roles`, `glympseAvatarUrl`).
Sessions and pickups use the same pattern (`sessionManager`, `pickupManager`); see `llms-full.txt`. Sessions only
start when `organization.config.isSessionModeEnabled`, so check it before showing session UI.

Data freshness: the server changes phases and completes operations too (push, geofence, arrival), so do not assume
only your own calls move a task. In normal use the server sends a push that tells the SDK to refresh, which is what
brings the local task list up to date after a completion. Without push (no FCM set up, or no
`glympse_fcm_application_id`) data refreshes through the org's auto-refresh setting (only while active), otherwise call
`refresh()` on the managers. `refresh()` does nothing before the profile has loaded. Without push the local task model
is not updated after a completion or phase change on its own: after `TASKS_OPERATION_COMPLETED` the task still reads
state STARTED and phase `live`, and no `TASKS_TASK_LIST_CHANGED` follows, until the next refresh or launch. Without
push, call `refresh()` after completing and after phase changes, and/or update your row from the event itself: on
`TASKS_OPERATION_COMPLETED`, `param1` is a `GOperation` and `taskId` identifies the task to show as completed.

Push (optional): add `glympseapi-push` and firebase-messaging, then add
`<meta-data android:name="glympse_fcm_application_id" android:value="..."/>` inside `<application>`. Get the value
from Glympse by sharing your app's package name; it is per-app, never copy another app's. The push module registers
its token and handles Glympse messages itself. If your app also has a `FirebaseMessagingService`, forward Glympse
payloads with `GlympseFCM.isGlympseMessage(msg)` / `GlympseFCM.handleMessage(ctx, msg)`.
Alternative manual path: `registerDeviceToken(type, token)` and `handleRemoteNotification(payload)`.

## 6. Pitfalls

- `loginWithToken` takes an absolute epoch time in milliseconds, at least one hour in the future. A duration, seconds,
  or a short expiry makes it return `false`.
- Permission timing: do not show a request as "denied" until the system dialog has resolved. Treat "denied + no
  rationale + already asked" as permanently denied and send the user to `Settings.ACTION_APPLICATION_DETAILS_SETTINGS`.
- The background-location grant must be re-checked after returning from Settings (re-evaluate on `ON_RESUME`).
- `STARTED` fires even when no login exists; pick the login-vs-main screen from `AUTHENTICATION_NEEDED` or
  `isLoginNeeded`, not from `STARTED` alone.
- Debug logging (`overrideLoggingLevels`) is for development; leave it off in production builds.
- `ENROUTE_MANAGER_SHOW_NOTIFICATION` carries a notification (`param1`: `GPrimitive`) for the host to display.
- A UI that shows "no tasks" before the first `TASKS_TASK_LIST_CHANGED`, or settings blanks before `SYNCED`, is a
  loading state, not an empty state.
- `createTask` takes a `GPrimitive` payload whose schema is not covered here; ask your Glympse representative for it,
  or have whoever administers your test organization assign tasks to your test agent.

## 7. Testing against Sandbox

- Point the SDK at Sandbox with `m.setBaseUrl("s-api.enterprise.glympse.com")` before `start()`. Pass a bare host
  (no `https://`). Guard it with `BuildConfig.DEBUG` or a build flavor.
- You need a Sandbox organization and agent (username and password): ask your Glympse representative. A manager agent
  has roles `agent, manager`.
- Each task can be started and completed once, so assign several test tasks to the agent before a run.
- Event order on a stored login: relaunch gives `STARTED`, then `SYNCED`, then `TASKS_TASK_LIST_CHANGED`, with no
  `AUTHENTICATION_NEEDED`. `logout(USER_ACTION)` gives `LOGGED_OUT` then `STOPPED`; a new manager then gives `STARTED`,
  `AUTHENTICATION_NEEDED`, and a credentials login works again.
