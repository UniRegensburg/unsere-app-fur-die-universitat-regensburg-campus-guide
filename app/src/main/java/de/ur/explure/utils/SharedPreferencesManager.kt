package de.ur.explure.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import com.mapbox.mapboxsdk.maps.Style

// TODO use object instead?
@SuppressLint("CommitPrefEdits")
class SharedPreferencesManager constructor(context: Context) {

    private val sharedPreferences: SharedPreferences
    private val editor: SharedPreferences.Editor

    init {
        sharedPreferences =
            context.getSharedPreferences(PREFERENCE_CONFIGURATION_NAME, Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()
    }

    /**
     * Check if this is the first time the user launches the app.
     */
    fun isFirstRun() = sharedPreferences.getBoolean(FIRST_LAUNCH, true)

    fun completedFirstRun() {
        editor.putBoolean(FIRST_LAUNCH, false)
        editor.apply()
    }

    /**
     * Get the map style that the user had set the last time or the Mapbox Streets - Style as default.
     */
    fun getCurrentMapStyle() = sharedPreferences.getString(MAP_STYLE, Style.MAPBOX_STREETS)

    fun setCurrentMapStyle(mapStyle: String) {
        editor.putString(MAP_STYLE, mapStyle)
        editor.apply()
    }

    companion object {
        private const val PREFERENCE_CONFIGURATION_NAME = "configuration"
        private const val FIRST_LAUNCH = "isFirstRun"
        private const val MAP_STYLE = "currentMapStyle"
    }
}
