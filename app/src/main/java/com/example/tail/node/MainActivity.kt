package com.example.tail.node

import android.Manifest.permission
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.R.attr
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tail.node.service.BackgroundLocationService
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.CameraUpdateFactory
import android.location.Criteria

import android.location.LocationManager

import androidx.core.app.ActivityCompat.startActivityForResult

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.R.attr.path
import android.os.*
import android.widget.Toast
import java.io.*
import android.R.attr.data
import android.R.attr.path
import android.R.attr.data











class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private val PERMISSION_REQUEST_CODE = 200
    var gpsService: BackgroundLocationService? = null
    private var mMap: GoogleMap? = null
    var recyclerView: RecyclerView? = null
    var lat: Double? = null
    var lon: Double? = null
    private val IGNORE_BATTERY_OPTIMIZATION_REQUEST = 1002
    var folder: File? = null
    private val REQUEST_CODE_ASK_PERMISSIONS = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)

        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (pm != null && !pm.isIgnoringBatteryOptimizations(packageName)) {
                askIgnoreOptimization()
            } else {
                startService()
            }
        } else {
            startService()
        }
//        startService()

        LocalBroadcastManager.getInstance(this).registerReceiver(
            mMessageReceiver,
            IntentFilter("onLocationBroadcast")
        )

        if (ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            createFile()
        } else {
            // Request permission from the user
            requestPermission()
        }

    }

    fun createFile(){
        val handler = Handler()
        handler.postDelayed(object : Runnable {
            override fun run() {
                if(lat!=null && lon!=null)
                    commitToFile(lat.toString(), lon.toString())
                handler.postDelayed(this, 300000)//5 min delay
            }
        }, 0)
    }

    private fun requestPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat
                .requestPermissions(
                    this@MainActivity,
                    arrayOf(WRITE_EXTERNAL_STORAGE),
                    REQUEST_CODE_ASK_PERMISSIONS
                )
        }
    }

    private fun askIgnoreOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:$packageName")
            startActivityForResult(intent, IGNORE_BATTERY_OPTIMIZATION_REQUEST)
        } else {
            startService()
        }
    }


    fun startService() {
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val intent = Intent(this.application, BackgroundLocationService::class.java)
            this.application.startService(intent)
            this.application.bindService(intent, serviceConnection, BIND_AUTO_CREATE)
        } else {
            Log.d("TAG", "requestPermissions: ")
            ActivityCompat.requestPermissions(
                this,
                arrayOf(permission.ACCESS_FINE_LOCATION,WRITE_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    fun stopService() {
        val intent = Intent(this.application, BackgroundLocationService::class.java)
        this.application.stopService(intent)
        this.application.unbindService(serviceConnection)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d("TAG", "onRequestPermissionsResult: $requestCode")

        if(requestCode == REQUEST_CODE_ASK_PERMISSIONS){
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission Granted
                    createFile()
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT)
                    .show()
            } else {
                // Permission Denied
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.size > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                startService()
            }
        }
    }

        val mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                // Get extra data included in the Intent
//                fetechLoaction()
                val lat = intent.getDoubleExtra("lat", 0.0)
                val lon = intent.getDoubleExtra("lon", 0.0)
                Log.d("receiver", "Got lat: $lat")

                onUpdateMap(lat, lon)
            }
        }

    override fun onDestroy() {
        super.onDestroy()
        gpsService!!.stopTracking()
        stopService()
    }

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val name = className.className
            if (name.endsWith("BackgroundLocationService")) {
                gpsService = (service as BackgroundLocationService.LocationServiceBinder).service
                gpsService!!.startTracking()
                Log.d("TAG", "onServiceConnected: GPS Ready")
            }
        }

        override fun onServiceDisconnected(className: ComponentName) {
            if (className.className == "BackgroundLocationService") {
                gpsService = null
            }
        }
    }

    fun onUpdateMap(lat: Double, lon: Double) {
        this.lat = lat
        this.lon = lon
        Log.d("TAG", "onUpdateMap: ")
        val latLng = LatLng(lat, lon)
        mMap!!.addMarker(MarkerOptions().position(latLng).title("Marker"))
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15f)
        mMap!!.animateCamera(cameraUpdate)
    }

    override fun onMapReady(p0: GoogleMap?) {
        mMap = p0
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val criteria = Criteria()

        val location: Location? = locationManager.getBestProvider(
            criteria,
            false
        )?.let {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(permission.ACCESS_FINE_LOCATION),
                    PERMISSION_REQUEST_CODE
                )
                return
            }
            locationManager.getLastKnownLocation(
                it
            )
        }

        if (location != null) {
            val lat: Double = location.getLatitude()
            val longi: Double = location.getLongitude()
            onUpdateMap(lat, longi)
            Log.d("TAG", "zoomMyCuurentLocation: location not null")
        }

    }

    @Throws(IOException::class)
    private fun commitToFile(
        lat: String, lon: String,
    ) {
        val entryString =
            "\nlat=$lat;lon=$lon;"


        // Get the directory for the user's public pictures directory.
        // Get the directory for the user's public pictures directory.
        val path =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS.toString() + "/TailNode/")

        // Make sure the path directory exists.

        // Make sure the path directory exists.
        if (!path.exists()) {
            // Make it, if it doesn't exit
            path.mkdirs()
        }

        val file = File(path, "savedData.txt")

        try {
            file.createNewFile()
            val fOut = FileOutputStream(file)
            val myOutWriter = OutputStreamWriter(fOut)
            myOutWriter.append(entryString)
            myOutWriter.close()
            fOut.close()
        } catch (e: IOException) {
            Log.e("Exception", "File write failed: $e")
        }

//        val fileout = openFileOutput("savedData.txt", MODE_APPEND)
//        val outputWriter = OutputStreamWriter(fileout)
//        outputWriter.write(entryString)
//        outputWriter.close()
        Log.d("TAG", "commitToFile: "+File(folder, "savedData.txt").path)
        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
    }


}