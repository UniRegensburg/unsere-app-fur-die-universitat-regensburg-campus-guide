package de.ur.explure

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.ui.NavigationUI
import de.ur.explure.extensions.setupWithNavController
import de.ur.explure.viewmodel.MainViewModel
import kotlinx.android.synthetic.main.fragment_main.*
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Main activity of the single activity application.
 */

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            setupBottomNavigation()
        }
    }

    /**
     * Sets up the bottom navigation bar with multiple navigation graphs
     * Called on first creation and when restoring state.
     */

    private fun setupBottomNavigation() {
        val controller = bottom_nav.setupWithNavController(
            navGraphIds = navGraphIds,
            fragmentManager = supportFragmentManager,
            containerId = R.id.nav_host_fragment,
            intent = intent
        )
        observeController(controller)
        viewModel.setNavigationController(controller)
    }

    /**
     * Sets up the action bar whenever the selected controller changes
     *
     * @param [LiveData]<[NavController]> object with the new navigation controller which should be observed
     */

    private fun observeController(controller: LiveData<NavController>) {
        controller.observe(this, Observer { navController ->
            NavigationUI.setupActionBarWithNavController(
                this,
                navController
            )
        })
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        setupBottomNavigation()
    }

    override fun onSupportNavigateUp(): Boolean {
        return viewModel.navigateUp()
    }

    companion object {

        /**
         * List of navigation graphs used in the bottom navigation of the app
         */

        val navGraphIds = listOf(
            R.navigation.nav_graph_discover,
            R.navigation.nav_graph_search,
            R.navigation.nav_graph_profile
        )
    }
}
