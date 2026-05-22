package com.glympse.enrouteexample

import android.content.Context
import com.glympse.enroute.android.api.EnRouteConstants
import com.glympse.enroute.android.api.EnRouteFactory
import com.glympse.enroute.android.api.GEnRouteManager
import java.util.Date

object EnRouteWrapper {
    var enroute: GEnRouteManager? = null
        private set

    fun initAndStart(context: Context) {
        enroute = EnRouteFactory.createEnRouteManager(context)
        enroute?.overrideLoggingLevels(1, 1)
        enroute?.authenticationMode = EnRouteConstants.AUTH_MODE_CREDENTIALS
        enroute?.addListener(EventListener)
        enroute?.start()
        enroute?.isActive = true
    }

    fun loginWithCredentials(context: Context, username: String, password: String) {
        if (enroute?.isStarted != true) {
            initAndStart(context)
        }
        enroute?.loginWithCredentials(username, password)
    }

    fun loginWithToken(context: Context, token: String, expiresIn: Long) {
        if (enroute?.isStarted != true) {
            initAndStart(context)
        }

        val now = Date()
        val expireTime = Date(now.time + expiresIn)
        enroute?.loginWithToken(token, expireTime.time)
    }
}
