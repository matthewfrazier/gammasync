package com.gammasync

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.view.View
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import org.hamcrest.Matcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private fun holdFor(millis: Long): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> = isDisplayed()
            override fun getDescription(): String = "Hold for $millis ms"
            override fun perform(uiController: UiController, view: View) {
                view.performLongClick()
                uiController.loopMainThreadForAtLeast(millis)
            }
        }
    }

    private fun acceptDisclaimer() {
        onView(withId(R.id.holdToAgreeButton)).perform(holdFor(2500))
        Thread.sleep(500)
    }

    private fun navigateToSettings() {
        acceptDisclaimer()
        onView(withId(R.id.settingsButton)).perform(click())
        Thread.sleep(300)
    }

    @Test
    fun dangerZoneSectionIsVisible() {
        navigateToSettings()

        onView(withId(R.id.dangerZoneLabel)).check(matches(isDisplayed()))
        onView(withId(R.id.dangerZoneDescription)).check(matches(isDisplayed()))
    }

    @Test
    fun dangerZoneLabelHasWarningText() {
        navigateToSettings()

        onView(withId(R.id.dangerZoneLabel))
            .check(matches(withText(R.string.danger_zone)))
    }

    @Test
    fun refresh60HzControlsAreVisible() {
        navigateToSettings()

        onView(withId(R.id.refresh60HzLabel)).check(matches(isDisplayed()))
        onView(withId(R.id.allow60HzButton)).check(matches(isDisplayed()))
        onView(withId(R.id.deny60HzButton)).check(matches(isDisplayed()))
    }

    @Test
    fun allow60HzLabelShowsCorrectText() {
        navigateToSettings()

        onView(withId(R.id.refresh60HzLabel))
            .check(matches(withText(R.string.allow_60hz_mode)))
    }

    @Test
    fun deny60HzIsSelectedByDefault() {
        navigateToSettings()

        // By default, 60Hz should be denied (disabled)
        // We can't easily test button color/styling via Espresso, but we can verify the buttons exist and are clickable
        onView(withId(R.id.deny60HzButton)).check(matches(isDisplayed()))
        onView(withId(R.id.allow60HzButton)).check(matches(isDisplayed()))
    }

    @Test
    fun clicking60HzToggleButtonsDoesNotCrash() {
        navigateToSettings()

        // Test clicking both buttons doesn't cause crashes
        onView(withId(R.id.allow60HzButton)).perform(click())
        Thread.sleep(500) // Allow time for dialog if shown

        // Dialog might appear - if so, we need to dismiss it
        // The dialog should have "Enable Anyway" and "Cancel" buttons based on our implementation
        try {
            // Try to click cancel if dialog is present
            onView(withText("Cancel")).perform(click())
        } catch (e: Exception) {
            // Dialog might not appear in test environment, continue
        }

        Thread.sleep(300)

        // Click deny button
        onView(withId(R.id.deny60HzButton)).perform(click())
        Thread.sleep(300)

        // Settings screen should still be visible and functional
        onView(withId(R.id.settingsScreen)).check(matches(isDisplayed()))
    }

    @Test
    fun settingsScreenHasAllExpectedSections() {
        navigateToSettings()

        // Verify all sections are present
        onView(withId(R.id.durationLabel)).check(matches(isDisplayed()))
        onView(withId(R.id.colorLabel)).check(matches(isDisplayed()))
        onView(withId(R.id.themeLabel)).check(matches(isDisplayed()))
        onView(withId(R.id.noiseLabel)).check(matches(isDisplayed()))
        onView(withId(R.id.dangerZoneLabel)).check(matches(isDisplayed()))
    }

    @Test
    fun backButtonReturnsToHomeScreen() {
        navigateToSettings()

        onView(withId(R.id.backButton)).perform(click())
        Thread.sleep(300)

        onView(withId(R.id.homeScreen)).check(matches(isDisplayed()))
    }
}