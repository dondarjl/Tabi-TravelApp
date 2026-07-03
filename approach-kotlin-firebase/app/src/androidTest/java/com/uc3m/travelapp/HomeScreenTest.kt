package com.uc3m.travelapp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Instrumented UI test for the Home screen (MA-08)
@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val testRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeScreen_fabIsDisplayed() {
        // Verify that the FAB to add a new trip is visible on the Home screen
        // This test only makes sense if the user is already logged in
        testRule
            .onNodeWithContentDescription("Add trip")
            .assertIsDisplayed()
    }

    @Test
    fun homeScreen_searchIconIsDisplayed() {
        // Verify that the search icon is visible in the top bar (MA-08)
        testRule
            .onNodeWithContentDescription("Search")
            .assertIsDisplayed()
    }

    @Test
    fun homeScreen_appTitleIsDisplayed() {
        // Verify that the app title appears in the top bar
        testRule
            .onNodeWithText("TravelApp")
            .assertIsDisplayed()
    }
}