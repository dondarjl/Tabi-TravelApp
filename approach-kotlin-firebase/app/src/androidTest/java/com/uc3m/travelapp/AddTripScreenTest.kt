package com.uc3m.travelapp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Instrumented UI test for the Add Trip screen (MA-08)
// These tests verify the login flow and form validation behavior
@RunWith(AndroidJUnit4::class)
class AddTripScreenTest {

    @get:Rule
    val testRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun addTripScreen_loginWithEmptyFieldsShowsError() {
        // Verify that trying to login with empty fields shows an error (MA-08)
        // This test works from the Login screen which is always the start state
        testRule.waitUntil(10000) {
            testRule.onNodeWithText("Sign in").isDisplayed()
        }

        // Try to sign in without filling in any fields
        testRule.onNodeWithText("Sign in").performClick()

        // Verify the error message appears
        testRule
            .onNodeWithText("Please fill in all fields")
            .assertIsDisplayed()
    }

    @Test
    fun addTripScreen_loginWithShortPasswordShowsError() {
        // Verify that a password shorter than 6 characters shows an error (MA-08)
        testRule.waitUntil(10000) {
            testRule.onNodeWithText("Sign in").isDisplayed()
        }

        testRule.onNodeWithText("Email").performTextInput("test@test.com")
        testRule.onNodeWithText("Password").performTextInput("123")
        testRule.onNodeWithText("Sign in").performClick()

        // Verify the password length error appears
        testRule
            .onNodeWithText("Password must be at least 6 characters")
            .assertIsDisplayed()
    }
}