package com.github.shingyx.boomswitch.ui

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.github.shingyx.boomswitch.R

fun setSystemBarColors(activity: AppCompatActivity, rootLayout: View) {
  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
    return
  }
  // navigation bar visibility
  activity.window.isNavigationBarContrastEnforced = false
  val windowInsetsController = WindowCompat.getInsetsController(activity.window, rootLayout)
  windowInsetsController.isAppearanceLightNavigationBars = !isNightMode(activity)

  ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { view, windowInsets ->
    // ui margins
    val statusBarInsets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
    val navigationBarInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
    view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
      topMargin = statusBarInsets.top
      bottomMargin = navigationBarInsets.bottom
    }
    // status bar background
    val statusBarView =
      View(activity).apply {
        layoutParams =
          ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, statusBarInsets.top)
        setBackgroundColor(activity.getColor(R.color.toolbar_color))
      }
    activity.addContentView(statusBarView, statusBarView.layoutParams)

    WindowInsetsCompat.CONSUMED
  }
}

private fun isNightMode(context: Context): Boolean {
  val nightModeFlags = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
  return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
}
