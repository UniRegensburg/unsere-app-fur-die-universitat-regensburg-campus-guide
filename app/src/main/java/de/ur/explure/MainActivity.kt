package de.ur.explure

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.onNavDestinationSelected
import androidx.navigation.ui.setupWithNavController
import com.crazylegend.viewbinding.viewBinder
import de.ur.explure.databinding.ActivityMainBinding
import de.ur.explure.map.PermissionHelper
import de.ur.explure.navigation.MainAppRouter
import de.ur.explure.viewmodel.MainViewModel
import de.ur.explure.viewmodel.MapViewModel
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.androidx.viewmodel.scope.emptyState
import timber.log.Timber

/**
 * Main activity of the single activity application.
 */

class MainActivity : AppCompatActivity() {

    // Uses this library to reduce viewbinding boilerplate code: https://github.com/FunkyMuse/KAHelpers/tree/master/viewbinding
    private val activityMainBinding by viewBinder(ActivityMainBinding::inflate)

    private val mapViewModel: MapViewModel by viewModel(state = emptyState())
    private val mainViewModel: MainViewModel by viewModel()
    private val mainAppRouter: MainAppRouter by inject()

    private val navController: NavController by lazy { setupNavController() }
    private val appBarConfiguration by lazy { AppBarConfiguration(topLevelDestinations) }
    private var destinationChangeListener: NavController.OnDestinationChangedListener? = null

    private val permissionHelper: PermissionHelper by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(activityMainBinding.root)

        setupBottomNavigation()
        setupToolbar()
        setupViewModelObservers()

        // only set the auth state observer if the activity is created from scratch
        if (savedInstanceState == null) {
            mainViewModel.observeAuthState(this)
        }
    }

    /*
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // restore the old state
        setupBottomNavigation()
        setupToolbar()
    }*/

    private fun setupViewModelObservers() {
        // hide the bottom navigation bar when the user enters route creation mode on the map
        mapViewModel.inRouteCreationMode.observe(this) {
            hideBottomNavDuringRouteCreation(it)
        }
    }

    private fun hideBottomNavDuringRouteCreation(routeCreationActive: Boolean) {
        if (routeCreationActive) {
            activityMainBinding.bottomNav.visibility = View.GONE
        } else {
            activityMainBinding.bottomNav.visibility = View.VISIBLE
        }
    }

    private fun setupBottomNavigation() {
        activityMainBinding.bottomNav.setupWithNavController(navController)
    }

    private fun setupToolbar() {
        setSupportActionBar(activityMainBinding.toolbar)
        supportActionBar?.setLogo(R.drawable.ic_home)
        // link the toolbar with the navigation controller
        activityMainBinding.toolbar.setupWithNavController(navController, appBarConfiguration)
    }

    /**
     * Returns the navigation controller associated with this activity.
     * See https://stackoverflow.com/questions/59275009/fragmentcontainerview-using-findnavcontroller.
     */

    private fun setupNavController(): NavController {
        // This is a workaround for https://issuetracker.google.com/issues/142847973 until the
        // fragmentContainerView works correctly with the navigation component.
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_container) as NavHostFragment

        // set the nav controller in the main app router
        mainAppRouter.initializeNavController(navHostFragment.navController)
        return navHostFragment.navController
    }

    private fun setUpNavDestinationChangeListener() {
        destinationChangeListener =
            NavController.OnDestinationChangedListener { _, destination, _ ->
                // Hide the bottom navigation bar in all views except the top level ones.
                // Also hide the appbar in the login and register fragment as well.
                when (destination.id) {
                    in topLevelDestinations -> {
                        activityMainBinding.bottomNav.visibility = View.VISIBLE
                        activityMainBinding.appBar.visibility = View.VISIBLE
                    }
                    in authGraphDestinations -> {
                        activityMainBinding.appBar.visibility = View.GONE
                        activityMainBinding.bottomNav.visibility = View.GONE
                    }
                    else -> {
                        activityMainBinding.bottomNav.visibility = View.GONE
                    }
                }
            }
        navController.addOnDestinationChangedListener(destinationChangeListener ?: return)
    }

    /**
     * Handle clicks on the Up-Buttons.
     */

    override fun onSupportNavigateUp(): Boolean {
        return mainAppRouter.navigateUp() || return super.onSupportNavigateUp()
    }

    /**
     * Handle clicks on the menu items.
     */

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val controller =
            mainAppRouter.getNavController() ?: return super.onOptionsItemSelected(item)
        return item.onNavDestinationSelected(controller)
    }

    override fun onStart() {
        super.onStart()
        Timber.d("in MainActivity onStart")
        setUpNavDestinationChangeListener()
    }

    override fun onResume() {
        super.onResume()
        Timber.d("in MainActivity onResume")
        // ! This onResume is always called after a permission request in any of the fragments!
        //  -> because of this the navDestinationChangeListener MUST NOT be setup in here!
    }

    override fun onPause() {
        super.onPause()
        Timber.d("in MainActivity onPause")
    }

    override fun onStop() {
        Timber.d("in MainActivity onStop")
        // remove the destinationChangeListener to prevent memory leaks
        destinationChangeListener?.let {
            navController.removeOnDestinationChangedListener(it)
        }
        super.onStop()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionHelper.onRequestLocationPermissionsResult(requestCode, permissions, grantResults)
    }

    companion object {

        // The top level views of the app
        val topLevelDestinations = setOf(
            R.id.discoverFragment,
            R.id.mapFragment,
            R.id.profileFragment
        )

        // The views in the authentication process
        val authGraphDestinations = setOf(
            R.id.loginFragment,
            R.id.registerFragment
        )
    }
}
