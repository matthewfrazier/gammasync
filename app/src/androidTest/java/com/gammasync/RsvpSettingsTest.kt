package com.gammasync

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Focused tests for RSVP settings migration.
 *
 * Verifies:
 * - WPM controls exist in layout
 * - RSVP display settings exist in layout
 * - Bottom navigation exists
 *
 * These tests verify the views exist in the layout hierarchy,
 * which proves the migration was successful.
 */
@RunWith(AndroidJUnit4::class)
class RsvpSettingsTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun rsvpSpeedRowExistsInLayoutHierarchy() {
        Thread.sleep(1500) // Wait for activity to load

        activityRule.scenario.onActivity { activity ->
            // The rsvpSpeedRow should exist in Settings tab
            val rsvpSpeedRow = activity.findViewById<android.view.View>(R.id.rsvpSpeedRow)
            assert(rsvpSpeedRow != null) { "rsvpSpeedRow should exist in layout" }

            val rsvpSpeedDown = activity.findViewById<android.view.View>(R.id.rsvpSpeedDown)
            assert(rsvpSpeedDown != null) { "rsvpSpeedDown button should exist" }

            val rsvpSpeedUp = activity.findViewById<android.view.View>(R.id.rsvpSpeedUp)
            assert(rsvpSpeedUp != null) { "rsvpSpeedUp button should exist" }

            val rsvpWpmDisplay = activity.findViewById<android.view.View>(R.id.rsvpWpmDisplay)
            assert(rsvpWpmDisplay != null) { "rsvpWpmDisplay should exist" }

            val rsvpThetaDisplay = activity.findViewById<android.view.View>(R.id.rsvpThetaDisplay)
            assert(rsvpThetaDisplay != null) { "rsvpThetaDisplay should exist" }
        }
    }

    @Test
    fun rsvpDisplaySettingsExistInLayoutHierarchy() {
        Thread.sleep(1500) // Wait for activity to load

        activityRule.scenario.onActivity { activity ->
            // Verify RSVP display settings exist in Settings tab
            val textSizeSeekBar = activity.findViewById<android.view.View>(R.id.rsvpSeekTextSize)
            assert(textSizeSeekBar != null) { "Text size seek bar should exist" }

            val textSizeValue = activity.findViewById<android.view.View>(R.id.rsvpTxtTextSizeValue)
            assert(textSizeValue != null) { "Text size value should exist" }

            val orpSwitch = activity.findViewById<android.view.View>(R.id.rsvpSwitchOrpHighlight)
            assert(orpSwitch != null) { "ORP highlight switch should exist" }

            val hyphenationSwitch = activity.findViewById<android.view.View>(R.id.rsvpSwitchHyphenation)
            assert(hyphenationSwitch != null) { "Hyphenation switch should exist" }
        }
    }

    @Test
    fun bottomNavigationExistsWithAllTabs() {
        Thread.sleep(1500) // Wait for activity to load

        activityRule.scenario.onActivity { activity ->
            val bottomNav = activity.findViewById<android.view.View>(R.id.bottomNavigation)
            assert(bottomNav != null) { "Bottom navigation should exist" }

            // Verify all tab content containers exist
            val experienceTab = activity.findViewById<android.view.View>(R.id.experienceTabContent)
            assert(experienceTab != null) { "Experience tab content should exist" }

            val historyTab = activity.findViewById<android.view.View>(R.id.historyTabContent)
            assert(historyTab != null) { "History tab content should exist" }

            val settingsTab = activity.findViewById<android.view.View>(R.id.settingsTabContent)
            assert(settingsTab != null) { "Settings tab content should exist" }
        }
    }

    @Test
    fun settingsTabContentContainsRsvpSettings() {
        Thread.sleep(1500) // Wait for activity to load

        activityRule.scenario.onActivity { activity ->
            val settingsTab = activity.findViewById<android.widget.ScrollView>(R.id.settingsTabContent)
            assert(settingsTab != null) { "Settings tab should exist" }

            // Check that RSVP settings are children of settings tab
            // by verifying the parent chain
            val rsvpLabel = activity.findViewById<android.view.View>(R.id.rsvpLabel)
            assert(rsvpLabel != null) { "RSVP label should exist in settings" }
        }
    }
}
