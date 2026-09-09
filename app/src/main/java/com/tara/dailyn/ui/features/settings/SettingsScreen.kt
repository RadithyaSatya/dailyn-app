package com.tara.dailyn.ui.features.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tara.dailyn.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onManageCategoriesClick: () -> Unit,
    allowPreviousDayEdits: Boolean,
    onAllowPreviousDayEditsChange: (Boolean) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.edit_previous_days_title)) },
                    supportingContent = {
                        Text(stringResource(R.string.edit_previous_days_description))
                    },
                    trailingContent = {
                        Switch(
                            checked = allowPreviousDayEdits,
                            onCheckedChange = onAllowPreviousDayEditsChange
                        )
                    }
                )
            }
            item {
                ListItem(
                    modifier = Modifier.clickable(onClick = onManageCategoriesClick),
                    headlineContent = { Text(stringResource(R.string.manage_categories_title)) },
                    supportingContent = {
                        Text(stringResource(R.string.manage_categories_description))
                    },
                    trailingContent = {
                        Icon(
                            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null
                        )
                    }
                )
            }
        }
    }
}
