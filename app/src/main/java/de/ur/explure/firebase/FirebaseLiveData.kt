package de.ur.explure.firebase

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class FirebaseData : MutableLiveData<FirebaseUser>() {

    private val firebaseAuth = FirebaseAuth.getInstance()

    private val user = firebaseAuth.currentUser

    fun firebaseLiveData() : FirebaseUser? {
        return user
    }
/*
    //beobachtet FirebaseAuth Status
    override fun onActive() {
        firebaseAuth.addAuthStateListener { authStateListener }
    }

    //hört auf FirebaseAuth Status zu beobachten
    override fun onInactive() {
        firebaseAuth.addAuthStateListener { authStateListener }
    }*/





}