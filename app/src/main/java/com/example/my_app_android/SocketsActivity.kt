package com.example.my_app_android

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import org.zeromq.SocketType
import org.zeromq.ZMQ
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SocketsActivity : AppCompatActivity(), LocationListener {

    private lateinit var serverView: TextView
    private lateinit var clientView: TextView
    private lateinit var clientText: EditText
    private lateinit var serverIpEdit: EditText
    private lateinit var sendBttn: Button
    private lateinit var handler: Handler
    private lateinit var switch: Switch

    private var flag = false

    private lateinit var locationManager: LocationManager
    private var currentLocation: Location? = null

    companion object {
        private const val PERMISSION_REQUEST_LOCATION = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sockets)

        initViews()
        setupLocation()
        startServerInBackground()

        serverView.text = "Сервер: Запущен на порту 12345"
        clientView.text = "Клиент: Готов к отправке"
    }

    private fun initViews() {
        serverView = findViewById(R.id.serverView)
        clientView = findViewById(R.id.clientView)
        sendBttn = findViewById(R.id.sendBttn)
        clientText = findViewById(R.id.client_text)
        serverIpEdit = findViewById(R.id.server_ip)
        handler = Handler(Looper.getMainLooper())
        switch = findViewById(R.id.switch1)
        switch.isChecked = flag

        sendBttn.setOnClickListener { Thread { startClient() }.start() }
        switch.setOnCheckedChangeListener { _, isChecked -> flag = isChecked }
    }

    private fun setupLocation() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (!checkPermissions()) {
            requestPermissions()
        } else {
            startLocationUpdates()
        }
    }

    private fun startLocationUpdates() {
        if (!checkPermissions()) return

        try {

            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L,
                0f,
                this
            )

            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                1000L,
                0f,
                this
            )

            val lastKnownGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val lastKnownNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            currentLocation = lastKnownGps ?: lastKnownNetwork

        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            PERMISSION_REQUEST_LOCATION
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates()
            }
        }
    }

    override fun onLocationChanged(location: Location) {
        currentLocation = location
    }

    private fun startServer() {
        val context = ZMQ.context(1)
        val socket = context.socket(SocketType.REP)
        socket.bind("tcp://*:12345")

        var counter = 0
        while (true) {
            counter++
            val requestBytes = socket.recv(0)
            val request = String(requestBytes, ZMQ.CHARSET)
            handler.post { serverView.text = "Получено: $request" }

            val response = "Получено сообщение №$counter"
            socket.send(response.toByteArray(ZMQ.CHARSET), 0)
        }
    }

    private fun startClient() {
        val context = ZMQ.context(1)
        val socket = context.socket(SocketType.REQ)

        val connectAddress = if (flag) {
            val ip = serverIpEdit.text.toString().trim()
            if (ip.isEmpty()) {
                handler.post { clientView.text = "Ошибка: IP не введён" }
                socket.close()
                context.term()
                return
            }
            "tcp://$ip:5555"
        } else {
            "tcp://localhost:12345"
        }

        try {
            socket.connect(connectAddress)

            val json = JSONObject().apply {
                put("message", clientText.text.toString().ifEmpty { "Hello from Android!" })

                currentLocation?.let { loc ->
                    put("latitude", loc.latitude)
                    put("longitude", loc.longitude)
                    put("altitude", loc.altitude)
                    put("time", SimpleDateFormat("HH:mm:ss dd.MM.yyyy", Locale.getDefault())
                        .format(Date(loc.time)))
                } ?: put("location_status", "недоступно")
            }

            val messageToSend = json.toString()
            socket.send(messageToSend.toByteArray(ZMQ.CHARSET), 0)

            val reply = String(socket.recv(0), ZMQ.CHARSET)
            handler.post { clientView.text = reply }

        } catch (e: Exception) {
            handler.post { clientView.text = "Ошибка: ${e.message}" }
        } finally {
            socket.close()
            context.term()
        }
    }

    private fun startServerInBackground() {
        Thread { startServer() }.start()
    }

    override fun onResume() {
        super.onResume()
        if (checkPermissions()) {
            startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        locationManager.removeUpdates(this)
    }
}