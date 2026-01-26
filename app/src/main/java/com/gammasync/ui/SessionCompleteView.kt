/*
 * MIT License
 * Copyright (c) 2026 matthewfrazier
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.gammasync.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import com.gammasync.R
import com.gammasync.infra.HapticFeedback

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
