package com.example.my_app_android

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Environment
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.round

class LocationActivity : AppCompatActivity(), LocationListener {

    companion object {
        private const val PERMISSION_REQUEST_ACCESS_LOCATION = 100
    }

    private lateinit var locationManager: LocationManager
    private lateinit var tvLat: TextView
    private lateinit var tvLon: TextView
    private lateinit var tvTime: TextView
    private lateinit var tvAlt: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        tvLat = findViewById(R.id.tv_lat)
        tvLon = findViewById(R.id.tv_lon)
        tvAlt = findViewById(R.id.tv_alt)
        tvTime = findViewById(R.id.tv_time)
    }

    override fun onResume() {
        super.onResume()
        updateCurrentLocation()
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    private fun updateCurrentLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                try {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        5000L,
                        0f,
                        this
                    )
                } catch (e: SecurityException) {
                    requestPermissions()
                }
            }
        } else {
            tvLat.text = "Permission is not granted"
            tvLon.text = "Permission is not granted"
            tvTime.text = "Permission is not granted"
            requestPermissions()
        }
    }

    private fun stopLocationUpdates() {
        locationManager.removeUpdates(this)
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_REQUEST_ACCESS_LOCATION
        )
    }

    private fun checkPermissions(): Boolean {
        return (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_ACCESS_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updateCurrentLocation()
            }
        }
    }

    private fun isLocationEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    override fun onLocationChanged(location: Location) {
        tvLat.text = "Широта: ${"%.8f".format(location.latitude).replace(',', '.')}"
        tvLon.text = "Долгота: ${"%.8f".format(location.longitude).replace(',', '.')}"
        tvAlt.text = "Высота: ${round(location.altitude)} м"

        val time = SimpleDateFormat("HH:mm:ss    dd.MM.yyyy", Locale.getDefault()).format(Date(location.time))
        tvTime.text = "Время: $time"

        tojson(location.latitude, location.longitude, location.altitude, location.time.toString())
    }

    private fun tojson(lat: Double, lon: Double, alt: Double, time: String) {
        val filename = "location.json"
        val downDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downDir, filename)

        val jsonObject: JSONObject
        val locationsArray: JSONArray

        if (!file.exists()) {
            jsonObject = JSONObject()
            locationsArray = JSONArray()
            jsonObject.put("locations", locationsArray)
        } else {
            jsonObject = JSONObject(file.readText())
            locationsArray = jsonObject.getJSONArray("locations")
        }

        val locationEntry = JSONObject()
        locationEntry.put("lat", lat)
        locationEntry.put("lon", lon)
        locationEntry.put("alt", round(alt))
        locationEntry.put("time", time)

        locationsArray.put(locationEntry)
        jsonObject.put("locations", locationsArray)

        file.writeText(jsonObject.toString())
    }
}