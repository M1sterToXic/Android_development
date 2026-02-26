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

        btnCalculator.setOnClickListener {
            val intent = Intent(this, CalculatorActivity::class.java)
            startActivity(intent)
        }

        btnMusicPlayer.setOnClickListener {
            val intent = Intent(this, MusicPlayerActivity::class.java)
            startActivity(intent)
        }

        btnLocation.setOnClickListener {
            val intent = Intent(this, LocationActivity::class.java)
            startActivity(intent)
        }

        btnSockets.setOnClickListener {
            val intent = Intent(this, SocketsActivity::class.java)
            startActivity(intent)
        }
    }
}