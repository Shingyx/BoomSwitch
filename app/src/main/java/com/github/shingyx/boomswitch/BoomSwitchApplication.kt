package com.github.shingyx.boomswitch

import android.app.Application
import com.github.shingyx.boomswitch.data.Preferences

class BoomSwitchApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Preferences.initialize(this)
        Preferences.appColorTheme.apply()
    }
}
