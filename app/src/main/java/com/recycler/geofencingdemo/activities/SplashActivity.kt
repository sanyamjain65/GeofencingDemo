package com.recycler.geofencingdemo.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.recycler.geofencingdemo.FirebaseDb
import com.recycler.geofencingdemo.MainActivity
import com.recycler.geofencingdemo.R
import java.util.HashMap

const val TAG = "SplashActivity"
const val RC_SIGN_IN = 102

class SplashActivity : AppCompatActivity() {

    private var handleDeepLink = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        intent.data?.run {
            if ("https" == this.scheme && "stay-at-home-01.firebaseapp.com" == this.host) {
                handleDeepLink = true
                handleDeepLink()
                try {
                    val instanceId = this.getQueryParameter("id")!!
                    Log.d(TAG, instanceId)
                } catch (e: Exception) {
                    Log.e(TAG, e.message!!)
                }
            }
        }


        val remoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(R.xml.remote_config)

        remoteConfig.fetchAndActivate()
                .addOnCompleteListener(this) {
                    if (!handleDeepLink) {
                        startMainActivity()
                    }
                }
    }

    private fun handleDeepLink() {
        if (FirebaseAuth.getInstance().currentUser === null) {
            startUserLogin()
        } else {
            val instanceId = intent.data!!.getQueryParameter("id")!!
            val data = hashMapOf("instanceId" to instanceId)
            FirebaseFunctions.getInstance()
                    .getHttpsCallable("addContact")
                    .call(data)
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            val result = it.result?.data as HashMap<*, *>
                            Log.d(TAG, result.toString())
                        } else {
                            it.exception?.run { Log.e(TAG, this.message!!) }
                        }
                        startMainActivity()
                    }
        }
    }

    private fun startUserLogin() {
        val providers = arrayListOf(
                AuthUI.IdpConfig.GoogleBuilder().build(),
                AuthUI.IdpConfig.FacebookBuilder().build())

        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build(),
                RC_SIGN_IN)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK) {
                FirebaseAuth.getInstance().currentUser?.run {
                    FirebaseDb.insertUser(this)
                    Toast.makeText(this@SplashActivity, "Welcome $displayName", Toast.LENGTH_SHORT).show()
                    handleDeepLink()
                }
            } else {
                Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startMainActivity() {
        Intent(this, MainActivity::class.java).run {
            startActivity(this)
            finish()
        }
    }
}
