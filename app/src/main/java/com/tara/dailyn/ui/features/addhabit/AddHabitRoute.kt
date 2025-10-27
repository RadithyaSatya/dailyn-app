// ui/features/addhabit/AddHabitRoute.kt
package com.tara.dailyn.ui.features.addhabit

import AddHabitViewModel
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tara.dailyn.data.local.db.AppDatabase
import com.tara.dailyn.data.repository.HabitRepository
import com.tara.dailyn.ui.features.addhabit.model.AddHabitEffect
import kotlinx.coroutines.flow.collectLatest
import com.tara.dailyn.R

@Composable
fun AddHabitRoute(
    onSaved: (String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current.applicationContext
    val db = remember { AppDatabase.get(context) }
    val repo = remember { HabitRepository(db.habitDao(), db.habitLogDao()) }

    val vm: AddHabitViewModel = viewModel(
        factory = remember(repo) { AddHabitViewModelFactory(repo) }
    )

    val state = vm.state.collectAsStateWithLifecycle().value

    LaunchedEffect(Unit) {
        vm.effects.collectLatest { eff ->
            when (eff) {
                is AddHabitEffect.Saved -> onSaved(eff.id)
                AddHabitEffect.Cancelled -> onCancel()
                AddHabitEffect.ValidationError -> {
                    Toast.makeText(
                        context,
                        context.getString(R.string.validation_error_message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    AddHabitScreen(
        state = state,
        onEvent = vm::onEvent
    )
}
