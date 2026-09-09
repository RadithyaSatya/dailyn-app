package com.tara.dailyn.ui.features.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tara.dailyn.data.preferences.AppSettings

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onManageCategoriesClick: () -> Unit
) {
    val context = LocalContext.current.applicationContext
    val appSettings = remember { AppSettings(context) }
    val allowPreviousDayEdits = appSettings.allowPreviousDayEdits.collectAsStateWithLifecycle().value

    SettingsScreen(
        onBack = onBack,
        onManageCategoriesClick = onManageCategoriesClick,
        allowPreviousDayEdits = allowPreviousDayEdits,
        onAllowPreviousDayEditsChange = appSettings::setAllowPreviousDayEdits
    )
}
