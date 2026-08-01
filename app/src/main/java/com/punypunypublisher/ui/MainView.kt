package com.punypunypublisher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.punypunypublisher.data.MarkdownFormat

@Composable
fun MainView(viewModel: AppViewModel, modifier: Modifier = Modifier) {
    val queued = MarkdownFormat.parsePosts(viewModel.postsText)
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Each line is one Mastodon post. The top line publishes next.",
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = viewModel.postsText,
            onValueChange = viewModel::onPostsTextChange,
            label = { Text("Posts queue (${queued.size} queued)") },
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onClick = { viewModel.loadPosts() }) { Text("Reload") }
            Button(onClick = { viewModel.savePosts() }, modifier = Modifier.weight(1f)) {
                Text("Save")
            }
        }
    }
}
