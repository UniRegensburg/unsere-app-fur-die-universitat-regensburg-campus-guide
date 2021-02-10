package de.ur.explure.views

import android.os.Bundle
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import de.ur.explure.R
import de.ur.explure.viewmodel.AuthenticationViewModel
import kotlinx.android.synthetic.main.fragment_login.*
import kotlinx.android.synthetic.main.reset_password.view.*
import org.koin.androidx.viewmodel.ext.android.viewModel
@Suppress("MaximumLineLength")
class LoginFragment : Fragment() {

   private val authenticationViewModel: AuthenticationViewModel by viewModel()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_login, container, false)
        authenticationViewModel.user.observe(viewLifecycleOwner, {
            if (it != null) { NavHostFragment.findNavController(this).navigate(R.id.action_loginFragment_to_mainFragment) }
        })
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setOnClickListener()
    }

    private fun setOnClickListener() {
        signUpButton.setOnClickListener { view: View ->
            view.findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
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
            Toast.makeText(context, R.string.loginFailed, Toast.LENGTH_LONG).show()
        }
    }

    private fun resetPassword() {
        val builder = AlertDialog.Builder(this.requireContext())
        builder.setTitle(R.string.builderText)
        val view = layoutInflater.inflate(R.layout.reset_password, null)
        builder.setView(view)
        builder.setPositiveButton(R.string.positiveButton) { dialog, which ->
            val email = view.edResetEmail.text.toString()
            if (email.isEmpty()) {
                Toast.makeText(context, R.string.emptyEmail, Toast.LENGTH_LONG).show()
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(context, R.string.wrongEmail, Toast.LENGTH_LONG).show()
            } else {
                authenticationViewModel.resetPassword(email)
                Toast.makeText(context, R.string.emailSent, Toast.LENGTH_LONG).show()
            }
        }
        builder.setNegativeButton(R.string.negativeButton) { dialog, which -> }
        builder.show()
    }

    private fun signInAnonymously() {
        authenticationViewModel.signInAnonymously()
    }
}
