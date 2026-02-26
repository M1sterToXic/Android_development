package com.example.my_app_android

import android.Manifest
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.telephony.*
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.json.JSONObject
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import java.text.SimpleDateFormat
import java.util.*
import android.app.usage.NetworkStats.Bucket

class ServiceBackgroundActivity : AppCompatActivity(), LocationListener {

    private lateinit var locationManager: LocationManager
    private lateinit var latitude_service: TextView
    private lateinit var longitude_service: TextView
    private lateinit var altitude_service: TextView
    private lateinit var current_time_service: TextView
    private lateinit var accuracy_service: TextView
    private lateinit var traffic_total: TextView
    private lateinit var top_apps_list: ListView
    private lateinit var bttn_lte: Button
    private lateinit var bttn_gsm: Button
    private lateinit var bttn_nr: Button
    private lateinit var list_cell_info1: ListView
    private lateinit var list_cell_info2: ListView
    private lateinit var CellIdentity: TextView
    private lateinit var CellSignalStrength: TextView
    private lateinit var updateIndicator: TextView
    private lateinit var serverIpInput: EditText

    private val handler = Handler(Looper.getMainLooper())
    private var flag: Int = 1
    private var lat_site: Double = 0.0
    private var lon_site: Double = 0.0
    private var currentCellInfo: CellInfo? = null
    private var locationUpdateCounter = 0
    private var lastLocationTime: Long = 0

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        private const val LOCATION_UPDATE_INTERVAL = 5000L
        private const val MIN_TIME_BETWEEN_UPDATES = 1000L
        private const val SERVER_PORT = 5555
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_service_background)

        val rootView = window.decorView.findViewById<ViewGroup>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupClickListeners()
        loadSavedServerIp()
        checkPermissions()
    }

    private fun initViews() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        latitude_service = findViewById(R.id.latitude_service)
        longitude_service = findViewById(R.id.longitude_service)
        altitude_service = findViewById(R.id.altitude_service)
        current_time_service = findViewById(R.id.current_time_service)
        accuracy_service = findViewById(R.id.accuracy_service)
        traffic_total = findViewById(R.id.traffic_total)
        top_apps_list = findViewById(R.id.top_apps_list)
        bttn_lte = findViewById(R.id.bttn_lte)
        bttn_gsm = findViewById(R.id.bttn_gsm)
        bttn_nr = findViewById(R.id.bttn_nr)
        CellIdentity = findViewById(R.id.CellIdentity)
        CellSignalStrength = findViewById(R.id.CellSignalStrength)
        list_cell_info1 = findViewById(R.id.list_cell_info1)
        list_cell_info2 = findViewById(R.id.list_cell_info2)
        updateIndicator = findViewById(R.id.update_indicator)
        serverIpInput = findViewById(R.id.server_ip_input)

        resetLocationUI()
        traffic_total.text = "Рассчет трафика..."
        CellIdentity.text = "Выберите тип сети"
        CellSignalStrength.text = "Выберите тип сети"

        top_apps_list.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1,
            listOf("Ожидание данных о трафике..."))

        updateIndicator.setTextColor(Color.parseColor("#9E9E9E"))
    }

    private fun loadSavedServerIp() {
        val sharedPref = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val savedIp = sharedPref.getString("server_ip", "192.168.0.11")
        serverIpInput.setText(savedIp)
    }

    private fun saveServerIp(ip: String) {
        val sharedPref = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("server_ip", ip)
            apply()
        }
    }

    private fun resetLocationUI() {
        latitude_service.text = "Ожидание GPS..."
        longitude_service.text = "Ожидание GPS..."
        altitude_service.text = "Ожидание GPS..."
        current_time_service.text = "Ожидание GPS..."
        accuracy_service.text = "Ожидание GPS..."
    }

    private fun setupClickListeners() {
        bttn_lte.setOnClickListener {
            flag = 1
            CellIdentity.text = "CellIdentityLTE"
            CellSignalStrength.text = "CellSignalStrengthLTE"
            updateButtonColors(bttn_lte, bttn_gsm, bttn_nr)
            getCellInfo()
        }

        bttn_gsm.setOnClickListener {
            flag = 2
            CellIdentity.text = "CellIdentityGSM"
            CellSignalStrength.text = "CellSignalStrengthGSM"
            updateButtonColors(bttn_gsm, bttn_lte, bttn_nr)
            getCellInfo()
        }

        bttn_nr.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                flag = 3
                CellIdentity.text = "CellIdentityNR"
                CellSignalStrength.text = "CellSignalStrengthNR"
                updateButtonColors(bttn_nr, bttn_lte, bttn_gsm)
                getCellInfo()
            }
        }

        serverIpInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val ip = serverIpInput.text.toString().trim()
                if (ip.isNotEmpty()) {
                    saveServerIp(ip)
                }
            }
        }
    }

    private fun updateButtonColors(selected: Button, other1: Button, other2: Button) {
        selected.setBackgroundColor(Color.parseColor("#2d2d2f"))
        other1.setBackgroundColor(Color.parseColor("#16c603"))
        other2.setBackgroundColor(Color.parseColor("#16c603"))
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_PHONE_STATE)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.PACKAGE_USAGE_STATS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.PACKAGE_USAGE_STATS)
            }
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissions.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        } else {
            startUpdates()
        }
    }

    private fun startUpdates() {
        handler.postDelayed({
            getCellInfo()
            getNetworkTrafficInfo()
        }, 1000)

        handler.postDelayed(object : Runnable {
            override fun run() {
                getCellInfo()
                getNetworkTrafficInfo()
                locationUpdateCounter++
                handler.postDelayed(this, 30000)
            }
        }, 30000)

        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        if (!checkLocationPermissions()) {
            runOnUiThread {
                resetLocationUI()
            }
            return
        }

        if (!isLocationEnabled()) {
            runOnUiThread {
                resetLocationUI()
            }
            return
        }

        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                try {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        LOCATION_UPDATE_INTERVAL,
                        0f,
                        this
                    )
                } catch (e: Exception) {
                    println("GPS provider error: ${e.message}")
                }

                try {
                    locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        LOCATION_UPDATE_INTERVAL,
                        0f,
                        this
                    )
                } catch (e: Exception) {
                    println("Network provider error: ${e.message}")
                }

                getLastKnownLocation()
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            runOnUiThread {
                resetLocationUI()
            }
        }
    }

    private fun getLastKnownLocation() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (lastKnownLocation != null) {
                    updateLocationUI(lastKnownLocation)
                    sendAllDataToServer()
                } else {
                    val lastKnownNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    lastKnownNetwork?.let {
                        updateLocationUI(it)
                        sendAllDataToServer()
                    }
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun isLocationEnabled(): Boolean {
        return try {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            false
        }
    }

    override fun onLocationChanged(location: Location) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastLocationTime < MIN_TIME_BETWEEN_UPDATES) {
            return
        }
        lastLocationTime = currentTime

        updateLocationUI(location)
        sendAllDataToServer()
    }

    private fun updateLocationUI(location: Location) {
        runOnUiThread {
            try {
                lat_site = location.latitude
                lon_site = location.longitude

                latitude_service.text = String.format("%.6f", location.latitude)
                longitude_service.text = String.format("%.6f", location.longitude)
                altitude_service.text = String.format("%.1f м", location.altitude)
                accuracy_service.text = String.format("%.1f м", location.accuracy)

                val date = Date(location.time)
                val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                current_time_service.text = format.format(date)

                updateIndicator.setTextColor(Color.parseColor("#40FF00"))
                handler.postDelayed({
                    updateIndicator.setTextColor(Color.parseColor("#9E9E9E"))
                }, 300)

                println("Location updated: ${location.latitude}, ${location.longitude}")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getCellInfo() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        try {
            when (flag) {
                1 -> getLteInfo()
                2 -> getGsmInfo()
                3 -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        getNrInfo()
                    }
                }
            }
        } catch (e: SecurityException) {
            // Игнорируем
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getLteInfo() {
        try {
            val cellIdentityList = mutableListOf<String>()
            val cellSignalStrengthList = mutableListOf<String>()
            val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

            val cellInfoList = telephonyManager.allCellInfo
            if (cellInfoList != null) {
                for (cellInfo in cellInfoList) {
                    if (cellInfo is CellInfoLte && cellInfo.isRegistered) {
                        currentCellInfo = cellInfo

                        with(cellInfo.cellIdentity) {
                            cellIdentityList.add("Cell ID: $ci")
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                cellIdentityList.add("Band: $bandwidth МГц")
                            }
                            cellIdentityList.add("EARFCN: $earfcn")
                            cellIdentityList.add("MCC: $mcc")
                            cellIdentityList.add("MNC: $mnc")
                            cellIdentityList.add("PCI: $pci")
                            cellIdentityList.add("TAC: $tac")
                        }

                        with(cellInfo.cellSignalStrength) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                cellSignalStrengthList.add("RSRP: $rsrp dBm")
                                cellSignalStrengthList.add("RSRQ: $rsrq dB")
                                cellSignalStrengthList.add("CQI: $cqi")
                                cellSignalStrengthList.add("RSSNR: $rssnr dB")
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                cellSignalStrengthList.add("RSSI: $rssi dBm")
                            }
                            cellSignalStrengthList.add("ASU Level: $asuLevel")
                            cellSignalStrengthList.add("Timing Advance: ${timingAdvance}")
                        }
                        break
                    }
                }
            }

            updateCellInfoLists(cellIdentityList, cellSignalStrengthList)

        } catch (e: Exception) {
        }
    }

    private fun getGsmInfo() {
        try {
            val cellIdentityList = mutableListOf<String>()
            val cellSignalStrengthList = mutableListOf<String>()
            val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

            val cellInfoList = telephonyManager.allCellInfo
            if (cellInfoList != null) {
                for (cellInfo in cellInfoList) {
                    if (cellInfo is CellInfoGsm && cellInfo.isRegistered) {
                        currentCellInfo = cellInfo

                        with(cellInfo.cellIdentity) {
                            cellIdentityList.add("Cell ID: $cid")
                            cellIdentityList.add("LAC: $lac")
                            cellIdentityList.add("ARFCN: $arfcn")
                            cellIdentityList.add("BSIC: $bsic")
                            cellIdentityList.add("MCC: $mcc")
                            cellIdentityList.add("MNC: $mnc")
                            cellIdentityList.add("PSC: $psc")
                        }

                        with(cellInfo.cellSignalStrength) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                cellSignalStrengthList.add("RSSI: $rssi dBm")
                            }
                            cellSignalStrengthList.add("Dbm: $dbm")
                            cellSignalStrengthList.add("ASU Level: $asuLevel")
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                cellSignalStrengthList.add("Timing Advance: ${timingAdvance}")
                            }
                        }
                        break
                    }
                }
            }

            updateCellInfoLists(cellIdentityList, cellSignalStrengthList)

        } catch (e: Exception) {
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getNrInfo() {
        try {
            val cellIdentityList = mutableListOf<String>()
            val cellSignalStrengthList = mutableListOf<String>()
            val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

            val cellInfoList = telephonyManager.allCellInfo
            if (cellInfoList != null) {
                for (cellInfo in cellInfoList) {
                    if (cellInfo is CellInfoNr && cellInfo.isRegistered) {
                        currentCellInfo = cellInfo

                        with(cellInfo.cellIdentity as CellIdentityNr) {
                            cellIdentityList.add("NCI: $nci")
                            cellIdentityList.add("PCI: $pci")
                            cellIdentityList.add("TAC: $tac")
                            cellIdentityList.add("NRARFCN: $nrarfcn")
                            cellIdentityList.add("MCC: $mccString")
                            cellIdentityList.add("MNC: $mncString")
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                cellIdentityList.add("Bands: ${bands?.joinToString()}")
                            }
                        }

                        with(cellInfo.cellSignalStrength as CellSignalStrengthNr) {
                            cellSignalStrengthList.add("SS-RSRP: $dbm dBm")
                            cellSignalStrengthList.add("SS-RSRQ: $ssRsrq dB")
                            cellSignalStrengthList.add("SS-SINR: $ssSinr dB")
                            cellSignalStrengthList.add("CSI-RSRP: $csiRsrp")
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                cellSignalStrengthList.add("Timing Advance: ${timingAdvanceMicros} µs")
                            }
                        }
                        break
                    }
                }
            }

            updateCellInfoLists(cellIdentityList, cellSignalStrengthList)

        } catch (e: Exception) {
        }
    }

    private fun updateCellInfoLists(identityList: List<String>, signalList: List<String>) {
        runOnUiThread {
            try {
                if (identityList.isNotEmpty()) {
                    val adapter1 = ArrayAdapter(this, android.R.layout.simple_list_item_1, identityList)
                    list_cell_info1.adapter = adapter1
                } else {
                    list_cell_info1.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1,
                        listOf("Нет информации о соте"))
                }

                if (signalList.isNotEmpty()) {
                    val adapter2 = ArrayAdapter(this, android.R.layout.simple_list_item_1, signalList)
                    list_cell_info2.adapter = adapter2
                } else {
                    list_cell_info2.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1,
                        listOf("Нет информации о сигнале"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getNetworkTrafficInfo() {
        try {
            val networkStatsManager = getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager

            val currentTime = System.currentTimeMillis()
            val startTime = currentTime - 30 * 24 * 60 * 60 * 1000L

            val totalRxBytes = mutableMapOf<Int, Long>()
            val totalTxBytes = mutableMapOf<Int, Long>()
            val appTraffic = mutableListOf<AppTraffic>()

            if (checkSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED) {
                val mobileBucket = networkStatsManager.querySummary(
                    ConnectivityManager.TYPE_MOBILE,
                    null,
                    startTime,
                    currentTime
                )

                while (mobileBucket.hasNextBucket()) {
                    val bucket = Bucket()
                    mobileBucket.getNextBucket(bucket)

                    val uid = bucket.uid
                    if (uid > 0) {
                        val rx = totalRxBytes.getOrDefault(uid, 0) + bucket.rxBytes
                        val tx = totalTxBytes.getOrDefault(uid, 0) + bucket.txBytes
                        totalRxBytes[uid] = rx
                        totalTxBytes[uid] = tx

                        val totalBytes = rx + tx
                        if (totalBytes > 0) {
                            val packageName = packageManager.getNameForUid(uid) ?: "Unknown"
                            val appName = try {
                                packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
                            } catch (e: Exception) {
                                packageName
                            }
                            appTraffic.add(AppTraffic(appName, rx, tx, totalBytes))
                        }
                    }
                }
                mobileBucket.close()

                val totalRx = totalRxBytes.values.sum()
                val totalTx = totalTxBytes.values.sum()
                val totalTraffic = totalRx + totalTx

                runOnUiThread {
                    traffic_total.text = String.format("%.2f МБ", totalTraffic / (1024.0 * 1024.0))
                }

                if (appTraffic.isNotEmpty()) {
                    appTraffic.sortByDescending { it.total }

                    val mean = appTraffic.map { it.total }.average()
                    val variance = appTraffic.map { (it.total - mean) * (it.total - mean) }.average()
                    val sigma = Math.sqrt(variance)
                    val twoSigma = mean + 2 * sigma

                    val topApps = appTraffic.filter { it.total >= twoSigma }.take(10)

                    runOnUiThread {
                        val topAppsStrings = topApps.map {
                            "${it.appName}: ↓${String.format("%.1f", it.rx / (1024.0 * 1024.0))}МБ ↑${String.format("%.1f", it.tx / (1024.0 * 1024.0))}МБ"
                        }

                        if (topAppsStrings.isNotEmpty()) {
                            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, topAppsStrings)
                            top_apps_list.adapter = adapter
                        }
                    }
                }
            }

        } catch (e: Exception) {
            // Игнорируем ошибки трафика
        }
    }

    data class AppTraffic(val appName: String, val rx: Long, val tx: Long, val total: Long)

    private fun sendAllDataToServer() {
        val ip = serverIpInput.text.toString().trim()
        if (ip.isEmpty()) {
            println("IP сервера не указан, данные не отправлены")
            return
        }

        Thread {
            try {
                val jsonObject = JSONObject()

                val locationData = JSONObject()
                locationData.put("latitude", lat_site)
                locationData.put("longitude", lon_site)
                jsonObject.put("location", locationData)

                if (currentCellInfo != null) {
                    val telephonyData = JSONObject()
                    when (currentCellInfo) {
                        is CellInfoLte -> {
                            val lte = currentCellInfo as CellInfoLte
                            val identity = JSONObject()
                            identity.put("type", "LTE")
                            identity.put("cell_id", lte.cellIdentity.ci)
                            identity.put("mcc", lte.cellIdentity.mcc)
                            identity.put("mnc", lte.cellIdentity.mnc)
                            identity.put("pci", lte.cellIdentity.pci)
                            identity.put("tac", lte.cellIdentity.tac)
                            identity.put("earfcn", lte.cellIdentity.earfcn)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                identity.put("bandwidth", lte.cellIdentity.bandwidth)
                            }

                            val strength = JSONObject()
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                strength.put("rsrp", lte.cellSignalStrength.rsrp)
                                strength.put("rsrq", lte.cellSignalStrength.rsrq)
                                strength.put("cqi", lte.cellSignalStrength.cqi)
                                strength.put("rssnr", lte.cellSignalStrength.rssnr)
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                strength.put("rssi", lte.cellSignalStrength.rssi)
                            }
                            strength.put("asu_level", lte.cellSignalStrength.asuLevel)
                            strength.put("timing_advance", lte.cellSignalStrength.timingAdvance)

                            telephonyData.put("identity", identity)
                            telephonyData.put("strength", strength)
                        }
                        is CellInfoGsm -> {
                            val gsm = currentCellInfo as CellInfoGsm
                            val identity = JSONObject()
                            identity.put("type", "GSM")
                            identity.put("cell_id", gsm.cellIdentity.cid)
                            identity.put("lac", gsm.cellIdentity.lac)
                            identity.put("arfcn", gsm.cellIdentity.arfcn)
                            identity.put("bsic", gsm.cellIdentity.bsic)

                            val strength = JSONObject()
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                strength.put("rssi", gsm.cellSignalStrength.rssi)
                            }
                            strength.put("dbm", gsm.cellSignalStrength.dbm)
                            strength.put("asu_level", gsm.cellSignalStrength.asuLevel)

                            telephonyData.put("identity", identity)
                            telephonyData.put("strength", strength)
                        }
                        is CellInfoNr -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                val nr = currentCellInfo as CellInfoNr
                                val identity = JSONObject()
                                identity.put("type", "NR")
                                identity.put("nci", (nr.cellIdentity as CellIdentityNr).nci)
                                identity.put("pci", (nr.cellIdentity as CellIdentityNr).pci)
                                identity.put("tac", (nr.cellIdentity as CellIdentityNr).tac)
                                identity.put("nrarfcn", (nr.cellIdentity as CellIdentityNr).nrarfcn)

                                val strength = JSONObject()
                                strength.put("ss_rsrp", (nr.cellSignalStrength as CellSignalStrengthNr).dbm)
                                strength.put("ss_rsrq", (nr.cellSignalStrength as CellSignalStrengthNr).ssRsrq)
                                strength.put("ss_sinr", (nr.cellSignalStrength as CellSignalStrengthNr).ssSinr)
                                strength.put("csi_rsrp", (nr.cellSignalStrength as CellSignalStrengthNr).csiRsrp)

                                telephonyData.put("identity", identity)
                                telephonyData.put("strength", strength)
                            }
                        }
                    }
                    jsonObject.put("telephony", telephonyData)
                }

                try {
                    ZContext().use { context ->
                        val socket = context.createSocket(SocketType.REQ)
                        socket.setReceiveTimeOut(3000)
                        socket.connect("tcp://$ip:$SERVER_PORT")
                        socket.send(jsonObject.toString().toByteArray(ZMQ.CHARSET), 0)
                        socket.recv(0)
                        println("Данные отправлены на сервер $ip:$SERVER_PORT")
                    }
                } catch (e: Exception) {
                    println("Ошибка подключения к серверу $ip:$SERVER_PORT: ${e.message}")
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun checkLocationPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startUpdates()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (checkLocationPermissions() && isLocationEnabled()) {
            startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            locationManager.removeUpdates(this)
        } catch (e: Exception) {
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            locationManager.removeUpdates(this)
        } catch (e: Exception) {
        }
        handler.removeCallbacksAndMessages(null)
    }
}