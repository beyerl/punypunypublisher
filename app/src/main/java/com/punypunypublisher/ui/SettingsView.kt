package com.punypunypublisher.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.punypunypublisher.data.Settings

@Composable
fun SettingsView(viewModel: AppViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val stored by viewModel.settings.collectAsState()

    // Local edits; null = untouched, show the stored value.
    var owner by remember { mutableStateOf<String?>(null) }
    var repo by remember { mutableStateOf<String?>(null) }
    var branch by remember { mutableStateOf<String?>(null) }
    var path by remember { mutableStateOf<String?>(null) }
    var token by remember { mutableStateOf<String?>(null) }
    var maxChars by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("GitHub repository", style = MaterialTheme.typography.titleMedium)
        Text(
            "Posts are stored in a JSON file in this repo. A GitHub Actions workflow there " +
                "publishes them to Mastodon; the Mastodon token lives only in the repo's Actions secrets.",
            style = MaterialTheme.typography.bodySmall,
        )
        Field("Owner", owner ?: stored.owner) { owner = it }
        Field("Repository", repo ?: stored.repo) { repo = it }
        Field("Branch", branch ?: stored.branch) { branch = it }
        Field("File path", path ?: stored.path) { path = it }
        OutlinedTextField(
            value = token ?: stored.token,
            onValueChange = { token = it },
            label = { Text("Fine-grained personal access token") },
            supportingText = { Text("Contents: Read and write, this repo only. Stored encrypted.") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = maxChars ?: stored.maxChars.toString(),
            onValueChange = { maxChars = it.filter(Char::isDigit) },
            label = { Text("Max characters per post (your instance's limit)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = {
                val new = Settings(
                    owner = owner ?: stored.owner,
                    repo = repo ?: stored.repo,
                    branch = branch ?: stored.branch,
                    path = path ?: stored.path,
                    token = token ?: stored.token,
                    maxChars = (maxChars ?: stored.maxChars.toString()).toIntOrNull()?.takeIf { it > 0 }
                        ?: stored.maxChars,
                )
                viewModel.saveSettings(new) {
                    Toast.makeText(context, "Saved. Loading posts…", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Save and connect") }

        viewModel.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (viewModel.snapshot != null && viewModel.error == null) {
            Text(
                "Connected: ${viewModel.snapshot?.posts?.size ?: 0} posts in ${stored.path}.",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        if (viewModel.legacyAvailable) {
            HorizontalDivider()
            Text("Old on-device queue", style = MaterialTheme.typography.titleMedium)
            Text(
                "This phone still has posts.md / published.md from the previous version. Import " +
                    "them into posts.json: queued lines become weekly upcoming posts, history " +
                    "becomes published posts. Already-present texts are skipped.",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedButton(
                enabled = stored.isConfigured && viewModel.snapshot != null && !viewModel.loading,
                onClick = {
                    viewModel.importLegacy { problem ->
                        Toast.makeText(context, problem ?: "Imported.", Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Import old posts") }
        }
    }
}

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}
