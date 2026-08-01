package com.punypunypublisher.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.punypunypublisher.data.model.PublishTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** App configuration: Mastodon credentials, weekly publish slots, and the last consumed slot id. */
data class Settings(
    val instanceUrl: String = "",
    val accessToken: String = "",
    val publishTimes: List<PublishTime> = emptyList(),
) {
    val isConfigured: Boolean
        get() = instanceUrl.isNotBlank() && accessToken.isNotBlank()
}

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            instanceUrl = prefs[KEY_INSTANCE_URL].orEmpty(),
            accessToken = prefs[KEY_ACCESS_TOKEN].orEmpty(),
            publishTimes = prefs[KEY_PUBLISH_TIMES]?.let { decodeTimes(it) }.orEmpty(),
        )
    }

    suspend fun snapshot(): Settings = settings.first()

    suspend fun setCredentials(instanceUrl: String, accessToken: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_INSTANCE_URL] = instanceUrl.trim()
            prefs[KEY_ACCESS_TOKEN] = accessToken.trim()
        }
    }

    suspend fun setPublishTimes(times: List<PublishTime>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PUBLISH_TIMES] = json.encodeToString(times)
        }
    }

    suspend fun lastPublishedSlot(): String? =
        context.dataStore.data.first()[KEY_LAST_SLOT]

    suspend fun setLastPublishedSlot(slotId: String) {
        context.dataStore.edit { prefs -> prefs[KEY_LAST_SLOT] = slotId }
    }

    private fun decodeTimes(raw: String): List<PublishTime> =
        runCatching { json.decodeFromString<List<PublishTime>>(raw) }.getOrDefault(emptyList())

    private companion object {
        val KEY_INSTANCE_URL = stringPreferencesKey("instance_url")
        val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        val KEY_PUBLISH_TIMES = stringPreferencesKey("publish_times")
        val KEY_LAST_SLOT = stringPreferencesKey("last_published_slot")
    }
}
