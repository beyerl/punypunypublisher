package com.punypunypublisher.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.punypunypublisher.data.MarkdownFormat
import com.punypunypublisher.data.MarkdownStore
import com.punypunypublisher.data.Settings
import com.punypunypublisher.data.SettingsStore
import com.punypunypublisher.data.model.PublishTime
import com.punypunypublisher.data.model.PublishedEntry
import com.punypunypublisher.scheduler.Scheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val settingsStore = SettingsStore(app)
    private val store = MarkdownStore(app.filesDir)

    val settings: StateFlow<Settings> =
        settingsStore.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Settings())

    /** Working copy of posts.md bound to the Main editor. */
    var postsText by mutableStateOf("")
        private set

    var publishedEntries by mutableStateOf<List<PublishedEntry>>(emptyList())
        private set

    /** Raw published.md text, kept for export. */
    var publishedRawText by mutableStateOf("")
        private set

    init {
        loadPosts()
        loadPublished()
    }

    fun loadPosts() {
        viewModelScope.launch { postsText = store.readPostsText() }
    }

    fun loadPublished() {
        viewModelScope.launch {
            publishedRawText = store.readPublishedText()
            publishedEntries = MarkdownFormat.parsePublished(publishedRawText)
        }
    }

    fun onPostsTextChange(text: String) {
        postsText = text
    }

    fun savePosts() {
        viewModelScope.launch { store.writePostsText(postsText) }
    }

    fun saveCredentials(instanceUrl: String, accessToken: String) {
        viewModelScope.launch { settingsStore.setCredentials(instanceUrl, accessToken) }
    }

    fun savePublishTimes(times: List<PublishTime>) {
        viewModelScope.launch { settingsStore.setPublishTimes(times) }
    }

    /** Flush editor contents, then enqueue an immediate publish of the next queued row. */
    fun publishNow() {
        viewModelScope.launch {
            store.writePostsText(postsText)
            Scheduler.publishNow(getApplication())
        }
    }
}
