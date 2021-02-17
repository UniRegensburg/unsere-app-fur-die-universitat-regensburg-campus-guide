package de.ur.explure.views

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import de.ur.explure.R

class ProfileFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_profile, container, false)

        val ownRoutes = rootView.findViewById<ImageButton>(R.id.ownRoutesButton)
        val favoriteRoutes = rootView.findViewById<ImageButton>(R.id.favoriteRoutesButton)
        val userStatistics = rootView.findViewById<ImageButton>(R.id.statisticsButton)
        val logOutButton = rootView.findViewById<ImageButton>(R.id.logOutButton)

        ownRoutes.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.createdRoutesFragment)
        }

        favoriteRoutes.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.favoriteRoutesFragment)
        }

        userStatistics.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.statisticsFragment)
        }

        logOutButton.setOnClickListener {
            Toast.makeText(activity, "Still to come!", Toast.LENGTH_SHORT).show()
        }

        return rootView
    }
}
