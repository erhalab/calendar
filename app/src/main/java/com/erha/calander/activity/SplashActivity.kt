package com.erha.calander.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.erha.calander.R
import com.erha.calander.dao.ConfigDao
import com.erha.calander.service.NotificationService
import com.erha.calander.type.LocalStorageKey
import com.erha.calander.util.TinyDB
import java.io.File


class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = getColor(R.color.default_background_color)
            decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        setContentView(R.layout.activity_splash)

        //启用通知Service
        val serviceIntent = Intent(this@SplashActivity, NotificationService::class.java)
        startService(serviceIntent)
        if (ConfigDao.hadEnterSplashScreen) {
            val i = Intent(this@SplashActivity, HomeActivity::class.java)
            intent?.apply {
                i.getStringExtra("defaultPage")?.let { Log.e(this.javaClass.name, it) }
                i.putExtras(this)
            }
            startActivity(i)
            finish()
        } else {
            launchMainActivity()
            ConfigDao.hadEnterSplashScreen = true
        }
    }

    override fun onStart() {
        super.onStart()
        TinyDB(applicationContext).apply {
            if (getBoolean(LocalStorageKey.USER_IS_LOGIN)) {
                getString(LocalStorageKey.USER_NICKNAME)?.apply {
                    (findViewById<TextView>(R.id.userNicknameTextView)).text = this
                }
                getString(LocalStorageKey.USER_PHOTO_CACHE_ID)?.apply {
                    if (isNotBlank()) {
                        Glide.with(this@SplashActivity)
                            .load(File(File(filesDir, "avator"), this))
                            .error(R.mipmap.image_default_avator)
                            .circleCrop()
                            .into(findViewById(R.id.avatorImageView))
                    }
                }
            }
        }
    }

    private fun launchMainActivity() {
        val handler = Handler()
        handler.postDelayed({
            val i = Intent(this@SplashActivity, HomeActivity::class.java)
            intent?.apply {
                i.putExtras(this)
            }
            startActivity(i)
            finish()
        }, 750)
    }
}