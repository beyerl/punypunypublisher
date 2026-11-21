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
import com.punypunypublisher.data.model.Post
import com.punypunypublisher.github.GitHubClient
import com.punypunypublisher.github.PostRepository
import com.punypunypublisher.github.PostsSnapshot
import com.punypunypublisher.posts.Edit
import com.punypunypublisher.posts.EditException
import com.punypunypublisher.posts.PostRules
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val settingsStore = SettingsStore(app)
    private val legacyStore = MarkdownStore(app.filesDir)
    private val client = GitHubClient()
    private val repository = PostRepository(client)

    val settings: StateFlow<Settings> =
        settingsStore.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    var snapshot by mutableStateOf<PostsSnapshot?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var legacyAvailable by mutableStateOf(false)
        private set

    val upcoming: List<Post> get() = snapshot?.posts.orEmpty().filter { !it.published }.sortedBy { it.date }
    val published: List<Post> get() = snapshot?.posts.orEmpty().filter { it.published }.sortedByDescending { it.date }

    init {
        viewModelScope.launch {
            settingsStore.removeLegacyKeys()
            legacyAvailable = legacyStore.hasLegacyData()
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val s = settingsStore.snapshot()
            if (!s.isConfigured) {
                error = null
                return@launch
            }
            loading = true
            try {
                snapshot = repository.load(s.repoRef())
                error = null
            } catch (e: IOException) {
                error = e.message ?: e.toString()
            } finally {
                loading = false
            }
        }
    }

    /** Apply [edit] on GitHub; [onDone] receives null on success or an error message. */
    fun submit(edit: Edit, onDone: (String?) -> Unit) {
        viewModelScope.launch { onDone(runEdit(edit)) }
    }

    private suspend fun runEdit(edit: Edit): String? {
        val s = settingsStore.snapshot()
        if (!s.isConfigured) return "Set up the GitHub repository and token in Settings first."
        loading = true
        return try {
            snapshot = repository.apply(s.repoRef(), edit, s.maxChars, snapshot)
            error = null
            null
        } catch (e: EditException) {
            e.message
        } catch (e: IOException) {
            "Not saved (are you online?): ${e.message}"
        } finally {
            loading = false
        }
    }

    fun saveSettings(new: Settings, onDone: () -> Unit) {
        viewModelScope.launch {
            settingsStore.save(new)
            snapshot = null
            onDone()
            refresh()
        }
    }

    fun importLegacy(onDone: (String?) -> Unit) {
        viewModelScope.launch {
            val queued = MarkdownFormat.parsePosts(legacyStore.readPostsText())
            val history = legacyStore.readPublished()
            val result = runEdit(Edit.ImportLegacy(queued, history, PostRules.today()))
            if (result == null) {
                legacyStore.markImported()
                legacyAvailable = false
            }
            onDone(result)
        }
    }

    override fun onCleared() {
        client.close()
    }
}
