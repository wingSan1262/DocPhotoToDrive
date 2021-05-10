package com.example.simplegallery

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle
import android.os.Handler
import android.os.Looper

class MainActivity : AppCompatActivity() {
    var mHandler : Handler? = null;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mHandler = Handler(Looper.myLooper() ?: throw ExceptionInInitializerError())

        mHandler?.postDelayed({
            val intent = Intent(this, SignInActivity2::class.java)
            startActivity(intent)
            finish()
        }, 1000)
    }


}
