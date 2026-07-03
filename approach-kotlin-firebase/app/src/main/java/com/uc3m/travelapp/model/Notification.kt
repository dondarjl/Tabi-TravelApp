package com.uc3m.travelapp.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

// Data class representing a notification document in Firestore (MA-04)
// Stored in: notifications/{ownerUid}/items/{notifId}
data class Notification(
        @DocumentId
        val id: String = "",
        val fromEmail: String = "",
        val tripDestination: String = "",
        val tripId: String = "",
        val type: String = "like", // "like" or "chat"
        @ServerTimestamp
        val timestamp: Date? = null
)