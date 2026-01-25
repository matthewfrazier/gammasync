package com.cognihertz.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.cognihertz.R

/**
 * Full-screen epilepsy warning disclaimer.
 * CRITICAL: Must be shown on EVERY cold launch per CLAUDE.md safety requirements.
 * Cannot be skipped or bypassed - requires 3-second hold to dismiss.
 */
class SafetyDisclaimerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var onDisclaimerAccepted: (() -> Unit)? = null

    private val holdButton: HoldProgressButton

    init {
        LayoutInflater.from(context).inflate(R.layout.view_safety_disclaimer, this, true)
        holdButton = findViewById(R.id.holdToAgreeButton)
        holdButton.onHoldComplete = {
            onDisclaimerAccepted?.invoke()
        }
    }
}
