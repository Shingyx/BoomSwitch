package com.github.shingyx.boomswitch.data

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import com.github.shingyx.boomswitch.R

enum class AppColorTheme(
    val nightModeValue: Int,
    @StringRes val descriptionResId: Int
) {
    LIGHT(AppCompatDelegate.MODE_NIGHT_NO, R.string.light),
    DARK(AppCompatDelegate.MODE_NIGHT_YES, R.string.dark),
    DEFAULT(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, R.string.system_default);

    fun apply() {
        AppCompatDelegate.setDefaultNightMode(nightModeValue)
    }

    companion object {
        fun fromNightModeValue(nightModeValue: Int): AppColorTheme {
            return when (nightModeValue) {
                AppCompatDelegate.MODE_NIGHT_NO -> LIGHT
                AppCompatDelegate.MODE_NIGHT_YES -> DARK
                else -> DEFAULT
            }
        }
    }
}
