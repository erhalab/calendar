package com.erha.calander.fragment

import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.util.DisplayMetrics
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
import com.afollestad.recyclical.datasource.emptyDataSourceTyped
import com.afollestad.recyclical.setup
import com.afollestad.recyclical.withItem
import com.erha.calander.R
import com.erha.calander.activity.*
import com.erha.calander.databinding.FragmentSettingsBinding
import com.erha.calander.model.RecyclerViewItem
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
import es.dmoral.toasty.Toasty
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
    ) : RecyclerViewItem() {
        class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val qmuiCommonListItemView: QMUICommonListItemView =
                itemView.findViewById(R.id.settingListItemView)
            val qmuiLinearLayout: QMUILinearLayout = itemView.findViewById(R.id.QMUILinearLayout)
        }
    }

    data class SimpleItem(
        var titleResId: Int,
        var key: String,
        var isFirst: Boolean = false,
        var isLast: Boolean = false,
        var textColor: Int,
    ) : RecyclerViewItem() {
        class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val textView: TextView = itemView.findViewById(R.id.textView)
            val qmuiLinearLayout: QMUILinearLayout = itemView.findViewById(R.id.QMUILinearLayout)
        }
    }

    data class SpaceItem(
        var key: String = "space"
    ) : RecyclerViewItem() {
        class Holder(itemView: View) : RecyclerView.ViewHolder(itemView)
    }

    data class Icon(
        var key: IIcon,
        var color: Int,
        var sizeCorrect: Int = 0
    )

    private lateinit var binding: FragmentSettingsBinding
    private lateinit var store: TinyDB
    private val dataSource = emptyDataSourceTyped<RecyclerViewItem>()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        saveInstanceState: Bundle?
    ): View? {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        store = TinyDB(binding.root.context)

        //??????ActivityResultLauncher
        val resultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                Log.e("registerForActivityResult", it.resultCode.toString())
            }

        binding.settingRecyclerView.setup {
            withDataSource(dataSource)
            withItem<SpaceItem, SpaceItem.Holder>(R.layout.item_list_space_20) {
                onBind(SpaceItem::Holder) { _, _ ->

                }
            }
            withItem<SimpleItem, SimpleItem.Holder>(R.layout.item_list_simple) {
                onBind(SimpleItem::Holder) { _, item ->
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
                    } else if (item.isLast && !item.isFirst) {
                        qmuiLinearLayout.setRadius(radius, QMUILayoutHelper.HIDE_RADIUS_SIDE_TOP)
                    } else if (item.isFirst && !item.isLast) {
                        qmuiLinearLayout.setRadius(radius, QMUILayoutHelper.HIDE_RADIUS_SIDE_BOTTOM)
                    } else {
                        qmuiLinearLayout.radius = 0
                    }
                }
                onClick { index ->
                    when (item.key) {
                        "exitApp" -> {
                            activity?.finishAffinity()
                        }
                        "resetGuide" -> {
                            MessageDialogBuilder(binding.root.context)
                                .setTitle("??????")
                                .setMessage("?????????????????????App?????????????????????????????????????????????????????????????????????")
                                .setSkinManager(QMUISkinManager.defaultInstance(context))
                                .addAction(
                                    "??????"
                                ) { dialog, _ -> dialog.dismiss() }
                                .addAction(
                                    0, "??????", QMUIDialogAction.ACTION_PROP_NEGATIVE
                                ) { dialog, _ ->
                                    for (i in store.all.keys) {
                                        if (i.startsWith(LocalStorageKey.USER_GUIDE_STATE_PREFIX)) {
                                            store.remove(i)
                                        }
                                    }
                                    Toasty.info(
                                        binding.root.context,
                                        "????????????",
                                        Toast.LENGTH_SHORT,
                                        false
                                    ).show()
                                    dialog.dismiss()
                                }
                                .create(com.qmuiteam.qmui.R.style.QMUI_Dialog).show()
                        }
                    }
                }
            }
            withItem<SettingItem, SettingItem.Holder>(R.layout.item_list_setting) {
                onBind(SettingItem::Holder) { index, item ->
                    qmuiCommonListItemView.text = getString(item.titleResId)
                    qmuiCommonListItemView.setImageDrawable(
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
                    } else if (item.isLast && !item.isFirst) {
                        qmuiLinearLayout.setRadius(radius, QMUILayoutHelper.HIDE_RADIUS_SIDE_TOP)
                    } else if (item.isFirst && !item.isLast) {
                        qmuiLinearLayout.setRadius(radius, QMUILayoutHelper.HIDE_RADIUS_SIDE_BOTTOM)
                    } else {
                        qmuiLinearLayout.radius = 0
                    }
                }
                onClick { index ->
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
                                            //??????????????????????????????
                                            MessageDialogBuilder(binding.root.context)
                                                .setTitle("?????????????????????")
                                                .setMessage("???Webview??????????????????????????????????????????????????????????????????????????????????????????????????????????????????")
                                                .setSkinManager(
                                                    QMUISkinManager.defaultInstance(
                                                        binding.root.context.applicationContext
                                                    )
                                                )
                                                .addAction(
                                                    "??????"
                                                ) { dialog, index -> dialog.dismiss() }
                                                .addAction(
                                                    0, "?????????", QMUIDialogAction.ACTION_PROP_POSITIVE
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
                                                //3?????????????????????????????????????????????Webview
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
                                                //?????????????????????????????????
                                                MessageDialogBuilder(binding.root.context)
                                                    .setTitle("???????????????")
                                                    .setMessage("???????????????????????????????????????????????????????????????Webview???????????????\n???????????????????????????App?????????30???????????????")
                                                    .setSkinManager(
                                                        QMUISkinManager.defaultInstance(
                                                            binding.root.context.applicationContext
                                                        )
                                                    )
                                                    .addAction(
                                                        0,
                                                        "??????",
                                                        QMUIDialogAction.ACTION_PROP_POSITIVE
                                                    ) { dialog, index ->
                                                        dialog.dismiss()
                                                    }
                                                    .create(com.qmuiteam.qmui.R.style.QMUI_Dialog)
                                                    .show()
                                            }
                                        }
                                        3 -> {
                                            //???????????????????????????????????????
                                            MessageDialogBuilder(binding.root.context)
                                                .setTitle("??????????????????")
                                                .setMessage("?????????????????????????????????????????????\n*????????????Webview??????????????????X5?????????X5?????????")
                                                .setSkinManager(
                                                    QMUISkinManager.defaultInstance(
                                                        binding.root.context.applicationContext
                                                    )
                                                )
                                                .addAction(
                                                    "??????"
                                                ) { dialog, index -> dialog.dismiss() }
                                                .addAction(
                                                    0, "????????????", QMUIDialogAction.ACTION_PROP_NEGATIVE
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

                }
                onLongClick { index ->
                }
            }
        }
        initDataSource()

        return binding.root
    }

    private fun initDataSource() {
        dataSource.clear()
        dataSource.add(
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
                titleResId = R.string.text_reset_guide,
                key = "resetGuide",
                isFirst = true,
                isLast = true,
                textColor = resources.getColor(R.color.dark_orange)
            ),
//            SpaceItem(),
//            SimpleItem(
//                titleResId = R.string.text_exit_app,
//                key = "exitApp",
//                isFirst = true,
//                isLast = true,
//                textColor = resources.getColor(R.color.qmui_config_color_red)
//            ),
        )
    }

    private fun initX5() {
        // ?????????TBS??????????????????WebView????????????????????????
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
                    // ?????????????????????
                    Log.e("X5", "?????????????????????")
                }

                /**
                 * ??????????????????
                 * ??????X5?????????????????????????????????????????????????????????????????????????????????????????????????????????false???????????????????????????????????????
                 * @param isX5 ????????????X5??????
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

    private fun showLanguageChoiceDialog() {
        val items = arrayOf(
            getString(R.string.language_zh), getString(R.string.language_en)
        )
        val locales = arrayOf(Locale.SIMPLIFIED_CHINESE, Locale.ENGLISH)
        var checkedIndex = 0
        val sta = store.getString(LocalStorageKey.LANGUAGE)
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
                Toast.makeText(activity, "???????????? " + items[which], Toast.LENGTH_SHORT).show()
                changeAppLanguage(locales[which])
                dialog.dismiss()
            }
            .create(com.qmuiteam.qmui.R.style.QMUI_Dialog).show()
    }

    private fun changeAppLanguage(locale: Locale) {
        store.putString(LocalStorageKey.LANGUAGE, locale.language)
        // ??????????????????
        val res: Resources = resources
        val dm: DisplayMetrics = res.displayMetrics
        val conf: Configuration = res.configuration
        conf.locale = locale
        res.updateConfiguration(conf, dm)
        Locale.setDefault(locale)
        onConfigurationChanged(conf)
        //??????????????????????????????
        binding.settingTitle.text = resources.getText(R.string.menu_setting_text)
        initDataSource()
        //?????? ???????????? ??????
        EventBus.getDefault().post(EventType.LANGUAGE_CHANGE)
    }


}