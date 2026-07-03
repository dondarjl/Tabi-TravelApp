package com.uc3m.travelapp.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

// Data class representing a trip document in the Firestore "trips" collection (MA-04)
data class Trip(

    @DocumentId
    val id: String = "",

    val title: String = "",
    val destination: String = "",
    val description: String = "",

    // URL of the cover image stored in Firebase Storage (MA-03)
    val coverImageUrl: String = "",

    // Author info
    val authorId: String = "",
    val authorEmail: String = "",

    // Dates stored as strings (e.g. "dd/mm/yyyy")
    val startDate: String = "",
    val endDate: String = "",

    // Budget breakdown in euros
    val totalBudget: Double = 0.0,
    val flightCost: Double = 0.0,
    val accommodationCost: Double = 0.0,
    val foodCost: Double = 0.0,
    val transportCost: Double = 0.0,
    val otherCost: Double = 0.0,
    val activitiesCost: Double = 0.0,

    // Legacy places field kept for backwards compatibility
    val places: List<String> = emptyList(),

    // Itinerary by day - added in Milestone 3
    // Each entry contains the day number, an optional title and the places visited (MA-04)
    val itinerary: List<DayEntry> = emptyList(),

    // Trip categories
    val categories: List<String> = emptyList(),

    // Likes system (MA-04)
    val likes: Int = 0,
    val likedBy: List<String> = emptyList(),

    @ServerTimestamp
    val timestamp: Date? = null
)