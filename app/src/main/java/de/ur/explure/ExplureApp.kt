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

class ExplureApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Setup mapbox instance
        initMapbox()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())

            Sherlock.init(this)
        }

        startKoin {
            androidContext(this@ExplureApp)
            modules(mainModule)
        }
    }

    private fun initMapbox() {
        Mapbox.getInstance(this, getString(R.string.access_token))

        /*
        MapboxNavigationProvider.create(
            MapboxNavigation.defaultNavigationOptionsBuilder(
                this,
                getString(R.string.access_token)
            ).build()
        )*/
    }
}
