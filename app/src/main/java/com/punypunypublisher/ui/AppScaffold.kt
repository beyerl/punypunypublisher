package com.punypunypublisher.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.punypunypublisher.data.model.Post

private enum class Tab(val title: String, val icon: ImageVector) {
    UPCOMING("Upcoming", Icons.Filled.Schedule),
    PUBLISHED("Published", Icons.Filled.CheckCircle),
    SETTINGS("Settings", Icons.Filled.Settings),
}

/** Editor target: a new post, or an existing upcoming one. */
private sealed interface Editing {
    data object New : Editing
    data class Existing(val post: Post) : Editing
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(viewModel: AppViewModel = viewModel()) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<Editing?>(null) }
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Tab.entries[tab].title) },
                actions = {
                    if (tab != Tab.SETTINGS.ordinal) {
                        IconButton(onClick = viewModel::refresh) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Reload from GitHub")
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (tab == Tab.UPCOMING.ordinal && settings.isConfigured && viewModel.snapshot != null) {
                FloatingActionButton(onClick = { editing = Editing.New }) {
                    Icon(Icons.Filled.Add, contentDescription = "New post")
                }
            }
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { entry ->
                    NavigationBarItem(
                        selected = tab == entry.ordinal,
                        onClick = { tab = entry.ordinal },
                        icon = { Icon(entry.icon, contentDescription = entry.title) },
                        label = { Text(entry.title) },
                    )
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (viewModel.loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            viewModel.error?.takeIf { tab != Tab.SETTINGS.ordinal }?.let { message ->
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = viewModel::refresh) { Text("Retry") }
                }
            }
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    tab == Tab.SETTINGS.ordinal -> SettingsView(viewModel)
                    !settings.isConfigured -> NotConfigured(onOpenSettings = { tab = Tab.SETTINGS.ordinal })
                    tab == Tab.UPCOMING.ordinal -> UpcomingView(
                        posts = viewModel.upcoming,
                        loaded = viewModel.snapshot != null,
                        onEdit = { editing = Editing.Existing(it) },
                    )
                    else -> PublishedView(viewModel.published, loaded = viewModel.snapshot != null)
                }
            }
        }
    }

    editing?.let { target ->
        val existing = (target as? Editing.Existing)?.post
        PostEditor(
            initial = existing,
            maxChars = settings.maxChars,
            busy = viewModel.loading,
            onSubmit = { edit, onResult ->
                viewModel.submit(edit) { problem ->
                    onResult(problem)
                    if (problem == null) editing = null
                }
            },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun NotConfigured(onOpenSettings: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(
            "Connect the app to the GitHub repository that holds posts.json.",
            style = MaterialTheme.typography.bodyLarge,
        )
        TextButton(onClick = onOpenSettings) { Text("Open Settings") }
    }
}
