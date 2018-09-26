# Android Push Integration

## Introduction

Here is quick guide on how to integrate Glympse Push into 3rd party Android application. Glympse API is using Firebase Cloud Messaging (FCM) provider for delivering PUSH notifications on Android devices with Google Play Services and Amazon Device Messaging (ADM) for Kindle devices. Previously, the Glympse SDK used Google Cloud Messaging (GCM) with a slightly different integration. See the [GCM to FCM Migration](/docs/core/client-sdk/guides/common/android-push#gcm-to-fcm-migration) section for details on updating.

## Firebase Cloud Messaging

To add support for Firebase Cloud Messaging usage in the Glympse API, the GlympseApiPush dependency must be added to your project.

1. Send your application's package name to Glympse so that we can register your application in our Firebase project. After we register your application, we will send you an application id to use in step 3.

2. Perform these additions to your application's build.gradle file:
```
dependencies {
  ...
  implementation 'com.glympse:glympseapi-push:2.42.164'
  implementation "com.google.firebase:firebase-messaging:17.3.0"
  ...
}
...
```

3. Add this line to your application's manifest using the application id you received from Glympse.
```xml
  <meta-data android:name="glympse_fcm_application_id" android:value="[INSERT_APPLICATION_ID_HERE]"/>
```

It is possible that Glympse API is not running when new PUSH message comes in. In this case an intent is broadcast to let the host application know that some Glympse activity is happening, while the platform is not started. The following intents are currently supported:

- `NotificationListener.INTENT_INVITE`
	Ticket invite or request invite was sent to the user over PUSH.
- `NotificationListener.INTENT_DATA`
	Direct message was sent to the user.
	
## Projects that already use FCM

If your application already uses Firebase Cloud Messaging it requires one additional change in your existing integration. Since all messages will be received by your listener regardless of where they came from, it will need to check to see if they came from Glympse and pass them along if so. 

Find your class that extends `FirebaseMessagingService` and make the following addition:

```java
...
@Override
public void onMessageReceived(RemoteMessage remoteMessage)
{
    // Check to see if this is a Glympse message
    if ( GlympseFCM.isGlympseMessage(remoteMessage) )
    {
        GlympseFCM.handleMessage(this, remoteMessage);
        return;
    }
}
...
```

## GCM to FCM Migration

If your application is currently setup to use Glympse's GCM functionality there are a few things you need to remove since they are no longer needed. Not all of these removals will apply to your application. It will depend on whether or not your application uses legacy registration methods and if it received GCM messages from sources other than Glympse.

1. Remove GlympseApiPush from settings.gradle if you were building against a local version of the module
```
// Remove these two lines
include ':GlympseApiPush'
...
project(':GlympseApiPush').projectDir = new File('glympse_sdk_path/Android/Source/GlympseApiPush')
```

2. If you were using a legacy setup for GCM you may also have this entry in your AndroidManifest.xml that needs to be removed
```xml
<!-- Remove these permissions -->
<uses-permission android:name="com.google.android.c2dm.permission.RECEIVE" />    
<permission
    android:name="APPLICATION_PACKAGE_NAME.permission.C2D_MESSAGE"     
    android:protectionLevel="signature" />
<uses-permission
    android:name="APPLICATION_PACKAGE_NAME.permission.C2D_MESSAGE" />
    
<!-- Remove this service -->
<receiver
    android:name="com.glympse.android.hal.GCMReceiver"
    android:permission="com.google.android.c2dm.permission.SEND">
    <intent-filter>
        <action android:name="com.google.android.c2dm.intent.RECEIVE" />
        <action android:name="com.google.android.c2dm.intent.REGISTRATION" />
        <category android:name="APPLICATION_PACKAGE_NAME" />
    </intent-filter>
</receiver>
```

3. Remove code for passing a token to Glympse. The Glympse Push library is able to take care of this step on its own now.
```java
// Remove checks to see if Glympse has a valid token
if ( !glympse.hasValidDeviceToken() )
{

}


// Remove the Glympse Sender_ID from the registration intent.
String senderIds = "OTHER_IDS," + com.glympse.android.hal.GCMReceiver.SENDER_ID;
registrationIntent.putExtra("sender", senderIds);

// If you still have a receiver for GCM registration, remove the parts that call glympse.registerDeviceToken
public class GCMReceiver extends BroadcastReceiver
{
    public void onReceive(Context context, Intent intent)
    {
        if ( intent.getAction().equals("com.google.android.c2dm.intent.REGISTRATION") )
        {
            String registrationId = intent.getStringExtra("registration_id");
            if ( null != registrationId )
            {
                GGlympsePartner glympse = ...;
                glympse.registerDeviceToken(registrationId);
            }
        }
    }
}
```

4. If you have existing code for passing received messages to Glympse it may look like this. See the section [Projects that already use FCM](/docs/core/client-sdk/guides/common/android-push#projects-that-already-use-fcm) which shows how it should look now.
```java
public void onReceive(Context context, Intent intent)
{
    if (intent.getAction().equals("com.google.android.c2dm.intent.RECEIVE"))
    {
        String payload = intent.getStringExtra("payload");
        String from = intent.getStringExtra("from");
        if ( com.glympse.android.hal.GCMReceiver.SENDER_ID.equals(from) )
        {
            // Propagate message payload to platform for handling.
            GGlympsePartner glympse = ...;               
            glympse.handleRemoteNotification(payload);
        }
    }
}
```

## Amazon Device Messaging Requirements

Using ADM requires an Amazon Developer account, an app registered on that account, and a few pieces of information from the Amazon Developer Portal. Glympse servers need your application's Client ID and Client Secret (obtained from the Amazon Developer Portal) so that it can generate an access token to communicate with your app. That information should be sent to [support@glympse.com](mailto:support@glympse.com) so it can be associated with your Glympse API key.

You will also need a Kindle device capable of receiving ADM notifications to test with.

### Using ADM Specifically with Glympse

Glympse API provides convenience wrappers for ADM functionality for non ADM-enabled applications. In this scenario host application is only responsible for proper manifest configuration.

1. Add the Amazon namespace to the top of your AndroidManifest.xml:
```xml
<manifest
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:amazon="http://schemas.amazon.com/apk/res/android"
...
```
2. Declare permissions required by ADM in your AndroidManifest.xml:
```xml
<uses-permission android:name="android.permission.WAKE_LOCK" />    
<uses-permission android:name="android.permission.INTERNET" />
<permission
    android:name="APPLICATION_PACKAGE_NAME.permission.RECEIVE_ADM_MESSAGE"
    android:protectionLevel="signature" />
<uses-permission android:name="APPLICATION_PACKAGE_NAME.permission.RECEIVE_ADM_MESSAGE" />
<uses-permission android:name="com.amazon.device.messaging.permission.RECEIVE" />
```
3. Explicitly enable ADM and declare if it's required inside the application tag in your AndroidManifest.xml:
```xml
<application
    android:icon="@drawable/ic_launcher"
    android:label="@string/app_name"
    android:theme="@style/AppTheme">

    <amazon:enable-feature
        android:name="com.amazon.device.messaging"
        android:required="false"/>
...
```
4. Declare the service and receiver that will handle ADM messages:
```xml
<service
    android:name="com.glympse.android.hal.ADMMessageHandler"
    android:exported="false" />
<receiver
    android:name="com.glympse.android.hal.ADMMessageHandler$Receiver"
    android:permission="com.amazon.device.messaging.permission.SEND" >
    <intent-filter>
        <action android:name="com.amazon.device.messaging.intent.REGISTRATION" />
        <action android:name="com.amazon.device.messaging.intent.RECEIVE" />
        <category android:name="APPLICATION_PACKAGE_NAME" />
    </intent-filter>
</receiver>
```
5. Specify an API key obtained through Amazon's Developer Console. This should also be placed within the application tag in the manifest. Instructions for obtaining an API key can be found [here](https://developer.amazon.com/public/apis/engage/device-messaging/tech-docs/02-obtaining-adm-credentials).
```xml
<meta-data android:name="AmazonAPIKey" android:value="@string/adm_api_key"/>
```
```xml
<resources>
    <string name="adm_api_key">YOUR_API_KEY</string>
</resources>
```

### Proguard Configuration

If your app uses Proguard you will have to add a few flags to your proguard configuration file to ensure certain classes and methods are not obfuscated. This is necessary because ADM needs to be able to locate these classes in order to register the app's token and deliver messages.
```
-dontwarn com.amazon.device.messaging.**
-keep class com.amazon.** {*;}
-keep public class * extends com.amazon.device.messaging.ADMMessageReceiver {*;}
-keep public class * extends com.amazon.device.messaging.ADMMessageHandlerBase {*;}
-keepclassmembers public class * extends com.amazon.device.messaging.ADMMessageReceiver {*;}
-keepclassmembers public class * extends com.amazon.device.messaging.ADMMessageHandlerBase {*;}
-libraryjars [RELATIVE PATH TO ADM JAR]/amazon-device-messaging-1.0.1.jar
```
### Using ADM with Other Senders

This scenario assumes that host application is already configured for ADM, In this scenario it is only required to plug in Glympse API correctly into existing code on host application side. Here is how to do that.

1. Check if Glympse platform already has PUSH token from previous registration and it is still valid.
```java
GGlympsePartner glympse = ...;
if ( !glympse.hasValidDeviceToken() )
{
    // Proceed to STEP 2
}
```
2. Pass registration ID to Glympse API (to associate it with Glympse user account).
The device token can be passed during initial registration:
```java
public class ADMMessageHandler extends ADMMessageHandlerBase
{
    public void onRegistered(final String newRegistrationId)
    {
        if ( null != newRegistrationId )
        {
            GGlympsePartner glympse = ...;
            glympse.registerDeviceToken(newRegistrationId);
        }
    }
}
```
Or if registration occured some time in the past:
```java
final ADM adm = new ADM(context);
if (adm.getRegistrationId() != null)
{
    GGlympsePartner glympse = ...;
    glympse.registerDeviceToken(adm.getRegistrationId());
}
```
3. Propagate PUSH messages to Glympse API for processing.
```java
public class ADMMessageHandler extends ADMMessageHandlerBase
{
    public void onMessage(Intent intent)
    {
        if (intent.getAction().equals("com.amazon.device.messaging.intent.RECEIVE"))
        {
            String payload = intent.getStringExtra("payload");
            String from = intent.getStringExtra("from");
            if ( com.glympse.android.hal.ADMMessageHandler.SENDER_ID.equals(from) )
            {
                // Propagate message payload to platform for handling.
                GGlympsePartner glympse = ...;
                glympse.handleRemoteNotification(payload);
            }
        }
    }
}
```
`IGlympsePartner::handleRemoteNotification(...)` analyzes message payload and spreads corresponding notifications based on message types. Messages are usually sent to `GE::LISTENER_PLATFORM` and `GEP::LISTENER_PLATFORM` listeners.

**NOTE** It is possible that Glympse API is not started, when PUSH message comes in. In this case it is up to host application to decide, whether Glympse platform should be started or not.
