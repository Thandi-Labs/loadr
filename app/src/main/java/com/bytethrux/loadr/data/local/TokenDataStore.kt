package com.bytethrux.loadr.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "loadr_prefs")

class TokenDataStore(private val context: Context) {

    companion object {
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val USERNAME_KEY = stringPreferencesKey("username")
    }

    val accessToken: Flow<String?> = context.dataStore.data
        .map { prefs -> prefs[ACCESS_TOKEN_KEY] }

    val username: Flow<String?> = context.dataStore.data
        .map { prefs -> prefs[USERNAME_KEY] }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN_KEY] = token
        }
    }

    suspend fun saveUsername(username: String) {
        context.dataStore.edit { prefs ->
            prefs[USERNAME_KEY] = username
        }
    }

    suspend fun clearToken() {
        context.dataStore.edit { prefs ->
            prefs.remove(ACCESS_TOKEN_KEY)
            prefs.remove(USERNAME_KEY)
        }
    }
}