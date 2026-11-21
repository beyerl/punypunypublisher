package com.punypunypublisher.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

private enum class Tab(val title: String, val icon: ImageVector) {
    UPCOMING("Upcoming", Icons.Filled.Edit),
    PUBLISHED("Published", Icons.Filled.CheckCircle),
    SETTINGS("Settings", Icons.Filled.Settings),
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppScaffold(viewModel: AppViewModel = viewModel()) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text(Tab.entries[tab].title) }) },
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
        // Scaffold padding covers the bars; consuming it before imePadding() makes the content
        // shrink to exactly the space above the keyboard instead of sliding under it.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding(),
        ) {
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
                    tab == Tab.UPCOMING.ordinal -> MainView(viewModel)
                    else -> PublishedView(viewModel.published, loaded = viewModel.snapshot != null)
                }
            }
        }
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
