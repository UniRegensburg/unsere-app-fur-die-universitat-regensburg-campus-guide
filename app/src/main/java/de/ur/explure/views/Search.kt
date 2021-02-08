package de.ur.explure.views

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import de.ur.explure.R
import kotlinx.android.synthetic.main.fragment_search.*


class Search : Fragment() {



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_search, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chillenButton.setOnClickListener{ view ->
            view.findNavController().navigate(R.id.action_search_to_testFragment)
        }

        lernenButton.setOnClickListener{ view ->
            view.findNavController().navigate(R.id.action_search_to_testFragment)
        }

        freizeitButton.setOnClickListener{ view ->
            view.findNavController().navigate(R.id.action_search_to_testFragment)
        }

        cafeteButton.setOnClickListener{ view ->
            view.findNavController().navigate(R.id.action_search_to_testFragment)
        }

        organisationButton.setOnClickListener{ view ->
            view.findNavController().navigate(R.id.action_search_to_testFragment)
        }

        bibliothekenButton.setOnClickListener{ view ->
            view.findNavController().navigate(R.id.action_search_to_testFragment)
        }
    }

}
