package com.uc3m.travelapp.model

// Data class representing a single day in a trip itinerary (MA-04)
// Stored as a list inside the Trip document in Firestore
data class DayEntry(
    val dayNumber: Int = 0,
    val title: String = "",
    // Places visited on this day - replaces the old top-level places field
    val places: List<String> = emptyList()
)