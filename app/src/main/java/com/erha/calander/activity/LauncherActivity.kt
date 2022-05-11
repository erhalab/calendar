package com.erha.calander.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.erha.calander.R
import com.erha.calander.service.NotificationService


class LauncherActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)

        //启用通知Service
        var serviceIntent = Intent(this@LauncherActivity, NotificationService::class.java)
        startService(serviceIntent)

        window.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = getColor(R.color.default_background_color)
            decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        launchMainActivity()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        launchMainActivity()
    }

    private fun launchMainActivity() {
        val handler = Handler()
        handler.postDelayed(object : Runnable {
            override fun run() {
                //Do something after 100ms
                //Intent i = new Intent("first app package","first app class name" );
                val i = Intent(this@LauncherActivity, MainActivity::class.java)
                startActivity(i)
                finish()
            }
        }, 750)
    }
}