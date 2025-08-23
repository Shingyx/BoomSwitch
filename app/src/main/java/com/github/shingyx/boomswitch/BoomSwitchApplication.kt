package com.github.shingyx.boomswitch

import android.app.Application
import com.github.shingyx.boomswitch.data.Preferences
import timber.log.Timber
import timber.log.Timber.DebugTree

class BoomSwitchApplication : Application() {
  override fun onCreate() {
    super.onCreate()

    if (BuildConfig.DEBUG) {
      Timber.plant(DebugTree())
    }

    Preferences.initialize(this)
    Preferences.appColorTheme.apply()
  }
}
