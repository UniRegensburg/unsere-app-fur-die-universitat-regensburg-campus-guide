package de.ur.explure.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.ui.NavigationUI
import de.ur.explure.R
import de.ur.explure.extensions.setupWithNavController
import de.ur.explure.viewmodel.HostViewModel
import kotlinx.android.synthetic.main.fragment_host.*
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Home fragment used as start view of the application
 *
 */

class HostFragment : Fragment() {

    private val viewModel: HostViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_host, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
            fragmentManager = childFragmentManager,
            containerId = R.id.nav_host_fragment,
            intent = requireActivity().intent
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
        controller.observe(viewLifecycleOwner, Observer { navController ->
            NavigationUI.setupActionBarWithNavController(
                requireActivity() as AppCompatActivity,
                navController
            )
        })
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupBottomNavigation()
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
