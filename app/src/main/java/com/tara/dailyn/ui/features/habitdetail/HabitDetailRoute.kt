// ui/features/habitdetail/HabitDetailRoute.kt
@file:OptIn(ExperimentalMaterial3Api::class)

package com.tara.dailyn.ui.features.habitdetail

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tara.dailyn.data.local.db.AppDatabase
import com.tara.dailyn.data.repository.HabitRepository
import com.tara.dailyn.ui.features.habitdetail.model.HabitDetailUi
import kotlinx.coroutines.launch
@Composable
fun HabitDetailRoute(
    habitId: String,
    onEdit: () -> Unit,
    onBack: () -> Unit,
    onDeleted: () -> Unit
) {
    val ctx = LocalContext.current.applicationContext
    val db = remember { AppDatabase.get(ctx) }
    val repo = remember { HabitRepository(db.habitDao(), db.habitLogDao()) }

    val vm: HabitDetailViewModel = viewModel(
        factory = remember(habitId) {
            object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(HabitDetailViewModel::class.java))
                    return HabitDetailViewModel(repo, habitId) as T
                }
            }
        }
    )

    val ui = vm.ui.collectAsStateWithLifecycle().value
    val scope = rememberCoroutineScope()

    HabitDetailScreen(
        ui = ui,
        onBack = onBack,
        onEdit = onEdit,
        onDeleteConfirm = {
            scope.launch {
                vm.deleteHabit()
                Toast.makeText(ctx, "Habit deleted", Toast.LENGTH_SHORT).show()
                onDeleted()
            }
        }
    )
}
