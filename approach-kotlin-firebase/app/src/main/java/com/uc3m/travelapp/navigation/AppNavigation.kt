package com.uc3m.travelapp.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.uc3m.travelapp.R
import com.uc3m.travelapp.screens.addtrip.AddTripScreen
import com.uc3m.travelapp.screens.auth.LoginScreen
import com.uc3m.travelapp.screens.chat.ChatScreen
import com.uc3m.travelapp.screens.detail.TripDetailScreen
import com.uc3m.travelapp.screens.editprofile.EditProfileScreen
import com.uc3m.travelapp.screens.edittrip.EditTripScreen
import com.uc3m.travelapp.screens.home.HomeScreen
import com.uc3m.travelapp.screens.liked.LikedScreen
import com.uc3m.travelapp.screens.notifications.NotificationsScreen
import com.uc3m.travelapp.screens.profile.ProfileScreen
import com.uc3m.travelapp.screens.userprofile.UserProfileScreen

private val bottomNavScreens = listOf(
    Routes.HOME,
    Routes.LIKED,
    Routes.NOTIFICATIONS,
    Routes.PROFILE
)

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val LIKED = "liked"
    const val NOTIFICATIONS = "notifications"
    const val TRIP_DETAIL = "trip_detail/{tripId}"
    const val ADD_TRIP = "add_trip"
    const val EDIT_TRIP = "edit_trip/{tripId}"
    const val CHAT = "chat/{tripId}"
    const val PROFILE = "profile"
    const val EDIT_PROFILE = "edit_profile"
    const val USER_PROFILE = "user_profile/{userId}"

    fun tripDetail(tripId: String) = "trip_detail/$tripId"
    fun editTrip(tripId: String) = "edit_trip/$tripId"
    fun chat(tripId: String) = "chat/$tripId"
    fun userProfile(userId: String) = "user_profile/$userId"
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Routes.LOGIN
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomNavScreens

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentRoute == Routes.HOME,
                        onClick = {
                            if (currentRoute != Routes.HOME) {
                                navController.navigate(Routes.HOME) {
                                    popUpTo(Routes.HOME) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = stringResource(R.string.home_title),
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = null,
                        alwaysShowLabel = false,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.LIKED,
                        onClick = {
                            if (currentRoute != Routes.LIKED) {
                                navController.navigate(Routes.LIKED) {
                                    popUpTo(Routes.HOME) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = stringResource(R.string.liked_title),
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = null,
                        alwaysShowLabel = false,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.NOTIFICATIONS,
                        onClick = {
                            if (currentRoute != Routes.NOTIFICATIONS) {
                                navController.navigate(Routes.NOTIFICATIONS) {
                                    popUpTo(Routes.HOME) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = stringResource(R.string.notifications_title),
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = null,
                        alwaysShowLabel = false,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.PROFILE,
                        onClick = {
                            if (currentRoute != Routes.PROFILE) {
                                navController.navigate(Routes.PROFILE) {
                                    popUpTo(Routes.HOME) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = stringResource(R.string.profile_title),
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = null,
                        alwaysShowLabel = false,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.HOME) {
                HomeScreen(
                    onTripClick = { tripId ->
                        navController.navigate(Routes.tripDetail(tripId))
                    },
                    onAddTripClick = { navController.navigate(Routes.ADD_TRIP) }
                )
            }

            composable(Routes.LIKED) {
                LikedScreen(
                    onTripClick = { tripId ->
                        navController.navigate(Routes.tripDetail(tripId))
                    }
                )
            }

            composable(Routes.NOTIFICATIONS) {
                NotificationsScreen(
                    onTripClick = { tripId ->
                        navController.navigate(Routes.tripDetail(tripId))
                    }
                )
            }

            composable(Routes.TRIP_DETAIL) { backStackEntry ->
                val tripId = backStackEntry.arguments?.getString("tripId") ?: ""
                TripDetailScreen(
                    tripId = tripId,
                    onChatClick = { id -> navController.navigate(Routes.chat(id)) },
                    onEditClick = { id -> navController.navigate(Routes.editTrip(id)) },
                    onUserProfileClick = { userId ->
                        navController.navigate(Routes.userProfile(userId))
                    },
                    onOwnProfileClick = {
                        navController.navigate(Routes.PROFILE) {
                            popUpTo(Routes.HOME) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Routes.ADD_TRIP) {
                AddTripScreen(
                    onTripSaved = { navController.popBackStack() },
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Routes.EDIT_TRIP) { backStackEntry ->
                val tripId = backStackEntry.arguments?.getString("tripId") ?: ""
                EditTripScreen(
                    tripId = tripId,
                    onTripSaved = { navController.popBackStack() },
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Routes.CHAT) { backStackEntry ->
                val tripId = backStackEntry.arguments?.getString("tripId") ?: ""
                ChatScreen(
                    tripId = tripId,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Routes.PROFILE) {
                ProfileScreen(
                    onLogout = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onTripClick = { tripId ->
                        navController.navigate(Routes.tripDetail(tripId))
                    },
                    onEditProfileClick = { navController.navigate(Routes.EDIT_PROFILE) }
                )
            }

            composable(Routes.EDIT_PROFILE) {
                EditProfileScreen(
                    onBackClick = { navController.popBackStack() },
                    onAccountDeleted = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.USER_PROFILE) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                UserProfileScreen(
                    userId = userId,
                    onTripClick = { tripId ->
                        navController.navigate(Routes.tripDetail(tripId))
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}