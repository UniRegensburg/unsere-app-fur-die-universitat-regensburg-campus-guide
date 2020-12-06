package de.ur.campusguide

import android.app.Application
import de.ur.campusguide.di.mainModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/**
 * Top level application used for global koin context
 *
 */

class CampusGuideApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@CampusGuideApp)
            modules(mainModule)
        }
    }
}