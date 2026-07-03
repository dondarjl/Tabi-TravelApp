package com.uc3m.travelapp.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uc3m.travelapp.FirebaseManager
import com.uc3m.travelapp.R

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {

    // We decided to put both login and register in the same screen
    // using a toggle instead of two separate screens to keep navigation simpler (MA-02)
    var isLoginMode by remember { mutableStateOf(true) }

    // Form fields state - using remember + mutableStateOf as explained in MA-02
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Feedback state for the user
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Context needed to read string resources composable functions (MA-02)
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = stringResource(R.string.app_name),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isLoginMode) stringResource(R.string.login_sign_in_subtitle)
            else stringResource(R.string.login_register_subtitle),
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(R.string.login_email_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // PasswordVisualTransformation hides the typed characters (MA-02)
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.login_password_label)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    // Basic validation before calling Firebase (MA-04)
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = context.getString(R.string.login_error_empty_fields)
                        return@Button
                    }
                    if (password.length < 6) {
                        errorMessage = context.getString(R.string.login_error_short_password)
                        return@Button
                    }

                    isLoading = true
                    errorMessage = ""

                    if (isLoginMode) {
                        // Sign in with email and password using Firebase Auth (MA-04)
                        FirebaseManager.auth
                            .signInWithEmailAndPassword(email.trim(), password.trim())
                            .addOnSuccessListener {
                                isLoading = false
                                onLoginSuccess()
                            }
                            .addOnFailureListener { exception ->
                                isLoading = false
                                errorMessage = exception.message
                                    ?: context.getString(R.string.login_error_login_failed)
                            }
                    } else {
                        // Create a new user account with Firebase Auth (MA-04)
                        FirebaseManager.auth
                            .createUserWithEmailAndPassword(email.trim(), password.trim())
                            .addOnSuccessListener {
                                isLoading = false
                                onLoginSuccess()
                            }
                            .addOnFailureListener { exception ->
                                isLoading = false
                                errorMessage = exception.message
                                    ?: context.getString(R.string.login_error_register_failed)
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isLoginMode) stringResource(R.string.login_sign_in_button)
                    else stringResource(R.string.login_create_account_button)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Toggle between login and register mode (MA-02)
        TextButton(
            onClick = {
                isLoginMode = !isLoginMode
                errorMessage = ""
            }
        ) {
            Text(
                text = if (isLoginMode) stringResource(R.string.login_no_account)
                else stringResource(R.string.login_already_account)
            )
        }
    }
}