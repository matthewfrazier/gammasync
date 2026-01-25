package com.cognihertz

import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matcher
import org.hamcrest.Matchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    /**
     * Custom action to hold (long press) a view for a specified duration.
     */
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

    /**
     * Helper to accept safety disclaimer (required before accessing home screen).
     */
    private fun acceptDisclaimer() {
        // Long press the accept button for 2 seconds
        onView(withId(R.id.holdToAgreeButton)).perform(holdFor(2500))
        Thread.sleep(500) // Wait for animation
    }

    // --- Disclaimer Screen Tests ---

    @Test
    fun disclaimerIsShownOnLaunch() {
        onView(withId(R.id.disclaimerScreen))
            .check(matches(isDisplayed()))
    }

    @Test
    fun acceptingDisclaimerShowsHomeScreen() {
        acceptDisclaimer()
        onView(withId(R.id.homeScreen))
            .check(matches(isDisplayed()))
    }

    // --- Mode Selection Tests ---

    @Test
    fun homeScreenShowsAllModeButtons() {
        acceptDisclaimer()

        onView(withId(R.id.modeNeuroSyncButton)).check(matches(isDisplayed()))
        onView(withId(R.id.modeMemoryButton)).check(matches(isDisplayed()))
        onView(withId(R.id.modeSleepButton)).check(matches(isDisplayed()))
        onView(withId(R.id.modeMigraineButton)).check(matches(isDisplayed()))
        onView(withId(R.id.modeMoodLiftButton)).check(matches(isDisplayed()))
    }

    @Test
    fun selectingNeuroSyncModeUpdatesDescription() {
        acceptDisclaimer()

        onView(withId(R.id.modeNeuroSyncButton)).perform(click())

        onView(withId(R.id.modeDescriptionText))
            .check(matches(withText(R.string.mode_neurosync_description)))
    }

    @Test
    fun selectingMemoryWriteModeUpdatesDescription() {
        acceptDisclaimer()

        onView(withId(R.id.modeMemoryButton)).perform(click())

        onView(withId(R.id.modeDescriptionText))
            .check(matches(withText(R.string.mode_memory_description)))
    }

    @Test
    fun selectingSleepRampModeUpdatesDescription() {
        acceptDisclaimer()

        onView(withId(R.id.modeSleepButton)).perform(click())

        onView(withId(R.id.modeDescriptionText))
            .check(matches(withText(R.string.mode_sleep_description)))
    }

    @Test
    fun selectingMigraineModeUpdatesDescription() {
        acceptDisclaimer()

        onView(withId(R.id.modeMigraineButton)).perform(click())

        onView(withId(R.id.modeDescriptionText))
            .check(matches(withText(R.string.mode_migraine_description)))
    }

    @Test
    fun selectingMoodLiftModeUpdatesDescription() {
        acceptDisclaimer()

        onView(withId(R.id.modeMoodLiftButton)).perform(click())

        onView(withId(R.id.modeDescriptionText))
            .check(matches(withText(R.string.mode_mood_lift_description)))
    }

    @Test
    fun moodLiftModeShowsXrealWarningWithoutExternalDisplay() {
        acceptDisclaimer()

        onView(withId(R.id.modeMoodLiftButton)).perform(click())

        // Warning should be visible because no XREAL is connected
        onView(withId(R.id.xrealWarningText)).check(matches(isDisplayed()))
    }

    @Test
    fun moodLiftModeDisablesStartButtonWithoutXreal() {
        acceptDisclaimer()

        onView(withId(R.id.modeMoodLiftButton)).perform(click())

        // Start button should be disabled without XREAL
        onView(withId(R.id.startSessionButton)).check(matches(not(isEnabled())))
    }

    @Test
    fun neuroSyncModeEnablesStartButton() {
        acceptDisclaimer()

        onView(withId(R.id.modeNeuroSyncButton)).perform(click())

        onView(withId(R.id.startSessionButton)).check(matches(isEnabled()))
    }

    // --- Duration Selection Tests ---

    @Test
    fun homeScreenShowsAllDurationButtons() {
        acceptDisclaimer()

        onView(withId(R.id.duration15Button)).check(matches(isDisplayed()))
        onView(withId(R.id.duration30Button)).check(matches(isDisplayed()))
        onView(withId(R.id.duration60Button)).check(matches(isDisplayed()))
    }

    // --- Session Start Tests ---

    @Test
    fun startingSessionShowsTherapyScreen() {
        acceptDisclaimer()

        // Select NeuroSync mode (doesn't require XREAL)
        onView(withId(R.id.modeNeuroSyncButton)).perform(click())
        onView(withId(R.id.startSessionButton)).perform(click())

        // Therapy screen should be displayed
        onView(withId(R.id.therapyScreen)).check(matches(isDisplayed()))
    }

    @Test
    fun therapyScreenHasPauseButton() {
        acceptDisclaimer()

        onView(withId(R.id.modeNeuroSyncButton)).perform(click())
        onView(withId(R.id.startSessionButton)).perform(click())

        // Tap to show controls
        onView(withId(R.id.therapyScreen)).perform(click())
        Thread.sleep(300)

        onView(withId(R.id.pauseButton)).check(matches(isDisplayed()))
    }

    @Test
    fun pausingSessionShowsPauseOverlay() {
        acceptDisclaimer()

        onView(withId(R.id.modeNeuroSyncButton)).perform(click())
        onView(withId(R.id.startSessionButton)).perform(click())

        // Tap to show controls, then pause
        onView(withId(R.id.therapyScreen)).perform(click())
        Thread.sleep(300)
        onView(withId(R.id.pauseButton)).perform(click())

        onView(withId(R.id.pauseOverlay)).check(matches(isDisplayed()))
    }

    @Test
    fun doneFromPauseGoesToHome() {
        acceptDisclaimer()

        onView(withId(R.id.modeNeuroSyncButton)).perform(click())
        onView(withId(R.id.startSessionButton)).perform(click())

        // Tap to show controls, pause, then done
        onView(withId(R.id.therapyScreen)).perform(click())
        Thread.sleep(300)
        onView(withId(R.id.pauseButton)).perform(click())
        onView(withId(R.id.doneButton)).perform(click())

        onView(withId(R.id.homeScreen)).check(matches(isDisplayed()))
    }

    // --- Mode Switching Integration Tests ---

    @Test
    fun canSwitchBetweenAllModesWithoutCrash() {
        acceptDisclaimer()

        // Rapidly switch through all modes
        onView(withId(R.id.modeNeuroSyncButton)).perform(click())
        onView(withId(R.id.modeMemoryButton)).perform(click())
        onView(withId(R.id.modeSleepButton)).perform(click())
        onView(withId(R.id.modeMigraineButton)).perform(click())
        onView(withId(R.id.modeMoodLiftButton)).perform(click())
        onView(withId(R.id.modeNeuroSyncButton)).perform(click())

        // Should still be on home screen without crash
        onView(withId(R.id.homeScreen)).check(matches(isDisplayed()))
    }

    @Test
    fun selectingModeUpdatesDurationToDefault() {
        acceptDisclaimer()

        // Memory Write has 20 min default, Sleep has 30 min default
        // Select different modes and verify we don't crash
        onView(withId(R.id.modeMemoryButton)).perform(click())
        onView(withId(R.id.modeSleepButton)).perform(click())

        // Should still be responsive
        onView(withId(R.id.startSessionButton)).check(matches(isEnabled()))
    }

    @Test
    fun startSessionWithDifferentModes() {
        acceptDisclaimer()

        // Test NeuroSync - pause and done goes to home
        onView(withId(R.id.modeNeuroSyncButton)).perform(click())
        onView(withId(R.id.startSessionButton)).perform(click())
        Thread.sleep(500)
        onView(withId(R.id.therapyScreen)).perform(click())
        Thread.sleep(300)
        onView(withId(R.id.pauseButton)).perform(click())
        onView(withId(R.id.doneButton)).perform(click())

        // Should be back on home, test Memory Write
        onView(withId(R.id.modeMemoryButton)).perform(click())
        onView(withId(R.id.startSessionButton)).perform(click())
        Thread.sleep(500)
        onView(withId(R.id.therapyScreen)).perform(click())
        Thread.sleep(300)
        onView(withId(R.id.pauseButton)).perform(click())
        onView(withId(R.id.doneButton)).perform(click())

        // Should be on home screen
        onView(withId(R.id.homeScreen)).check(matches(isDisplayed()))
    }

    // --- Settings & Color Scheme Tests ---

    @Test
    fun settingsButtonOpensSettingsScreen() {
        acceptDisclaimer()

        onView(withId(R.id.settingsButton)).perform(click())

        onView(withId(R.id.settingsScreen)).check(matches(isDisplayed()))
    }

    @Test
    fun changingColorSchemeDoesNotShowDisclaimer() {
        acceptDisclaimer()

        // Go to settings
        onView(withId(R.id.settingsButton)).perform(click())
        Thread.sleep(300)

        // Change color scheme by clicking blue
        onView(withId(R.id.colorBlue)).perform(click())
        Thread.sleep(300)

        // Go back to home
        onView(withId(R.id.backButton)).perform(click())
        Thread.sleep(300)

        // Should be on home screen, NOT disclaimer
        onView(withId(R.id.homeScreen)).check(matches(isDisplayed()))
        // Disclaimer should NOT be visible (it's in ViewFlipper but not displayed)
        onView(withId(R.id.disclaimerScreen)).check(matches(not(isDisplayed())))
        // The hold-to-agree button should not be visible
        onView(withId(R.id.holdToAgreeButton)).check(matches(not(isDisplayed())))
    }

    @Test
    fun changingMultipleColorSchemesNeverShowsDisclaimer() {
        acceptDisclaimer()

        // Go to settings and change colors multiple times
        onView(withId(R.id.settingsButton)).perform(click())
        Thread.sleep(300)

        onView(withId(R.id.colorBlue)).perform(click())
        Thread.sleep(200)
        onView(withId(R.id.colorPurple)).perform(click())
        Thread.sleep(200)
        onView(withId(R.id.colorGreen)).perform(click())
        Thread.sleep(200)
        onView(withId(R.id.colorOrange)).perform(click())
        Thread.sleep(200)
        onView(withId(R.id.colorRed)).perform(click())
        Thread.sleep(200)
        onView(withId(R.id.colorTeal)).perform(click())
        Thread.sleep(200)

        // Go back
        onView(withId(R.id.backButton)).perform(click())
        Thread.sleep(300)

        // Should still be on home, never shown disclaimer
        onView(withId(R.id.homeScreen)).check(matches(isDisplayed()))
        onView(withId(R.id.disclaimerScreen)).check(matches(not(isDisplayed())))
        onView(withId(R.id.holdToAgreeButton)).check(matches(not(isDisplayed())))
    }
}
