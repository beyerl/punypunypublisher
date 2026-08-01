package com.punypunypublisher.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.punypunypublisher.export.Exporter
import com.punypunypublisher.ui.components.PublishTimesEditor

@Composable
fun SettingsView(viewModel: AppViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()

    // Show the stored value until the user starts editing (null = untouched).
    var urlEdit by remember { mutableStateOf<String?>(null) }
    var tokenEdit by remember { mutableStateOf<String?>(null) }
    val url = urlEdit ?: settings.instanceUrl
    val token = tokenEdit ?: settings.accessToken

    LaunchedEffect(Unit) { viewModel.loadPublished() }

    val exportPosts = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/markdown"),
    ) { uri -> uri?.let { Exporter.writeTo(context, it, viewModel.postsText) } }

    val exportPublished = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/markdown"),
    ) { uri -> uri?.let { Exporter.writeTo(context, it, viewModel.publishedRawText) } }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Mastodon credentials", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = url,
            onValueChange = { urlEdit = it },
            label = { Text("Instance URL (e.g. https://mastodon.social)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = token,
            onValueChange = { tokenEdit = it },
            label = { Text("Access token") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = {
                viewModel.saveCredentials(url, token)
                Toast.makeText(context, "Credentials saved", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Save credentials") }

        HorizontalDivider()

        Text("Publish times (weekly, UTC)", style = MaterialTheme.typography.titleMedium)
        PublishTimesEditor(
            times = settings.publishTimes,
            onAdd = { added ->
                if (settings.publishTimes.none { it == added }) {
                    viewModel.savePublishTimes(settings.publishTimes + added)
                }
            },
            onRemove = { removed ->
                viewModel.savePublishTimes(settings.publishTimes.filterNot { it == removed })
            },
            modifier = Modifier.fillMaxWidth(),
        )

        HorizontalDivider()

        Text("Actions", style = MaterialTheme.typography.titleMedium)
        OutlinedButton(
            onClick = {
                viewModel.publishNow()
                Toast.makeText(context, "Publishing next queued post…", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Publish next now") }

        HorizontalDivider()

        Text("Export", style = MaterialTheme.typography.titleMedium)
        OutlinedButton(
            onClick = { exportPosts.launch("posts.md") },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Export posts.md") }
        OutlinedButton(
            onClick = { exportPublished.launch("published.md") },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Export published.md") }
    }
}
