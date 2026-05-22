package com.glympse.enrouteexample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import com.glympse.enroute.android.api.EnRouteConstants
import com.glympse.enroute.android.api.GAgent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentInfoScreen(navController: NavController) {
    var agent by remember { mutableStateOf<GAgent?>(null) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agent") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ){ innerPadding ->
        LaunchedEffect(Unit) {
            agent = EnRouteWrapper.enroute?.selfAgent
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
            agent?.let { agent ->
                Text(
                    text = "Agent #${agent.id}",
                    style = MaterialTheme.typography.bodyLarge
                )

                SubcomposeAsyncImage(
                    model = agent.glympseAvatarUrl,
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    },
                    modifier = Modifier
                        .height(100.dp)
                        .width(100.dp)
                        .clip(CircleShape)
                )

                // General information
                val generalInfoRows = listOf(
                    InfoRowData("Display Name", agent.displayName),
                    InfoRowData("Name", agent.name),
                    InfoRowData("Email", agent.email),
                    InfoRowData("Roles", agent.roles.joinToString(","))
                )
                InfoCard(
                    title = "General Information",
                    rows = generalInfoRows
                )

                Button(
                    modifier = Modifier
                        .padding(horizontal = 16.dp),
                    onClick = {
                        EnRouteWrapper.enroute?.logout(EnRouteConstants.LOGOUT_REASON_USER_ACTION)
                    }) {
                    Text("Logout")
                }
            }
        }
    }
}
