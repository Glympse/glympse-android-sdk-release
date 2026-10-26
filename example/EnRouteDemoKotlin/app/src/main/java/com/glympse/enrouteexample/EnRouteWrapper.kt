package com.glympse.enrouteexample

import android.content.Context
import com.glympse.enroute.android.api.EnRouteConstants
import com.glympse.enroute.android.api.EnRouteFactory
import com.glympse.enroute.android.api.GEnRouteManager
import java.util.Date

object EnRouteWrapper {
    var enroute: GEnRouteManager? = null
        private set

    private var isForeground = false

    fun initAndStart(context: Context) {
        // A started manager is reused. A stopped one (after logout) cannot be restarted, so it is replaced.
        if (enroute?.isStarted == true) {
            return
        }
        enroute = EnRouteFactory.createEnRouteManager(context)
        enroute?.overrideLoggingLevels(1, 1)
        enroute?.authenticationMode = EnRouteConstants.AUTH_MODE_CREDENTIALS
        enroute?.addListener(EventListener)
        enroute?.start()
        // setActive is reference counted and a new manager starts at zero, so mirror the current app state.
        if (isForeground) {
            enroute?.isActive = true
        }
    }

    // Call on foreground/background transitions only. Each true must be paired with one false.
    fun setForeground(foreground: Boolean) {
        if (foreground == isForeground) {
            return
        }
        isForeground = foreground
        enroute?.isActive = foreground
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

        // expiresIn is milliseconds from now. The SDK expects an absolute epoch time in milliseconds.
        val now = Date()
        val expireTime = Date(now.time + expiresIn)
        enroute?.loginWithToken(token, expireTime.time)
    }
}
