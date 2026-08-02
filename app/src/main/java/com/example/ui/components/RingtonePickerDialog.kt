package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class RingtoneItem(
    val name: String,
    val uri: String
)

@Composable
fun RingtonePickerDialog(
    currentRingtoneUri: String,
    onDismiss: () -> Unit,
    onRingtoneSelected: (name: String, uri: String) -> Unit
) {
    val presets = remember {
        listOf(
            RingtoneItem("Default Alarm Tone", "default"),
            RingtoneItem("Gentle Chime", "preset_gentle_chime"),
            RingtoneItem("Morning Bell", "preset_morning_bell"),
            RingtoneItem("Digital Pulse", "preset_digital_pulse"),
            RingtoneItem("Sunrise Melody", "preset_sunrise_melody"),
            RingtoneItem("Radar Echo", "preset_radar_echo"),
            RingtoneItem("Bright Morning", "preset_bright_morning")
        )
    }

    var selectedItem by remember {
        mutableStateOf(presets.find { it.uri == currentRingtoneUri } ?: presets.first())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onRingtoneSelected(selectedItem.name, selectedItem.uri)
            }) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MusicNote, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Choose Ringtone")
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(presets) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedItem = item }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (item.uri == selectedItem.uri),
                            onClick = { selectedItem = item }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    )
}
