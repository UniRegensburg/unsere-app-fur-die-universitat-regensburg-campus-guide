package de.ur.explure.views

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.crazylegend.viewbinding.viewBinding
import de.ur.explure.R
import de.ur.explure.databinding.FragmentDiscoverBinding
import de.ur.explure.viewmodel.DiscoverViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import de.ur.explure.viewmodel.TestViewModel
import kotlinx.android.synthetic.main.fragment_discover.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class DiscoverFragment : Fragment(R.layout.fragment_discover) {

    private val binding by viewBinding(FragmentDiscoverBinding::bind)
    private val viewModel: DiscoverViewModel by viewModel()
    private val testViewModel: TestViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.showMapButton.setOnClickListener {
            viewModel.showMap()
        }

        testViewModel.loginUser()
        setOnClickListeners()
    }

    private fun setOnClickListeners() {
        binding.button.setOnClickListener {
            testViewModel.testAction()
        }
    }
}
