package com.recycler.geofencingdemo

import android.util.Log
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.functions.FirebaseFunctions

object FirebaseDb {

    fun insertUser(user: FirebaseUser) {
        val userMap = hashMapOf(
                "name" to user.displayName,
                "photoUrl" to user.photoUrl.toString()
        )
        FirebaseDatabase.getInstance().reference.child("users").child(user.uid).setValue(userMap)
    }

    fun insertInstanceId(user:FirebaseUser, instanceId: String) {
        FirebaseDatabase.getInstance().reference.child("user_instance_mapping").child(user.uid).setValue(instanceId)
        FirebaseDatabase.getInstance().reference.child("instance_user_mapping").child(instanceId).setValue(user.uid)
    }

    fun insertToken(token: String) {
        FirebaseFunctions.getInstance()
                .getHttpsCallable("addDeviceToken")
                .call(hashMapOf("token" to token))
                .addOnSuccessListener {
                    Log.d("insertToken", it.data.toString())
                }
    }

    fun triggerAlert() {
        FirebaseFunctions.getInstance()
                .getHttpsCallable("triggerAlert")
                .call()
                .addOnSuccessListener {
                    Log.d("insertToken", it.data.toString())
                }
    }
}