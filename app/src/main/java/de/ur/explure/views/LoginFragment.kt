package de.ur.explure.views

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import de.ur.explure.R
import de.ur.explure.viewmodel.AuthenticationViewModel
import kotlinx.android.synthetic.main.fragment_login.*
import kotlinx.android.synthetic.main.reset_password.view.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class LoginFragment : Fragment(R.layout.fragment_login) {

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
        signUpButton.setOnClickListener {
            authenticationViewModel.navigateToRegister()
        }
        loginButton.setOnClickListener {
            login()
        }
        resetPasswordTV.setOnClickListener {
            resetPassword()
        }
        withoutAccountButton.setOnClickListener {
            signInAnonymously()
        }
    }

    private fun login() {
        val email = edLoginEmail.text.toString()
        val password = edLoginPassword.text.toString()
        if (email.isNotEmpty() && password.isNotEmpty()) {
            authenticationViewModel.signIn(email, password)
        } else {
            Toast.makeText(context, R.string.login_failed, Toast.LENGTH_LONG).show()
        }
    }

    private fun resetPassword() {
        val builder = AlertDialog.Builder(this.requireContext())
        builder.setTitle(R.string.builder_text)
        val view = layoutInflater.inflate(R.layout.reset_password, null)
        builder.setView(view)
        builder.setPositiveButton(R.string.positive_button) { _, _ ->
            val email = view.edResetEmail.text.toString()
            if (email.isEmpty()) {
                Toast.makeText(context, R.string.empty_email, Toast.LENGTH_LONG).show()
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(context, R.string.wrong_email, Toast.LENGTH_LONG).show()
            } else {
                authenticationViewModel.resetPassword(email)
                Toast.makeText(context, R.string.email_sent, Toast.LENGTH_LONG).show()
            }
        }
        builder.setNegativeButton(R.string.negative_button) { _, _ -> }
        builder.show()
    }

    private fun signInAnonymously() {
        authenticationViewModel.signInAnonymously()
    }
}
