package com.uc3m.travelapp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.compose.ui.test.performClick

// Instrumented UI test for the Login screen (MA-08)
// This test runs on the AVD and interacts with the real UI
@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    // createAndroidComposeRule launches the real MainActivity before each test (MA-08)
    @get:Rule
    val testRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun loginScreen_signInButtonIsDisplayedAndEnabled() {
        // If the user is not logged in, the Login screen should be visible
        // We verify that the Sign in button exists and is enabled (MA-08)
        testRule
            .onNodeWithText("Sign in")
            .assertIsDisplayed()
        testRule
            .onNodeWithText("Sign in")
            .assertIsEnabled()
    }

    @Test
    fun loginScreen_emailAndPasswordFieldsAreDisplayed() {
        // Verify that both input fields are visible on the login screen
        testRule
            .onNodeWithText("Email")
            .assertIsDisplayed()
        testRule
            .onNodeWithText("Password")
            .assertIsDisplayed()
    }

    @Test
    fun loginScreen_toggleToRegisterModeWorks() {
        // Tap the toggle button and verify the screen switches to register mode
        // The button text changes from "Don't have an account?" to "Already have an account?"
        testRule
            .onNodeWithText("Don't have an account? Register")
            .assertIsDisplayed()
        testRule
            .onNodeWithText("Don't have an account? Register")
            .performClick()
        testRule
            .onNodeWithText("Already have an account? Sign in")
            .assertIsDisplayed()
    }
}