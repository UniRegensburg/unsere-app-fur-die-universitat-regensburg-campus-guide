package de.ur.explure.views

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import de.ur.explure.R
import de.ur.explure.viewmodel.SingleWaypointViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class SingleWaypointDialogFragment : DialogFragment(R.layout.dialog_single_waypoint) {

    private val viewModel: SingleWaypointViewModel by viewModel()
    private val navArgs: SingleWaypointDialogFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setObservers()
        viewModel.setWayPoint(navArgs.waypoint)
    }

    private fun setObservers() {
        viewModel.wayPoint.observe(viewLifecycleOwner, { wayPoint ->
            wayPoint?.run {
                Timber.d(this.toString())
            }
        })
    }

}