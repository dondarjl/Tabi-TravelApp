package com.uc3m.travelapp.screens.userprofile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.uc3m.travelapp.FirebaseManager
import com.uc3m.travelapp.R
import com.uc3m.travelapp.model.Trip
import com.uc3m.travelapp.model.UserProfile
import com.uc3m.travelapp.screens.home.TripCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: String,
    onTripClick: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var userTrips by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var isLoadingProfile by remember { mutableStateOf(true) }
    var isLoadingTrips by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    // Load the public profile of the user (MA-04)
    LaunchedEffect(userId) {
        FirebaseManager.firestore
            .collection("users")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                isLoadingProfile = false
                if (error != null) {
                    errorMessage = error.message
                        ?: context.getString(R.string.public_profile_error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    userProfile = snapshot.toObject(UserProfile::class.java)
                }
            }
    }

    // Load trips published by this user (MA-04)
    LaunchedEffect(userId) {
        FirebaseManager.firestore
            .collection("trips")
            .whereEqualTo("authorId", userId)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                isLoadingTrips = false
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    userTrips = snapshot.documents.mapNotNull { it.toObject(Trip::class.java) }
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = userProfile?.username?.ifEmpty {
                            stringResource(R.string.public_profile_title)
                        } ?: stringResource(R.string.public_profile_title),
                        fontWeight = FontWeight.Bold
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
            isLoadingProfile -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            errorMessage.isNotEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // --- Profile header ---
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Profile photo
                            if (userProfile?.photoUrl?.isNotEmpty() == true) {
                                AsyncImage(
                                    model = userProfile!!.photoUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(80.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier.size(80.dp).clip(CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = userProfile?.username?.ifEmpty {
                                    userProfile?.email ?: ""
                                } ?: "",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = userProfile?.email ?: "",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (userProfile?.bio?.isNotEmpty() == true) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = userProfile!!.bio,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Trip count
                            Text(
                                text = stringResource(
                                    R.string.public_profile_trips_count,
                                    userTrips.size
                                ),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    item { HorizontalDivider() }

                    item {
                        Text(
                            text = stringResource(R.string.public_profile_trips_label),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }

                    // --- Trips list ---
                    if (isLoadingTrips) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    } else if (userTrips.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = stringResource(R.string.profile_no_trips_title),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        items(userTrips) { trip ->
                            TripCard(trip = trip, onClick = { onTripClick(trip.id) })
                        }
                    }

                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }
    }
}