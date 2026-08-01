package com.punypunypublisher.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.punypunypublisher.data.model.PublishTime
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/**
 * Lists the configured weekly publish slots (each with its human label + cron string) and lets
 * the user add or remove them. The README asks for a cron-based, easy-to-read editor.
 */
@Composable
fun PublishTimesEditor(
    times: List<PublishTime>,
    onAdd: (PublishTime) -> Unit,
    onRemove: (PublishTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (times.isEmpty()) {
            Text(
                "No publish times yet. Posts will not be published until you add at least one.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        times.forEach { time ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
                        Text(time.humanLabel(), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "cron: ${time.cron()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { onRemove(time) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove ${time.humanLabel()}")
                    }
                }
            }
        }

        AddPublishTimeRow(onAdd = onAdd)
    }
}

@Composable
private fun AddPublishTimeRow(onAdd: (PublishTime) -> Unit) {
    var weekday by remember { mutableIntStateOf(1) }
    var hour by remember { mutableIntStateOf(9) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LabeledDropdown(
            label = "Day",
            options = (1..7).toList(),
            selected = weekday,
            optionLabel = { DayOfWeek.of(it).getDisplayName(TextStyle.FULL, Locale.ENGLISH) },
            onSelect = { weekday = it },
            modifier = Modifier.weight(1f),
        )
        LabeledDropdown(
            label = "Hour (UTC)",
            options = (0..23).toList(),
            selected = hour,
            optionLabel = { "%02d:00".format(it) },
            onSelect = { hour = it },
            modifier = Modifier.weight(1f),
        )
        OutlinedButton(onClick = { onAdd(PublishTime(weekday, hour)) }) {
            Text("Add")
        }
    }
}

@Composable
private fun <T> LabeledDropdown(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text("$label: ${optionLabel(selected)}")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}
