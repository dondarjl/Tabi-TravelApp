package com.uc3m.travelapp

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

// We use a singleton object to avoid creating multiple Firebase instances
// throughout the app. This way all screens share the same connection.
// We learned about this pattern in the MA-04 lecture on Firebase setup.
object FirebaseManager {

    // Auth instance - used for login and register (MA-04)
    val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    // Firestore instance - our main database for trips and messages (MA-04)
    // We chose Firestore over a local database because we need data
    // to be shared between all users of the app
    val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    // Storage instance - used for trip cover images and profile photos (MA-03)
    val storage: FirebaseStorage by lazy {
        FirebaseStorage.getInstance()
    }

    // Shortcut to get the current logged-in user
    // Returns null if no user is logged in
    val currentUser get() = auth.currentUser

    val isUserLoggedIn get() = auth.currentUser != null
}