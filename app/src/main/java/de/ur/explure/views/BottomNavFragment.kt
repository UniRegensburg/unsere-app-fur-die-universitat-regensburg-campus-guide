package de.ur.explure.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.navigation.NavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import de.ur.explure.R
import de.ur.explure.extensions.setupWithNavController
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
            setupBottomNavigation()
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
    private fun setupBottomNavigation() {
        val controller = bottom_nav.setupWithNavController(
            navGraphIds = navGraphIds,
            fragmentManager = childFragmentManager,
            containerId = R.id.nav_host_main_container,
            intent = requireActivity().intent
        )
        observeController(controller)
    }

    /**
     * Adjusts the toolbar whenever the selected controller changes and updates the current nav controller.
     * @param [LiveData]<[NavController]> object with the new navigation controller which should be observed
     */
    private fun observeController(controller: LiveData<NavController>) {
        controller.observe(requireActivity(), { navController ->
            toolbar.setupWithNavController(navController, appBarConfiguration)
            bottomNavViewModel.initializeNavController(navController)
        })
        bottomNavViewModel.currentNavController = controller
    }

    private fun setUpNavDestinationChangeListener() {
        destinationChangeListener =
            NavController.OnDestinationChangedListener { _, destination, _ ->
                // hide the bottom navigation bar in all views except the top level ones
                if (destination.id in navGraphDestinations) {
                    bottom_nav.visibility = View.VISIBLE
                } else {
                    bottom_nav.visibility = View.GONE
                }
            }

        bottomNavViewModel.currentNavController?.observe(this) {
            val changeListener = destinationChangeListener ?: return@observe
            it.addOnDestinationChangedListener(changeListener)
        }
    }

    override fun onStart() {
        super.onStart()
        setUpNavDestinationChangeListener()
    }

    override fun onStop() {
        super.onStop()
        // remove the destinationChangeListener of the current nav controller to prevent memory leaks
        destinationChangeListener?.let {
            bottomNavViewModel.currentNavController?.value?.removeOnDestinationChangedListener(it)
        }
        // also remove viewmodel observer and reset current nav controller
        bottomNavViewModel.currentNavController?.removeObservers(this)
        bottomNavViewModel.resetCurrentNavController()
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
