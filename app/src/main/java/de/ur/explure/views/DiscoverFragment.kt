package de.ur.explure.views

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import de.ur.explure.R
import de.ur.explure.viewmodel.TestViewModel
import kotlinx.android.synthetic.main.fragment_discover.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class DiscoverFragment : Fragment(R.layout.fragment_discover) {

    private val viewModel: TestViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.loginUser()
        setOnClickListeners()
    }

    private fun setOnClickListeners() {
        button.setOnClickListener {
            viewModel.testAction()
        }
    }
}
