package com.punypunypublisher.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.punypunypublisher.github.RepoRef
import com.punypunypublisher.posts.PostRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Where posts.json lives on GitHub, the token to reach it, and the post length limit. */
data class Settings(
    val owner: String = DEFAULT_OWNER,
    val repo: String = DEFAULT_REPO,
    val branch: String = DEFAULT_BRANCH,
    val path: String = DEFAULT_PATH,
    val token: String = "",
    val maxChars: Int = PostRules.DEFAULT_MAX_CHARS,
) {
    val isConfigured: Boolean
        get() = owner.isNotBlank() && repo.isNotBlank() && branch.isNotBlank() &&
            path.isNotBlank() && token.isNotBlank()

    fun repoRef() = RepoRef(owner.trim(), repo.trim(), branch.trim(), path.trim(), token.trim())

    companion object {
        const val DEFAULT_OWNER = "beyerl"
        const val DEFAULT_REPO = "punypunypublisher"
        const val DEFAULT_BRANCH = "main"
        const val DEFAULT_PATH = "posts.json"
    }
}

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            owner = prefs[KEY_OWNER] ?: Settings.DEFAULT_OWNER,
            repo = prefs[KEY_REPO] ?: Settings.DEFAULT_REPO,
            branch = prefs[KEY_BRANCH] ?: Settings.DEFAULT_BRANCH,
            path = prefs[KEY_PATH] ?: Settings.DEFAULT_PATH,
            token = prefs[KEY_TOKEN_ENC]?.let { TokenCipher.decrypt(it) }.orEmpty(),
            maxChars = prefs[KEY_MAX_CHARS] ?: PostRules.DEFAULT_MAX_CHARS,
        )
    }

    suspend fun snapshot(): Settings = settings.first()

    suspend fun save(s: Settings) {
        context.dataStore.edit { prefs ->
            prefs[KEY_OWNER] = s.owner.trim()
            prefs[KEY_REPO] = s.repo.trim()
            prefs[KEY_BRANCH] = s.branch.trim()
            prefs[KEY_PATH] = s.path.trim()
            prefs[KEY_MAX_CHARS] = s.maxChars
            if (s.token.isBlank()) prefs.remove(KEY_TOKEN_ENC)
            else prefs[KEY_TOKEN_ENC] = TokenCipher.encrypt(s.token.trim())
        }
    }

    /**
     * Drop settings from the on-device publisher (≤ 0.1): the Mastodon token now lives only in
     * GitHub Actions secrets, and publish times are replaced by per-post dates.
     */
    suspend fun removeLegacyKeys() {
        context.dataStore.edit { prefs ->
            LEGACY_KEYS.forEach { prefs.remove(stringPreferencesKey(it)) }
        }
    }

    private companion object {
        val KEY_OWNER = stringPreferencesKey("github_owner")
        val KEY_REPO = stringPreferencesKey("github_repo")
        val KEY_BRANCH = stringPreferencesKey("github_branch")
        val KEY_PATH = stringPreferencesKey("github_path")
        val KEY_TOKEN_ENC = stringPreferencesKey("github_token_enc")
        val KEY_MAX_CHARS = intPreferencesKey("max_chars")
        val LEGACY_KEYS = listOf("instance_url", "access_token", "publish_times", "last_published_slot")
    }
}
