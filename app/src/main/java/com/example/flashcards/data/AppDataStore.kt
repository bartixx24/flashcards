package com.example.flashcards.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private const val APP_PREFERENCES_NAME = "app_preferences"

private val Context.dataStore : DataStore<Preferences> by preferencesDataStore(name = APP_PREFERENCES_NAME)

class AppDataStore(context: Context) {

    /** sets sorting */
    private val SORT_OPTION = stringPreferencesKey("sort_option")

    suspend fun saveSortOptionToPreferencesDataStore(sortOption: String, context: Context) {
        context.dataStore.edit {preferences -> preferences[SORT_OPTION] = sortOption }
    }

    val sortOptionPreferencesFlow: Flow<String> = context.dataStore.data
        .catch {
            if(it is IOException) {
                it.printStackTrace()
                emit(emptyPreferences())
            }
            else throw it
        }
        .map { preferences -> preferences[SORT_OPTION] ?: "Original order" }

    /** flashcards sorting */
    private val SORT_FLASHCARDS_OPTION = stringPreferencesKey("sort_flashcards_option")

    suspend fun saveSortFlashcardsOptionToPreferencesDataStore(sortFlashcardsOption: String, context: Context) {
        context.dataStore.edit { preferences -> preferences[SORT_FLASHCARDS_OPTION] = sortFlashcardsOption }
    }

    val sortFlashcardsOptionPreferencesFlow: Flow<String> = context.dataStore.data
        .catch {
            if(it is IOException) {
                it.printStackTrace()
                emit(emptyPreferences())
            } else throw it
        }
        .map { preferences -> preferences[SORT_FLASHCARDS_OPTION] ?: "Original order" }

    /** app theme */
    private val THEME_OPTION = stringPreferencesKey("theme_option")

    suspend fun saveThemeOption(themeOption: String, context: Context) {
        context.dataStore.edit { preferences -> preferences[THEME_OPTION] = themeOption }
    }

    val themeOptionFlow: Flow<String> = context.dataStore.data
        .catch {
            if(it is IOException) {
                it.printStackTrace()
                emit(emptyPreferences())
            } else throw it
        }
        .map { preferences -> preferences[THEME_OPTION] ?: "light" }

}