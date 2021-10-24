package com.example.tail.node

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.tail.node.db.Pref

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Pref.initializeInstance(this)

        Log.d("TAG", "onCreate: splash "+Pref.instance!!.isLoggedIn)

        if(Pref.instance!!.isLoggedIn!!){
            startActivity(Intent(this, MainActivity::class.java))
        }
        else{
            startActivity(Intent(this, LoginActivity::class.java))
        }

    }
}