package com.erha.calander.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.recyclical.datasource.emptyDataSourceTyped
import com.afollestad.recyclical.setup
import com.afollestad.recyclical.withItem
import com.erha.calander.R
import com.erha.calander.databinding.ActivitySettingTimeBinding
import com.erha.calander.model.RecyclerViewItem
import com.erha.calander.popup.BeginWeekPopup
import com.erha.calander.type.EventType
import com.erha.calander.type.LocalStorageKey
import com.erha.calander.util.TinyDB
import com.lxj.xpopup.XPopup
import com.qmuiteam.qmui.layout.QMUILayoutHelper
import com.qmuiteam.qmui.layout.QMUILinearLayout
import com.qmuiteam.qmui.util.QMUIDisplayHelper
import com.qmuiteam.qmui.widget.grouplist.QMUICommonListItemView
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.text.SimpleDateFormat
import java.util.*


class SettingTimeActivity : AppCompatActivity() {
    data class TimeItem(
        var key: String,
        var titleResId: Int,
        var subtitleResId: Int,
        var isSwitch: Boolean = false,
        var isFirst: Boolean = false,
        var isLast: Boolean = false
    ) : RecyclerViewItem() {
        class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val qmuiCommonListItemView: QMUICommonListItemView =
                itemView.findViewById(R.id.timeListItemView)
            val qmuiLinearLayout: QMUILinearLayout = itemView.findViewById(R.id.QMUILinearLayout)
        }
    }

    data class SpaceItem(
        var key: String = "space"
    ) : RecyclerViewItem() {
        class Holder(itemView: View) : RecyclerView.ViewHolder(itemView)
    }

    //布局binding
    private lateinit var binding: ActivitySettingTimeBinding
    private lateinit var store: TinyDB
    private val dataSource = emptyDataSourceTyped<RecyclerViewItem>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingTimeBinding.inflate(layoutInflater)
        store = TinyDB(binding.root.context)
        setContentView(binding.root)
        EventBus.getDefault().register(this)

        window.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = getColor(R.color.default_background_color)
            decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        binding.backButton.setOnClickListener { v ->
            run {
                setResult(Activity.RESULT_CANCELED, Intent())
                finish()
            }
        }
        dataSource.add(
            TimeItem(
                key = "beginWeek",
                titleResId = R.string.setting_time_the_first_week,
                subtitleResId = R.string.setting_time_the_first_week_subtitle,
                isFirst = true
            ),
//            TimeItem(
//                key = "firstDayOfWeek",
//                titleResId = R.string.setting_time_first_day_of_week,
//                subtitleResId = R.string.setting_time_first_day_of_week_subtitle,
//                isLast = true
//            ),
            SpaceItem()
        )
        //初始化列表
        binding.timeRecyclerView.setup {
            withDataSource(dataSource)
            withItem<SpaceItem, SpaceItem.Holder>(R.layout.item_list_space_20) {
                onBind(SpaceItem::Holder) { _, _ ->
                }
            }
            withItem<TimeItem, TimeItem.Holder>(R.layout.item_list_time) {
                onBind(TimeItem::Holder) { index, item ->
                    val paddingVer = QMUIDisplayHelper.dp2px(binding.root.context, 10)
                    this.qmuiCommonListItemView.apply {
                        when (item.key) {
                            "beginWeek" -> detailText = getFirstWeekSubtitle()
                            else -> detailText = getString(item.subtitleResId)
                        }
                        text = getString(item.titleResId)
                        if (item.isSwitch) {
                            accessoryType = QMUICommonListItemView.ACCESSORY_TYPE_SWITCH
                        } else {
                            accessoryType = QMUICommonListItemView.ACCESSORY_TYPE_CHEVRON
                        }
                        orientation = QMUICommonListItemView.VERTICAL
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
                    XPopup.Builder(binding.root.context)
                        .dismissOnBackPressed(false)
                        .dismissOnTouchOutside(false)
                        .asCustom(BeginWeekPopup(binding.root.context))
                        .show()
                }
                onLongClick { index ->
                }
            }
        }

    }

    //更新切换起始周的副标题
    private fun refreshFirstWeekSubtitle() {
        ((binding.timeRecyclerView.getChildAt(0) as QMUILinearLayout).getChildAt(0) as QMUICommonListItemView).apply {
            detailText = getFirstWeekSubtitle()
        }
    }

    //获取切换起始周的副标题
    private fun getFirstWeekSubtitle(): String {
        //通过语言确定格式
        var format: SimpleDateFormat
        when (store.getString(LocalStorageKey.LANGUAGE)) {
            Locale.ENGLISH.language -> {
                format = SimpleDateFormat("MMM d", Locale.ENGLISH)
            }
            else -> format = SimpleDateFormat("MMMd日", Locale.CHINESE)
        }
        var text = resources.getText(R.string.text_default)

        var weekDate = store.getString(LocalStorageKey.FIRST_WEEK).toString()
        var calendar: Calendar? = null
        if (weekDate.isNotBlank()) {
            calendar = Calendar.getInstance().apply {
                set(Calendar.MONTH, weekDate.split(",")[0].toInt() - 1)
                set(Calendar.DAY_OF_MONTH, weekDate.split(",")[1].toInt())
            }
            text = resources.getString(
                R.string.setting_time_the_first_week_subtitle,
                format.format(calendar.time)
            )
        }
        return text.toString()
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onEvent(str: String?) {
        when (str) {
            EventType.FIRST_WEEK_CHANGE -> {
                refreshFirstWeekSubtitle()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

}