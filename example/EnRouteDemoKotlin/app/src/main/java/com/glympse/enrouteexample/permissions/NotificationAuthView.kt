package com.glympse.enrouteexample.permissions

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NotificationAuthView(showTitle: Boolean = false) {
    val context = LocalContext.current

    // Notification permission is only runtime for Android 13 (API 33) and above
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Notification permission is not required for this Android version.",
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val notificationPermissionState = rememberPermissionState(
        permission = Manifest.permission.POST_NOTIFICATIONS
    )

    var showRationaleDialog by remember { mutableStateOf(false) }
    var requestAttempted by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (showTitle) {
            Text("Notification Permission", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))
        }

        val status: PermissionStatusEnum
        if (notificationPermissionState.status == PermissionStatus.Granted) {
            status = PermissionStatusEnum.GRANTED
        } else if (requestAttempted) {
            status = PermissionStatusEnum.DENIED
        } else {
            status = PermissionStatusEnum.NOT_REQUESTED
        }
        PermissionStatusItem("Notification permission", status, onRequestPermission = {
            val isPermanentlyDenied =
                !(notificationPermissionState.status as PermissionStatus.Denied).shouldShowRationale
            if (isPermanentlyDenied && requestAttempted) {
                openAppSettings(context)
            } else if (notificationPermissionState.status.shouldShowRationale) {
                showRationaleDialog = true
            } else {
                notificationPermissionState.launchPermissionRequest()
                coroutineScope.launch {
                    delay(500L)
                    requestAttempted = true
                }
            }
        })

        if (showRationaleDialog) {
            PermissionRationaleDialog(
                title = "Notifications Permission",
                message = "This app uses notifications to keep you updated on important events and information, even when you're not actively using it. Please grant permission to enable this feature.",
                onConfirm = {
                    showRationaleDialog = false
                    notificationPermissionState.launchPermissionRequest()
                },
                onDismiss = {
                    showRationaleDialog = false
                }
            )
        }
    }
}
