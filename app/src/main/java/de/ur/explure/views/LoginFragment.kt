package de.ur.explure.views

import android.app.AlertDialog
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import com.crazylegend.viewbinding.viewBinding
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import de.ur.explure.R
import de.ur.explure.databinding.FragmentLoginBinding
import de.ur.explure.utils.showSnackbar
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
                showSnackbar(
                        requireActivity(),
                        it,
                        R.id.login_container,
                        Snackbar.LENGTH_LONG,
                        colorRes = R.color.colorError
                )
            }
        })
    }

    private fun setOnClickListener() {
        binding.signUpButton.setOnClickListener {
            authenticationViewModel.navigateToRegister()
        }
        binding.loginButton.setOnClickListener {
            // hides keyboard to show snackbar
            hideKeyboard()
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
            showSnackbar(
                    requireActivity(),
                    R.string.login_failed,
                    R.id.login_container,
                    Snackbar.LENGTH_LONG,
                    colorRes = R.color.colorError
            )
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
                    showSnackbar(
                            requireActivity(),
                            R.string.empty_email,
                            R.id.login_container,
                            Snackbar.LENGTH_LONG,
                            colorRes = R.color.colorError
                    )
                }
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    showSnackbar(requireActivity(),
                            R.string.wrong_email,
                            R.id.login_container,
                            Snackbar.LENGTH_LONG,
                            colorRes = R.color.colorError
                    )
                } else {
                    authenticationViewModel.resetPassword(email)
                    showSnackbar(
                            requireActivity(),
                            R.string.email_sent,
                            R.id.login_container,
                            Snackbar.LENGTH_LONG,
                            colorRes = R.color.colorInfo
                    )
                }
            }
            .setNegativeButton(R.string.negative_button) { _, _ -> }
            .show()
    }

    private fun signInAnonymously() {
        authenticationViewModel.signInAnonymously()
    }

    private fun hideKeyboard() {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }
}
