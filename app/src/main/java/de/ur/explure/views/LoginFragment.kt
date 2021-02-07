package de.ur.explure.views

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import de.ur.explure.R
import de.ur.explure.viewmodel.AuthenticationViewModel
import kotlinx.android.synthetic.main.fragment_login.*
import kotlinx.android.synthetic.main.reset_password.view.*

class LoginFragment : Fragment() {

    private lateinit var authenticationViewModel: AuthenticationViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_login, container, false)
        //init viewModel with data
        authenticationViewModel = ViewModelProvider(this).get(AuthenticationViewModel::class.java)
        authenticationViewModel.getLiveData().observe(viewLifecycleOwner, {
            if(it != null) {
                NavHostFragment.findNavController(this).navigate(R.id.action_loginFragment_to_mainFragment)
                }
        })
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        signUpButton.setOnClickListener { view: View->
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
            Toast.makeText(context, "Anmeldung fehlgeschlagen", Toast.LENGTH_LONG).show()
        }
    }

    private fun resetPassword() {
        val builder = AlertDialog.Builder(this.requireContext())
        builder.setTitle("Passwort vergessen")
        val view = layoutInflater.inflate(R.layout.reset_password, null)
        builder.setView(view)
        builder.setPositiveButton("Zurücksetzen") { dialog, which ->
            val email = view.edResetEmail
            authenticationViewModel.resetPassword(email)
        }
        builder.setNegativeButton("Schließen") { dialog, which -> }
        builder.show()
    }

    private fun signInAnonymously() {
        authenticationViewModel.signInAnonymously()
    }

}