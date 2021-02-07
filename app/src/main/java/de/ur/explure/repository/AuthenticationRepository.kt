package de.ur.explure.repository


import android.util.Patterns
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class AuthenticationRepository {

    //private var firebaseData: FirebaseData = FirebaseData()
    private var user: MutableLiveData<FirebaseUser> = MutableLiveData<FirebaseUser>()
    private var userLoggedOut: MutableLiveData<Boolean> = MutableLiveData<Boolean>()
    private var firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    init {

        //checks if user is already logged in
       /* if(firebaseAuth.currentUser != null) {
            user.postValue(firebaseAuth.currentUser)
            userLoggedOut.postValue(false)
        }*/
    }

    fun registerUser(email: String, password: String) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                            user.postValue(firebaseAuth.currentUser)
                    } else {
                        //Toast.makeText(this, "Registrierung fehlgeschlagen", Toast.LENGTH_SHORT).show()
                    }
                }
    }

    fun signIn(email : String, password : String) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        user.postValue(firebaseAuth.currentUser)
                    } else {
                       //Toast.makeText(this, "Anmeldung fehlgeschlagen", Toast.LENGTH_SHORT).show()
                    }
                }
    }

    fun resetPassword(email: EditText) {
        if(email.text.toString().isEmpty()) {
            return
        }
        if(!Patterns.EMAIL_ADDRESS.matcher(email.text.toString()).matches()){
            return
        }
        firebaseAuth.sendPasswordResetEmail(email.text.toString())
                .addOnCompleteListener {
                    task->
                    if(task.isSuccessful) {
                    //Toast.makeText(this, "Email gesendet", Toast.LENGTH_SHORT).show()
                    }
                }
        }

    fun signInAnonymously() {
        firebaseAuth.signInAnonymously()
                .addOnCompleteListener{ task ->
                    if (task.isSuccessful) {
                        user.postValue(firebaseAuth.currentUser)
                    } else {
                        //Toast.makeText(..., "Anmeldung fehlgeschlagen", Toast.LENGTH_SHORT).show()
                    }
                }
    }

    fun logout() {
        firebaseAuth.signOut()
        userLoggedOut.postValue(true)
    }

    fun getLiveData() : MutableLiveData<FirebaseUser> {
        return user
    }

    fun getLoggedOutLiveData() : MutableLiveData<Boolean> {
        return userLoggedOut
    }

}