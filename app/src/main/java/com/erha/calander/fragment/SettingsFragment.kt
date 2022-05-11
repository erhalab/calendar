package com.erha.calander.fragment

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.recyclical.datasource.dataSourceTypedOf
import com.afollestad.recyclical.setup
import com.afollestad.recyclical.withItem
import com.erha.calander.R
import com.erha.calander.activity.*
import com.erha.calander.databinding.FragmentSettingsBinding
import com.erha.calander.type.EventType
import com.erha.calander.type.LocalStorageKey
import com.erha.calander.util.TinyDB
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.typeface.library.fontawesome.FontAwesome
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizeDp
import com.qmuiteam.qmui.layout.QMUILayoutHelper
import com.qmuiteam.qmui.layout.QMUILinearLayout
import com.qmuiteam.qmui.skin.QMUISkinManager
import com.qmuiteam.qmui.util.QMUIDisplayHelper
import com.qmuiteam.qmui.widget.dialog.QMUIDialog.CheckableDialogBuilder
import com.qmuiteam.qmui.widget.dialog.QMUIDialog.MessageDialogBuilder
import com.qmuiteam.qmui.widget.dialog.QMUIDialogAction
import com.qmuiteam.qmui.widget.grouplist.QMUICommonListItemView
import com.tencent.smtt.export.external.TbsCoreSettings
import com.tencent.smtt.sdk.QbSdk
import org.greenrobot.eventbus.EventBus
import java.util.*

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    data class SettingItem(
        var titleResId: Int,
        var icon: Icon,
        var isFirst: Boolean = false,
        var isLast: Boolean = false,
        var activity: Class<out AppCompatActivity>? = null,
        var key: String = "default"
    )

    data class SimpleItem(
        var titleResId: Int,
        var key: String,
        var isFirst: Boolean = false,
        var isLast: Boolean = false,
        var textColor: Int,
    )

    data class SpaceItem(
        var key: String = "space"
    )

    data class Icon(
        var key: IIcon,
        var color: Int,
        var sizeCorrect: Int = 0
    )

    private lateinit var binding: FragmentSettingsBinding
    private lateinit var store: TinyDB
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        saveInstanceState: Bundle?
    ): View? {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        store = TinyDB(binding.root.context)
        // dataSourceTypedOf(...) here creates a DataSource<Person>
        val dataSource = dataSourceTypedOf(
            SettingItem(
                titleResId = R.string.setting_import_class,
                icon = Icon(
                    key = FontAwesome.Icon.faw_chalkboard_teacher,
                    color = Color.parseColor("#4d70fa"),
                    sizeCorrect = 2
                ),
                isFirst = true,
                isLast = true,
                key = "importClass"
            ).apply {
            },
            SpaceItem(),
            SettingItem(
                titleResId = R.string.setting_componment,
                icon = Icon(
                    key = FontAwesome.Icon.faw_list_ol, color = Color.parseColor("#4d70fa"),
                    sizeCorrect = 2
                ),
                isFirst = true,
                isLast = true
            ).apply {
                activity = SettingModelActivity::class.java
            },
            SpaceItem(),
            SettingItem(
                titleResId = R.string.setting_language,
                icon = Icon(
                    key = FontAwesome.Icon.faw_language,
                    color = Color.parseColor("#4d70fa")
                ),
                isFirst = true,
                key = "language"
            ),
            SettingItem(
                titleResId = R.string.setting_time,
                icon = Icon(key = FontAwesome.Icon.faw_clock, color = Color.parseColor("#4d70fa"))
            ).apply {
                activity = SettingTimeActivity::class.java
            },
            SettingItem(
                titleResId = R.string.setting_notify,
                icon = Icon(key = FontAwesome.Icon.faw_music, color = Color.parseColor("#4d70fa")),
                isLast = true
            ).apply {
                activity = SettingNotificationActivity::class.java
            },
            SpaceItem(),
            SettingItem(
                titleResId = R.string.setting_about,
                icon = Icon(
                    key = FontAwesome.Icon.faw_info_circle,
                    color = Color.parseColor("#ffb001")
                ),
                isFirst = true,
                isLast = true,
            ).apply {
                activity = AboutActivity::class.java
            },
            SpaceItem(),
            SimpleItem(
                titleResId = R.string.text_exit_app,
                key = "exitApp",
                isFirst = true,
                isLast = true,
                textColor = resources.getColor(R.color.qmui_config_color_red)
            ),
        )

        //创建ActivityResultLauncher
        val resultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                Log.e("registerForActivityResult", it.resultCode.toString())
//            if(it.resultCode == Activity.RESULT_OK){
//                val result = it.data?.getStringExtra("testBean") ?: null
//            }
            }

        // setup{} is an extension method on RecyclerView
        binding.settingRecyclerView.setup {
            withDataSource(dataSource)
            withItem<SpaceItem, SpaceItemHolder>(R.layout.item_list_space_20) {
                onBind(::SpaceItemHolder) { _, _ ->

                }
            }
            withItem<SimpleItem, SimpleItemHolder>(R.layout.item_list_simple) {
                onBind(::SimpleItemHolder) { _, item ->
                    textView.apply {
                        text = getString(item.titleResId)
                        setTextColor(item.textColor)
                    }
                    qmuiLinearLayout.apply {
                        val paddingVer = QMUIDisplayHelper.dp2px(binding.root.context, 15)
                        setPadding(
                            paddingLeft, paddingVer,
                            paddingRight, paddingVer
                        )
                    }

                    val radius =
                        resources.getDimensionPixelSize(R.dimen.listview_radius)
                    if (item.isFirst && item.isLast) {
                        qmuiLinearLayout.radius = radius
                    } else if (item.isLast) {
                        qmuiLinearLayout.setRadius(radius, QMUILayoutHelper.HIDE_RADIUS_SIDE_TOP)
                    } else if (item.isFirst) {
                        qmuiLinearLayout.setRadius(radius, QMUILayoutHelper.HIDE_RADIUS_SIDE_BOTTOM)
                    }
                }
                onClick { index ->
                    when (item.key) {
                        "exitApp" -> {
                            activity?.finishAffinity()
                        }
                    }
                }
            }
            withItem<SettingItem, SettingItemHolder>(R.layout.item_list_setting) {
                onBind(::SettingItemHolder) { index, item ->
                    setting.text = getString(item.titleResId)
                    setting.setImageDrawable(
                        IconicsDrawable(
                            binding.root.context,
                            item.icon.key
                        ).apply {
                            colorInt = item.icon.color
                            sizeDp = 19 + item.icon.sizeCorrect
                        })
                    val radius =
                        resources.getDimensionPixelSize(R.dimen.listview_radius)
                    if (item.isFirst && item.isLast) {
                        qmuiLinearLayout.radius = radius
                    } else if (item.isLast) {
                        qmuiLinearLayout.setRadius(radius, QMUILayoutHelper.HIDE_RADIUS_SIDE_TOP)
                    } else if (item.isFirst) {
                        qmuiLinearLayout.setRadius(radius, QMUILayoutHelper.HIDE_RADIUS_SIDE_BOTTOM)
                    }
                }
                onClick { index ->
                    // item is a `val` in `this` here
                    when {
                        item.activity != null -> {
                            resultLauncher.launch(Intent(activity, item.activity))
                        }
                        else -> {
                            when (item.key) {
                                "language" -> {
                                    showLanguageChoiceDialog()
                                }
                                "importClass" -> {
                                    var status =
                                        store.getInt(LocalStorageKey.LAST_LAUNCH_WEBVIEW_SUCCESS)
                                    when (status) {
                                        -1, 0 -> {
                                            //提醒用户可能发生闪退
                                            MessageDialogBuilder(binding.root.context)
                                                .setTitle("首次初始化导入")
                                                .setMessage("因Webview问题，首次导入可能闪退。如果点击下一步后，发生闪退，请重启应用然后重试导入。")
                                                .setSkinManager(
                                                    QMUISkinManager.defaultInstance(
                                                        binding.root.context.applicationContext
                                                    )
                                                )
                                                .addAction(
                                                    "取消"
                                                ) { dialog, index -> dialog.dismiss() }
                                                .addAction(
                                                    0, "下一步", QMUIDialogAction.ACTION_PROP_POSITIVE
                                                ) { dialog, index ->
                                                    dialog.dismiss()
                                                    store.putInt(
                                                        LocalStorageKey.LAST_LAUNCH_WEBVIEW_SUCCESS,
                                                        1
                                                    )
                                                    resultLauncher.launch(
                                                        Intent(
                                                            activity,
                                                            ImportClassSystemActivity::class.java
                                                        )
                                                    )
                                                }
                                                .create(com.qmuiteam.qmui.R.style.QMUI_Dialog)
                                                .show()
                                        }
                                        2 -> {
                                            resultLauncher.launch(
                                                Intent(
                                                    activity,
                                                    ImportClassSystemActivity::class.java
                                                )
                                            )
                                        }
                                        4 -> {
                                            resultLauncher.launch(
                                                Intent(
                                                    activity,
                                                    ImportClassActivity::class.java
                                                )
                                            )
                                        }
                                        1 -> {
                                            if (!QbSdk.isNeedInitX5FirstTime() && QbSdk.isTbsCoreInited()) {
                                                //3代表，内核表示加载完成了，启动Webview
                                                store.putInt(
                                                    LocalStorageKey.LAST_LAUNCH_WEBVIEW_SUCCESS,
                                                    3
                                                )
                                                resultLauncher.launch(
                                                    Intent(
                                                        activity,
                                                        ImportClassActivity::class.java
                                                    )
                                                )
                                            } else {
                                                initX5()
                                                //提醒用户等待初始化结果
                                                MessageDialogBuilder(binding.root.context)
                                                    .setTitle("捕获到异常")
                                                    .setMessage("上一次导入课程界面启动失败，您无法借助默认Webview导入课程。\n请连接互联网后重启App，等待30秒后重试。")
                                                    .setSkinManager(
                                                        QMUISkinManager.defaultInstance(
                                                            binding.root.context.applicationContext
                                                        )
                                                    )
                                                    .addAction(
                                                        0,
                                                        "好的",
                                                        QMUIDialogAction.ACTION_PROP_POSITIVE
                                                    ) { dialog, index ->
                                                        dialog.dismiss()
                                                    }
                                                    .create(com.qmuiteam.qmui.R.style.QMUI_Dialog)
                                                    .show()
                                            }
                                        }
                                        3 -> {
                                            //走到这一步，就说明不合适他
                                            MessageDialogBuilder(binding.root.context)
                                                .setTitle("无法导入课程")
                                                .setMessage("本次自动导入失败，您可以重试。\n*启动默认Webview失败，初始化X5后启动X5失败。")
                                                .setSkinManager(
                                                    QMUISkinManager.defaultInstance(
                                                        binding.root.context.applicationContext
                                                    )
                                                )
                                                .addAction(
                                                    "取消"
                                                ) { dialog, index -> dialog.dismiss() }
                                                .addAction(
                                                    0, "重置流程", QMUIDialogAction.ACTION_PROP_NEGATIVE
                                                ) { dialog, index ->
                                                    dialog.dismiss()
                                                    store.putInt(
                                                        LocalStorageKey.LAST_LAUNCH_WEBVIEW_SUCCESS,
                                                        0
                                                    )
                                                }
                                                .create(com.qmuiteam.qmui.R.style.QMUI_Dialog)
                                                .show()
                                        }
                                    }
                                }
                            }
                        }

                    }

//                    activity?.overridePendingTransition(R.anim.slide_right_in,R.anim.slide_left_out)
                }
                onLongClick { index ->
                    // item is a `val` in `this` here
                }
            }
        }

        return binding.root
    }

    private fun initX5() {
        // 在调用TBS初始化、创建WebView之前进行如下配置
        val map = HashMap<String, Any>()
        map[TbsCoreSettings.TBS_SETTINGS_USE_SPEEDY_CLASSLOADER] = true
        map[TbsCoreSettings.TBS_SETTINGS_USE_DEXLOADER_SERVICE] = true
        map[TbsCoreSettings.TBS_SETTINGS_USE_PRIVATE_CLASSLOADER] = true
        QbSdk.initTbsSettings(map)
        QbSdk.unForceSysWebView()
        QbSdk.setDownloadWithoutWifi(true)
        QbSdk.initX5Environment(
            binding.root.context.applicationContext,
            object : QbSdk.PreInitCallback {
                override fun onCoreInitFinished() {
                    // 内核初始化完成
                    Log.e("X5", "内核初始化完成")
                }

                /**
                 * 预初始化结束
                 * 由于X5内核体积较大，需要依赖网络动态下发，所以当内核不存在的时候，默认会回调false，此时将会使用系统内核代替
                 * @param isX5 是否使用X5内核
                 */
                override fun onViewInitFinished(isX5: Boolean) {
                    store.putBoolean(LocalStorageKey.X5_INIT, true)
                    Log.e("X5", "onViewInitFinished -> ${isX5}")
                    Log.e("X5", "getIsInitX5Environment -> ${QbSdk.getIsInitX5Environment()}")
                    Log.e("X5", "getTBSInstalling -> ${QbSdk.getTBSInstalling()}")
                    Log.e("X5", "getOnlyDownload -> ${QbSdk.getOnlyDownload()}")
                }
            })
    }

    class SettingItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val setting: QMUICommonListItemView = itemView.findViewById(R.id.settingListItemView)
        val qmuiLinearLayout: QMUILinearLayout = itemView.findViewById(R.id.QMUILinearLayout)
    }

    class SimpleItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.textView)
        val qmuiLinearLayout: QMUILinearLayout = itemView.findViewById(R.id.QMUILinearLayout)
    }

    class SpaceItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private fun showLanguageChoiceDialog() {
        val items = arrayOf(
            getString(R.string.language_zh), getString(R.string.language_en)
        )
        val locals = arrayOf(Locale.SIMPLIFIED_CHINESE, Locale.ENGLISH)
        var checkedIndex = 0
        val sta = store.getString("language")
        sta?.apply {
            when (this) {
                Locale.SIMPLIFIED_CHINESE.language -> checkedIndex = 0
                Locale.ENGLISH.language -> checkedIndex = 1
            }
        }
        CheckableDialogBuilder(binding.root.context)
            .setCheckedIndex(checkedIndex)
            .setSkinManager(QMUISkinManager.defaultInstance(context))
            .addItems(items) { dialog, which ->
                Toast.makeText(activity, "你选择了 " + items[which], Toast.LENGTH_SHORT).show()
                store.putString("language", locals[which].language)
                val intent = Intent()
                intent.putExtra("msg", "EVENT_REFRESH_LANGUAGE")
                binding.root.context.sendBroadcast(intent)
                changeAppLanguage()
                EventBus.getDefault().post(EventType.LANGUAGE_CHANGE)
                dialog.dismiss()
            }
            .create(com.qmuiteam.qmui.R.style.QMUI_Dialog).show()
    }

    private fun changeAppLanguage() {
        val sta = store.getString("language") //这是SharedPreferences工具类，用于保存设置，代码很简单，自己实现吧
        // 本地语言设置
        val myLocale = Locale(sta)
        val res = resources
        val dm = res.displayMetrics
        val conf: Configuration = res.configuration
        conf.locale = myLocale
        res.updateConfiguration(conf, dm)
        (activity as MainActivity).recreateFragment(this)
    }


}