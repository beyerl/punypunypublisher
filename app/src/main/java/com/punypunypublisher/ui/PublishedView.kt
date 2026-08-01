package com.punypunypublisher.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.punypunypublisher.data.model.PublishedEntry
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val TIME_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC)

@Composable
fun PublishedView(entries: List<PublishedEntry>, modifier: Modifier = Modifier) {
    if (entries.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nothing published yet.", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }
    // Newest first.
    val ordered = entries.reversed()
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(ordered) { entry -> PublishedRow(entry) }
    }
}

@Composable
private fun PublishedRow(entry: PublishedEntry) {
    val uriHandler = LocalUriHandler.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(entry.text, style = MaterialTheme.typography.bodyLarge)
            Text(
                TIME_FORMAT.format(entry.publishedAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (entry.statusUrl.isNotBlank()) {
                Text(
                    entry.statusUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clickable { uriHandler.openUri(entry.statusUrl) },
                )
            }
        }
    }
}
