package com.example.my_app_android

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.TrafficStats
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.telephony.*
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class BackgroundDataService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var currentLocation: Location? = null

    private var serverIp = "192.168.0.11"
    private var currentFlag = 1

    private var sendLocation = true
    private var sendLTE = true
    private var sendGSM = true
    private var sendNR = true
    private var sendTraffic = true

    private val NOTIFICATION_ID = 12345
    private val CHANNEL_ID = "background_service_channel"

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        startLocationUpdates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serverIp = intent?.getStringExtra("server_ip") ?: serverIp
        currentFlag = intent?.getIntExtra("flag", 1) ?: 1
        serviceScope.launch {
            while (isActive) {
                currentLocation?.let { location ->
                    sendAllData(location)
                }
                delay(5000)
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Background Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Data Collection Service")
            .setContentText("Collecting location and cell info")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(2000)
            .build()
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    currentLocation = location
                    sendLocationUpdate(location)
                    getCellInfo()
                }
            }
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
        }
    }

    private fun sendLocationUpdate(location: Location) {
        val intent = Intent("LOCATION_UPDATE")
        intent.putExtra("latitude", location.latitude.toString())
        intent.putExtra("longitude", location.longitude.toString())
        intent.putExtra("altitude", location.altitude.toInt().toString())
        intent.putExtra("accuracy", location.accuracy.toString())
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        intent.putExtra("time", sdf.format(Date(location.time)))
        val totalTraffic = TrafficStats.getTotalRxBytes() + TrafficStats.getTotalTxBytes()
        intent.putExtra("traffic", "${totalTraffic / 1024 / 1024} MB")
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun getCellInfo() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val cellInfoList = telephonyManager.allCellInfo
        val identityList = mutableListOf<String>()
        val strengthList = mutableListOf<String>()

        if (cellInfoList != null) {
            for (cellInfo in cellInfoList) {
                when {
                    cellInfo is CellInfoLte && currentFlag == 1 -> {
                        with(cellInfo.cellIdentity) {
                            identityList.add("Band: $bandwidth")
                            identityList.add("CI: $ci")
                            identityList.add("EARFCN: $earfcn")
                            identityList.add("MCC: $mcc")
                            identityList.add("MNC: $mnc")
                            identityList.add("PCI: $pci")
                            identityList.add("TAC: $tac")
                        }
                        with(cellInfo.cellSignalStrength) {
                            strengthList.add("ASU: $asuLevel")
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                strengthList.add("CQI: $cqi")
                                strengthList.add("RSRP: $rsrp")
                                strengthList.add("RSRQ: $rsrq")
                                strengthList.add("RSSNR: $rssnr")
                            }
                            strengthList.add("RSSI: $rssi")
                            strengthList.add("TA: $timingAdvance")
                        }
                    }
                    cellInfo is CellInfoGsm && currentFlag == 2 -> {
                        with(cellInfo.cellIdentity) {
                            identityList.add("CID: $cid")
                            identityList.add("BSIC: $bsic")
                            identityList.add("ARFCN: $arfcn")
                            identityList.add("LAC: $lac")
                            identityList.add("MCC: $mcc")
                            identityList.add("MNC: $mnc")
                        }
                        with(cellInfo.cellSignalStrength) {
                            strengthList.add("Dbm: $dbm")
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                strengthList.add("RSSI: $rssi")
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                strengthList.add("TA: $timingAdvance")
                            }
                        }
                    }
                    cellInfo is CellInfoNr && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && currentFlag == 3 -> {
                        val cellIdentity = cellInfo.cellIdentity as CellIdentityNr
                        val cellSignal = cellInfo.cellSignalStrength as CellSignalStrengthNr
                        with(cellIdentity) {
                            identityList.add("Band: ${bands?.joinToString()}")
                            identityList.add("NCI: $nci")
                            identityList.add("PCI: $pci")
                            identityList.add("NRARFCN: $nrarfcn")
                            identityList.add("TAC: $tac")
                            identityList.add("MCC: $mccString")
                            identityList.add("MNC: $mncString")
                        }
                        with(cellSignal) {
                            strengthList.add("RSRP: $dbm")
                            strengthList.add("RSRQ: $ssRsrq")
                            strengthList.add("SINR: $ssSinr")
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                strengthList.add("TA: ${timingAdvanceMicros} µs")
                            }
                        }
                    }
                }
            }
        }

        val intent = Intent("CELL_INFO_UPDATE")
        intent.putStringArrayListExtra("identity_list", ArrayList(identityList))
        intent.putStringArrayListExtra("strength_list", ArrayList(strengthList))
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun sendAllData(location: Location) {
        try {
            val json = JSONObject()

            if (sendLocation) {
                val loc = JSONObject()
                loc.put("latitude", location.latitude)
                loc.put("longitude", location.longitude)
                loc.put("altitude", location.altitude.toInt())
                loc.put("accuracy", location.accuracy)
                loc.put("current_time", location.time)
                json.put("location", loc)
            }

            if (sendTraffic) {
                val traffic = JSONObject()
                traffic.put("total_rx", TrafficStats.getTotalRxBytes())
                traffic.put("total_tx", TrafficStats.getTotalTxBytes())
                traffic.put("total", TrafficStats.getTotalRxBytes() + TrafficStats.getTotalTxBytes())
                json.put("traffic", traffic)
            }

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED
            ) {
                val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                val cellInfoList = telephonyManager.allCellInfo
                val cellsArray = JSONArray()
                if (cellInfoList != null) {
                    for (cellInfo in cellInfoList) {
                        val cellJson = JSONObject()
                        when (cellInfo) {
                            is CellInfoLte -> {
                                if (!sendLTE) continue
                                cellJson.put("type", "LTE")
                                with(cellInfo.cellIdentity) {
                                    cellJson.put("band", bandwidth)
                                    cellJson.put("cell_identity", ci)
                                    cellJson.put("earfcn", earfcn)
                                    cellJson.put("mcc", mcc)
                                    cellJson.put("mnc", mnc)
                                    cellJson.put("pci", pci)
                                    cellJson.put("tac", tac)
                                }
                                with(cellInfo.cellSignalStrength) {
                                    cellJson.put("asu_level", asuLevel)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        cellJson.put("cqi", cqi)
                                        cellJson.put("rsrp", rsrp)
                                        cellJson.put("rsrq", rsrq)
                                        cellJson.put("rssnr", rssnr)
                                    }
                                    cellJson.put("rssi", rssi)
                                    cellJson.put("timing_advance", timingAdvance)
                                }
                            }
                            is CellInfoGsm -> {
                                if (!sendGSM) continue
                                cellJson.put("type", "GSM")
                                with(cellInfo.cellIdentity) {
                                    cellJson.put("cell_identity", cid)
                                    cellJson.put("bsic", bsic)
                                    cellJson.put("arfcn", arfcn)
                                    cellJson.put("lac", lac)
                                    cellJson.put("mcc", mcc)
                                    cellJson.put("mnc", mnc)
                                }
                                with(cellInfo.cellSignalStrength) {
                                    cellJson.put("dbm", dbm)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                        cellJson.put("rssi", rssi)
                                    }
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        cellJson.put("timing_advance", timingAdvance)
                                    }
                                }
                            }
                            is CellInfoNr -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    if (!sendNR) continue
                                    cellJson.put("type", "NR")
                                    val cellIdentity = cellInfo.cellIdentity as CellIdentityNr
                                    val cellSignal = cellInfo.cellSignalStrength as CellSignalStrengthNr
                                    with(cellIdentity) {
                                        cellJson.put("band", bands?.joinToString())
                                        cellJson.put("nci", nci.toString())
                                        cellJson.put("pci", pci)
                                        cellJson.put("nrarfcn", nrarfcn)
                                        cellJson.put("tac", tac)
                                        cellJson.put("mcc", mccString?.toIntOrNull() ?: 0)
                                        cellJson.put("mnc", mncString?.toIntOrNull() ?: 0)
                                    }
                                    with(cellSignal) {
                                        cellJson.put("ss_rsrp", dbm)
                                        cellJson.put("ss_rsrq", ssRsrq)
                                        cellJson.put("ss_sinr", ssSinr)
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                            cellJson.put("timing_advance", timingAdvanceMicros)
                                        }
                                    }
                                }
                            }
                        }
                        if (cellJson.length() > 0) {
                            cellsArray.put(cellJson)
                        }
                    }
                }
                json.put("cells", cellsArray)
            }

            Thread {
                try {
                    ZContext().use { context ->
                        val socket = context.createSocket(SocketType.REQ)
                        socket.receiveTimeOut = 3000
                        socket.connect("tcp://$serverIp:5555")
                        socket.send(json.toString().toByteArray(ZMQ.CHARSET), 0)
                        val response = String(socket.recv(0) ?: ByteArray(0), ZMQ.CHARSET)
                        if (response.isNotEmpty() && response != "OK") {
                            handleServerCommand(response)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()
            
            saveDataToFile(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleServerCommand(response: String) {
        try {
            val command = JSONObject(response)
            if (command.getString("type") == "filter_update") {
                val filters = command.getJSONObject("filters")
                sendLocation = filters.optBoolean("location", sendLocation)
                sendLTE = filters.optBoolean("lte", sendLTE)
                sendGSM = filters.optBoolean("gsm", sendGSM)
                sendNR = filters.optBoolean("nr", sendNR)
                sendTraffic = filters.optBoolean("traffic", sendTraffic)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveDataToFile(json: JSONObject) {
        val filename = "background_data.json"
        val downDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downDir, filename)

        var locationsArray: JSONArray

        if (!file.exists()) {
            locationsArray = JSONArray()
            android.util.Log.d("BackgroundData", "Created new file: ${file.absolutePath}")
        } else {
            try {
                val existingContent = file.readText()
                android.util.Log.d("BackgroundData", "File exists, size: ${file.length()} bytes")
                val existingJson = JSONObject(existingContent)
                val existingArray = existingJson.getJSONArray("locations")
                locationsArray = existingArray
                android.util.Log.d("BackgroundData", "Existing entries: ${locationsArray.length()}")
            } catch (e: Exception) {
                android.util.Log.e("BackgroundData", "Error reading file, starting fresh: ${e.message}")
                e.printStackTrace()
                locationsArray = JSONArray()
            }
        }

        val locationEntry = JSONObject()

        val currentTimeMillis = System.currentTimeMillis()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        locationEntry.put("time", sdf.format(Date(currentTimeMillis)))
        locationEntry.put("time_milliseconds", currentTimeMillis)

        if (json.has("location")) {
            val loc = json.getJSONObject("location")
            locationEntry.put("latitude", loc.getDouble("latitude"))
            locationEntry.put("longitude", loc.getDouble("longitude"))
            locationEntry.put("altitude", loc.getDouble("altitude"))
            locationEntry.put("accuracy", loc.getDouble("accuracy"))
        }

        if (json.has("traffic")) {
            val traffic = json.getJSONObject("traffic")
            locationEntry.put("traffic", traffic)
        }

        if (json.has("cells")) {
            val cells = json.getJSONArray("cells")
            locationEntry.put("cells", cells)
        }

        locationsArray.put(locationEntry)

        val outputJson = JSONObject()
        outputJson.put("locations", locationsArray)

        try {
            file.writeText(outputJson.toString())
            android.util.Log.d("BackgroundData", "Saved successfully. New size: ${file.length()} bytes, entries: ${locationsArray.length()}")
        } catch (e: Exception) {
            android.util.Log.e("BackgroundData", "Error writing file: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        serviceJob.cancel()
    }
}