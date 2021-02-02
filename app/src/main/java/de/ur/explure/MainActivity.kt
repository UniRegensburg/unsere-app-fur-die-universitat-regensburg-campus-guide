package de.ur.explure

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.navigation.NavController
import androidx.navigation.ui.AppBarConfiguration
import de.ur.explure.extensions.setupWithNavController
import de.ur.explure.viewmodel.MainViewModel
import kotlinx.android.synthetic.main.activity_main.*
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Main activity of the single activity application.
 */

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private val viewModel: MainViewModel by viewModel()

    private val appBarConfiguration by lazy { AppBarConfiguration(navGraphDestinations) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // only setup bottom navigation and toolbar if the activity is created from scratch
        if (savedInstanceState == null) {
            setupBottomNavigation()
            setupToolbar()
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // restore the old state
        setupBottomNavigation()
        setupToolbar()
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setLogo(R.drawable.ic_home)
    }

    /**
     * Sets up the bottom navigation bar with multiple navigation graphs.
     * Called on first creation and when restoring state.
     */
    private fun setupBottomNavigation() {
        val controller = bottom_nav.setupWithNavController(
            navGraphIds = navGraphIds,
            fragmentManager = supportFragmentManager,
            containerId = R.id.nav_host_container,
            intent = intent
        )
        observeController(controller)
    }

    /**
     * Adjusts the toolbar whenever the selected controller changes and updates the current nav controller.
     * @param [LiveData]<[NavController]> object with the new navigation controller which should be observed
     */
    private fun observeController(controller: LiveData<NavController>) {
        controller.observe(this, { navController ->
            viewModel.initializeNavController(navController)
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        return viewModel.navigateUp()
    }

    companion object {

        // List of navigation graphs used in the bottom navigation of the app
        val navGraphIds = listOf(
            R.navigation.nav_graph_discover,
            R.navigation.nav_graph_search,
            R.navigation.nav_graph_profile
        )

        // The top level views of the app
        val navGraphDestinations = setOf(
            R.id.discoverFragment,
            R.id.searchFragment,
            R.id.profileFragment
        )
    }
}
