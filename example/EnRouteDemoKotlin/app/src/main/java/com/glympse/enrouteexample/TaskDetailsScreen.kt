package com.glympse.enrouteexample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.glympse.android.core.CoreConstants
import com.glympse.android.core.GPrimitive
import com.glympse.android.lib.GTrackPrivate
import com.glympse.enroute.android.api.GTask
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailsScreen(navController: NavController, taskId: String) {
    var task by remember { mutableStateOf<GTask?>(null) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Task Detail") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ){ innerPadding ->
        LaunchedEffect(Unit) {
            task = EnRouteWrapper.enroute?.taskManager?.findTaskById(taskId.toLong())
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colorScheme.surfaceVariant)
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            task?.let { task ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    modifier = Modifier
                                        .weight(1f),
                                    text = "Task #$taskId",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    modifier = Modifier
                                        .background(
                                            color = MaterialTheme.colorScheme.primaryFixed,
                                            shape = RoundedCornerShape(percent = 50)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp),

                                    text = task.phase ?: "",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                // General information
                val generalInfoRows = listOf(
                    InfoRowData("Description", task.description),
                    task.operation?.let { operation ->
                        InfoRowData("Destination", operation.ticket.destination.name)
                        InfoRowData("Location", "${operation.ticket.destination.latitude}, ${operation.ticket.destination.longitude}")
                    },
                    InfoRowData("Agent ID", task.agentId.toString()),
                    InfoRowData("Foreign ID", task.foreignId)
                )
                InfoCard(
                    title = "General Information",
                    rows = generalInfoRows
                )

                // Timeline
                val timelineRows = listOf(
                    InfoRowData("Due Time", formatTime(task.dueTime)),
                    InfoRowData("Arrived Time", formatTime(task.arrivedTime)),
                    InfoRowData("Completed Time", formatTime(task.completedTime))
                )
                InfoCard(
                    title = "Timeline",
                    rows = timelineRows
                )

                // Operation
                task.operation?.let { operation ->
                    val operationRows = listOf(
                        InfoRowData("Operation ID", operation.id.toString()),
                        InfoRowData("State", operation.state.toString()),
                        InfoRowData("Start Time", formatTime(operation.startTime))
                    )
                    InfoCard(
                        title = "Operation",
                        rows = operationRows
                    )
                }

                // Ticket
                task.operation?.ticket?.let { ticket ->
                    val ticketRows = listOf(
                        InfoRowData("Ticket ID", ticket.id),
                        InfoRowData("Travel Mode", getTravelMode(ticket.travelMode?.mode)),
                        InfoRowData("ETA", formatEta(ticket.eta)),
                        ticket.route?.let { route ->
                            InfoRowData("Route Distance", "${route.distance}m")
                        },
                        InfoRowData("Visibility", ticket.visibility.string),
                    )
                    InfoCard(
                        title = "Ticket",
                        rows = ticketRows
                    )
                }

                // Metadata
                if (task.metadata.length() > 0) {
                    val metadataRows = task.metadata.map {
                        val key = it.getString("n")
                        val value = it.get("v")
                        InfoRowData(key, value.asString())
                    }
                    InfoCard(
                        title = "Metadata",
                        rows = metadataRows
                    )
                }

            }
        }
    }
}

@Composable
fun formatTime(epoch: Long?): String {
    return if (epoch == null || epoch <= 0) {
        "Not set"
    } else {
        val dateFormat = DateFormat.getDateTimeInstance(
            DateFormat.LONG,
            DateFormat.LONG
        )
        dateFormat.format(Date(epoch))
    }
}

@Composable
fun formatEta(msFromNow: Long?): String {
    return if (msFromNow == null || msFromNow <= 0) {
        "Not set"
    } else {
        val dateFormat = DateFormat.getDateTimeInstance(
            DateFormat.LONG,
            DateFormat.LONG
        )
        val now = Date()
        dateFormat.format(Date(now.time + msFromNow))
    }
}

fun getTravelMode(id: Int?): String {
    return when (id) {
        0 -> "Drive"
        1 -> "Transit"
        2 -> "Bike"
        3 -> "Walk"
        else -> "Unknown"
    }
}

fun GPrimitive.asString(): String {
    return when (this.type()) {
        CoreConstants.PRIMITIVE_TYPE_STRING -> this.string
        CoreConstants.PRIMITIVE_TYPE_LONG -> this.long.toString()
        CoreConstants.PRIMITIVE_TYPE_DOUBLE -> this.double.toString()
        CoreConstants.PRIMITIVE_TYPE_BOOLEAN -> this.bool.toString()
        else -> ""
    }
}

data class InfoRowData(
    val label: String,
    val value: String?
)

@Composable
fun InfoCard(
    title: String,
    rows: List<InfoRowData?>,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            rows.forEach { row ->
                row?.let {
                    InfoRow(label = row.label, value = row.value)
                }
            }
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value ?: "N/A",
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(2f)
        )
    }
}
