package de.ur.explure.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import de.ur.explure.R
import de.ur.explure.databinding.FragmentDiscoverBinding
import de.ur.explure.utils.viewLifecycle
import de.ur.explure.viewmodel.DiscoverViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class DiscoverFragment : Fragment(R.layout.fragment_discover) {

    private var binding: FragmentDiscoverBinding by viewLifecycle()
    private val viewModel: DiscoverViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDiscoverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.showMapButton.setOnClickListener {
            viewModel.showMap()
        }
    }
}
