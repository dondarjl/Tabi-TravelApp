package com.uc3m.travelapp.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

// Data class representing a message in the chat of a trip (MA-04)
// Messages are stored in a subcollection: chats/{tripId}/messages
data class Message(

    // @DocumentId automatically maps the Firestore document ID to this field
    @DocumentId
    val id: String = "",

    val text: String = "",

    // We store the author ID and email to avoid extra Firestore queries
    // when displaying who sent each message
    val authorId: String = "",
    val authorEmail: String = "",

    // @ServerTimestamp ensures messages are always ordered by server time
    // regardless of the device clock of each user
    @ServerTimestamp
    val timestamp: Date? = null
)