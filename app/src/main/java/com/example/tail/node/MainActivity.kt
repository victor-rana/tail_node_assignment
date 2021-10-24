package com.example.tail.node

import android.Manifest.permission
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
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
import android.os.Handler
import android.os.Looper
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import android.os.Build

import android.os.PowerManager
import androidx.core.app.ActivityCompat.startActivityForResult

import android.content.Intent
import android.net.Uri
import android.provider.Settings


class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private val PERMISSION_REQUEST_CODE = 200
    var gpsService: BackgroundLocationService? = null
    private var mMap: GoogleMap? = null
    var recyclerView: RecyclerView? = null
    var lat: Double? = null
    var lon: Double? = null
    private val IGNORE_BATTERY_OPTIMIZATION_REQUEST = 1002

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

        val handler = Handler()
        handler.postDelayed(object : Runnable {
            override fun run() {
                if(lat!=null && lon!=null)
                    commitToFile(lat.toString(), lon.toString())
                handler.postDelayed(this, 300000)//5 min delay
            }
        }, 0)

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
                arrayOf(permission.ACCESS_FINE_LOCATION),
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
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
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
            "lat=$lat;lon=$lon;"
        val fOut: FileOutputStream = openFileOutput(
            "savedData.txt",
            MODE_APPEND
        )
        val osw = OutputStreamWriter(fOut)
        osw.write(entryString)
        osw.flush()
        osw.close()
    }


}