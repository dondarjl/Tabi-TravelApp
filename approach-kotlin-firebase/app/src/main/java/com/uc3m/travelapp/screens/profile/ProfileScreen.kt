package com.uc3m.travelapp.screens.profile

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import coil.compose.AsyncImage
import com.uc3m.travelapp.FirebaseManager
import com.uc3m.travelapp.R
import com.uc3m.travelapp.model.Trip
import com.uc3m.travelapp.model.UserProfile
import com.uc3m.travelapp.screens.home.TripCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onTripClick: (String) -> Unit,
    onEditProfileClick: () -> Unit
) {
    val currentUser = FirebaseManager.currentUser
    val context = LocalContext.current

    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var myTrips by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var isLoadingTrips by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    // Load user profile from Firestore (MA-04)
    LaunchedEffect(currentUser?.uid) {
        if (currentUser == null) return@LaunchedEffect
        FirebaseManager.firestore
            .collection("users")
            .document(currentUser.uid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    userProfile = snapshot.toObject(UserProfile::class.java)
                }
            }
    }

    // Load trips authored by the current user (MA-04)
    LaunchedEffect(currentUser?.uid) {
        if (currentUser == null) {
            isLoadingTrips = false
            return@LaunchedEffect
        }
        FirebaseManager.firestore
            .collection("trips")
            .whereEqualTo("authorId", currentUser.uid)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                isLoadingTrips = false
                if (error != null) {
                    errorMessage = error.message
                        ?: context.getString(R.string.profile_error_loading)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    myTrips = snapshot.documents.mapNotNull { it.toObject(Trip::class.java) }
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.profile_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                actions = {
                    IconButton(onClick = onEditProfileClick) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.profile_cd_edit),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                windowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp)
            )
        }
    ) { innerPadding ->

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
                    if (userProfile?.photoUrl?.isNotEmpty() == true) {
                        AsyncImage(
                            model = userProfile!!.photoUrl,
                            contentDescription = stringResource(R.string.edit_profile_photo_cd),
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(52.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (userProfile?.username?.isNotEmpty() == true)
                            userProfile!!.username
                        else stringResource(R.string.profile_username_placeholder),
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = currentUser?.email
                            ?: stringResource(R.string.profile_unknown_user),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (userProfile?.bio?.isNotEmpty() == true) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = userProfile!!.bio,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Stats row — only My Trips count now
                    Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = myTrips.size.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.profile_my_trips_title),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        FirebaseManager.auth.signOut()
                        onLogout()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        text = stringResource(R.string.profile_sign_out_button),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            item {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.profile_my_trips_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            when {
                isLoadingTrips -> {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                errorMessage.isNotEmpty() -> {
                    item {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                myTrips.isEmpty() -> {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(32.dp))
                            Text(
                                text = stringResource(R.string.profile_no_trips_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.profile_no_trips_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    items(myTrips) { trip ->
                        TripCard(trip = trip, onClick = { onTripClick(trip.id) })
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}