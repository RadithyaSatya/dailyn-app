package com.tara.dailyn.ui.features.home.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@Composable
fun DateStrip(
    selectedDate: LocalDate,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }
    val days = remember { (-14..14).map { today.plusDays(it.toLong()) } }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = 14.coerceIn(0, days.lastIndex))

    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        items(days) { day ->
            DayChip(
                date = day,
                selected = day == selectedDate,
                onClick = { onSelect(day) }
            )
        }
    }
}

