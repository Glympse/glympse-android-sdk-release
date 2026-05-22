package com.glympse.enrouteexample.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationAuthView(showTitle: Boolean = false) {
    val context = LocalContext.current

    val foregroundLocationPermissions = listOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
    )

    val backgroundLocationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
    } else {
        null
    }

    val foregroundPermissionsState = rememberMultiplePermissionsState(
        permissions = foregroundLocationPermissions
    )

    val backgroundPermissionState = backgroundLocationPermission?.let {
        rememberPermissionState(permission = it)
    }

    var showRationaleDialogForForeground by remember { mutableStateOf(false) }
    var showRationaleDialogForBackground by remember { mutableStateOf(false) }
    var foregroundRequestAttempted by remember { mutableStateOf(false) }
    var backgroundRequestAttempted by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (showTitle) {
            Text("Location Permissions", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))
        }

        val foregroundStatus: PermissionStatusEnum
        if (foregroundPermissionsState.allPermissionsGranted) {
            foregroundStatus = PermissionStatusEnum.GRANTED
        } else if (foregroundRequestAttempted) {
            foregroundStatus = PermissionStatusEnum.DENIED
        } else {
            foregroundStatus = PermissionStatusEnum.NOT_REQUESTED
        }
        PermissionStatusItem("Foreground Location", foregroundStatus, onRequestPermission = {
            if (!foregroundPermissionsState.shouldShowRationale && foregroundRequestAttempted) {
                openAppSettings(context)
            } else if (foregroundPermissionsState.shouldShowRationale) {
                showRationaleDialogForForeground = true
            } else {
                coroutineScope.launch {
                    delay(500L)
                    foregroundRequestAttempted = true
                }
                foregroundPermissionsState.launchMultiplePermissionRequest()
            }
        })

        Spacer(modifier = Modifier.height(24.dp))

        // --- Background Location Status & Button (only if foreground is granted and background is needed) ---
        if (foregroundPermissionsState.allPermissionsGranted && backgroundPermissionState != null) {
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            val backgroundStatus: PermissionStatusEnum
            if (backgroundPermissionState.status == PermissionStatus.Granted) {
                backgroundStatus = PermissionStatusEnum.GRANTED
            } else if (backgroundRequestAttempted) {
                backgroundStatus = PermissionStatusEnum.DENIED
            } else {
                backgroundStatus = PermissionStatusEnum.NOT_REQUESTED
            }
            PermissionStatusItem("Background Location", backgroundStatus, onRequestPermission = {
                val isPermanentlyDenied = !(backgroundPermissionState.status as PermissionStatus.Denied).shouldShowRationale
                if (isPermanentlyDenied && backgroundRequestAttempted) {
                    openAppSettings(context)
                } else if (backgroundPermissionState.status.shouldShowRationale) {
                    showRationaleDialogForBackground = true
                } else {
                    coroutineScope.launch {
                        delay(500L)
                        backgroundRequestAttempted = true
                    }
                    backgroundPermissionState.launchPermissionRequest()
                }
            })
        }

        // --- Rationale Dialogs ---
        if (showRationaleDialogForForeground) {
            PermissionRationaleDialog(
                title = "Foreground Location Needed",
                message = "This app needs foreground location access to show your position on the map and provide relevant local information.",
                onConfirm = {
                    showRationaleDialogForForeground = false
                    foregroundPermissionsState.launchMultiplePermissionRequest()
                },
                onDismiss = { showRationaleDialogForForeground = false }
            )
        }

        if (showRationaleDialogForBackground && backgroundPermissionState != null) {
            PermissionRationaleDialog(
                title = "Background Location Needed",
                message = "To provide continuous location updates and alerts even when the app is not in use (e.g., for geofencing or tracking a route in the background), please grant background location access. You will be taken to settings or a specific system dialog.",
                onConfirm = {
                    showRationaleDialogForBackground = false
                    backgroundPermissionState.launchPermissionRequest() // This will often take user to settings on newer Android versions
                },
                onDismiss = { showRationaleDialogForBackground = false }
            )
        }

        // --- Guidance if permission denied and rationale not shown (likely "Don't ask again") ---
        if (foregroundRequestAttempted &&
            !foregroundPermissionsState.allPermissionsGranted &&
            !foregroundPermissionsState.shouldShowRationale &&
            !showRationaleDialogForForeground
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Foreground location was denied. Please enable it in app settings for core functionality.",
                color = MaterialTheme.colorScheme.error
            )
        }

        if (backgroundRequestAttempted &&
            backgroundPermissionState != null &&
            backgroundPermissionState.status != PermissionStatus.Granted &&
            !backgroundPermissionState.status.shouldShowRationale &&
            !showRationaleDialogForBackground
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Background location was denied. Please enable 'Allow all the time' in app settings if you need background features.",
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun PermissionRationaleDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmButtonText: String = "Proceed",
    dismissButtonText: String = "Cancel"
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissButtonText)
            }
        }
    )
}

fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }

    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    }
}

enum class PermissionStatusEnum {
    GRANTED, DENIED, NOT_REQUESTED
}

@Composable
fun PermissionStatusItem(title: String, statusEnum: PermissionStatusEnum, onRequestPermission: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val statusColor: Color
                val statusIcon: ImageVector
                val statusText: String
                val itemAlpha = if (false) 0.5f else 1f

                when (statusEnum) {
                    PermissionStatusEnum.GRANTED -> {
                        statusColor = MaterialTheme.colorScheme.primary
                        statusIcon = Icons.Filled.CheckCircle
                        statusText = "Granted"
                    }
                    PermissionStatusEnum.DENIED -> {
                        statusColor = MaterialTheme.colorScheme.error
                        statusIcon = Icons.Filled.Warning
                        statusText = "Denied"
                    }
                    PermissionStatusEnum.NOT_REQUESTED -> {
                        statusColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        statusIcon = Icons.Filled.AddCircle
                        statusText = "Not yet requested"
                    }
                }

                Icon(
                    imageVector = statusIcon,
                    contentDescription = "Status: $statusText",
                    tint = statusColor.copy(alpha = itemAlpha),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = itemAlpha),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = statusText,
                        color = statusColor.copy(alpha = itemAlpha),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (statusEnum != PermissionStatusEnum.GRANTED) {
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier.padding(top = 8.dp)
                        .fillMaxWidth()
                ) {
                    Text("Request $title")
                }
            }
        }
    }
}
