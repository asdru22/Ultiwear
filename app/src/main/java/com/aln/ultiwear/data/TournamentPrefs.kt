package com.aln.ultiwear.data


import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// create a DataStore instance, called tournament_prefs
private val Context.dataStore by preferencesDataStore("tournament_prefs")

class TournamentPrefs(private val context: Context) {

    // define the keys to store in the DataStore
    companion object {
        val SELECTED_TOURNAMENT_NAME = stringPreferencesKey("selected_tournament_name")
        val SELECTED_TOURNAMENT_DATE = stringPreferencesKey("selected_tournament_date")
    }

    // context.dataStore.data is a Flow of preferences objects
    // for each object, map the name to the date
    // it emits the current preferences everytime something changes.
    val selectedTournament: Flow<Pair<String?, String?>> = context.dataStore.data.map { prefs ->
        prefs[SELECTED_TOURNAMENT_NAME] to prefs[SELECTED_TOURNAMENT_DATE]
    }

    // save the tournament
    suspend fun saveTournament(name: String, date: String) {
        context.dataStore.edit { prefs ->
            prefs[SELECTED_TOURNAMENT_NAME] = name
            prefs[SELECTED_TOURNAMENT_DATE] = date
        }
    }
}
