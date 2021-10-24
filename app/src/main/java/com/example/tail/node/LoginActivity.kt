package com.example.tail.node

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import com.example.tail.node.db.Pref

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        Pref.initializeInstance(this)

        var etName = findViewById<EditText>(R.id.etName)
        var etPhone = findViewById<EditText>(R.id.etPhone)
        var btLogin = findViewById<Button>(R.id.btLogin)

        btLogin.setOnClickListener {
            if(!TextUtils.isEmpty(etName.text.toString()) && !TextUtils.isEmpty(etPhone.text.toString())){
                Pref.instance!!.isLoggedIn = true
                Pref.instance!!.userName = etName.text.toString()
                Pref.instance!!.userPhone = etPhone.text.toString()
                startActivity(Intent(this, MainActivity::class.java))
            }
        }
    }
}