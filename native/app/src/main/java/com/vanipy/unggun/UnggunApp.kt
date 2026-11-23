package com.vanipy.unggun

import android.app.Application
import com.vanipy.unggun.data.AppStore
import com.vanipy.unggun.data.LogStore
import com.vanipy.unggun.data.Prefs
import com.vanipy.unggun.util.Notifier

class UnggunApp : Application() {
    override fun onCreate() {
        super.onCreate()
        LogStore.init(this)
        Prefs.init(this)
        AppStore.init(this)
        Notifier.createChannels(this)
        LogStore.log("app", "unggun ${BuildConfig.VERSION_NAME} started")
    }
}
