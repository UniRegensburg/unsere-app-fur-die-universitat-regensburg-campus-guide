package de.ur.explure

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.onNavDestinationSelected
import de.ur.explure.viewmodel.BottomNavViewModel
import de.ur.explure.viewmodel.StateViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Main activity of the single activity application.
 */
class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private val stateViewModel: StateViewModel by viewModel()
    private val bottomNavViewModel: BottomNavViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
