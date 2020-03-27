package com.recycler.geofencingdemo.activities

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.recycler.geofencingdemo.MainActivity
import com.recycler.geofencingdemo.R

const val TAG = "SplashActivity"
const val RC_SIGN_IN = 10

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        var remoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(R.xml.remote_config)

        remoteConfig.fetchAndActivate()
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val updated = task.result
                        Log.d(TAG, "Config params updated: $updated")
                        Toast.makeText(this, "Fetch and activate succeeded",
                                Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Fetch failed",
                                Toast.LENGTH_SHORT).show()
                    }
                    FirebaseAuth.getInstance().currentUser?.let { startMainActivity() }
                }

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

    private fun startMainActivity() {
        Intent(this, MainActivity::class.java).run {
            startActivity(this)
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in
                val user = FirebaseAuth.getInstance().currentUser
                Toast.makeText(this, "Welcome " + user?.displayName, Toast.LENGTH_SHORT).show()
                startMainActivity()
                // ...
            } else {
                Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
