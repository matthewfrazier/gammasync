package com.gammasync

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun timerDisplaysInitialValue() {
        onView(withId(R.id.timerText))
            .check(matches(isDisplayed()))
            .check(matches(withText("00:00")))
    }

    @Test
    fun startButtonIsEnabledInitially() {
        onView(withId(R.id.startButton))
            .check(matches(isDisplayed()))
            .check(matches(isEnabled()))
    }

    @Test
    fun stopButtonIsDisabledInitially() {
        onView(withId(R.id.stopButton))
            .check(matches(isDisplayed()))
            .check(matches(not(isEnabled())))
    }

    @Test
    fun clickingStartDisablesStartAndEnablesStop() {
        onView(withId(R.id.startButton)).perform(click())

        onView(withId(R.id.startButton)).check(matches(not(isEnabled())))
        onView(withId(R.id.stopButton)).check(matches(isEnabled()))
    }

    @Test
    fun clickingStopEnablesStartAndDisablesStop() {
        onView(withId(R.id.startButton)).perform(click())
        onView(withId(R.id.stopButton)).perform(click())

        onView(withId(R.id.startButton)).check(matches(isEnabled()))
        onView(withId(R.id.stopButton)).check(matches(not(isEnabled())))
    }

    @Test
    fun resetButtonResetsTimerToZero() {
        onView(withId(R.id.startButton)).perform(click())
        Thread.sleep(1500) // Let timer tick
        onView(withId(R.id.resetButton)).perform(click())

        onView(withId(R.id.timerText)).check(matches(withText("00:00")))
        onView(withId(R.id.startButton)).check(matches(isEnabled()))
        onView(withId(R.id.stopButton)).check(matches(not(isEnabled())))
    }

    @Test
    fun timerIncrementsAfterStart() {
        onView(withId(R.id.startButton)).perform(click())
        Thread.sleep(1500)
        onView(withId(R.id.stopButton)).perform(click())

        onView(withId(R.id.timerText)).check(matches(withText("00:01")))
    }
}
