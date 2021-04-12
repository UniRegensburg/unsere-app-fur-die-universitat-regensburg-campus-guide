package de.ur.explure.utils

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.mapbox.mapboxsdk.maps.Style

@SuppressLint("CommitPrefEdits")
class SharedPreferencesManager constructor(context: Application) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFERENCE_CONFIGURATION_NAME, Context.MODE_PRIVATE)

    private val editor: SharedPreferences.Editor = sharedPreferences.edit()

    /**
     * Check if this is the first time the user launches the app.
     */
    fun isFirstRun() = sharedPreferences.getBoolean(FIRST_LAUNCH, true)

    fun completedFirstRun() {
        editor.putBoolean(FIRST_LAUNCH, false)
        editor.apply()
    }

    fun isFirstTimeRouteCreation() = sharedPreferences.getBoolean(FIRST_ROUTE_CREATION, true)

    fun completedRouteCreationTutorial() {
        editor.putBoolean(FIRST_ROUTE_CREATION, false)
        editor.apply()
    }

    fun isFirstTimeMapMatching() = sharedPreferences.getBoolean(FIRST_TIME_MAP_MATCHING, true)

    fun finishedMapMatchingExplanation() {
        editor.putBoolean(FIRST_TIME_MAP_MATCHING, false)
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

    fun getBuildingExtrusionShown() = sharedPreferences.getBoolean(BUILDING_EXTRUSION, true)

    fun setBuildingExtrusionShown(isActive: Boolean) {
        editor.putBoolean(BUILDING_EXTRUSION, isActive)
        editor.apply()
    }

    companion object {
        private const val PREFERENCE_CONFIGURATION_NAME = "configuration"
        private const val FIRST_LAUNCH = "isFirstRun"
        private const val FIRST_ROUTE_CREATION = "isFirstRouteCreation"
        private const val FIRST_TIME_MAP_MATCHING = "isFirstTimeMapMatching"
        private const val MAP_STYLE = "currentMapStyle"
        private const val BUILDING_EXTRUSION = "buildingExtrusionShown"
    }
}
