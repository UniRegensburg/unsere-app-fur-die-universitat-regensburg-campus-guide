package de.ur.explure.views

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import de.ur.explure.R
import de.ur.explure.viewmodel.AuthenticationViewModel
import kotlinx.android.synthetic.main.fragment_register.*


class RegisterFragment : Fragment() {
    private lateinit var authenticationViewModel: AuthenticationViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_register, container, false)
        //init viewmodel with data
        authenticationViewModel = ViewModelProvider(this).get(AuthenticationViewModel::class.java)
        authenticationViewModel.getLiveData().observe(viewLifecycleOwner, {
            if(it != null) {
                NavHostFragment.findNavController(this).navigate(R.id.action_registerFragment_to_mainFragment)
            }
        })
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        backToLoginButton.setOnClickListener { view : View ->
            view.findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }
        registerButton.setOnClickListener {
            register()
        }
    }

    private fun register() {
        val email = edRegisterEmail.text.toString()
        val password = edRegisterPassword.text.toString()
        val confPassword = editCPassword.text.toString()
        if (email.isNotEmpty() && password.isNotEmpty() && password.equals(confPassword)) {
            authenticationViewModel.register(email, password)
        } else {
            Toast.makeText(context, "Registrirung fehlgeschlagen", Toast.LENGTH_SHORT).show()
        }
    }
}