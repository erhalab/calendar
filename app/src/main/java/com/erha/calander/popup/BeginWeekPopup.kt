package com.erha.calander.popup

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import androidx.annotation.NonNull
import cn.carbswang.android.numberpickerview.library.NumberPickerView
import com.erha.calander.R
import com.erha.calander.type.EventType
import com.erha.calander.type.LocalStorageKey
import com.erha.calander.util.TinyDB
import com.lxj.xpopup.core.CenterPopupView
import com.qmuiteam.qmui.layout.QMUILinearLayout
import com.qmuiteam.qmui.util.QMUIDisplayHelper
import org.greenrobot.eventbus.EventBus
import java.text.SimpleDateFormat
import java.util.*


class BeginWeekPopup  //注意：自定义弹窗本质是一个自定义View，但是只需重写一个参数的构造，其他的不要重写，所有的自定义弹窗都是这样。
    (@NonNull context: Context?) : CenterPopupView(context!!) {
    // 返回自定义弹窗的布局
    override fun getImplLayoutId(): Int {
        return R.layout.popup_setting_time_first_week
    }

    private lateinit var store: TinyDB
    private var isCustom: Boolean = false

    // 执行初始化操作，比如：findView，设置点击，或者任何你弹窗内的业务逻辑
    override fun onCreate() {
        super.onCreate()
        store = TinyDB(context)
        var customDate = ""
        if (!store.getString(LocalStorageKey.FIRST_WEEK).isNullOrBlank()) {
            isCustom = true
            customDate = store.getString(LocalStorageKey.FIRST_WEEK).toString()
        }
        //取消按钮事件
        findViewById<View>(R.id.buttomCancle).setOnClickListener {
            dismiss() // 关闭弹窗
        }
        //完成按钮事件
        findViewById<View>(R.id.buttomFinish).setOnClickListener {
            store.apply {
                if (isCustom) {
                    putString(
                        LocalStorageKey.FIRST_WEEK,
                        "${(findViewById<View>(R.id.monthPicker) as NumberPickerView).value + 1},${
                            (findViewById<View>(
                                R.id.dayPicker
                            ) as NumberPickerView).value + 1
                        }"
                    )
                } else {
                    putString(LocalStorageKey.FIRST_WEEK, "")
                }
            }
            EventBus.getDefault().post(EventType.FIRST_WEEK_CHANGE)
            dismiss() // 关闭弹窗
        }
        //初始化圆角和阴影
        (findViewById<View>(R.id.QMUILinearLayout) as QMUILinearLayout).apply {
            setRadiusAndShadow(
                resources.getDimensionPixelSize(R.dimen.popup_radius),
                QMUIDisplayHelper.dp2px(context, 5),
                0.3F
            )
        }
        //初始化第一个轮盘
        (findViewById<View>(R.id.monthPicker) as NumberPickerView).apply {
            refreshByNewDisplayedValues(resources.getStringArray(R.array.month))
            setOnValueChangedListener { picker, oldVal, newVal ->
                run {
                    refreshDayPicker(newVal + 1)
                    refreshSubText()
                }
            }
            setOnValueChangeListenerInScrolling { picker, oldVal, newVal ->
                run {
                    refreshSubText()
                }
            }
        }
        if (isCustom) {
            (findViewById<View>(R.id.customZone) as LinearLayout).visibility = View.VISIBLE
            (findViewById<View>(R.id.monthPicker) as NumberPickerView).value =
                customDate.split(",")[0].toInt() - 1
        } else {
            (findViewById<View>(R.id.customZone) as LinearLayout).visibility = View.GONE
        }
        (findViewById<View>(R.id.customCheckBox) as RadioButton).apply {
            isChecked = isCustom
        }
        (findViewById<View>(R.id.defaultCheckBox) as RadioButton).apply {
            isChecked = !isCustom
        }

        //初始化第二个轮盘
        (findViewById<View>(R.id.dayPicker) as NumberPickerView).apply {
            if (isCustom) {
                refreshDayPicker(customDate.split(",")[0].toInt())
                value = customDate.split(",")[1].toInt() - 1
                refreshSubText(customDate)
            }
            setOnValueChangedListener { picker, oldVal, newVal ->
                run {
                    refreshSubText()
                }
            }
            setOnValueChangeListenerInScrolling { picker, oldVal, newVal ->
                run {
                    refreshSubText()
                }
            }
        }
        //拦截选项里面的CheckBox点击事件
        (findViewById<View>(R.id.customCheckBox) as RadioButton).apply {
            setOnClickListener {
                clickOnFirstWeekItem(true)
            }
        }
        (findViewById<View>(R.id.defaultCheckBox) as RadioButton).apply {
            setOnClickListener {
                clickOnFirstWeekItem(false)
            }
        }
    }

    private fun clickOnFirstWeekItem(isCustom: Boolean) {
        if (isCustom == this.isCustom) {
            return
        }
        this.isCustom = isCustom
        (findViewById<View>(R.id.customZone) as LinearLayout).visibility =
            if (isCustom) View.VISIBLE else View.GONE

        var monthValue = Calendar.getInstance().get(Calendar.MONTH)
        var dayValue = Calendar.getInstance().get(Calendar.DAY_OF_MONTH) - 1

        if (isCustom) {
            //如果是自定义的话
            val customDate = store.getString(LocalStorageKey.FIRST_WEEK).toString()
            if (customDate.isNotBlank()) {
                monthValue =
                    customDate.split(",")[0].toInt() - 1
                dayValue = customDate.split(",")[1].toInt() - 1
            }
            (findViewById<View>(R.id.monthPicker) as NumberPickerView).value = monthValue
            refreshDayPicker(monthValue + 1)
            (findViewById<View>(R.id.dayPicker) as NumberPickerView).value = dayValue
        }
        refreshSubText("${monthValue + 1},${dayValue + 1}")
    }

    //添加参数以试图修正文本显示
    private fun refreshSubText(customDate: String = "") {
        val c = Calendar.getInstance().apply {
            set(Calendar.MONTH, (findViewById<View>(R.id.monthPicker) as NumberPickerView).value)
            set(
                Calendar.DAY_OF_MONTH,
                (findViewById<View>(R.id.dayPicker) as NumberPickerView).value + 1
            )
        }
        Log.e(
            "refreshSubText",
            (findViewById<View>(R.id.monthPicker) as NumberPickerView).value.toString() + " " + (findViewById<View>(
                R.id.dayPicker
            ) as NumberPickerView).value.toString()
        )
        if (customDate.isNotBlank()) {
            c.apply {
                set(Calendar.MONTH, customDate.split(",")[0].toInt() - 1)
                set(
                    Calendar.DAY_OF_MONTH,
                    customDate.split(",")[1].toInt()
                )
            }
        }
        val format: SimpleDateFormat = when (store.getString("language")) {
            Locale.ENGLISH.language -> {
                SimpleDateFormat("MMM dd", Locale.ENGLISH)
            }
            else -> SimpleDateFormat("MMMd日", Locale.CHINESE)
        }

        (findViewById<View>(R.id.beginWeekSubtitle) as TextView).text =
            resources.getString(R.string.popup_first_week_subtitle, format.format(c.time))
        Log.e(
            "refreshSubText",
            resources.getString(R.string.popup_first_week_subtitle, format.format(c.time))
        )
    }

    //此处month从1开始
    private fun refreshDayPicker(month: Int) {
        val days = arrayOfNulls<String>(getMonthDays(month))
        var end = resources.getText(R.string.text_day).toString()
        when (store.getString("language")) {
            Locale.ENGLISH.language -> end = ""
        }
        for (i in days.indices) {
            days[i] = "${i + 1}${end}"
        }
        (findViewById<View>(R.id.dayPicker) as NumberPickerView).apply {
            refreshByNewDisplayedValues(days)
        }
    }

    //此处month从1开始
    private fun getMonthDays(month: Int): Int {
        val cal: Calendar = Calendar.getInstance()
        cal.set(Calendar.YEAR, 2022)
        cal.set(Calendar.MONTH, month - 1)
        cal.set(Calendar.DATE, 1)
        cal.roll(Calendar.DATE, -1)
        return cal.getActualMaximum(Calendar.DATE)
    }

    /**
     * 弹窗的宽度，用来动态设定当前弹窗的宽度，受getMaxWidth()限制
     *
     * @return
     */
    override fun getPopupWidth(): Int {
        return 0
    }

    /**
     * 弹窗的高度，用来动态设定当前弹窗的高度，受getMaxHeight()限制
     *
     * @return
     */
    override fun getPopupHeight(): Int {
        return 0
    }
}