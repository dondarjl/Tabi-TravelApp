package com.uc3m.travelapp.screens.editprofile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.uc3m.travelapp.FirebaseManager
import com.uc3m.travelapp.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBackClick: () -> Unit,
    onAccountDeleted: () -> Unit
) {
    val currentUser = FirebaseManager.currentUser
    val context = LocalContext.current

    var username by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var currentPhotoUrl by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var newPassword by remember { mutableStateOf("") }
    var currentPassword by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var isLoadingProfile by remember { mutableStateOf(true) }
    var successMessage by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var profileSaved by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) selectedImageUri = uri
    }

    // Load existing profile data to preload fields (MA-04)
    LaunchedEffect(currentUser?.uid) {
        if (currentUser == null) {
            isLoadingProfile = false
            return@LaunchedEffect
        }
        FirebaseManager.firestore
            .collection("users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { snapshot ->
                isLoadingProfile = false
                if (snapshot.exists()) {
                    username = snapshot.getString("username") ?: ""
                    bio = snapshot.getString("bio") ?: ""
                    currentPhotoUrl = snapshot.getString("photoUrl") ?: ""
                }
            }
            .addOnFailureListener { isLoadingProfile = false }
    }

    // Navigate back after saving profile successfully
    LaunchedEffect(profileSaved) {
        if (profileSaved) onBackClick()
    }

    // Delete account confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.edit_profile_delete_account_button)) },
            text = { Text(stringResource(R.string.edit_profile_delete_confirm)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        isLoading = true
                        // Delete the user account from Firebase Auth (MA-04)
                        currentUser?.delete()
                            ?.addOnSuccessListener {
                                isLoading = false
                                onAccountDeleted()
                            }
                            ?.addOnFailureListener { exception ->
                                isLoading = false
                                errorMessage = exception.message
                                    ?: context.getString(R.string.edit_profile_delete_error)
                            }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.edit_profile_delete_confirm_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.edit_profile_cancel_button))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.edit_profile_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.edit_profile_cd_back)
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

        if (isLoadingProfile) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // --- Profile photo ---
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when {
                        selectedImageUri != null -> {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = stringResource(R.string.edit_profile_photo_cd),
                                modifier = Modifier.size(80.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                        currentPhotoUrl.isNotEmpty() -> {
                            AsyncImage(
                                model = currentPhotoUrl,
                                contentDescription = stringResource(R.string.edit_profile_photo_cd),
                                modifier = Modifier.size(80.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                        else -> {
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
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (currentPhotoUrl.isNotEmpty() || selectedImageUri != null)
                                stringResource(R.string.edit_profile_photo_change)
                            else stringResource(R.string.edit_profile_photo_button)
                        )
                    }
                }
            }

            item { HorizontalDivider() }

            // --- Username field ---
            item {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(R.string.edit_profile_username_label)) },
                    placeholder = { Text(stringResource(R.string.edit_profile_username_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // --- Bio field ---
            item {
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text(stringResource(R.string.edit_profile_bio_label)) },
                    placeholder = { Text(stringResource(R.string.edit_profile_bio_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
            }

            // --- Success / error messages ---
            item {
                if (successMessage.isNotEmpty()) {
                    Text(
                        text = successMessage,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )
                }
                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp
                    )
                }
            }

            // --- Save profile button ---
            item {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    Button(
                        onClick = {
                            if (currentUser == null) return@Button
                            isLoading = true
                            successMessage = ""
                            errorMessage = ""

                            // If new photo selected upload first then save profile (MA-03)
                            if (selectedImageUri != null) {
                                val storageRef = FirebaseManager.storage
                                    .reference
                                    .child("profiles/${currentUser.uid}.jpg")
                                storageRef.putFile(selectedImageUri!!)
                                    .addOnSuccessListener {
                                        storageRef.downloadUrl
                                            .addOnSuccessListener { downloadUrl ->
                                                saveProfile(
                                                    uid = currentUser.uid,
                                                    email = currentUser.email ?: "",
                                                    username = username,
                                                    bio = bio,
                                                    photoUrl = downloadUrl.toString(),
                                                    onSuccess = {
                                                        isLoading = false
                                                        profileSaved = true
                                                    },
                                                    onError = { msg ->
                                                        isLoading = false
                                                        errorMessage = msg
                                                    }
                                                )
                                            }
                                            .addOnFailureListener { exception ->
                                                isLoading = false
                                                errorMessage = exception.message
                                                    ?: context.getString(R.string.edit_profile_error_saving)
                                            }
                                    }
                                    .addOnFailureListener { exception ->
                                        isLoading = false
                                        errorMessage = exception.message
                                            ?: context.getString(R.string.edit_profile_error_saving)
                                    }
                            } else {
                                // No new photo - save profile directly (MA-04)
                                saveProfile(
                                    uid = currentUser.uid,
                                    email = currentUser.email ?: "",
                                    username = username,
                                    bio = bio,
                                    photoUrl = currentPhotoUrl,
                                    onSuccess = {
                                        isLoading = false
                                        profileSaved = true
                                    },
                                    onError = { msg ->
                                        isLoading = false
                                        errorMessage = msg
                                    }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.edit_profile_save_button))
                    }
                }
            }

            item { HorizontalDivider() }

            // --- Change password section ---
            item {
                Text(
                    text = stringResource(R.string.edit_profile_change_password_button),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Current password needed to reauthenticate before updating (MA-04)
            item {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text(stringResource(R.string.edit_profile_current_password_label)) },
                    placeholder = { Text(stringResource(R.string.edit_profile_current_password_placeholder)) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text(stringResource(R.string.edit_profile_new_password_label)) },
                    placeholder = { Text(stringResource(R.string.edit_profile_new_password_placeholder)) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                OutlinedButton(
                    onClick = {
                        if (currentPassword.isBlank() || newPassword.isBlank()) {
                            errorMessage = context.getString(R.string.login_error_empty_fields)
                            return@OutlinedButton
                        }
                        if (newPassword.length < 6) {
                            errorMessage = context.getString(R.string.edit_profile_error_short_password)
                            return@OutlinedButton
                        }
                        isLoading = true
                        errorMessage = ""
                        successMessage = ""

                        // Reauthenticate before updating password - Firebase requires it (MA-04)
                        val credential = com.google.firebase.auth.EmailAuthProvider
                            .getCredential(currentUser?.email ?: "", currentPassword)

                        currentUser?.reauthenticate(credential)
                            ?.addOnSuccessListener {
                                currentUser.updatePassword(newPassword)
                                    .addOnSuccessListener {
                                        isLoading = false
                                        newPassword = ""
                                        currentPassword = ""
                                        successMessage = context.getString(
                                            R.string.edit_profile_password_success
                                        )
                                    }
                                    .addOnFailureListener { exception ->
                                        isLoading = false
                                        errorMessage = exception.message
                                            ?: context.getString(R.string.edit_profile_password_error)
                                    }
                            }
                            ?.addOnFailureListener { exception ->
                                isLoading = false
                                errorMessage = exception.message
                                    ?: context.getString(R.string.edit_profile_password_error)
                            }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.edit_profile_change_password_button))
                }
            }

            item { HorizontalDivider() }

            // --- Delete account section ---
            item {
                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.edit_profile_delete_account_button))
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

// Helper function to save the user profile document in Firestore (MA-04)
// We use set() so it creates the document if it does not exist yet
private fun saveProfile(
    uid: String,
    email: String,
    username: String,
    bio: String,
    photoUrl: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    // We do NOT include uid in the map because the document ID already is the uid (MA-04)
    val profileData = mapOf(
        "email" to email,
        "username" to username,
        "bio" to bio,
        "photoUrl" to photoUrl
    )
    FirebaseManager.firestore
        .collection("users")
        .document(uid)
        .set(profileData)
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { exception -> onError(exception.message ?: "") }
}