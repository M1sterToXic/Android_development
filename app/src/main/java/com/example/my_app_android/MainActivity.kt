package com.example.my_app_android

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnCalculator = findViewById<Button>(R.id.btn_calculator)
        val btnMusicPlayer = findViewById<Button>(R.id.btn_music_player)
        val btnLocation = findViewById<Button>(R.id.btn_location)
        val btnSockets = findViewById<Button>(R.id.btn_sockets)
        val btnServiceBackground = findViewById<Button>(R.id.btn_service_background)

        btnCalculator.setOnClickListener {
            startActivity(Intent(this, CalculatorActivity::class.java))
        }

        btnMusicPlayer.setOnClickListener {
            startActivity(Intent(this, MusicPlayerActivity::class.java))
        }

        btnLocation.setOnClickListener {
            startActivity(Intent(this, LocationActivity::class.java))
        }

        btnSockets.setOnClickListener {
            startActivity(Intent(this, SocketsActivity::class.java))
        }

        btnServiceBackground.setOnClickListener {
            startActivity(Intent(this, ServiceBackgroundActivity::class.java))
        }
    }
}