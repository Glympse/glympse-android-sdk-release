# glympse-android-sdk-release

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
    // Glympse Core SDK
    implementation 'com.glympse:glympseapi:2.+'
    // Glympse En Route SDK
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
