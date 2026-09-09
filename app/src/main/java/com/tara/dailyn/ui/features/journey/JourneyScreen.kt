@file:OptIn(ExperimentalMaterial3Api::class)

package com.tara.dailyn.ui.features.journey

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tara.dailyn.R
import com.tara.dailyn.ui.features.journey.model.JourneyDateSectionUi
import com.tara.dailyn.ui.features.journey.model.JourneyEventKind
import com.tara.dailyn.ui.features.journey.model.JourneyTimelineItemUi
import com.tara.dailyn.ui.features.journey.model.JourneyUiState
import com.tara.dailyn.ui.theme.AchievementGold
import com.tara.dailyn.ui.theme.BrandOrange
import com.tara.dailyn.ui.theme.BrandOrangeDeep
import com.tara.dailyn.ui.theme.BrandOrangeSecondary
import com.tara.dailyn.ui.theme.CompletedGreen
import com.tara.dailyn.ui.theme.ErrorRed
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun JourneyScreen(state: JourneyUiState) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.journey_title)) }
            )
        }
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            if (state.sections.isEmpty()) {
                item {
                    EmptyJourneyState()
                }
            } else {
                items(state.sections, key = { it.date.toString() }) { section ->
                    JourneyDateSection(section = section)
                }
            }
        }
    }
}

@Composable
private fun JourneyDateSection(section: JourneyDateSectionUi) {
    val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault())

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = section.date.format(dateFormatter),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        SummaryCard(
            title = section.summaryTitle,
            subtitle = section.summarySubtitle
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            section.items.forEach { item ->
                JourneyEventRow(item = item)
            }
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    subtitle: String
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(BrandOrangeDeep, BrandOrange, BrandOrangeSecondary)
                    )
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun JourneyEventRow(item: JourneyTimelineItemUi) {
    val accent = when (item.kind) {
        JourneyEventKind.COMPLETED -> CompletedGreen
        JourneyEventKind.MISSED -> ErrorRed
        JourneyEventKind.STREAK -> BrandOrange
        JourneyEventKind.ACHIEVEMENT -> AchievementGold
        JourneyEventKind.CHANGE -> MaterialTheme.colorScheme.secondary
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = if (item.isHighlighted) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        tonalElevation = if (item.isHighlighted) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(accent)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (item.isHighlighted) FontWeight.SemiBold else FontWeight.Medium
                )
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.accentLabel.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = item.accentLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = accent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyJourneyState() {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "\uD83D\uDC3B Kuma is waiting...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Complete your first habit to start your journey.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
