package com.github.shingyx.boomswitch.data

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import com.github.shingyx.boomswitch.R

class AppColorTheme private constructor(
    val nightModeValue: Int,
    @StringRes val descriptionResId: Int
) {
    fun apply() {
        AppCompatDelegate.setDefaultNightMode(nightModeValue)
    }

    override fun equals(other: Any?): Boolean {
        return nightModeValue == (other as? AppColorTheme)?.nightModeValue
    }

    override fun hashCode(): Int {
        return nightModeValue
    }

    companion object {
        private val LIGHT = AppColorTheme(AppCompatDelegate.MODE_NIGHT_NO, R.string.light)
        private val DARK = AppColorTheme(AppCompatDelegate.MODE_NIGHT_YES, R.string.dark)
        private val DEFAULT = AppColorTheme(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, R.string.system_default)

        val LIST = listOf(LIGHT, DARK, DEFAULT)

        fun fromNightModeValue(nightModeValue: Int): AppColorTheme {
            return when (nightModeValue) {
                AppCompatDelegate.MODE_NIGHT_NO -> LIGHT
                AppCompatDelegate.MODE_NIGHT_YES -> DARK
                else -> DEFAULT
            }
        }
    }
}
