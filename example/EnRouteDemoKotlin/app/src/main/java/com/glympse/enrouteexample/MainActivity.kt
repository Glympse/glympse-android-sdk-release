package com.glympse.enrouteexample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.glympse.enrouteexample.permissions.LocationAuthView
import com.glympse.enrouteexample.permissions.NotificationAuthView


class MainActivity : ComponentActivity() {
    private val lifecycleObserver = AppLifecycleObserver()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EnRouteWrapper.initAndStart(this)
        lifecycle.addObserver(lifecycleObserver)
        lifecycleObserver.onForeground = {
            EnRouteWrapper.enroute?.isActive = true
        }
        lifecycleObserver.onBackground = {
            EnRouteWrapper.enroute?.isActive = false
        }
        enableEdgeToEdge()
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController: NavHostController = rememberNavController()

    // Setup Nav graph
    NavHost(
        navController = navController,
        startDestination = AppDestinations.LOADING_ROUTE) {
        composable(route = AppDestinations.LOADING_ROUTE) {
            LoadingScreen()
        }
        composable(route = AppDestinations.LOGIN_ROUTE) {
            LoginScreen(navController)
        }
        composable(route = AppDestinations.TASKS_ROUTE) {
            TasksScreen(navController)
        }
        composable(route = AppDestinations.PERMISSIONS_ROUTE) {
            PermissionsScreen(navController)
        }
        composable(route = AppDestinations.TASK_DETAIL_ROUTE) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId") ?: ""
            TaskDetailsScreen(navController, taskId)
        }
        composable(route = AppDestinations.ORG_INFO_ROUTE) {
            OrgInfoScreen(navController)
        }
        composable(route = AppDestinations.AGENT_INFO_ROUTE) {
            AgentInfoScreen(navController)
        }
    }

    val isLoggedIn by EventListener.isLoggedIn.collectAsState(initial = false)
    val firstStarted by EventListener.firstStarted.collectAsState(initial = false)
    LaunchedEffect(navController, isLoggedIn, firstStarted) {
        if (!firstStarted) {
            // Continue loading
        } else if (isLoggedIn) {
            navController.navigateToRouteAsRoot(AppDestinations.TASKS_ROUTE)
        } else {
            navController.navigateToRouteAsRoot(AppDestinations.LOGIN_ROUTE)
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Permissions") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LocationAuthView()
            NotificationAuthView()
        }
    }
}

fun NavController.navigateToRouteAsRoot(route: String) {
    navigate(route) {
        popUpTo(graph.id) {
            inclusive = true
        }
        launchSingleTop = true
    }
}

object AppDestinations {
    const val LOADING_ROUTE = "loading"
    const val PERMISSIONS_ROUTE = "permissions"
    const val LOGIN_ROUTE = "login"
    const val TASKS_ROUTE = "tasks"
    const val TASK_DETAIL_ROUTE = "task/{taskId}"
    const val ORG_INFO_ROUTE = "org"
    const val AGENT_INFO_ROUTE = "agent"
}

class AppLifecycleObserver : DefaultLifecycleObserver {
    var onForeground: (() -> Unit)? = null
    var onBackground: (() -> Unit)? = null

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        onForeground?.invoke()
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        onBackground?.invoke()
    }
}
