package com.example.my_app_android

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class ServiceBackgroundActivity : AppCompatActivity() {

    private lateinit var latitudeText: TextView
    private lateinit var longitudeText: TextView
    private lateinit var altitudeText: TextView
    private lateinit var accuracyText: TextView
    private lateinit var timeText: TextView
    private lateinit var trafficTotalText: TextView
    private lateinit var bttnLte: Button
    private lateinit var bttnGsm: Button
    private lateinit var bttnNr: Button
    private lateinit var listCellInfo1: ListView
    private lateinit var listCellInfo2: ListView
    private lateinit var cellIdentityTitle: TextView
    private lateinit var cellSignalTitle: TextView
    private lateinit var bStart: Button
    private lateinit var bStop: Button
    private lateinit var serverIpInput: EditText

    private var flag = 1

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    private val messageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            when (intent.action) {
                "LOCATION_UPDATE" -> {
                    latitudeText.text = intent.getStringExtra("latitude")
                    longitudeText.text = intent.getStringExtra("longitude")
                    altitudeText.text = intent.getStringExtra("altitude")
                    accuracyText.text = intent.getStringExtra("accuracy")
                    timeText.text = intent.getStringExtra("time")
                    trafficTotalText.text = intent.getStringExtra("traffic")
                }
                "CELL_INFO_UPDATE" -> {
                    val identityList = intent.getStringArrayListExtra("identity_list")
                    val strengthList = intent.getStringArrayListExtra("strength_list")
                    if (identityList != null) {
                        listCellInfo1.adapter = ArrayAdapter(
                            this@ServiceBackgroundActivity,
                            android.R.layout.simple_list_item_1,
                            identityList
                        )
                    }
                    if (strengthList != null) {
                        listCellInfo2.adapter = ArrayAdapter(
                            this@ServiceBackgroundActivity,
                            android.R.layout.simple_list_item_1,
                            strengthList
                        )
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_service_background)

        latitudeText = findViewById(R.id.latitude_service)
        longitudeText = findViewById(R.id.longitude_service)
        altitudeText = findViewById(R.id.altitude_service)
        accuracyText = findViewById(R.id.accuracy_service)
        timeText = findViewById(R.id.current_time_service)
        trafficTotalText = findViewById(R.id.traffic_total)
        bttnLte = findViewById(R.id.bttn_lte)
        bttnGsm = findViewById(R.id.bttn_gsm)
        bttnNr = findViewById(R.id.bttn_nr)
        cellIdentityTitle = findViewById(R.id.CellIdentity)
        cellSignalTitle = findViewById(R.id.CellSignalStrength)
        listCellInfo1 = findViewById(R.id.list_cell_info1)
        listCellInfo2 = findViewById(R.id.list_cell_info2)
        bStart = findViewById(R.id.bStartBg)
        bStop = findViewById(R.id.bStopBg)
        serverIpInput = findViewById(R.id.server_ip_input)

        loadSavedServerIp()
        checkPermissions()

        LocalBroadcastManager.getInstance(this).registerReceiver(
            messageReceiver,
            IntentFilter("LOCATION_UPDATE")
        )
        LocalBroadcastManager.getInstance(this).registerReceiver(
            messageReceiver,
            IntentFilter("CELL_INFO_UPDATE")
        )

        bttnLte.setOnClickListener {
            flag = 1
            updateButtonColors(bttnLte)
            cellIdentityTitle.text = "CellIdentityLTE"
            cellSignalTitle.text = "CellSignalStrengthLTE"
            Intent(this, BackgroundDataService::class.java).apply {
                putExtra("flag", flag)
                startService(this)
            }
        }

        bttnGsm.setOnClickListener {
            flag = 2
            updateButtonColors(bttnGsm)
            cellIdentityTitle.text = "CellIdentityGSM"
            cellSignalTitle.text = "CellSignalStrengthGSM"
            Intent(this, BackgroundDataService::class.java).apply {
                putExtra("flag", flag)
                startService(this)
            }
        }

        bttnNr.setOnClickListener {
            flag = 3
            updateButtonColors(bttnNr)
            cellIdentityTitle.text = "CellIdentityNR"
            cellSignalTitle.text = "CellSignalStrengthNR"
            Intent(this, BackgroundDataService::class.java).apply {
                putExtra("flag", flag)
                startService(this)
            }
        }

        bStart.setOnClickListener {
            val ip = serverIpInput.text.toString().trim()
            if (ip.isNotEmpty()) saveServerIp(ip)
            val intent = Intent(this, BackgroundDataService::class.java)
            intent.putExtra("server_ip", ip)
            intent.putExtra("flag", flag)
            startService(intent)
        }

        bStop.setOnClickListener {
            val intent = Intent(this, BackgroundDataService::class.java)
            stopService(intent)
        }
    }

    private fun loadSavedServerIp() {
        val sharedPref = getSharedPreferences("app_settings", MODE_PRIVATE)
        val savedIp = sharedPref.getString("server_ip", "192.168.0.11")
        serverIpInput.setText(savedIp)
    }

    private fun saveServerIp(ip: String) {
        val sharedPref = getSharedPreferences("app_settings", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("server_ip", ip)
            apply()
        }
    }

    private fun updateButtonColors(selected: Button) {
        bttnLte.setBackgroundColor(if (selected == bttnLte) Color.parseColor("#2d2d2f") else Color.parseColor("#16c603"))
        bttnGsm.setBackgroundColor(if (selected == bttnGsm) Color.parseColor("#2d2d2f") else Color.parseColor("#16c603"))
        bttnNr.setBackgroundColor(if (selected == bttnNr) Color.parseColor("#2d2d2f") else Color.parseColor("#16c603"))
    }

    private fun checkPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_PHONE_STATE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        val toRequest = permissions.filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (toRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, toRequest.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() &&
            grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver)
    }
}