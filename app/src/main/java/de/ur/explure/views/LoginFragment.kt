package de.ur.explure.views

import android.app.AlertDialog
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.crazylegend.viewbinding.viewBinding
import com.google.android.material.textfield.TextInputEditText
import de.ur.explure.R
import de.ur.explure.databinding.FragmentLoginBinding
import de.ur.explure.viewmodel.AuthenticationViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class LoginFragment : Fragment(R.layout.fragment_login) {

    private val binding by viewBinding(FragmentLoginBinding::bind)
    private val authenticationViewModel: AuthenticationViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // lock the screen rotation for the login process as this messes up the auth observer in the
        // main activity
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_NOSENSOR

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
        binding.signUpButton.setOnClickListener {
            authenticationViewModel.navigateToRegister()
        }
        binding.loginButton.setOnClickListener {
            login()
        }
        binding.resetPasswordTV.setOnClickListener {
            resetPassword()
        }
        binding.withoutAccountButton.setOnClickListener {
            signInAnonymously()
        }
    }

    private fun login() {
        val email = binding.edLoginEmail.text.toString()
        val password = binding.edLoginPassword.text.toString()
        if (email.isNotEmpty() && password.isNotEmpty()) {
            authenticationViewModel.signIn(email, password)
        } else {
            Toast.makeText(context, R.string.login_failed, Toast.LENGTH_LONG).show()
        }
    }

    private fun resetPassword() {
        val resetPasswordView = layoutInflater.inflate(R.layout.reset_password, null)
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.builder_text)
            .setView(resetPasswordView)
            .setPositiveButton(R.string.positive_button) { _, _ ->
                val email =
                    resetPasswordView.findViewById<TextInputEditText>(R.id.edResetEmail).text.toString()
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
            .setNegativeButton(R.string.negative_button) { _, _ -> }
            .show()
    }

    private fun signInAnonymously() {
        authenticationViewModel.signInAnonymously()
    }
}
