package com.erha.calander.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import cn.authing.guard.network.AuthClient
import cn.hutool.core.util.IdUtil
import cn.hutool.http.HttpUtil
import com.bumptech.glide.Glide
import com.erha.calander.R
import com.erha.calander.activity.LoginActivity
import com.erha.calander.activity.UserCenterActivity
import com.erha.calander.databinding.FragmentAccountBinding
import com.erha.calander.type.EventType
import com.erha.calander.type.LocalStorageKey
import com.erha.calander.util.TinyDB
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File


class AccountFragment : Fragment(R.layout.fragment_account) {
    private lateinit var binding: FragmentAccountBinding
    private var isLogin = false
    private lateinit var store: TinyDB
    var loadUserInfoRunnable = Runnable {
        try {
            AuthClient.getCurrentUser { code, message, data ->
                if (code == 200) {
                    isLogin = true
                    activity?.runOnUiThread {
                        Glide.with(this)
                            .load(data.photo)
                            .circleCrop()
                            .error(R.mipmap.image_default_avator)
                            .into(binding.avatorImageView)
                        store.putBoolean(LocalStorageKey.USER_IS_LOGIN, true)
                        data.nickname.apply {
                            store.putString(LocalStorageKey.USER_NICKNAME, this)
                            binding.userNicknameTextView.text = this
                        }
                        data.email.split("@")[0].replaceRange(3, 7, "****").apply {
                            store.putString(LocalStorageKey.USER_PHONE, this)
                            binding.userPhoneTextvView.text = this
                        }
                    }
                    //缓存用户的头像
                    data.photo.apply {
                        var toCache = true
                        store.getString(LocalStorageKey.USER_PHOTO_CACHE_URL)?.apply {
                            if (isNotBlank() && this == data.photo) toCache = false
                        }
                        Log.e("新增头像缓存", toCache.toString())
                        if (toCache) {
                            var id = IdUtil.simpleUUID()
                            var file = File(File(binding.root.context.filesDir, "avator"), id)
                            try {
                                HttpUtil.downloadFileFromUrl(this, file)
                                store.putString(LocalStorageKey.USER_PHOTO_CACHE_URL, this)
                                store.putString(LocalStorageKey.USER_PHOTO_CACHE_ID, id)
                            } catch (e: Exception) {
                                store.putBoolean(LocalStorageKey.USER_IS_LOGIN, false)
                            }
                        }

                    }

                } else {
                    isLogin = false
                    activity?.runOnUiThread {
                        store.putBoolean(LocalStorageKey.USER_IS_LOGIN, false)
                        binding.userNicknameTextView.text = "未登录"
                        binding.userPhoneTextvView.text = "轻触快速登录"
                        Glide.with(this)
                            .load(R.mipmap.image_default_avator)
                            .circleCrop()
                            .into(binding.avatorImageView)
                    }
                }
            }
        } catch (e: java.lang.Exception) {
        } catch (e: Exception) {
        }
    }
    private lateinit var loadUserInfoThread: Thread

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        saveInstanceState: Bundle?
    ): View? {
        binding = FragmentAccountBinding.inflate(inflater, container, false)
        store = TinyDB(binding.root.context)
        store.apply {
            //加载用户信息缓存
            var loadPhoto = false
            if (getBoolean(LocalStorageKey.USER_IS_LOGIN)) {
                getString(LocalStorageKey.USER_NICKNAME)?.apply {
                    binding.userNicknameTextView.text = this
                }
                getString(LocalStorageKey.USER_PHONE)?.apply {
                    binding.userPhoneTextvView.text = this
                }
                getString(LocalStorageKey.USER_PHOTO_CACHE_ID)?.apply {
                    if (isNotBlank()) {
                        loadPhoto = true
                        Glide.with(this@AccountFragment)
                            .load(File(File(binding.root.context.filesDir, "avator"), this))
                            .circleCrop()
                            .into(binding.avatorImageView)
                    }
                }

            } else {
                binding.userNicknameTextView.text = "未登录"
                binding.userPhoneTextvView.text = ""
            }
            if (!loadPhoto) {
                Glide.with(this@AccountFragment)
                    .load(R.mipmap.image_default_avator)
                    .circleCrop()
                    .into(binding.avatorImageView)
            }
        }

        //创建ActivityResultLauncher
        val resultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                Log.e("registerForActivityResult", it.resultCode.toString())
            }

        binding.userInfoConstraintLayout.setOnClickListener { v ->
            if (isLogin) {
                resultLauncher.launch(Intent(activity, UserCenterActivity::class.java))
            } else {
                resultLauncher.launch(Intent(activity, LoginActivity::class.java))
            }
        }

        EventBus.getDefault().register(this)
        loadUserInfoThread = Thread(loadUserInfoRunnable).apply {
            start()
        }

        return binding.root
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onEvent(str: String?) {
        when (str) {
            EventType.USER_INFO_CHANGE -> {
                loadUserInfoThread = Thread(loadUserInfoRunnable).apply {
                    start()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
        try {
            loadUserInfoThread.interrupt()
            loadUserInfoThread.stop()
        } catch (e: Exception) {
        } catch (e: java.lang.Exception) {
        }
    }

}