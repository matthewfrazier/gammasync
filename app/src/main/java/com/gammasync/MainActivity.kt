package com.gammasync

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val textView = TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 24f
            setPadding(48, 48, 48, 48)
        }

        setContentView(textView)
    }
}
