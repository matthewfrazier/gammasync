package com.cognihertz.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import com.cognihertz.R
import com.cognihertz.infra.HapticFeedback

/**
 * Session complete screen with summary and navigation options.
 */
class SessionCompleteView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var onStartAnother: (() -> Unit)? = null
    var onExit: (() -> Unit)? = null

    private val durationSummary: TextView
    private val startAnotherButton: Button
    private val exitButton: Button
    private val haptics = HapticFeedback(context)

    init {
        LayoutInflater.from(context).inflate(R.layout.view_session_complete, this, true)

        durationSummary = findViewById(R.id.durationSummary)
        startAnotherButton = findViewById(R.id.startAnotherButton)
        exitButton = findViewById(R.id.exitButton)

        startAnotherButton.setOnClickListener {
            haptics.heavyClick()
            onStartAnother?.invoke()
        }

        exitButton.setOnClickListener {
            haptics.click()
            onExit?.invoke()
        }
    }

    fun setSessionDuration(minutes: Int) {
        durationSummary.text = context.getString(R.string.session_duration_format, minutes)
    }
}
