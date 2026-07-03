package com.uc3m.travelapp.model

// Data class representing a user profile document in Firestore (MA-04)
// Stored in the "users" collection with the Firebase Auth UID as document ID
data class UserProfile(
        val uid: String = "",
        val email: String = "",
        val username: String = "",
        val bio: String = "",
        val photoUrl: String = ""
)