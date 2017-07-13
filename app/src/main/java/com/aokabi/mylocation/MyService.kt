package com.aokabi.mylocation

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.Intent.getIntent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.*
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.widget.Toast

import java.io.IOException
import java.util.Timer

import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.NoSuchElementException

class MyService : Service(), LocationListener {

    private var locationManager: LocationManager? = null //位置情報を管理している

    private var attendance = false

    private var clubroom = false

    private var mTimer: Timer? = null
    private var _messenger: Messenger? = null



    override fun onCreate() {
        super.onCreate()
        Log.i("TestService", "onCreate")
    }


    private fun checkPermission(context: Context): Boolean {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun locationStart() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gpsEnabled = locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (!gpsEnabled) {
            // GPSを設定するように促す
            val settingsIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(settingsIntent)
            Log.d("debug", "not gpsEnable, startActivity")
        } else {
            Log.d("debug", "gpsEnabled")
        }
        checkPermission(this)
        // 1000msec,1m
        locationManager!!.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0f, this)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.i("TestService,", "onStartCommand")
        val mm: MyMessenger = intent.getParcelableExtra<MyMessenger>("handle")
        _messenger = mm?.messenger
        locationStart()

        Toast.makeText(this, "Myservice onStart", Toast.LENGTH_SHORT).show()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("TestService", "onDestroy")
        locationManager!!.removeUpdates(this)

        if (mTimer != null) {
            mTimer!!.cancel()
            mTimer = null
        }
        Toast.makeText(this, "MyService onDestroy",
                Toast.LENGTH_SHORT).show()
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.i("TestService", "onBind")
        locationStart()
        //return _messenger.binder
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}

    override fun onProviderEnabled(provider: String) {}

    override fun onProviderDisabled(provider: String) {}

    override fun onLocationChanged(location: Location) {
        val workspace_la = getString(R.string.workspace_la).toDouble()
        val workspace_lo = getString(R.string.workspace_lo).toDouble()
        val clubroom_la = getString(R.string.clubroom_la).toDouble()
        val clubroom_lo = getString(R.string.clubroom_lo).toDouble()
        // 緯度
        //TextView latitudeView = (TextView) findViewById(R.id.latitude);
        //latitudeView.setText("Latitude:" + location.getLatitude());
        Log.d("debug", "Latitude:" + location.latitude)

        // 経度
        //TextView longitudeView = (TextView) findViewById(R.id.longitude);
        //longitudeView.setText("Longitude:" + location.getLongitude());
        Log.d("debug", "Longitude:" + location.longitude)

        sendActivity(location)

        if (workspace_la - 0.004 < location.latitude && location.latitude < workspace_la + 0.004 && workspace_lo - 0.004 < location.longitude && location.longitude < workspace_lo + 0.004) {
            if (!attendance) {
                attendance = true
                sendRequest("出勤")
            }
        } else {
            if (attendance) {
                attendance = false
                sendRequest("退勤")
            }
        }
        if (clubroom_la - 0.001 < location.latitude && location.latitude < clubroom_la + 0.001 && clubroom_lo - 0.001 < location.longitude && location.longitude < clubroom_lo + 0.001) {
            if (!clubroom) {
                clubroom = true
                sendRequest("I'm at 部室")
            }
        } else {
            if (clubroom) {
                clubroom = false
                sendRequest("退室")
            }
        }


    }

    fun sendRequest(message: String) {
        val formBody = FormBody.Builder()
                .add("token", getString(R.string.token))
                .add("channel", getString(R.string.mychannel))
                .add("text", message

                )
                .build()

        val slackRequest = Request.Builder()
                .url("https://slack.com/api/chat.postMessage")
                .post(formBody)
                .build()

        val callback = object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // do nothing
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                val body = response.body().string()
                Handler(Looper.getMainLooper()).post { Toast.makeText(this@MyService, body, Toast.LENGTH_LONG).show() }
            }
        }

        OkHttpClient().newCall(slackRequest).enqueue(callback)
    }

    fun sendActivity(location: Location) {
        try {
            _messenger?.send(Message.obtain(null, 0, location))
            Log.d("send", "sendできた")
        } catch (e: RemoteException) {
            e.printStackTrace()
        } catch (e: NoSuchElementException) {
            e.printStackTrace()
        }
    }
}
