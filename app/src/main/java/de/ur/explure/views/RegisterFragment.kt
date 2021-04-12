package de.ur.explure.views

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import com.crazylegend.viewbinding.viewBinding
import com.google.android.material.snackbar.Snackbar
import de.ur.explure.R
import de.ur.explure.databinding.FragmentRegisterBinding
import de.ur.explure.utils.showSnackbar
import de.ur.explure.viewmodel.AuthenticationViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class RegisterFragment : Fragment(R.layout.fragment_register) {

    private val binding by viewBinding(FragmentRegisterBinding::bind)
    private val authenticationViewModel: AuthenticationViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observe()
        setOnClickListener()
    }

    private fun observe() {
        authenticationViewModel.userInfo.observe(viewLifecycleOwner, {
            it?.let {
                showSnackbar(
                        requireActivity(),
                        it,
                        R.id.register_container,
                        Snackbar.LENGTH_LONG,
                        colorRes = R.color.colorError
                )
            }
        })
    }

    private fun setOnClickListener() {
        binding.backToLoginButton.setOnClickListener {
            authenticationViewModel.goBackToLogin()
        }
        binding.registerButton.setOnClickListener {
            // hides keyboard to show snackbar
            hideKeyboard()
            register()
        }
    }

    private fun register() {
        val email = binding.edRegisterEmail.text.toString()
        val password = binding.edRegisterPassword.text.toString()
        val confPassword = binding.editCPassword.text.toString()
        if (email.isNotEmpty() && password.isNotEmpty() && password == confPassword) {
            authenticationViewModel.register(email, password)
        } else {
            showSnackbar(
                    requireActivity(),
                    R.string.registration_failed,
                    R.id.register_container,
                    Snackbar.LENGTH_LONG,
                    colorRes = R.color.colorError
            )
        }
    }

    private fun hideKeyboard() {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }
}
