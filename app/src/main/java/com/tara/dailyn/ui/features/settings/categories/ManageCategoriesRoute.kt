package com.tara.dailyn.ui.features.settings.categories

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tara.dailyn.data.local.db.AppDatabase
import com.tara.dailyn.data.repository.HabitRepository

@Composable
fun ManageCategoriesRoute(
    onBack: () -> Unit
) {
    val context = LocalContext.current.applicationContext
    val db = remember { AppDatabase.get(context) }
    val repo = remember { HabitRepository(db.categoryDao(), db.habitDao(), db.habitLogDao(), context) }

    val vm: ManageCategoriesViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                ManageCategoriesViewModel(repo)
            }
        }
    )

    val state = vm.state.collectAsStateWithLifecycle().value
    ManageCategoriesScreen(
        state = state,
        onBack = onBack,
        onUpdateCategory = vm::updateCategory,
        onDeleteCategory = vm::deleteCategory,
        onErrorShown = vm::clearError
    )
}
