package com.glympse.enrouteexample

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    var username by mutableStateOf("")
    var password by mutableStateOf("")

    fun login() {
        EnRouteWrapper.loginWithCredentials(application, username, password)
    }
}
