package com.glympse.enrouteexample.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

fun areLocationPermissionsGranted(
    context: Context
): Boolean {
    val fineLocationGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val backgroundLocationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

    return fineLocationGranted && backgroundLocationGranted
}

fun isNotificationPermissionGranted(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        // On Android 12 and below, notification permission is not required.
        return true
    }

    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
}

fun areRequiredPermissionsGranted(context: Context): Boolean {
    return isNotificationPermissionGranted(context) && areLocationPermissionsGranted(context)
}