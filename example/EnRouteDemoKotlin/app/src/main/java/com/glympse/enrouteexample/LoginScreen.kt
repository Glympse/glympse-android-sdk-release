package com.glympse.enrouteexample

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@Composable
fun LoginScreen(navController: NavController, loginViewModel: LoginViewModel = viewModel()) {
    val isLoginFailed by EventListener.isLoginFailed.collectAsState(initial = false)
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(isLoginFailed) {
        if (isLoginFailed) {
            isLoading = false
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Login", style = MaterialTheme.typography.headlineLarge)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = loginViewModel.username,
            onValueChange = { loginViewModel.username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = loginViewModel.password,
            onValueChange = { loginViewModel.password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                isLoading = true
                loginViewModel.login()
            },
            modifier = Modifier
                .fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text("Login")
        }
        if (isLoginFailed) {
            Text("Invalid Credentials",
                color = Color.Red)
        }
    }
}
