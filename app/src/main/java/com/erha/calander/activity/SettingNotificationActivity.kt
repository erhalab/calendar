package com.erha.calander.activity

import SystemUtil
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings.*
import android.util.Log
import android.view.View
import android.widget.CheckBox
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.recyclical.datasource.emptyDataSourceTyped
import com.afollestad.recyclical.setup
import com.afollestad.recyclical.withItem
import com.erha.calander.R
import com.erha.calander.dao.ConfigDao
import com.erha.calander.dao.NotificationDao
import com.erha.calander.databinding.ActivitySettingNotificationBinding
import com.erha.calander.model.RecyclerViewItem
import com.erha.calander.popup.NotificationHelpPopup
import com.erha.calander.popup.SelectCourseNotifyTimesPopup
import com.erha.calander.type.LocalStorageKey
import com.erha.calander.type.NotificationChannelType
import com.erha.calander.util.TinyDB
import com.lxj.xpopup.XPopup
import com.qmuiteam.qmui.layout.QMUILayoutHelper
import com.qmuiteam.qmui.layout.QMUILinearLayout
import com.qmuiteam.qmui.skin.QMUISkinManager
import com.qmuiteam.qmui.util.QMUIDisplayHelper
import com.qmuiteam.qmui.widget.dialog.QMUIDialog
import com.qmuiteam.qmui.widget.dialog.QMUIDialogAction
import com.qmuiteam.qmui.widget.grouplist.QMUICommonListItemView
import es.dmoral.toasty.Toasty
import java.text.SimpleDateFormat

class NotificationSettingWatcherThread(
    var checkBox: CheckBox?,
    var context: android.content.Context?,
    var activity: SettingNotificationActivity
) : Thread() {
    override fun run() {
        try {
            Log.e("NotificationSettingWatcherThread", "watching")
            context?.apply {
                var notificationManager =
                    this.getSystemService(AppCompatActivity.NOTIFICATION_SERVICE) as NotificationManager
                checkBox?.apply {
                    activity.runOnUiThread {
                        this.isChecked = notificationManager.areNotificationsEnabled()
                    }
                    sleep(1000)
                    run()
                }
            }
        } catch (e: Exception) {
        } catch (e: java.lang.Exception) {
        }

    }
}


class SettingNotificationActivity : AppCompatActivity() {

    data class SettingItem(
        var title: String,
        var text: String,
        var isFirst: Boolean = false,
        var isLast: Boolean = false,
        var key: String = "default"
    ) : RecyclerViewItem() {
        class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val qmuiCommonListItemView: QMUICommonListItemView =
                itemView.findViewById(R.id.settingListItemView)
            val qmuiLinearLayout: QMUILinearLayout = itemView.findViewById(R.id.QMUILinearLayout)
        }
    }

    data class SwitcherItem(
        var title: String,
        var text: String,
        var isFirst: Boolean = false,
        var isLast: Boolean = false,
        var storeKey: String
    ) : RecyclerViewItem() {
        class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val qmuiCommonListItemView: QMUICommonListItemView =
                itemView.findViewById(R.id.settingListItemView)
            val qmuiLinearLayout: QMUILinearLayout = itemView.findViewById(R.id.QMUILinearLayout)
        }
    }

    data class SpecialSwitcherItem(
        var title: String,
        var text: String,
        var isFirst: Boolean = false,
        var isLast: Boolean = false,
        var key: String
    ) : RecyclerViewItem() {
        class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val qmuiCommonListItemView: QMUICommonListItemView =
                itemView.findViewById(R.id.settingListItemView)
            val qmuiLinearLayout: QMUILinearLayout = itemView.findViewById(R.id.QMUILinearLayout)
        }
    }

    data class SpaceItem(
        var key: String = "space"
    ) : RecyclerViewItem() {
        class SpaceItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    }

    var notificationSettingWatcherThread = NotificationSettingWatcherThread(null, null, this)

    //??????binding
    private lateinit var binding: ActivitySettingNotificationBinding
    private val dataSource = emptyDataSourceTyped<RecyclerViewItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        notificationSettingWatcherThread.context = binding.root.context

        binding.helpClickZone.setOnClickListener {
            XPopup.Builder(this)
                .isViewMode(true)
                .hasShadowBg(false)
                .offsetY(35)
                .atView(binding.helpIcon)
                .asCustom(NotificationHelpPopup(this))
                .show()
        }
        binding.backButton.setOnClickListener {
            finish()
        }
        binding.appStartTime.text = "???????????????????????????${
            SimpleDateFormat.getDateTimeInstance().format(ConfigDao.startTime.timeInMillis)
        }"
        dataSource.add(
            SpecialSwitcherItem(
                title = "?????????????????????",
                text = "????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????",
                isFirst = true,
                isLast = true,
                key = "ACTION_APP_NOTIFICATION_SETTINGS"
            ),
            SpaceItem(),
            SettingItem(
                title = "??????????????????",
                text = "????????????????????????????????????",
                isFirst = true,
                key = "courseNotification"
            ),
            SettingItem(
                title = "??????????????????",
                text = "??????????????????????????????\n???????????????????????????????????????????????????",
                isLast = true,
                key = "courseChannel"
            ),
            SpaceItem(),
            SwitcherItem(
                title = "??????????????????",
                text = "????????????????????????????????????\n????????????????????????????????????",
                storeKey = LocalStorageKey.REPOST_ONE_TASK_NOTIFICATION,
                isFirst = true
            ),
            SwitcherItem(
                title = "??????????????????",
                text = "????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????~",
                storeKey = LocalStorageKey.CLEAR_ONE_NOTIFICATION_EQUAL_CLEAR_TASK_NOTIFICATIONS,
                isLast = true
            ),
            SpaceItem(),
            SettingItem(
                title = "????????????",
                text = "???????????????????????????????????????????????????AI??????????????????????????????????????????????????????????????????????????????????????????????????????\n???????????????????????????????????????????????????????????????",
                isFirst = true,
                key = "ACTION_APP_NOTIFICATION_SETTINGS"
            ),
            SettingItem(
                title = "??????????????????????????????",
                text = "?????????????????????????????????????????????????????????",
                key = "ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"
            ),
            SettingItem(
                title = "??????????????????",
                text = "???????????????????????????~",
                isLast = true,
                key = "AUTO_START"
            )
        )
        // setup{} is an extension method on RecyclerView
        binding.settingRecyclerView.setup {
            withDataSource(dataSource)
            withItem<SpaceItem, SpaceItem.SpaceItemHolder>(R.layout.item_list_space_20) {
                onBind(SpaceItem::SpaceItemHolder) { _, _ ->
                }
            }
            withItem<SettingItem, SettingItem.Holder>(R.layout.item_list_setting_notification) {
                onBind(SettingItem::Holder) { index, item ->
                    qmuiCommonListItemView.text = item.title
                    qmuiCommonListItemView.detailText = item.text
                    qmuiCommonListItemView.orientation = QMUICommonListItemView.VERTICAL
                    val paddingVer = QMUIDisplayHelper.dp2px(binding.root.context, 10)
                    qmuiCommonListItemView.setPadding(
                        qmuiCommonListItemView.paddingLeft, paddingVer,
                        qmuiCommonListItemView.paddingRight, paddingVer
                    )
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
                        "courseNotification" -> {
                            XPopup.Builder(binding.root.context)
                                .dismissOnBackPressed(false)
                                .dismissOnTouchOutside(false)
                                .asCustom(SelectCourseNotifyTimesPopup(binding.root.context))
                                .show()
                        }
                        "courseChannel" -> {
                            var notificationManager =
                                applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                            if (!(notificationManager.areNotificationsEnabled())) {
                                QMUIDialog.MessageDialogBuilder(this@SettingNotificationActivity)
                                    .setTitle("?????????????????????")
                                    .setMessage("??????????????????????????????????????????????????????????????????????????????")
                                    .setCanceledOnTouchOutside(false)
                                    .setSkinManager(
                                        QMUISkinManager.defaultInstance(
                                            applicationContext
                                        )
                                    )
                                    .addAction(
                                        0,
                                        "??????",
                                        QMUIDialogAction.ACTION_PROP_NEGATIVE
                                    ) { dialog, index ->
                                        dialog.dismiss()
                                    }
                                    .addAction(
                                        0, "?????????", QMUIDialogAction.ACTION_PROP_POSITIVE
                                    ) { dialog, index ->
                                        openNotificationSetting()
                                        dialog.dismiss()
                                    }
                                    .create(com.qmuiteam.qmui.R.style.QMUI_Dialog).show()
                            } else {

                                val intent =
                                    Intent(ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                                        putExtra(EXTRA_APP_PACKAGE, packageName)
                                        putExtra(
                                            EXTRA_CHANNEL_ID,
                                            NotificationChannelType.COURSE
                                        )
                                    }
                                startActivity(intent)

                            }
                        }
                        "ACTION_APP_NOTIFICATION_SETTINGS" -> {
                            openNotificationSetting()
                        }
                        "ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" -> {
                            try {
                                val i = Intent()
                                i.action = ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                                startActivity(i)
                            } catch (e: Exception) {
                                Toasty.info(binding.root.context, "???????????????????????????").show()
                            }
                        }
                        "AUTO_START" -> {
                            try {
                                try {
                                    val mIntent = Intent()
                                    mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    mIntent.action = "android.settings.APPLICATION_DETAILS_SETTINGS"
                                    mIntent.data =
                                        Uri.fromParts(
                                            "package",
                                            binding.root.context.packageName,
                                            null
                                        )
                                    startActivity(mIntent)
                                } catch (e: Exception) {
                                    if (SystemUtil.getDeviceBrand() == SystemUtil.PHONE_XIAOMI) {
                                        val intent2 = Intent()
                                        intent2.action = "miui.intent.action.OP_AUTO_START"
                                        intent2.addCategory("android.intent.category.DEFAULT")
                                        startActivity(intent2)
                                    }
                                }
                            } catch (e: Exception) {
                                Toasty.info(binding.root.context, "???????????????????????????").show()
                            }
                        }
                        else -> {}
                    }

                }
            }

            withItem<SwitcherItem, SwitcherItem.Holder>(R.layout.item_list_setting_notification) {
                onBind(SwitcherItem::Holder) { index, item ->
                    qmuiCommonListItemView.text = item.title
                    qmuiCommonListItemView.detailText = item.text
                    qmuiCommonListItemView.accessoryType =
                        QMUICommonListItemView.ACCESSORY_TYPE_SWITCH
                    qmuiCommonListItemView.orientation = QMUICommonListItemView.VERTICAL
                    val paddingVer = QMUIDisplayHelper.dp2px(binding.root.context, 10)
                    qmuiCommonListItemView.setPadding(
                        qmuiCommonListItemView.paddingLeft, paddingVer,
                        qmuiCommonListItemView.paddingRight, paddingVer
                    )
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
                    qmuiCommonListItemView.switch.isChecked =
                        TinyDB(binding.root.context).getBoolean(item.storeKey)
                    qmuiCommonListItemView.switch.setOnCheckedChangeListener { buttonView, isChecked ->
                        TinyDB(binding.root.context).putBoolean(item.storeKey, isChecked)
                        when (item.storeKey) {
                            LocalStorageKey.REPOST_ONE_TASK_NOTIFICATION -> NotificationDao.repostOneTaskNotification =
                                isChecked
                        }
                    }
                }
                onClick { index ->
                }
            }

            withItem<SpecialSwitcherItem, SpecialSwitcherItem.Holder>(R.layout.item_list_setting_notification) {
                onBind(SpecialSwitcherItem::Holder) { index, item ->
                    qmuiCommonListItemView.text = item.title
                    qmuiCommonListItemView.detailText = item.text
                    qmuiCommonListItemView.accessoryType =
                        QMUICommonListItemView.ACCESSORY_TYPE_SWITCH
                    qmuiCommonListItemView.orientation = QMUICommonListItemView.VERTICAL
                    val paddingVer = QMUIDisplayHelper.dp2px(binding.root.context, 10)
                    qmuiCommonListItemView.setPadding(
                        qmuiCommonListItemView.paddingLeft, paddingVer,
                        qmuiCommonListItemView.paddingRight, paddingVer
                    )
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
                    qmuiCommonListItemView.switch.isClickable = false
                    when (item.key) {
                        "ACTION_APP_NOTIFICATION_SETTINGS" -> {
                            notificationSettingWatcherThread.checkBox =
                                qmuiCommonListItemView.switch
                            notificationSettingWatcherThread.start()
                        }
                    }

                }
                onClick { index ->
                    when (item.key) {
                        "ACTION_APP_NOTIFICATION_SETTINGS" -> {
                            openNotificationSetting()
                        }
                    }
                }
            }

        }
    }


    private fun openNotificationSetting() {
        try {
            //?????????App????????????
            val intent = Intent()
            intent.action = ACTION_APP_NOTIFICATION_SETTINGS
            //????????????????????? API 26, ???8.0??????8.0??????????????????
            intent.putExtra(EXTRA_APP_PACKAGE, packageName)
            intent.putExtra(EXTRA_CHANNEL_ID, applicationInfo.uid)
            //????????????????????? API21??????25?????? 5.0??????7.1 ???????????????????????????
            intent.putExtra("app_package", packageName)
            intent.putExtra("app_uid", applicationInfo.uid)
            startActivity(intent)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            val intent = Intent()
            intent.action = ACTION_APPLICATION_DETAILS_SETTINGS
            val uri = Uri.fromParts("package", packageName, null)
            intent.data = uri
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        try {
            notificationSettingWatcherThread.interrupt()
            notificationSettingWatcherThread.stop()
            notificationSettingWatcherThread.destroy()
        } catch (e: Exception) {
        } catch (e: java.lang.Exception) {
        }
        super.onDestroy()

    }

}
