package de.ur.explure.views

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.crazylegend.viewbinding.viewBinding
import de.ur.explure.R
import de.ur.explure.adapter.CategorySpinnerAdapter
import de.ur.explure.databinding.FragmentCreateRouteBinding
import de.ur.explure.databinding.FragmentLoginBinding
import de.ur.explure.viewmodel.CreateRouteViewModel
import org.koin.android.ext.android.bind
import org.koin.androidx.viewmodel.compat.ScopeCompat.viewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class CreateRouteFragment : Fragment(R.layout.fragment_create_route) {

    private val viewModel : CreateRouteViewModel by viewModel()
    private val binding by viewBinding(FragmentCreateRouteBinding::bind)


    private lateinit var categoryAdapter : CategorySpinnerAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setObservers()
        viewModel.getCategories()
    }

    private fun setObservers() {
        viewModel.categories.observe(viewLifecycleOwner, { categories ->
            if (categories != null) {
                categoryAdapter = CategorySpinnerAdapter(requireContext(), categories)
                binding.sCountry.adapter = categoryAdapter
                binding.sCountry.setSelection(0)
            }
        })
    }
}