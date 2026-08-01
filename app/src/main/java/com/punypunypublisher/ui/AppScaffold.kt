package com.punypunypublisher.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel

private enum class Tab(val title: String, val icon: ImageVector) {
    COMPOSE("Compose", Icons.Filled.Edit),
    PUBLISHED("Published", Icons.Filled.CheckCircle),
    SETTINGS("Settings", Icons.Filled.Settings),
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(viewModel: AppViewModel = viewModel()) {
    var tab by rememberSaveable { mutableIntStateOf(0) }

    val select: (Int) -> Unit = { target ->
        if (tab == Tab.COMPOSE.ordinal && target != Tab.COMPOSE.ordinal) viewModel.savePosts()
        when (target) {
            Tab.COMPOSE.ordinal -> viewModel.loadPosts()
            Tab.PUBLISHED.ordinal -> viewModel.loadPublished()
        }
        tab = target
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(Tab.entries[tab].title) }) },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { entry ->
                    NavigationBarItem(
                        selected = tab == entry.ordinal,
                        onClick = { select(entry.ordinal) },
                        icon = { Icon(entry.icon, contentDescription = entry.title) },
                        label = { Text(entry.title) },
                    )
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                Tab.COMPOSE.ordinal -> MainView(viewModel)
                Tab.PUBLISHED.ordinal -> PublishedView(viewModel.publishedEntries)
                Tab.SETTINGS.ordinal -> SettingsView(viewModel)
            }
        }
    }
}
