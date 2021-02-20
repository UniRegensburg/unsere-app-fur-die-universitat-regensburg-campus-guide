package de.ur.explure.views

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Toast
import de.ur.explure.R
import de.ur.explure.viewmodel.AuthenticationViewModel
import kotlinx.android.synthetic.main.fragment_register.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*

class RegisterFragment : Fragment(R.layout.fragment_register) {

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
        backToLoginButton.setOnClickListener {
            authenticationViewModel.goBackToLogin()
        }
        registerButton.setOnClickListener {
            register()
        }
    }

    private fun register() {
        val email = edRegisterEmail.text.toString()
        val password = edRegisterPassword.text.toString()
        val confPassword = editCPassword.text.toString()
        if (email.isNotEmpty() && password.isNotEmpty() && password == confPassword) {
            authenticationViewModel.register(email, password)
        } else {
            Toast.makeText(context, R.string.registration_failed, Toast.LENGTH_SHORT).show()
        }
    }
}