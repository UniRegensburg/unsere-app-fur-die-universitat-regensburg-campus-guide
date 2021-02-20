package de.ur.explure

import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.onNavDestinationSelected
import de.ur.explure.viewmodel.BottomNavViewModel
import de.ur.explure.viewmodel.StateViewModel
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

    private val stateViewModel: StateViewModel by viewModel()
    private val bottomNavViewModel: BottomNavViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(activityMainBinding.root)

        setupNavController()
        startObservingAuthState()
    }

    private fun setupNavController() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_state_container) as NavHostFragment
        stateViewModel.initializeStateNavController(navHostFragment.navController)
    }

    private fun startObservingAuthState() {
        stateViewModel.observeAuthState(this)
    }

    /**
     * Handle clicks on the Up-Buttons.
     */

    override fun onSupportNavigateUp(): Boolean {
        return bottomNavViewModel.navigateUp() ||
                stateViewModel.navigateUp() ||
                return super.onSupportNavigateUp()
    }

    /**
     * Handle clicks on the menu items.
     */

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val controller =
            bottomNavViewModel.currentNavController?.value
                ?: return super.onOptionsItemSelected(item)
        return item.onNavDestinationSelected(controller)
    }
}
