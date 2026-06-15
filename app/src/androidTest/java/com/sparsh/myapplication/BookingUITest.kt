package com.sparsh.myapplication

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookingUITest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun clearDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("hotel_fund_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        composeTestRule.activityRule.scenario.recreate()
    }

    @Test
    fun testAddEditDeleteBookingFlow() {
        // 1. Initially on Dashboard, verify empty state
        composeTestRule.onNodeWithText("No bookings yet").assertIsDisplayed()

        // 2. Click "Add Booking" tab
        composeTestRule.onNodeWithText("Add Booking").performClick()

        // 3. Select MMT Platform
        composeTestRule.onNodeWithText("MMT").performClick()

        // 4. Input Guest Name
        composeTestRule.onNodeWithText("Guest Name").performTextInput("Test MMT Guest")

        // 5. Add Room
        composeTestRule.onNodeWithText("Add Item").performClick()
        composeTestRule.onNodeWithText("Room Category").performClick()

        // 6. Set Room Details
        composeTestRule.onNode(hasText("Room No.")).performScrollTo().performTextInput("104")
        composeTestRule.onNode(hasText("Rate")).performScrollTo().performTextInput("2500")

        // 7. Input platform commission
        composeTestRule.onNodeWithText("OTA Commission / Expenses").performScrollTo().performTextInput("375")

        // 8. Submit booking
        composeTestRule.onNodeWithText("Add Booking to Funds").performScrollTo().performClick()

        // 9. Back on Dashboard, check that the booking is listed
        composeTestRule.onNodeWithText("Test MMT Guest").assertIsDisplayed()
        composeTestRule.onNodeWithText("MMT").assertIsDisplayed()
        composeTestRule.onNodeWithText("Room 104").assertIsDisplayed()
        // Match substring to bypass formatting spacing differences
        composeTestRule.onNode(hasText("2,500", substring = true)).assertIsDisplayed()

        // 10. Click the booking palette (which should open to Edit mode)
        composeTestRule.onNodeWithText("Test MMT Guest").performClick()

        // 11. Verify we are on Edit Booking screen
        composeTestRule.onNodeWithText("Edit Booking").assertIsDisplayed()
        composeTestRule.onNodeWithText("Save Changes").assertIsDisplayed()

        // 12. Edit the guest name
        composeTestRule.onNodeWithText("Guest Name").performTextClearance()
        composeTestRule.onNodeWithText("Guest Name").performTextInput("Updated MMT Guest")

        // 13. Save Changes
        composeTestRule.onNodeWithText("Save Changes").performScrollTo().performClick()

        // 14. Verify updated name on Dashboard
        composeTestRule.onNodeWithText("Updated MMT Guest").assertIsDisplayed()

        // 15. Navigate to Search Screen
        composeTestRule.onNodeWithText("Search").performClick()

        // 16. Search guest
        composeTestRule.onNodeWithText("Search Guest Name, Room or Beds").performTextInput("Updated")
        composeTestRule.onNodeWithText("Updated MMT Guest").assertIsDisplayed()

        // 17. Click the filtered result -> Opens edit booking
        composeTestRule.onNodeWithText("Updated MMT Guest").performClick()
        composeTestRule.onNodeWithText("Edit Booking").assertIsDisplayed()

        // 18. Go back to Dashboard and delete
        composeTestRule.onNodeWithText("Dashboard").performClick()
        composeTestRule.onNodeWithContentDescription("Delete Booking").performClick()

        // 19. Verify empty state again
        composeTestRule.onNodeWithText("No bookings yet").assertIsDisplayed()
    }

    @Test
    fun testDormBedsMismatchValidation() {
        // Click Add Booking tab
        composeTestRule.onNodeWithText("Add Booking").performClick()

        // Select Direct Platform
        composeTestRule.onNodeWithText("Direct").performClick()

        // Add Dorm Item with 3 beds
        composeTestRule.onNodeWithText("Add Item").performClick()
        composeTestRule.onNodeWithText("Dorm Category").performClick()

        // Select beds count
        composeTestRule.onNodeWithText("+").performClick() // 2 beds
        composeTestRule.onNodeWithText("+").performClick() // 3 beds
        composeTestRule.onNodeWithText("Add").performClick()

        // In Dorm allocation card, input bed ranges that sum to 2 instead of 3
        composeTestRule.onNode(hasText("Enter beds, e.g. 1,2,3 or 1-4")).performScrollTo().performTextInput("1")
        // Second dorm room
        composeTestRule.onNode(hasText("Enter beds, e.g. 5,6 or 1-2")).performScrollTo().performTextInput("5")

        // Enter Dorm Rate
        composeTestRule.onNodeWithText("Total Amount for Dorm Beds").performScrollTo().performTextInput("1200")

        // Click Add Booking to Funds
        composeTestRule.onNodeWithText("Add Booking to Funds").performScrollTo().performClick()

        // Validation dialog popup should be shown
        composeTestRule.onNodeWithText("Validation Error").assertIsDisplayed()
        composeTestRule.onNodeWithText("Error: The number of beds entered in Room A and B (2 beds) does not match the selected bed count (3 beds).").assertIsDisplayed()

        // Dismiss dialog
        composeTestRule.onNodeWithText("OK").performClick()
        composeTestRule.onNodeWithText("Validation Error").assertDoesNotExist()
    }
}
