package com.example.maplibrenativedemo

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button

class HomeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        findViewById<Button>(R.id.openMapButton).setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }
    }
}

