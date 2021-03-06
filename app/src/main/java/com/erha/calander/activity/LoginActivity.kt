package com.erha.calander.activity

import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import cn.authing.guard.data.UserInfo
import cn.authing.guard.network.AuthClient
import com.erha.calander.R
import com.erha.calander.dao.SMSDao
import com.erha.calander.dao.SecretKeyDao
import com.erha.calander.data.SingleSMS
import com.erha.calander.databinding.ActivityLoginBinding
import com.erha.calander.type.EventType
import com.erha.calander.util.TinyDB
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.impl.LoadingPopupView
import com.qmuiteam.qmui.util.QMUIDisplayHelper
import com.tencentcloudapi.common.Credential
import com.tencentcloudapi.common.exception.TencentCloudSDKException
import com.tencentcloudapi.common.profile.ClientProfile
import com.tencentcloudapi.common.profile.HttpProfile
import com.tencentcloudapi.sms.v20190711.SmsClient
import com.tencentcloudapi.sms.v20190711.models.SendSmsRequest
import com.tencentcloudapi.sms.v20190711.models.SendSmsResponse
import com.tencentcloudapi.sms.v20190711.models.SendStatus
import es.dmoral.toasty.Toasty
import org.greenrobot.eventbus.EventBus
import org.json.JSONObject
import java.util.*
import java.util.regex.Pattern


class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var loadingPopup: LoadingPopupView
    private var loadingPopupInit = false
    private lateinit var store: TinyDB
    private lateinit var phoneSMS: SingleSMS
    private val phonePattern: Pattern =
        Pattern.compile("^((13[0-9])|(15[^4])|(18[0-9])|(17[1-9]))\\d{8}$")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        store = TinyDB(binding.root.context)
        binding.backButton.setOnClickListener {
            finish()
        }

        binding.loginPhoneQMUILinearLayout.apply {
            radius = resources.getDimensionPixelSize(R.dimen.listview_radius)
        }
        binding.loginButtonQMUILinearLayout.apply {
            setRadiusAndShadow(
                70, QMUIDisplayHelper.dp2px(binding.root.context, 5), 0.3F
            )
        }
        store.getString("loginPhoneUserInputTemp")?.apply {
            if (this.isNotBlank()) {
                binding.phoneNumberInput.setText(this, TextView.BufferType.EDITABLE)
            }
        }
        binding.sendSmsText.setOnClickListener {
            Log.e("test", "sendSmsText setOnClickListener")
            if (checkPhone()) {
                store.putString("loginPhoneUserInputTemp", binding.phoneNumberInput.text.toString())
                initLoadingPopup()
                if (!loadingPopup.isShow) {
                    loadingPopup.setTitle("?????????????????????")
                    loadingPopup.show()
                }
                val code = ((Math.random() * 9 + 1) * Math.pow(10.0, 5.0)).toInt().toString()
                val seconds = 60 * 5
                sendSms(binding.phoneNumberInput.text.toString(), code, seconds)

                val expiredTime = Calendar.getInstance()
                expiredTime.add(Calendar.SECOND, seconds)
                phoneSMS = SingleSMS(
                    code = code,
                    phone = binding.phoneNumberInput.text.toString(),
                    createTime = Calendar.getInstance(),
                    expiredTime = expiredTime
                )
            }

        }
        //test
        val expiredTime = Calendar.getInstance()
        expiredTime.add(Calendar.SECOND, 10000000)
        SMSDao.list.add(
            SingleSMS(
                code = "111111",
                phone = "13700000000",
                createTime = Calendar.getInstance(),
                expiredTime = expiredTime
            )
        )

        binding.loginButton.setOnClickListener {
            if (checkPhone() && checkCode()) {
                val now = Calendar.getInstance()
                var checkCode = false
                initLoadingPopup()
                loadingPopup.show()
                for (i in SMSDao.list) {
                    if (!i.used
                        && i.phone == binding.phoneNumberInput.text.toString()
                        && i.code == binding.verificationCodeInputView.text.toString()
                        && i.createTime.compareTo(now) <= 0
                        && i.expiredTime.compareTo(now) >= 0
                    ) {
                        checkCode = true
                        break
                    }
                }

                if (checkCode) {
                    loadingPopup.setTitle("?????????")
                    login(binding.phoneNumberInput.text.toString())
                } else {
                    Toasty.error(binding.root.context, "???????????????????????????", Toast.LENGTH_LONG, true).show()
                    loadingPopup.dismiss()
                }
//                finish()
            }
        }


    }

    private fun initLoadingPopup() {
        if (loadingPopupInit) {
            return
        }
        loadingPopup = XPopup.Builder(binding.root.context)
            .autoDismiss(false)
            .asLoading("")
        loadingPopupInit = true

    }


    private fun checkPhone(): Boolean {
        return if (phonePattern.matcher(binding.phoneNumberInput.text).matches()) {
            true
        } else {
            when (binding.phoneNumberInput.text.length) {
                0 -> "?????????????????????"
                in 1..10 -> "?????????????????????"
                else -> "?????????????????????????????????"
            }.apply {
                Toasty.error(binding.root.context, this, Toast.LENGTH_SHORT, true).show()
            }
            false
        }
    }

    private fun checkCode(): Boolean {
        return if (binding.verificationCodeInputView.text.length == 6) {
            true
        } else {
            when (binding.verificationCodeInputView.text.length) {
                0 -> "?????????????????????"
                else -> "????????????????????????"
            }.apply {
                Toasty.error(binding.root.context, this, Toast.LENGTH_SHORT, true).show()
            }
            false
        }
    }

    private fun sendSmsCallback(sendStatus: SendStatus?) {
        runOnUiThread {
            loadingPopup.dismiss()
            when {
                sendStatus?.code == "Ok" -> {
                    SMSDao.list.add(phoneSMS)
                    object : CountDownTimer(10000 * 6, 100) {
                        override fun onTick(millisUntilFinished: Long) {
                            binding.sendSmsText.isEnabled = false
                            if (millisUntilFinished / 1000 > 0) {
                                binding.sendSmsText.text = "?????????(" + millisUntilFinished / 1000 + ")"
                            }
                        }

                        override fun onFinish() {
                            binding.sendSmsText.isEnabled = true
                            binding.sendSmsText.text = "???????????????"
                        }
                    }.start()
                    Toasty.success(binding.root.context, "????????????", Toast.LENGTH_LONG, true).show()
                }
                sendStatus == null -> {
                    Toasty.error(binding.root.context, "????????????", Toast.LENGTH_LONG, true).show()
                }
                else -> {
                    Toasty.error(binding.root.context, sendStatus.message, Toast.LENGTH_LONG, true)
                        .show()
                }
            }

        }
    }

    private fun login(phone: String, isNew: Boolean = false) {
        Thread {
            AuthClient.loginByAccount(
                "${phone}@neu.app", "riMm78jUHCxWd4"
            ) { code: Int, message: String?, userInfo: UserInfo? ->
                Log.e("AuthClient", "loginByAccount ${code}")
                if (code == 200) {
                    //????????????
                    if (isNew) {
                        runOnUiThread {
                            loadingPopup.setTitle("??????????????????")
                        }
                        updateDefaultUserInfo()
                    } else {
                        userInfo?.apply {
                            runOnUiThread {
                                //???????????????????????????????????????
                                loadingPopup.dismissWith {
                                    Toasty.success(
                                        binding.root.context,
                                        "????????????:${this.nickname}",
                                        Toast.LENGTH_LONG,
                                        true
                                    ).show()
                                    EventBus.getDefault().post(EventType.USER_INFO_CHANGE)
                                    finish()
                                }
                            }
                        }
                        return@loginByAccount
                    }
                } else if (code == 2333) {
                    //??????????????????
                    runOnUiThread {
                        loadingPopup.setTitle("?????????...")
                    }
                    register(phone)
                } else {
                    runOnUiThread {
                        //???????????????????????????????????????
                        loadingPopup.dismissWith {
                            Toasty.error(
                                binding.root.context,
                                "??????????????????????????????",
                                Toast.LENGTH_LONG,
                                true
                            ).show()
                        }
                    }
                }
            }
        }.start()
    }

    private fun register(phone: String) {
        Thread {
            AuthClient.registerByEmail(
                "${phone}@neu.app", "riMm78jUHCxWd4"
            ) { code: Int, message: String?, userInfo: UserInfo? ->
                if (code == 200) {
                    loadingPopup.setTitle("????????????????????????")
                    login(phone, true)
                } else {
                    runOnUiThread {
                        //???????????????????????????????????????
                        loadingPopup.dismissWith {
                            Toasty.error(
                                binding.root.context,
                                "??????????????????????????????",
                                Toast.LENGTH_LONG,
                                true
                            ).show()
                        }
                    }
                }
            }
        }.start()
    }

    private fun updateDefaultUserInfo() {
        Thread {
            AuthClient.uploadAvatar(assets.open("default_avator.png")) { code, _, _ ->
                if (code == 200) {
                    val info = JSONObject()
                    info.put("nickname", "?????????")
                    loadingPopup.setTitle("??????????????????")
                    AuthClient.updateProfile(info) { code2, _, _ ->
                        if (code2 == 200) {
                            runOnUiThread {
                                //???????????????????????????????????????
                                loadingPopup.dismissWith {
                                    Toasty.success(
                                        binding.root.context,
                                        "????????????:?????????",
                                        Toast.LENGTH_LONG,
                                        true
                                    ).show()
                                    finish()
                                }
                            }
                        } else {
                            runOnUiThread {
                                //???????????????????????????????????????
                                loadingPopup.dismissWith {
                                    Toasty.error(
                                        binding.root.context,
                                        "?????????????????????",
                                        Toast.LENGTH_LONG,
                                        true
                                    ).show()
                                }
                            }
                        }
                    }
                } else {
                    runOnUiThread {
                        //???????????????????????????????????????
                        loadingPopup.dismissWith {
                            Toasty.error(
                                binding.root.context,
                                "?????????????????????",
                                Toast.LENGTH_LONG,
                                true
                            ).show()
                        }
                    }
                }
            }
        }.start()
    }

    private fun sendSms(phone: String, code: String, seconds: Int) {
        //???????????????????????????
        Thread {
            try {
                val cred = Credential(
                    SecretKeyDao.TencentSMSSecretID,
                    SecretKeyDao.TencentSMSSecretKey
                )
                val httpProfile = HttpProfile()
                httpProfile.reqMethod = "POST"
                httpProfile.connTimeout = 10
                httpProfile.endpoint = "sms.tencentcloudapi.com"
                val clientProfile = ClientProfile()
                clientProfile.signMethod = "HmacSHA256"
                clientProfile.httpProfile = httpProfile
                val client = SmsClient(cred, "ap-beijing", clientProfile)
                val req = SendSmsRequest()
                req.smsSdkAppid = SecretKeyDao.TencentSMSSDKAppid
                req.sign = "????????????"
                req.templateID = "1380609"
                val templateParamSet = arrayOf(code, (seconds / 60).toString())
                req.templateParamSet = templateParamSet
                val phoneNumberSet = arrayOf("+86${phone}")
                req.phoneNumberSet = phoneNumberSet
                req.sessionContext = "$phone,$code"
                req.extendCode = ""
                req.senderId = ""
                val res: SendSmsResponse = client.SendSms(req)
                Log.e("message Send", SendSmsResponse.toJsonString(res))
                sendSmsCallback(res.sendStatusSet[0])

            } catch (e: TencentCloudSDKException) {
                e.printStackTrace()
                sendSmsCallback(null)
            }
        }.start()
    }

}
