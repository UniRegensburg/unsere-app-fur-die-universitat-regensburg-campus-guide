package de.ur.explure

import android.app.Application
import com.mapbox.mapboxsdk.Mapbox
import com.singhajit.sherlock.core.Sherlock
import de.ur.explure.di.mainModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber

/**
 * Top level application used for global koin context
 *
 */

@Suppress("Unused")
class ExplureApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Setup mapbox instance
        initMapbox()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())

            Sherlock.init(this)
        }

        // setup dependency injection
        startKoin {
            androidContext(this@ExplureApp)
            modules(mainModule)
        }
    }

    private fun initMapbox() {
        Mapbox.getInstance(this, getString(R.string.access_token))
    }
}
