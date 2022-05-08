package com.erha.calander.activity

import ando.file.core.FileUtils
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.erha.calander.R
import com.erha.calander.dao.CourseDao
import com.erha.calander.data.model.SimpleNEUClass
import com.erha.calander.databinding.ActivityImportClassBinding
import com.erha.calander.popup.ImportClassHelpPopup
import com.erha.calander.type.EventType
import com.erha.calander.type.SettingType
import com.erha.calander.util.TinyDB
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import com.lxj.xpopup.XPopup
import com.qmuiteam.qmui.skin.QMUISkinManager
import com.qmuiteam.qmui.widget.dialog.QMUIDialog.MessageDialogBuilder
import com.qmuiteam.qmui.widget.dialog.QMUIDialogAction
import com.tencent.smtt.export.external.interfaces.SslErrorHandler
import com.tencent.smtt.sdk.WebView
import com.tencent.smtt.sdk.WebViewClient
import es.dmoral.toasty.Toasty
import org.greenrobot.eventbus.EventBus
import java.io.ByteArrayInputStream
import java.io.File


class ImportClassActivity : AppCompatActivity() {

    data class Semester(
        var id: Int,
        var text: String,
        var beginDay: String
    )

    private lateinit var binding: ActivityImportClassBinding
    private lateinit var store: TinyDB

    private var isClassPage = false
    private var isTablePage = false

    private var semesterId = -1
    private var semesters = ArrayList<Semester>()

    init {
        semesters.add(Semester(id = 56, text = "2021-2022秋季(上学期)", beginDay = "2021,9,5"))
        semesters.add(Semester(id = 57, text = "2021-2022春季(下学期)", beginDay = "2022,2,27"))
    }

    private var urlIndex = 1
    private var loginUrl = arrayOf(
        "http://219.216.96.4/eams/",
        "https://webvpn.neu.edu.cn/http/77726476706e69737468656265737421a2a618d275613e1e275ec7f8/eams/homeExt.action"
    )
    private var tableUrl = arrayOf(
        "http://219.216.96.4/eams/courseTableForStd.action",
        "https://webvpn.neu.edu.cn/http/77726476706e69737468656265737421a2a618d275613e1e275ec7f8/eams/courseTableForStd.action?vpn-12-o1-219.216.96.4&_=1651797953485"
    )
    private var classUrl = arrayOf(
        "http://219.216.96.4/eams/courseTableForStd!courseTable.action?",
        "https://webvpn.neu.edu.cn/http/77726476706e69737468656265737421a2a618d275613e1e275ec7f8/eams/courseTableForStd!courseTable.action?vpn-12-o1-219.216.96.4&"
    )

    private fun askInternetType() {
        MessageDialogBuilder(this)
            .setTitle("你正在使用校园网？")
            .setMessage("包括：NEU-2.4G等全系列校园WIFI、校园宽带以及有线校园网。")
            .setCanceledOnTouchOutside(false)
            .setSkinManager(QMUISkinManager.defaultInstance(applicationContext))
            .addAction(
                "是的"
            ) { dialog, index ->
                urlIndex = 0
                binding.webview.loadUrl(loginUrl[urlIndex])
                askSemester()
                dialog.dismiss()
            }
            .addAction(
                0, "非校园网", QMUIDialogAction.ACTION_PROP_POSITIVE
            ) { dialog, index ->
                urlIndex = 1
                binding.webview.loadUrl(loginUrl[urlIndex])
                askSemester()
                dialog.dismiss()
            }
            .create(com.qmuiteam.qmui.R.style.QMUI_Dialog).show()
    }

    private fun askSemester() {
        var i = ArrayList<String>()
        for (s in semesters) {
            i.add(s.text)
        }
        var array = arrayOfNulls<String>(i.size)
        i.toArray(array)
        //需要用户设置学期
        XPopup.Builder(this@ImportClassActivity)
            .dismissOnTouchOutside(false)
            .dismissOnBackPressed(false)
            .isDestroyOnDismiss(true)
            .asCenterList(
                "请选择目标学期", array
            ) { position, text ->
                for (s in semesters) {
                    if (text == s.text) {
                        semesterId = s.id
                        break
                    }
                }

                XPopup.Builder(this@ImportClassActivity)
                    .isViewMode(true)
                    .hasShadowBg(false)
                    .offsetY(35)
                    .atView(binding.helpIcon)
                    .asCustom(ImportClassHelpPopup(this@ImportClassActivity))
                    .show()

            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        store.putInt(SettingType.LAST_LAUNCH_WEBVIEW_SUCCESS, 4)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImportClassBinding.inflate(layoutInflater)
        setContentView(binding.root)
        store = TinyDB(binding.root.context)
        askInternetType()
        WebView.setWebContentsDebuggingEnabled(false)
        binding.webview.loadUrl(loginUrl[1])
        binding.webview.apply {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                binding.webview.settings.mixedContentMode =
                    android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }
            settings.safeBrowsingEnabled = false
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.javaScriptEnabled = true
            Toasty.info(
                applicationContext,
                "x5 ${binding.webview.isX5Core}",
                Toast.LENGTH_SHORT
            ).show()
            //系统默认会通过手机浏览器打开网页，为了能够直接通过WebView显示网页，则必须设置
            //设置WebViewClient
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    //使用WebView加载显示url
                    view.loadUrl(url)
                    return true
                }

                override fun onReceivedSslError(
                    p0: WebView?,
                    p1: SslErrorHandler?,
                    p2: com.tencent.smtt.export.external.interfaces.SslError?
                ) {
                    super.onReceivedSslError(p0, p1, p2)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    if (url == null || url.isEmpty()) {
                        return
                    }
                    if (isTablePage) {
                        isTablePage = false
                        val javaScript =
                            "(function (){return window.document.body.outerHTML.match(/(?<=bg\\.form\\.addInput\\(form,\\\"ids\\\",\\\").*?(?=\\\")/)[0]})()"
                        binding.webview.evaluateJavascript(javaScript) { value ->
                            if (value == null || value.length <= 4) {
                                Toasty.error(
                                    binding.root.context,
                                    "无法获取到课程表ID", Toast.LENGTH_LONG
                                ).show()
                                return@evaluateJavascript
                            }
                            value.apply {
                                val id = this.substring(1, length - 1)
                                isClassPage = true
                                when (urlIndex) {
                                    0 -> binding.webview.loadUrl(classUrl[0] + "setting.kind=std&semester.id=${semesterId}&ids=${id}")
                                    1 -> binding.webview.loadUrl(classUrl[1] + "&setting.kind=std&semester.id=${semesterId}&ids=${id}")
                                }
                            }
                        }
                    }
                    if (isClassPage) {
                        isClassPage = false
                        val javaScript =
                            "(function (){let t=document.getElementsByTagName(\"html\")[0].innerHTML,e=[];var i=/(?<=var[ \\t]*actTeachers[ \\t]*=.*name:[ \\t]*\\\").*?(?=\\\")/,n=/(?<=activity[\\t ]*=[ \\t]*new[\\t ]*TaskActivity\\(.*\\\"[0-9A-Za-z]{4,10}\\([0-9A-Za-z]{4,10}\\)\\\"[\\t ]*,[\\t ]*\\\").*?(?=\\\")/,a=/(?<=activity[\\t ]*=[\\t ]*new[\\t ]*TaskActivity\\(([^\"]*?\\\"){7})[^\"]*?(?=\\\")/,c=/(?<=\\\")[0-1]{53}(?=\\\")/,r=/index[\\t ]*=[\\t ]*[0-6][\\t ]*\\*[\\t ]*unitCount[\\t ]*\\+[\\t ]*[0-9][0-1]?[\\t ]*;/g,s=/(?<=index[\\t ]*=[\\t ]*)[0-6](?=[\\t ]*\\*)/,h=/(?<=index[\\t ]*=[\\t ]*.*?unitCount[\\t ]*\\+[\\t ]*)[0-9][0-1]?/,m=String(t).match(/var[ \\t]*teachers[\\t ]*=[\\t ]*[\\s\\S]*?(?=table0\\.activities\\[index\\]\\[table0\\.activities\\[index\\]\\.length\\][ \\t]*=[\\t ]*activity;[\\r\\n\\t ]*[vt])/g);for(let t=0;t<m.length;t++){const o=m[t];let u={name:String,position:String,teacher:String,weeks:[],day:Number,sections:[]};u.name=o.match(n)[0],u.position=o.match(a)[0],u.teacher=o.match(i)[0],u.day=Number(o.match(s)[0])+1;let g=o.match(c)[0];for(let t=0;t<g.length;t++)\"1\"==g[t]&&u.weeks.push(t);var l=o.match(r);for(let t=0;t<l.length;t++){const e=l[t];u.sections.push(Number(e.match(h)[0])+1)}e.push(u)}return JSON.stringify(e)})()"
                        binding.webview.evaluateJavascript(
                            javaScript
                        ) { value ->
                            if (value == null) {
                                Toasty.error(
                                    binding.root.context,
                                    R.string.string_import_class_fail, Toast.LENGTH_LONG
                                ).show()
                                return@evaluateJavascript
                            }
                            value.apply {
                                val json = substring(1, length - 1).replace("\\", "")

                                var classList: List<SimpleNEUClass>? = null
                                try {
                                    classList = Gson().fromJson(
                                        json,
                                        (object : TypeToken<List<SimpleNEUClass>>() {}.type)
                                    )
                                } catch (e: JsonParseException) {
                                    Toasty.error(
                                        binding.root.context,
                                        R.string.string_import_class_fail, Toast.LENGTH_LONG
                                    ).show()
                                    return@evaluateJavascript
                                }

                                classList?.forEach { simpleNEUClass ->
                                    Log.e("Class Information", simpleNEUClass.toString())
                                }

                                MessageDialogBuilder(this@ImportClassActivity)
                                    .setTitle("⚠️提醒")
                                    .setMessage("保存该课表会覆盖之前的课表数据，你考虑下？")
                                    .setCanceledOnTouchOutside(false)
                                    .setCancelable(false)
                                    .setSkinManager(QMUISkinManager.defaultInstance(context))
                                    .addAction(
                                        "再想想"
                                    ) { dialog, index -> dialog.dismiss() }
                                    .addAction(
                                        0, "覆盖", QMUIDialogAction.ACTION_PROP_NEGATIVE
                                    ) { dialog, index ->

                                        for (s in semesters) {
                                            if (s.id == semesterId) {
                                                store.putString(
                                                    SettingType.COURSE_FIRST_DAY,
                                                    s.beginDay
                                                )
                                                store.putString(
                                                    SettingType.FIRST_WEEK,
                                                    "${s.beginDay.split(",")[1]},${
                                                        s.beginDay.split(
                                                            ","
                                                        )[2]
                                                    }"
                                                )
                                            }
                                        }
                                        //创建线程更新，不要让ui卡住
                                        Thread {
                                            FileUtils.write2File(
                                                input = ByteArrayInputStream(json.toByteArray()),
                                                file = File(File(filesDir, "course"), "data.json"),
                                                overwrite = true
                                            )
                                            CourseDao.reload(
                                                store.getString(SettingType.COURSE_FIRST_DAY),
                                                store.getString(SettingType.COURSE_NOTIFY_TIME)
                                            )
                                            //发布事件变化通知
                                            EventBus.getDefault().post(EventType.EVENT_CHANGE)
                                        }.start()

                                        Toasty.success(
                                            applicationContext,
                                            "导入成功",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        dialog.dismiss()
                                        finish()
                                    }
                                    .create(com.qmuiteam.qmui.R.style.QMUI_Dialog).show()


                            }
                        }
                    }

                }
            }
            binding.loginSuccess.setOnClickListener { v ->
                run {
                    //用户说登录成功了欸
                    isTablePage = true
                    loadUrl(tableUrl[urlIndex])
                }
            }
            binding.helpClickZone.setOnClickListener {
                XPopup.Builder(this@ImportClassActivity)
                    .isViewMode(true)
                    .hasShadowBg(false)
                    .offsetY(35)
                    .atView(binding.helpIcon)
                    .asCustom(ImportClassHelpPopup(this@ImportClassActivity))
                    .show()
            }

            binding.backButton.setOnClickListener { v ->
                run {
                    finish()
                }
            }


        }

    }

    //点击返回上一页面而不是退出浏览器
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        binding.webview.apply {
            if (keyCode == KeyEvent.KEYCODE_BACK && this.canGoBack()) {
                this.goBack()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    //销毁Webview
    override fun onDestroy() {
        binding.webview.apply {
            loadDataWithBaseURL(null, "", "text/html", "utf-8", null)
            clearHistory()
            (parent as ViewGroup).removeView(this)
            destroy()
        }
        super.onDestroy()
    }
}