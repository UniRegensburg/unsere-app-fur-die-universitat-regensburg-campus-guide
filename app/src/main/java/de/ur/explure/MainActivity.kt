package de.ur.explure

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.onNavDestinationSelected
import com.crazylegend.viewbinding.viewBinder
import de.ur.explure.databinding.ActivityMainBinding
import de.ur.explure.navigation.MainAppRouter
import de.ur.explure.navigation.StateAppRouter
import de.ur.explure.viewmodel.StateViewModel
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Main activity of the single activity application.
 */
class MainActivity : AppCompatActivity() {

    // Uses this library to reduce viewbinding boilerplate code: https://github.com/FunkyMuse/KAHelpers/tree/master/viewbinding
    private val activityMainBinding by viewBinder(ActivityMainBinding::inflate)

    private val stateViewModel: StateViewModel by viewModel()
    private val mainAppRouter: MainAppRouter by inject()
    private val stateAppRouter: StateAppRouter by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(activityMainBinding.root)

        setupNavController()
        startObservingAuthState()
    }

    private fun setupNavController() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_state_container) as NavHostFragment
        stateAppRouter.initializeNavController(navHostFragment.navController)
    }

    private fun startObservingAuthState() {
        stateViewModel.observeAuthState(this)
    }

    /**
     * Handle clicks on the Up-Buttons.
     */

    override fun onSupportNavigateUp(): Boolean {
        return mainAppRouter.navigateUp() ||
                stateAppRouter.navigateUp() ||
                return super.onSupportNavigateUp()
    }

    /**
     * Handle clicks on the menu items.
     */

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val controller =
            mainAppRouter.getNullableNavController()
                ?: return super.onOptionsItemSelected(item)
        return item.onNavDestinationSelected(controller)
    }
}
