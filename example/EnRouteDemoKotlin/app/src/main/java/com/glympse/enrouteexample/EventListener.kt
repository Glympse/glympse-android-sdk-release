package com.glympse.enrouteexample

import com.glympse.android.toolbox.listener.GListener
import com.glympse.android.toolbox.listener.GSource
import com.glympse.enroute.android.api.EnRouteConstants
import com.glympse.enroute.android.api.EnRouteEvents
import com.glympse.enroute.android.api.GTask
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

object EventListener : GListener {
    private val _firstStarted = MutableStateFlow(false)
    val firstStarted: Flow<Boolean> = _firstStarted
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: Flow<Boolean> = _isLoggedIn
    private val _isLoginFailed = MutableStateFlow(false)
    val isLoginFailed: Flow<Boolean> = _isLoginFailed
    private val _areTasksSynced = MutableStateFlow(false)
    val areTasksSynced = _areTasksSynced
    private val _tasks = MutableStateFlow<List<TaskState>>(emptyList())
    val tasks: Flow<List<TaskState>> = _tasks

    private fun updateTaskList() {
        val tasks = EnRouteWrapper.enroute?.taskManager?.tasks?.toList() ?: emptyList()
        _tasks.value = tasks.map { TaskState(it) }
    }

    override fun eventsOccurred(
        source: GSource?,
        listener: Int,
        events: Int,
        param1: Any?,
        param2: Any?
    ) {
        if (listener == EnRouteEvents.LISTENER_ENROUTE_MANAGER) {
            if ((events and EnRouteEvents.ENROUTE_MANAGER_LOGIN_COMPLETED) != 0) {
                _isLoggedIn.value = true
            }
            if ((events and EnRouteEvents.ENROUTE_MANAGER_LOGGED_OUT) != 0) {
                val reasonCode = param1 as Long
                if (reasonCode.toInt() == EnRouteConstants.LOGOUT_REASON_INVALID_CREDENTIALS) {
                    _isLoginFailed.value = true
                }
                _isLoggedIn.value = false
            }
            if ((events and EnRouteEvents.ENROUTE_MANAGER_SYNCED) != 0) {
                _isLoggedIn.value = true
                EnRouteWrapper.enroute?.taskManager?.addListener(this)
            }
            if ((events and EnRouteEvents.ENROUTE_MANAGER_STARTED) != 0) {
                _isLoginFailed.value = false
                _isLoggedIn.value = EnRouteWrapper.enroute?.isLoginNeeded == false
                _firstStarted.value = true
            }
            if ((events and EnRouteEvents.ENROUTE_MANAGER_STOPPED) != 0) {
                _areTasksSynced.value = false
            }
        } else if (listener == EnRouteEvents.LISTENER_TASKS) {
            if ((events and EnRouteEvents.TASKS_TASK_LIST_CHANGED) != 0) {
                updateTaskList()
                _areTasksSynced.value = true
            }
            if ((events and EnRouteEvents.TASKS_TASK_PHASE_CHANGED) != 0) {
                updateTaskList() // Can be more specific and just update the changed task, but for simplicity we refresh the whole list here
            }
        }
    }
}

data class TaskState(
    val task: GTask,
    val phase: String? = task.phase,
    val description: String? = task.description
)
