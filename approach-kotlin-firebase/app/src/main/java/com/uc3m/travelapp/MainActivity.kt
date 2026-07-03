package com.uc3m.travelapp

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.uc3m.travelapp.navigation.AppNavigation
import com.uc3m.travelapp.navigation.Routes
import com.uc3m.travelapp.ui.theme.TravelAppTheme

class MainActivity : ComponentActivity() {

    // Launcher to request the POST_NOTIFICATIONS permission at runtime (MA-07)
    // On Android 13+ the user must explicitly grant this permission
    // On older versions the permission is granted automatically at install time
    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // We do not need to handle the result here because the notification
        // is only shown after the user publishes a trip, not immediately
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request notification permission on Android 13 (API 33) and above (MA-07)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(
                android.Manifest.permission.POST_NOTIFICATIONS
            )
        }

        enableEdgeToEdge()
        setContent {
            TravelAppTheme {
                // If user is already logged in, start directly on Home
                // Otherwise start on Login
                val startDestination = if (FirebaseManager.isUserLoggedIn) {
                    Routes.HOME
                } else {
                    Routes.LOGIN
                }
                AppNavigation(startDestination = startDestination)
            }
        }
    }
}