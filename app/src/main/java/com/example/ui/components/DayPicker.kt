package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class RepeatPreset {
    ONCE,
    EVERY_DAY,
    WEEKDAYS,
    WEEKENDS,
    CUSTOM
}

@Composable
fun DayPicker(
    selectedDays: List<Int>, // 1=Mon, 2=Tue, ..., 7=Sun
    onDaysSelected: (List<Int>) -> Unit,
    modifier: Modifier = Modifier
) {
    val daysMap = listOf(
        1 to "M",
        2 to "T",
        3 to "W",
        4 to "T",
        5 to "F",
        6 to "S",
        7 to "S"
    )

    val currentPreset = when (selectedDays.sorted()) {
        emptyList<Int>() -> RepeatPreset.ONCE
        listOf(1, 2, 3, 4, 5, 6, 7) -> RepeatPreset.EVERY_DAY
        listOf(1, 2, 3, 4, 5) -> RepeatPreset.WEEKDAYS
        listOf(6, 7) -> RepeatPreset.WEEKENDS
        else -> RepeatPreset.CUSTOM
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Repeat Days",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Presets row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = currentPreset == RepeatPreset.ONCE,
                onClick = { onDaysSelected(emptyList()) },
                label = { Text("Once") }
            )
            FilterChip(
                selected = currentPreset == RepeatPreset.EVERY_DAY,
                onClick = { onDaysSelected(listOf(1, 2, 3, 4, 5, 6, 7)) },
                label = { Text("Every day") }
            )
            FilterChip(
                selected = currentPreset == RepeatPreset.WEEKDAYS,
                onClick = { onDaysSelected(listOf(1, 2, 3, 4, 5)) },
                label = { Text("Weekdays") }
            )
            FilterChip(
                selected = currentPreset == RepeatPreset.WEEKENDS,
                onClick = { onDaysSelected(listOf(6, 7)) },
                label = { Text("Weekends") }
            )
        }

        // Days circle toggles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            daysMap.forEach { (dayIndex, dayLabel) ->
                val isSelected = selectedDays.contains(dayIndex)
                Surface(
                    onClick = {
                        val newDays = if (isSelected) {
                            selectedDays - dayIndex
                        } else {
                            (selectedDays + dayIndex).sorted()
                        }
                        onDaysSelected(newDays)
                    },
                    shape = CircleShape,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = dayLabel,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}
