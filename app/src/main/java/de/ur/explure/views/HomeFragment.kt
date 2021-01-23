package de.ur.explure.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import de.ur.explure.databinding.FragmentHomeBinding
import de.ur.explure.utils.viewLifecycle

/**
 * Home fragment used as start view of the application
 *
 */

class HomeFragment : Fragment() {

    // This property is only valid between onCreateView and onDestroyView.
    private var binding: FragmentHomeBinding by viewLifecycle()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater)
        return binding.root
    }
}
