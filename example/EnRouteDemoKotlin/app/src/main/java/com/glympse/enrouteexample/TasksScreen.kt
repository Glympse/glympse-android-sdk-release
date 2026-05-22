package com.glympse.enrouteexample

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.glympse.enroute.android.api.EnRouteConstants
import com.glympse.enroute.android.api.GTask
import com.glympse.enrouteexample.permissions.areRequiredPermissionsGranted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(navController: NavController) {
    Scaffold(
        bottomBar = { TasksBottomBar(navController) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            PermissionsView(navController)

            val tasks by EventListener.tasks.collectAsState(initial = emptyList())
            val isSynced by EventListener.areTasksSynced.collectAsState(initial = false)
            var actionMenuTask: GTask? by remember { mutableStateOf(null) }
            var taskToComplete: GTask? by remember { mutableStateOf(null) }
            if (tasks.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color = MaterialTheme.colorScheme.surfaceVariant),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                    items(items = tasks, key = { it.task.id }) { taskState ->
                        TaskItem(
                            modifier = Modifier
                                .clickable(onClick = {
                                    val taskId = taskState.task.id.toString()
                                    navController.navigate("task/$taskId")
                                })
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp), taskState
                        ) {
                            actionMenuTask = taskState.task
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color = MaterialTheme.colorScheme.surfaceVariant),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isSynced) {
                        Text(
                            text = "No Tasks to Display",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    } else {
                        CircularProgressIndicator()
                    }
                }
            }

            TaskActionSheet(
                task = actionMenuTask,
                onDismiss = { actionMenuTask = null },
                onConfirmComplete = { task ->
                    taskToComplete = task
                    actionMenuTask = null
                }
            )

            taskToComplete?.let { task ->
                BasicAlertDialog(onDismissRequest = { taskToComplete = null}) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text(text = "Complete Task", style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.padding(16.dp))
                            Text(text = "Are you sure you want to complete this task?")

                            Spacer(modifier = Modifier.padding(24.dp))
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Spacer(modifier = Modifier.weight(1f))
                                TextButton(onClick = { taskToComplete = null }) {
                                    Text("Cancel")
                                }
                                TextButton(onClick = {
                                    taskToComplete = null
                                    EnRouteWrapper.enroute?.taskManager?.completeOperation(task.operation)
                                }) {
                                    Text("Confirm")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionsView(navController: NavController) {
    val context = LocalContext.current
    val isPermissionGranted by remember {
        derivedStateOf { areRequiredPermissionsGranted(context) }
    }
    if (!isPermissionGranted) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable(onClick = {
                    navController.navigate(AppDestinations.PERMISSIONS_ROUTE)
                }),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Text(
                text = "Required permissions are missing. Tap to configure.",
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
fun TasksBottomBar(navController: NavController) {
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                onClick = {
                    navController.navigate(AppDestinations.ORG_INFO_ROUTE)
                }) {
                Text("Org Info")
            }
            Button(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                onClick = {
                    navController.navigate(AppDestinations.AGENT_INFO_ROUTE)
                }) {
                Text("Agent Info")
            }
        }
    }
}


@Composable
fun TaskItem(modifier: Modifier = Modifier, task: TaskState, onMenuClick: () -> Unit) {
    Card(
        modifier = modifier,
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
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier
                            .weight(1f),
                        text = task.description ?: "No Description",
                        style = MaterialTheme.typography.bodyLarge)
                    Text(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primaryFixed,
                                shape = RoundedCornerShape(percent = 50))
                            .padding(horizontal = 8.dp, vertical = 4.dp),

                        text = task.phase ?: "",
                        style = MaterialTheme.typography.bodySmall)

                    IconButton(onClick = {
                        onMenuClick()
                    }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Actions")
                    }
                }
            }
        }
    }
}

enum class TaskAction {
    start,
    live,
    arrive,
    pause,
    complete
}
data class ActionItem(
    val id: TaskAction,
    val title: String,
    val isDestructive: Boolean = false,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskActionSheet(
    modifier: Modifier = Modifier,
    task: GTask?,
    onDismiss: () -> Unit,
    onConfirmComplete: (GTask) -> Unit,
) {
    if (task == null) return

    val actions = buildList {
        when (task.phase) {
            EnRouteConstants.PHASE_PROPERTY_NEW(),
            EnRouteConstants.PHASE_PROPERTY_UNKNOWN() -> {
                add(ActionItem(TaskAction.start, "Start Task") {
                    EnRouteWrapper.enroute?.taskManager?.startTask(task)
                })
            }
            EnRouteConstants.PHASE_PROPERTY_PRE(),
            EnRouteConstants.PHASE_PROPERTY_ETA(),
            EnRouteConstants.PHASE_PROPERTY_SCHEDULED(),
            EnRouteConstants.PHASE_PROPERTY_QUASI(),
            EnRouteConstants.PHASE_PROPERTY_READY(),
            EnRouteConstants.PHASE_PROPERTY_NOT_COMPLETED() -> {
                add(ActionItem(TaskAction.live, "Start Tracking") {
                    EnRouteWrapper.enroute?.taskManager?.setTaskPhase(task, EnRouteConstants.PHASE_PROPERTY_LIVE())
                })
            }
            EnRouteConstants.PHASE_PROPERTY_LIVE() -> {
                add(ActionItem(TaskAction.arrive, "Mark Arrived") {
                    EnRouteWrapper.enroute?.taskManager?.setTaskPhase(task, EnRouteConstants.PHASE_PROPERTY_ARRIVED())
                })
                add(ActionItem(TaskAction.pause, "Pause Task") {
                    EnRouteWrapper.enroute?.taskManager?.setTaskPhase(task, EnRouteConstants.PHASE_PROPERTY_NOT_COMPLETED())
                })
                add(ActionItem(TaskAction.complete, "Complete Task", isDestructive = true) {
                    onConfirmComplete(task)
                })
            }
            EnRouteConstants.PHASE_PROPERTY_ARRIVED() -> {
                add(ActionItem(TaskAction.complete, "Complete Task", isDestructive = true) {
                    onConfirmComplete(task)
                })
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Task #${task.id}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            actions.forEachIndexed { index, action ->
                val textColor = if (action.isDestructive) {
                    Color.Red
                } else {
                    MaterialTheme.colorScheme.primary
                }

                TextButton(
                    onClick = {
                        action.onClick()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = action.title,
                        color = textColor,
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 18.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (index < actions.size - 1) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }
        }
    }
}
