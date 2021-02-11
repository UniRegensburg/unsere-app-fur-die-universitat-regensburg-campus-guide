package de.ur.explure

import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.navigation.NavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.onNavDestinationSelected
import androidx.navigation.ui.setupWithNavController
import com.crazylegend.viewbinding.viewBinder
import de.ur.explure.databinding.ActivityMainBinding
import de.ur.explure.extensions.setupWithNavController
import de.ur.explure.viewmodel.MainViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

/**
 * Main activity of the single activity application.
 */
class MainActivity : AppCompatActivity() {

    // Uses this library to reduce viewbinding boilerplate code: https://github.com/FunkyMuse/KAHelpers/tree/master/viewbinding
    private val activityMainBinding by viewBinder(ActivityMainBinding::inflate)

    private val viewModel: MainViewModel by viewModel()

    private val appBarConfiguration by lazy { AppBarConfiguration(navGraphDestinations) }

    private var destinationChangeListener: NavController.OnDestinationChangedListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(activityMainBinding.root)

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
        setUpNavDestinationChangeListener()
    }

    override fun onStart() {
        super.onStart()
        setUpNavDestinationChangeListener()
    }

    private fun setupToolbar() {
        setSupportActionBar(activityMainBinding.toolbar)
        supportActionBar?.setLogo(R.drawable.ic_home)
    }

    /**
     * Sets up the bottom navigation bar with multiple navigation graphs.
     * Called on first creation and when restoring state.
     */
    private fun setupBottomNavigation() {
        val controller = activityMainBinding.bottomNav.setupWithNavController(
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
            activityMainBinding.toolbar.setupWithNavController(navController, appBarConfiguration)
            viewModel.initializeNavController(navController)
        })
        viewModel.setCurrentNavController(controller)
    }

    private fun setUpNavDestinationChangeListener() {
        destinationChangeListener =
            NavController.OnDestinationChangedListener { _, destination, _ ->
                // hide the bottom navigation bar in all views except the top level ones
                if (destination.id in navGraphDestinations) {
                    activityMainBinding.bottomNav.visibility = View.VISIBLE
                } else {
                    activityMainBinding.bottomNav.visibility = View.GONE
                }
            }

        viewModel.getCurrentNavController()?.observe(this) {
            val changeListener = destinationChangeListener ?: return@observe
            it.addOnDestinationChangedListener(changeListener)
        }
    }

    override fun onStop() {
        super.onStop()

        // remove the destinationChangeListener of the current nav controller to prevent memory leaks
        destinationChangeListener?.let {
            viewModel.getCurrentNavController()?.value?.removeOnDestinationChangedListener(it)
        }
        // also remove viewmodel observer and reset current nav controller
        viewModel.getCurrentNavController()?.removeObservers(this)
        viewModel.resetCurrentNavController()
    }

    /**
     * Handle clicks on the Up-Buttons.
     */
    override fun onSupportNavigateUp(): Boolean {
        return viewModel.navigateUp() || return super.onSupportNavigateUp()
    }

    /**
     * Handle clicks on the menu items.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val controller =
            viewModel.getCurrentNavController()?.value ?: return super.onOptionsItemSelected(item)
        return item.onNavDestinationSelected(controller)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Timber.w("Unhandled Configuration Change occured!")
    }

    override fun onDestroy() {
        super.onDestroy()
        // MapboxNavigationProvider.destroy()
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
