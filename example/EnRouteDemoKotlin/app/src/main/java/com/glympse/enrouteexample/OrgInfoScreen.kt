package com.glympse.enrouteexample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.glympse.enroute.android.api.GOrganization

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrgInfoScreen(navController: NavController) {
    var org by remember { mutableStateOf<GOrganization?>(null) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Organization") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ){ innerPadding ->
        LaunchedEffect(Unit) {
            org = EnRouteWrapper.enroute?.organization
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colorScheme.surfaceVariant)
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            org?.let { org ->
                Text(
                    text = "Organization #${org.id}",
                    style = MaterialTheme.typography.bodyLarge
                )

                // General information
                val generalInfoRows = listOf(
                    InfoRowData("Name", org.name),
                    InfoRowData("Referrer ID", org.referrerOrgId.toString()),
                    InfoRowData("Admin Email", org.adminEmail)
                )
                InfoCard(
                    title = "General Information",
                    rows = generalInfoRows
                )

                // Config
                val configRows = listOf(
                    InfoRowData("Shifts Enabled", org.config.areShiftsEnabled().toString()),
                    InfoRowData("Arrival Phase", org.config.arrivalPhase),
                    InfoRowData("Branding ID", org.config.brandingId),
                    InfoRowData("Completion Phases", org.config.completionPhases.joinToString(",")),
                    InfoRowData("Default Travel Mode", org.config.defaultTravelMode),
                    InfoRowData("Final Phase", org.config.finalPhase),
                    InfoRowData("Initial Phase", org.config.initialPhase),
                    InfoRowData("Initial Tracking Phase", org.config.initialTrackingPhase),
                    InfoRowData("Photo Upload Enabled", org.config.isPhotoUploadEnabled.toString()),
                    InfoRowData("Pickup Mode", org.config.isPickupEnabled.toString()),
                    InfoRowData("Session Mode", org.config.isSessionModeEnabled.toString()),
                    InfoRowData("Signature Upload Enabled", org.config.isSignatureUploadEnabled.toString()),
                    InfoRowData("Picker Disabled Task Phases", org.config.pickerDisabledTaskPhases.joinToString(",")),
                )
                InfoCard(
                    title = "Config",
                    rows = configRows
                )

            }
        }
    }
}
