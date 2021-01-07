package de.ur.explure

import android.app.Application
import de.ur.explure.di.mainModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber

/**
 * Top level application used for global koin context
 *
 */

class CampusGuideApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        startKoin {
            androidContext(this@CampusGuideApp)
            modules(mainModule)
        }
    }
}