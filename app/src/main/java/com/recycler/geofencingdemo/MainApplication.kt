package com.recycler.geofencingdemo

import android.app.Application
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.iid.FirebaseInstanceId
import com.recycler.geofencingdemo.utils.TinyDB

const val TAG = "MainApplication"

class MainApplication : Application() {

    lateinit var deviceId: String

    override fun onCreate() {
        super.onCreate()

        FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener {
            deviceId = it.id
        }
        val tinyDb = TinyDB(this)
        FirebaseAuth.getInstance().currentUser?.let {
            Log.d(TAG, "checking out")
            FirebaseDb.insertToken(tinyDb.getString("token"))
        }
    }
}