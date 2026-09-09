package com.tara.dailyn.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** App-wide preferences that affect how a user records habit progress. */
class AppSettings(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    private val _allowPreviousDayEdits = MutableStateFlow(
        preferences.getBoolean(KEY_ALLOW_PREVIOUS_DAY_EDITS, false)
    )
    val allowPreviousDayEdits: StateFlow<Boolean> = _allowPreviousDayEdits.asStateFlow()

    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == KEY_ALLOW_PREVIOUS_DAY_EDITS) {
            _allowPreviousDayEdits.value = preferences.getBoolean(key, false)
        }
    }

    init {
        preferences.registerOnSharedPreferenceChangeListener(preferenceListener)
    }

    fun setAllowPreviousDayEdits(allowed: Boolean) {
        preferences.edit().putBoolean(KEY_ALLOW_PREVIOUS_DAY_EDITS, allowed).apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "dailyn_settings"
        private const val KEY_ALLOW_PREVIOUS_DAY_EDITS = "allow_previous_day_edits"
    }
}
