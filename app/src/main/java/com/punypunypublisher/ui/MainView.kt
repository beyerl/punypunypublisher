package com.punypunypublisher.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Upcoming tab: the whole queue as one text, one post per line, top line publishes next.
 * One Save button writes the queue to GitHub. The field takes the space left over by the
 * buttons, so the buttons stay visible, even with the keyboard open.
 */
@Composable
fun MainView(viewModel: AppViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var problem by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Each line is one Mastodon post. The top line publishes next.",
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = viewModel.queueText,
            onValueChange = {
                viewModel.onQueueTextChange(it)
                problem = null
            },
            label = { Text("Posts queue (${viewModel.queuedCount} queued)") },
            enabled = viewModel.snapshot != null,
            isError = problem != null,
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
        problem?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onClick = viewModel::refresh, enabled = !viewModel.loading) { Text("Reload") }
            Button(
                onClick = {
                    viewModel.saveQueue { result ->
                        problem = result
                        if (result == null) Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = !viewModel.loading && viewModel.snapshot != null,
                modifier = Modifier.weight(1f),
            ) { Text(if (viewModel.loading) "Saving…" else "Save") }
        }
    }
}
