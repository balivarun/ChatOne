package com.connectchat.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.connectchat.data.api.model.User
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferences @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val USER_ID = stringPreferencesKey("user_id")
        val USER_DISPLAY_NAME = stringPreferencesKey("user_display_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_AVATAR_URL = stringPreferencesKey("user_avatar_url")
        val THEME = stringPreferencesKey("theme")
    }

    val accessToken: Flow<String?> = context.dataStore.data.map { it[ACCESS_TOKEN] }
    val refreshToken: Flow<String?> = context.dataStore.data.map { it[REFRESH_TOKEN] }
    val userId: Flow<String?> = context.dataStore.data.map { it[USER_ID] }
    val userDisplayName: Flow<String?> = context.dataStore.data.map { it[USER_DISPLAY_NAME] }
    val userEmail: Flow<String?> = context.dataStore.data.map { it[USER_EMAIL] }
    val userAvatarUrl: Flow<String?> = context.dataStore.data.map { it[USER_AVATAR_URL] }
    val theme: Flow<String> = context.dataStore.data.map { it[THEME] ?: "system" }

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN] = accessToken
            prefs[REFRESH_TOKEN] = refreshToken
        }
    }

    suspend fun saveUser(user: User) {
        context.dataStore.edit { prefs ->
            prefs[USER_ID] = user.id
            prefs[USER_DISPLAY_NAME] = user.displayName
            prefs[USER_EMAIL] = user.email
            user.avatarUrl?.let { prefs[USER_AVATAR_URL] = it }
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { it[THEME] = theme }
    }
}
