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

    //布局binding
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
        binding.appStartTime.text = "本次应用启动时间：${
            SimpleDateFormat.getDateTimeInstance().format(ConfigDao.startTime.timeInMillis)
        }"
        dataSource.add(
            SpecialSwitcherItem(
                title = "应用总的通知权",
                text = "如果应用本身不能发出铃声，那么分类通知也不能发出铃声（例如课程提醒）。其他类似。",
                isFirst = true,
                isLast = true,
                key = "ACTION_APP_NOTIFICATION_SETTINGS"
            ),
            SpaceItem(),
            SettingItem(
                title = "课程提醒时机",
                text = "所有课程共享一种提醒机制",
                isFirst = true,
                key = "courseNotification"
            ),
            SettingItem(
                title = "课程提醒方式",
                text = "铃声、震动、呼吸灯；\n是否在锁屏推送、屏幕上方小通知等等",
                isLast = true,
                key = "courseChannel"
            ),
            SpaceItem(),
            SwitcherItem(
                title = "重复发出提醒",
                text = "开启时，即启用通知互斥。\n具体说明请点击右上方查看",
                storeKey = LocalStorageKey.REPOST_ONE_TASK_NOTIFICATION,
                isFirst = true
            ),
            SwitcherItem(
                title = "通知被划掉时",
                text = "开启时，划掉视作该事项不再提醒，等同于点击通知按钮。阁下慎重开启，任何形式划掉他都会触发~",
                storeKey = LocalStorageKey.CLEAR_ONE_NOTIFICATION_EQUAL_CLEAR_TASK_NOTIFICATIONS,
                isLast = true
            ),
            SpaceItem(),
            SettingItem(
                title = "通知过滤",
                text = "如果手机有智能通知分类、自动过滤、AI分组等功能，建议取消过滤本应用（设置通知等级为最高、重要、不过滤）；\n否则，有可能导致一个事项同时存在多条推送。",
                isFirst = true,
                key = "ACTION_APP_NOTIFICATION_SETTINGS"
            ),
            SettingItem(
                title = "关闭此应用的电池优化",
                text = "仅针对该应用有效，需要您授权或手动操作",
                key = "ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"
            ),
            SettingItem(
                title = "自启动该应用",
                text = "提高通知推送成功率~",
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
                    val radius =
                        resources.getDimensionPixelSize(R.dimen.listview_radius)
                    qmuiCommonListItemView.orientation = QMUICommonListItemView.VERTICAL
                    val paddingVer = QMUIDisplayHelper.dp2px(binding.root.context, 10)
                    qmuiCommonListItemView.setPadding(
                        qmuiCommonListItemView.paddingLeft, paddingVer,
                        qmuiCommonListItemView.paddingRight, paddingVer
                    )
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
                                    .setTitle("应用没有通知权")
                                    .setMessage("应用无法发送任何通知，请启用它，再调整课程提醒方式。")
                                    .setCanceledOnTouchOutside(false)
                                    .setSkinManager(
                                        QMUISkinManager.defaultInstance(
                                            applicationContext
                                        )
                                    )
                                    .addAction(
                                        0,
                                        "取消",
                                        QMUIDialogAction.ACTION_PROP_NEGATIVE
                                    ) { dialog, index ->
                                        dialog.dismiss()
                                    }
                                    .addAction(
                                        0, "去设置", QMUIDialogAction.ACTION_PROP_POSITIVE
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
                                Toasty.info(binding.root.context, "无法跳转到系统界面").show()
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
                                Toasty.info(binding.root.context, "无法跳转到系统界面").show()
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
                    val radius =
                        resources.getDimensionPixelSize(R.dimen.listview_radius)
                    qmuiCommonListItemView.accessoryType =
                        QMUICommonListItemView.ACCESSORY_TYPE_SWITCH
                    qmuiCommonListItemView.orientation = QMUICommonListItemView.VERTICAL
                    val paddingVer = QMUIDisplayHelper.dp2px(binding.root.context, 10)
                    qmuiCommonListItemView.setPadding(
                        qmuiCommonListItemView.paddingLeft, paddingVer,
                        qmuiCommonListItemView.paddingRight, paddingVer
                    )
                    if (item.isFirst && item.isLast) {
                        qmuiLinearLayout.radius = radius
                    } else if (item.isLast) {
                        qmuiLinearLayout.setRadius(radius, QMUILayoutHelper.HIDE_RADIUS_SIDE_TOP)
                    } else if (item.isFirst) {
                        qmuiLinearLayout.setRadius(radius, QMUILayoutHelper.HIDE_RADIUS_SIDE_BOTTOM)
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
                    val radius =
                        resources.getDimensionPixelSize(R.dimen.listview_radius)
                    qmuiCommonListItemView.accessoryType =
                        QMUICommonListItemView.ACCESSORY_TYPE_SWITCH
                    qmuiCommonListItemView.orientation = QMUICommonListItemView.VERTICAL
                    val paddingVer = QMUIDisplayHelper.dp2px(binding.root.context, 10)
                    qmuiCommonListItemView.setPadding(
                        qmuiCommonListItemView.paddingLeft, paddingVer,
                        qmuiCommonListItemView.paddingRight, paddingVer
                    )
                    if (item.isFirst && item.isLast) {
                        qmuiLinearLayout.radius = radius
                    } else if (item.isLast) {
                        qmuiLinearLayout.setRadius(radius, QMUILayoutHelper.HIDE_RADIUS_SIDE_TOP)
                    } else if (item.isFirst) {
                        qmuiLinearLayout.setRadius(radius, QMUILayoutHelper.HIDE_RADIUS_SIDE_BOTTOM)
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
            //去打开App通知权限
            val intent = Intent()
            intent.action = ACTION_APP_NOTIFICATION_SETTINGS
            //这种方案适用于 API 26, 即8.0（含8.0）以上可以用
            intent.putExtra(EXTRA_APP_PACKAGE, packageName)
            intent.putExtra(EXTRA_CHANNEL_ID, applicationInfo.uid)
            //这种方案适用于 API21——25，即 5.0——7.1 之间的版本可以使用
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
