package de.ur.explure.views

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.crazylegend.viewbinding.viewBinding
import de.ur.explure.R
import de.ur.explure.databinding.FragmentRegisterBinding
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
        authenticationViewModel.toast.observe(viewLifecycleOwner, {
            it?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun setOnClickListener() {
        binding.backToLoginButton.setOnClickListener {
            authenticationViewModel.goBackToLogin()
        }
        binding.registerButton.setOnClickListener {
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
            Toast.makeText(context, R.string.registration_failed, Toast.LENGTH_SHORT).show()
        }
    }
}
