# Glympse Android SDK Maven repository

This repository hosts the Android version of all Glympse SDKs. SDKs may also be downloaded from https://developer.glympse.com/.

## Which SDKs do I need?

The En Route SDK contains the interfaces for Enterprise use cases. If your app is for employees handling Tasks or Pickups, you'll be using the En Route SDK.

The Core SDK is required for all use cases. It handles communication with Glympse servers, location, and battery management, etc.

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
