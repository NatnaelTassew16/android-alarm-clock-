package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    is24Hour: Boolean = false,
    onDismiss: () -> Unit,
    onTimeSelected: (hour: Int, minute: Int) -> Unit
) {
    var hour by remember { mutableIntStateOf(initialHour) }
    var minute by remember { mutableIntStateOf(initialMinute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onTimeSelected(hour, minute) }) {
                Text("OK", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = {
            Text(
                text = "Set Alarm Time",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Hour selector column
                    NumberColumn(
                        value = if (!is24Hour) {
                            val h = hour % 12
                            if (h == 0) 12 else h
                        } else hour,
                        range = if (is24Hour) 0..23 else 1..12,
                        onIncrement = {
                            hour = (hour + 1) % 24
                        },
                        onDecrement = {
                            hour = if (hour == 0) 23 else hour - 1
                        }
                    )

                    Text(
                        text = ":",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // Minute selector column
                    NumberColumn(
                        value = minute,
                        range = 0..59,
                        formatTwoDigits = true,
                        onIncrement = {
                            minute = (minute + 1) % 60
                        },
                        onDecrement = {
                            minute = if (minute == 0) 59 else minute - 1
                        }
                    )

                    if (!is24Hour) {
                        Spacer(modifier = Modifier.width(16.dp))
                        // AM / PM toggle
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val isAm = hour < 12
                            Surface(
                                selected = isAm,
                                onClick = {
                                    if (!isAm) hour -= 12
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isAm) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "AM",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAm) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Surface(
                                selected = !isAm,
                                onClick = {
                                    if (isAm) hour += 12
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (!isAm) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "PM",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isAm) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun NumberColumn(
    value: Int,
    range: IntRange,
    formatTwoDigits: Boolean = false,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(onClick = onIncrement) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Increase")
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(72.dp)
                    .height(64.dp)
            ) {
                Text(
                    text = if (formatTwoDigits) String.format("%02d", value) else value.toString(),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        IconButton(onClick = onDecrement) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Decrease")
        }
    }
}
