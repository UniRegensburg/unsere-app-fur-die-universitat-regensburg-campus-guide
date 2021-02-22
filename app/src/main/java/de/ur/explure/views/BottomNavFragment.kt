package de.ur.explure.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import de.ur.explure.R
import de.ur.explure.viewmodel.BottomNavViewModel
import kotlinx.android.synthetic.main.bottom_nav_fragment.*

class BottomNavFragment : Fragment(R.layout.bottom_nav_fragment) {

    private val appBarConfiguration by lazy { AppBarConfiguration(navGraphDestinations) }

    private var destinationChangeListener: NavController.OnDestinationChangedListener? = null

    private val bottomNavViewModel: BottomNavViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = super.onCreateView(inflater, container, savedInstanceState)
        if (savedInstanceState == null) {
            setupToolbar()
        }
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) {
            val navController = getHostNavController()
            bottomNavViewModel.initNavController(navController)
            setUpNavDestinationChangeListener(navController)
            setupBottomNavigation(navController)
            linkNavControllerToToolbar(navController)
        }
    }

    private fun setupToolbar() {
        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.setLogo(R.drawable.ic_home)
    }

    /**
     * Sets up the bottom navigation bar with multiple navigation graphs.
     * Called on first creation and when restoring state.
     */

    private fun setupBottomNavigation(navController: NavController) {
        NavigationUI.setupWithNavController(
            bottom_nav,
            navController
        )
    }

    private fun linkNavControllerToToolbar(navController: NavController) {
        toolbar.setupWithNavController(navController, appBarConfiguration)
    }

    private fun getHostNavController(): NavController {
        val hostFragment =
            childFragmentManager.findFragmentById(R.id.nav_host_main_container) as NavHostFragment
        return hostFragment.findNavController()
    }

    private fun setUpNavDestinationChangeListener(navController: NavController) {
        destinationChangeListener =
            NavController.OnDestinationChangedListener { _, destination, _ ->
                // hide the bottom navigation bar in all views except the top level ones
                if (destination.id in navGraphDestinations) {
                    bottom_nav.visibility = View.VISIBLE
                } else {
                    bottom_nav.visibility = View.GONE
                }
            }
        navController.addOnDestinationChangedListener(destinationChangeListener ?: return)
    }

    override fun onResume() {
        val navController = bottomNavViewModel.getCurrentNavController() ?: getHostNavController()
        setUpNavDestinationChangeListener(navController)
        super.onResume()
    }

    override fun onPause() {
        val navController = bottomNavViewModel.getCurrentNavController() ?: getHostNavController()
        destinationChangeListener?.let {
            navController.removeOnDestinationChangedListener(it)
        }
        super.onPause()
    }

    companion object {

        // The top level views of the app
        val navGraphDestinations = setOf(
            R.id.discoverFragment,
            R.id.searchFragment,
            R.id.profileFragment
        )
    }
}
