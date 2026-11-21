package com.punypunypublisher.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.punypunypublisher.data.model.Post

/** Tab 1: unpublished posts; the top one goes out next. Tap to edit. */
@Composable
fun UpcomingView(posts: List<Post>, loaded: Boolean, onEdit: (Post) -> Unit) {
    when {
        !loaded -> Centered("Loading posts from GitHub…")
        posts.isEmpty() -> Centered("Nothing scheduled. Tap + to write a post.")
        else -> PostList(posts) { post ->
            PostCard(post, onClick = { onEdit(post) })
        }
    }
}

/** Tab 2: posts the workflow has actually published, newest first. Read-only. */
@Composable
fun PublishedView(posts: List<Post>, loaded: Boolean) {
    val uriHandler = LocalUriHandler.current
    when {
        !loaded -> Centered("Loading posts from GitHub…")
        posts.isEmpty() -> Centered("Nothing published yet.")
        else -> PostList(posts) { post ->
            PostCard(post, onClick = null) {
                post.statusUrl?.let { url ->
                    Text(
                        url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.padding(top = 4.dp).clickable { uriHandler.openUri(url) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PostList(posts: List<Post>, row: @Composable (Post) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(posts, key = { it.id }) { row(it) }
    }
}

@Composable
private fun PostCard(post: Post, onClick: (() -> Unit)?, extra: @Composable () -> Unit = {}) {
    val base = Modifier.fillMaxWidth()
    Card(modifier = if (onClick != null) base.clickable(onClick = onClick) else base) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                post.text,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 8,
                overflow = TextOverflow.Ellipsis,
            )
            extra()
        }
    }
}

@Composable
private fun Centered(message: String) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
    }
}
