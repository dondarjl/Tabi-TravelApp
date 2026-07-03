package com.uc3m.travelapp.screens.detail

import android.location.Geocoder
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.uc3m.travelapp.FirebaseManager
import com.uc3m.travelapp.R
import com.uc3m.travelapp.model.Trip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    tripId: String,
    onChatClick: (String) -> Unit,
    onEditClick: (String) -> Unit,
    onUserProfileClick: (String) -> Unit,
    onOwnProfileClick: () -> Unit,
    onBackClick: () -> Unit
){
    var trip by remember { mutableStateOf<Trip?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    val currentUserId = FirebaseManager.currentUser?.uid ?: ""
    val isAuthor = trip?.authorId == currentUserId

    LaunchedEffect(tripId) {
        FirebaseManager.firestore
            .collection("trips")
            .document(tripId)
            .addSnapshotListener { snapshot, error ->
                isLoading = false
                if (error != null) {
                    errorMessage = error.message
                        ?: context.getString(R.string.detail_error_loading)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    trip = snapshot.toObject(Trip::class.java)
                } else {
                    errorMessage = context.getString(R.string.detail_error_not_found)
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = trip?.title ?: stringResource(R.string.detail_default_title),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.detail_cd_back)
                        )
                    }
                },
                actions = {
                    if (isAuthor) {
                        IconButton(onClick = { onEditClick(tripId) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.edit_trip_cd_edit),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                windowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp)
            )
        }
    ) { innerPadding ->

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            errorMessage.isNotEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
                }
            }
            trip != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (trip!!.coverImageUrl.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        ) {
                            AsyncImage(
                                model = trip!!.coverImageUrl,
                                contentDescription = stringResource(R.string.detail_cd_cover_image),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            // Gradient overlay so the tab bar reads well over any image (MA-02)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.35f)
                                            )
                                        )
                                    )
                            )
                            // Trip title overlaid on the image bottom left
                            Text(
                                text = trip!!.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(16.dp),
                                maxLines = 2
                            )
                        }
                    }

                    // Four tabs: Overview, Budget, Map, Itinerary (MA-02)
                    val tabs = listOf(
                        stringResource(R.string.detail_tab_overview),
                        stringResource(R.string.detail_tab_budget),
                        stringResource(R.string.detail_tab_map),
                        stringResource(R.string.detail_tab_itinerary)
                    )

                    TabRow(selectedTabIndex = selectedTab) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { Text(title, fontSize = 12.sp) }
                            )
                        }
                    }

                    when (selectedTab) {
                        0 -> OverviewTab(
                            trip = trip!!,
                            isAuthor = isAuthor,
                            onChatClick = { onChatClick(tripId) },
                            // If author is the current user navigate to own profile, otherwise to public profile (MA-02)
                            onAuthorClick = {
                                if (trip!!.authorId == currentUserId) {
                                    onOwnProfileClick()
                                } else {
                                    onUserProfileClick(trip!!.authorId)
                                }
                            }
                        )
                        1 -> BudgetTab(trip = trip!!)
                        2 -> MapTab(destination = trip!!.destination)
                        3 -> ItineraryTab(trip = trip!!)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OverviewTab(
    trip: Trip,
    isAuthor: Boolean,
    onChatClick: () -> Unit,
    onAuthorClick: () -> Unit
) {
    val currentUserId = FirebaseManager.currentUser?.uid ?: ""
    val hasLiked = trip.likedBy.contains(currentUserId)

    val categoryLabels = mapOf(
        "urban" to stringResource(R.string.category_urban),
        "cultural" to stringResource(R.string.category_cultural),
        "nature" to stringResource(R.string.category_nature),
        "gastronomic" to stringResource(R.string.category_gastronomic),
        "party" to stringResource(R.string.category_party),
        "beach" to stringResource(R.string.category_beach),
        "adventure" to stringResource(R.string.category_adventure),
        "relax" to stringResource(R.string.category_relax),
        "roadtrip" to stringResource(R.string.category_roadtrip),
        "backpacker" to stringResource(R.string.category_backpacker)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = trip.destination,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${trip.startDate} → ${trip.endDate}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Author row - clickable to navigate to public profile (MA-02)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.clickable { onAuthorClick() }
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.detail_posted_by, trip.authorEmail),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (trip.categories.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.detail_categories_label),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                trip.categories.forEach { key ->
                    FilterChip(
                        selected = true,
                        onClick = {},
                        label = { Text(categoryLabels[key] ?: key, fontSize = 12.sp) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        if (trip.description.isNotEmpty()) {
            Text(text = trip.description, fontSize = 16.sp, lineHeight = 24.sp)
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Likes row (MA-04)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = {
                    if (hasLiked) {
                        // Remove like
                        FirebaseManager.firestore.collection("trips").document(trip.id)
                            .update(mapOf(
                                "likes" to com.google.firebase.firestore.FieldValue.increment(-1),
                                "likedBy" to com.google.firebase.firestore.FieldValue.arrayRemove(currentUserId)
                            ))
                    } else {
                        // Add like
                        FirebaseManager.firestore.collection("trips").document(trip.id)
                            .update(mapOf(
                                "likes" to com.google.firebase.firestore.FieldValue.increment(1),
                                "likedBy" to com.google.firebase.firestore.FieldValue.arrayUnion(currentUserId)
                            ))

                        // Save notification for the trip author (MA-04)
                        // Only notify if the liker is not the author of the trip
                        if (trip.authorId != currentUserId) {
                            val currentUserEmail = FirebaseManager.currentUser?.email ?: ""
                            val notificationData = mapOf(
                                "fromEmail" to currentUserEmail,
                                "tripDestination" to trip.destination,
                                "tripId" to trip.id,
                                "type" to "like",
                                "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                            )
                            // Store notification under the trip author's UID (MA-04)
                            FirebaseManager.firestore
                                .collection("notifications")
                                .document(trip.authorId)
                                .collection("items")
                                .add(notificationData)
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = if (hasLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (hasLiked) stringResource(R.string.detail_cd_unlike)
                    else stringResource(R.string.detail_cd_like),
                    tint = if (hasLiked) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = stringResource(R.string.detail_likes_count, trip.likes),
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Chat button - text changes if it is your own trip (MA-02)
        Button(onClick = onChatClick, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = if (isAuthor) stringResource(R.string.detail_chat_own_trip)
                else stringResource(R.string.detail_contact_button)
            )
        }
    }
}

// --- Itinerary Tab ---
// Shows the day by day breakdown of the trip (MA-02)
@Composable
fun ItineraryTab(trip: Trip) {
    if (trip.itinerary.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.detail_itinerary_empty),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        trip.itinerary.forEach { day ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Day number header
                    Text(
                        text = stringResource(R.string.detail_day_label, day.dayNumber),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    // Places visited that day
                    if (day.places.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.detail_day_places_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        day.places.forEach { place ->
                            Text(
                                text = "• $place",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun MapTab(destination: String) {
    val context = LocalContext.current
    var location by remember { mutableStateOf<LatLng?>(null) }
    var mapError by remember { mutableStateOf("") }
    var isGeocodingLoading by remember { mutableStateOf(true) }

    LaunchedEffect(destination) {
        isGeocodingLoading = true
        withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val results = geocoder.getFromLocationName(destination, 1)
                if (!results.isNullOrEmpty()) {
                    location = LatLng(results[0].latitude, results[0].longitude)
                } else {
                    mapError = context.getString(R.string.detail_map_error)
                }
            } catch (e: Exception) {
                mapError = e.message ?: context.getString(R.string.detail_map_error)
            }
        }
        isGeocodingLoading = false
    }

    when {
        isGeocodingLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.detail_map_loading),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        mapError.isNotEmpty() -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = mapError, color = MaterialTheme.colorScheme.error)
            }
        }
        location != null -> {
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(location!!, 12f)
            }
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                Marker(
                    state = rememberMarkerState(position = location!!),
                    title = destination,
                    snippet = stringResource(R.string.detail_map_cd_marker, destination)
                )
            }
        }
    }
}

@Composable
fun BudgetTab(trip: Trip) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.detail_budget_title),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (trip.flightCost > 0)
            BudgetItem(label = stringResource(R.string.detail_budget_flights), amount = trip.flightCost)
        if (trip.accommodationCost > 0)
            BudgetItem(label = stringResource(R.string.detail_budget_accommodation), amount = trip.accommodationCost)
        if (trip.foodCost > 0)
            BudgetItem(label = stringResource(R.string.detail_budget_food), amount = trip.foodCost)
        if (trip.transportCost > 0)
            BudgetItem(label = stringResource(R.string.detail_budget_transport), amount = trip.transportCost)
        if (trip.activitiesCost > 0)
            BudgetItem(label = stringResource(R.string.detail_budget_activities), amount = trip.activitiesCost)
        if (trip.otherCost > 0)
            BudgetItem(label = stringResource(R.string.detail_budget_other), amount = trip.otherCost)

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.detail_budget_total),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                text = "€${"%.0f".format(trip.totalBudget)}",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun BudgetItem(label: String, amount: Double) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 16.sp)
            Text(
                text = "€${"%.0f".format(amount)}",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }
    }
}