# Glympse Android SDK Maven repository

This repository hosts the Android version of all Glympse SDKs. SDKs may also be downloaded from https://developer.glympse.com/.

## Which SDKs do I need?

The En Route SDK contains the interfaces for Enterprise use cases. If your app is for employees handling Tasks or Pickups, you'll be using the En Route SDK ([integration guide](integration/enroute/AGENTS.md)).

The Core SDK is required for all use cases. It handles communication with Glympse servers, location, and battery management, etc. If your app uses the Core platform directly, for example to send and view Glympses, see its [integration guide](integration/core/AGENTS.md).

## Integration guides

Step-by-step integration guides are in [`integration/`](integration/). They are written to be read by developers and by
AI coding agents.

| SDK | Guide | API outline |
|---|---|---|
| En Route SDK (Tasks, Pickups, agent login) | [`integration/enroute/AGENTS.md`](integration/enroute/AGENTS.md) | [`integration/enroute/llms-full.txt`](integration/enroute/llms-full.txt) |
| Core SDK (anonymous user, send and view Glympses) | [`integration/core/AGENTS.md`](integration/core/AGENTS.md) | [`integration/core/llms-full.txt`](integration/core/llms-full.txt) |

Each guide covers installation, manifest and permission requirements, the exact start-up call order, lifecycle and
threading rules, the main flows and common pitfalls. `AGENTS.md` is the concise guide; `llms-full.txt` is a reference
outline of the API (managers, events, constants and models).

To have your coding agent use a guide, copy that SDK's `AGENTS.md` and `llms-full.txt` into the root of your app's
repository (keep the two files together, as `AGENTS.md` refers to `llms-full.txt`). Other pieces:

- [`example/EnRouteDemoKotlin`](example/EnRouteDemoKotlin) is a working En Route reference app.
- [`PUSH-README.md`](PUSH-README.md) covers push notification setup.

## Installing

Add this repository to your application's build.gradle
```
repositories {
    maven {url "https://raw.github.com/Glympse/glympse-android-sdk-release/master"}
}
```

Add this dependency to your build.gradle
```
dependencies {
    // Glympse Core SDK (required)
    implementation 'com.glympse:glympseapi:2.+'
    // Glympse En Route SDK (for Enterprise customers)
    implementation 'com.glympse:enrouteapi:2.+'
    
    // Optional - Glympse Push library
    implementation 'com.glympse:glympseapi-push:2.+'
    // Optional - Glympse Map control
    implementation 'com.glympse:glympseapi-map:2.+'
    // Optional - Glympse UI controls
    implementation 'com.glympse:glympseapi-controls:2.+'
    // Optional - Glympse contacts UI controls
    implementation 'com.glympse:glympseapi-contacts:2.+'
}
```
