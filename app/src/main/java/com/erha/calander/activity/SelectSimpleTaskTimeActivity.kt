package com.erha.calander.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.erha.calander.R
import com.erha.calander.databinding.ActivitySelectSimpleTaskTimeBinding
import com.erha.calander.popup.SelectTaskNotifyTimesPopup
import com.erha.calander.popup.SelectTaskNotifyTimesPopupCallback
import com.erha.calander.type.LocalStorageKey
import com.erha.calander.util.CalendarUtil
import com.erha.calander.util.TinyDB
import com.lxj.xpopup.XPopup
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.fontawesome.FontAwesome
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizeDp
import com.philliphsu.bottomsheetpickers.BottomSheetPickerDialog
import com.philliphsu.bottomsheetpickers.date.DatePickerDialog
import com.philliphsu.bottomsheetpickers.time.BottomSheetTimePickerDialog
import com.philliphsu.bottomsheetpickers.time.numberpad.NumberPadTimePickerDialog
import com.qmuiteam.qmui.util.QMUIDisplayHelper
import com.qmuiteam.qmui.widget.grouplist.QMUICommonListItemView
import es.dmoral.toasty.Toasty
import java.text.SimpleDateFormat
import java.util.*

object SelectSimpleTaskTime {
    const val requestCode = 1024
}

class SelectSimpleTaskTimeActivity : AppCompatActivity(), DatePickerDialog.OnDateSetListener,
    BottomSheetTimePickerDialog.OnTimeSetListener, SelectTaskNotifyTimesPopupCallback {
    //布局binding
    private lateinit var binding: ActivitySelectSimpleTaskTimeBinding
    private lateinit var store: TinyDB
    private val task = TaskTimeAndNotify()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectSimpleTaskTimeBinding.inflate(layoutInflater)
        store = TinyDB(binding.root.context)
        setContentView(binding.root)

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
        resources.getDimensionPixelSize(R.dimen.listview_radius).apply {
            binding.taskDDLQMUILinearLayout.radius = this
            binding.taskNotifyTimeQMUILinearLayout.radius = this
            binding.taskTimeQMUILinearLayout.radius = this
        }
        intent?.apply {
            task.hasTime = getBooleanExtra("hasTime", task.hasTime)
            task.date = getSerializableExtra("date") as Calendar
            task.isAllDay = getBooleanExtra("isAllDay", task.isAllDay)
            task.beginTime = getSerializableExtra("beginTime") as Calendar
            task.endTime = getSerializableExtra("endTime") as Calendar
            task.isDDL = getBooleanExtra("isDDL", task.isDDL)
            task.notifyTimes = getSerializableExtra("notifyTimes") as ArrayList<Int>
        }
        binding.submitZone.setOnClickListener {
            val intent = Intent()
            intent.apply {
                putExtra("hasTime", task.hasTime)
                putExtra("date", task.date)
                putExtra("isAllDay", task.isAllDay)
                putExtra("beginTime", task.beginTime)
                putExtra("endTime", task.endTime)
                putExtra("isDDL", task.isDDL)
                putExtra("notifyTimes", task.notifyTimes)
            }
            setResult(RESULT_OK, intent)
            finish()
        }
        refreshAllItems()
    }

    private var isInitItems = false
    private var isOpenTimePickerDialog4BeginTime = false
    private fun refreshAllItems() {
        binding.apply {
            taskDate.apply {
                if (!isInitItems) {
                    text = "日期"
                    setImageDrawable(
                        IconicsDrawable(
                            binding.root.context,
                            FontAwesome.Icon.faw_calendar
                        ).apply {
                            colorInt = resources.getColor(R.color.black)
                            sizeDp = 16
                        })
                    setOnClickListener {
                        openDatePickerDialog()
                    }
                    setOnLongClickListener {
                        Toasty.info(binding.root.context, "已清除日期", Toast.LENGTH_LONG, false).show()
                        task.hasTime = false
                        refreshAllItems()
                        true
                    }
                    orientation = QMUICommonListItemView.HORIZONTAL
                    setTipPosition(QMUICommonListItemView.TIP_POSITION_LEFT)
                }
                detailText = if (!task.hasTime) {
                    "未设置"
                } else {
                    SimpleDateFormat.getDateInstance().format(task.date.timeInMillis)
                }
            }
            taskAllDay.apply {
                if (!isInitItems) {
                    text = "全天事件"
                    setImageDrawable(
                        IconicsDrawable(
                            binding.root.context,
                            FontAwesome.Icon.faw_calendar_day
                        ).apply {
                            colorInt = resources.getColor(R.color.black)
                            sizeDp = 16
                        })
                    switch.setOnCheckedChangeListener { buttonView, isChecked ->
                        task.isAllDay = isChecked
                        refreshAllItems()
                    }
                    orientation = QMUICommonListItemView.HORIZONTAL
                    setTipPosition(QMUICommonListItemView.TIP_POSITION_LEFT)
                }
                switch.isEnabled = task.hasTime
                switch.isChecked = task.isAllDay
                if (task.isAllDay) {
                    binding.taskBeginTime.visibility = View.GONE
                    binding.taskEndTime.visibility = View.GONE
                } else {
                    binding.taskBeginTime.visibility = View.VISIBLE
                    binding.taskEndTime.visibility = View.VISIBLE
                }
            }
            taskBeginTime.apply {
                if (!isInitItems) {
                    text = "开始时间"
                    setImageDrawable(
                        IconicsDrawable(
                            binding.root.context,
                            FontAwesome.Icon.faw_clock
                        ).apply {
                            colorInt = resources.getColor(R.color.black)
                            sizeDp = 16
                        })
                    orientation = QMUICommonListItemView.HORIZONTAL
                    setTipPosition(QMUICommonListItemView.TIP_POSITION_LEFT)
                    setOnClickListener {
                        openTimePickerDialog(true)
                    }
                }
                detailText = if (!task.hasTime) {
                    "未设置日期"
                } else {
                    SimpleDateFormat.getTimeInstance().format(task.beginTime.timeInMillis)
                }

            }
            taskEndTime.apply {
                if (!isInitItems) {
                    text = "结束时间"
                    setImageDrawable(
                        IconicsDrawable(
                            binding.root.context,
                            FontAwesome.Icon.faw_clock
                        ).apply {
                            colorInt = resources.getColor(R.color.black)
                            sizeDp = 16
                        })
                    orientation = QMUICommonListItemView.HORIZONTAL
                    setTipPosition(QMUICommonListItemView.TIP_POSITION_LEFT)
                    setOnClickListener {
                        openTimePickerDialog(false)
                    }
                }
                detailText = if (!task.hasTime) {
                    "未设置日期"
                } else {
                    SimpleDateFormat.getTimeInstance().format(task.endTime.timeInMillis)
                }

            }
            taskDDL.apply {
                if (!isInitItems) {
                    text = "DDL"
                    setImageDrawable(
                        IconicsDrawable(
                            binding.root.context,
                            FontAwesome.Icon.faw_hourglass_end
                        ).apply {
                            colorInt = resources.getColor(R.color.black)
                            sizeDp = 16
                        })
                    orientation = QMUICommonListItemView.HORIZONTAL
                    setTipPosition(QMUICommonListItemView.TIP_POSITION_LEFT)
                    switch.setOnCheckedChangeListener { buttonView, isChecked ->
                        task.isDDL = isChecked
                    }
                }
                switch.isChecked = task.isDDL
            }
            taskNotifyTime.apply {
                if (!isInitItems) {
                    text = "提醒时机"
                    setImageDrawable(
                        IconicsDrawable(
                            binding.root.context,
                            FontAwesome.Icon.faw_bell
                        ).apply {
                            colorInt = resources.getColor(R.color.black)
                            sizeDp = 16
                        })
                    orientation = QMUICommonListItemView.HORIZONTAL
                    setTipPosition(QMUICommonListItemView.TIP_POSITION_LEFT)
                    setOnClickListener {
                        val selectTaskNotifyTimesPopup =
                            SelectTaskNotifyTimesPopup(binding.root.context)
                        selectTaskNotifyTimesPopup.selectedTimes = task.notifyTimes
                        selectTaskNotifyTimesPopup.callback = this@SelectSimpleTaskTimeActivity
                        XPopup.Builder(binding.root.context)
                            .dismissOnBackPressed(false)
                            .dismissOnTouchOutside(false)
                            .asCustom(selectTaskNotifyTimesPopup)
                            .show()
                    }
                    setOnLongClickListener {
                        Toasty.info(binding.root.context, "已清除提醒", Toast.LENGTH_LONG, false).show()
                        task.notifyTimes = ArrayList()
                        true
                    }
                    val paddingVer = QMUIDisplayHelper.dp2px(binding.root.context, 15)
                    setPadding(
                        paddingLeft, paddingVer,
                        paddingRight, paddingVer
                    )
                }
                detailText = if (!task.hasTime) {
                    "未设置日期无法设置提醒"
                } else {
                    if (task.notifyTimes.isEmpty()) {
                        "未设置提醒"
                    } else {
                        task.notifyTimes.sort()
                        var s = ""
                        var isFirst = true
                        for (time in task.notifyTimes) {
                            if (isFirst) {
                                s = getTimeText(time)
                                isFirst = false
                            } else {
                                s += ",${getTimeText(time)}"
                            }
                        }
                        s
                    }
                }
            }
        }
        if (!isInitItems) {
            isInitItems = true
        }
    }

    private fun openTimePickerDialog(is4BeginTime: Boolean) {
        isOpenTimePickerDialog4BeginTime = is4BeginTime
        var builder: BottomSheetPickerDialog = NumberPadTimePickerDialog.newInstance(
            this@SelectSimpleTaskTimeActivity,
            true
        )
        (builder as NumberPadTimePickerDialog).apply {
            setHint("输入时间")
        }
        builder.setAccentColor(resources.getColor(R.color.default_active))
        builder.setHeaderColor(resources.getColor(R.color.default_active))
        builder.show(supportFragmentManager, "Time")
    }

    private fun openDatePickerDialog() {
        //打开日期选择器
        val min = Calendar.getInstance()
        val max = Calendar.getInstance()
        max.add(Calendar.YEAR, 1)
        min.add(Calendar.YEAR, -1)
        val now = Calendar.getInstance()
        var builder: BottomSheetPickerDialog = DatePickerDialog.newInstance(
            this@SelectSimpleTaskTimeActivity,
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH),
            now.get(Calendar.DAY_OF_MONTH)
        )
        builder.setHeaderColor(resources.getColor(R.color.default_active))
        builder.setAccentColor(resources.getColor(R.color.default_active))
        var dateDialog: DatePickerDialog = builder as DatePickerDialog
        dateDialog.minDate = min
        dateDialog.maxDate = max
        dateDialog.setYearRange(
            Calendar.getInstance().get(Calendar.YEAR) - 2,
            Calendar.getInstance().get(Calendar.YEAR) + 2
        )
        //加载用户设置的每周的第一天
        dateDialog.firstDayOfWeek =
            when (TinyDB(binding.root.context).getInt(LocalStorageKey.FIRST_DAY_OF_WEEK)) {
                Calendar.MONDAY -> Calendar.MONDAY
                Calendar.SATURDAY -> Calendar.SATURDAY
                Calendar.SUNDAY -> Calendar.SUNDAY
                else -> Calendar.SUNDAY
            }
        builder.show(supportFragmentManager, "Time")
    }

    private fun getTimeText(minutesTotal: Int): String {
        if (minutesTotal == 0) {
            return "准时"
        }
        var string = "提前"
        var days = minutesTotal / 60 / 24
        if (days > 0) {
            string = "${string}${days}天"
        }
        var hours = (minutesTotal - days * 60 * 24) / 60
        if (hours > 0) {
            string = "${string}${hours}小时"
        }
        var minutes = (minutesTotal - days * 60 * 24 - hours * 60)
        if (minutes > 0) {
            string = "${string}${minutes}分钟"
        }
        return string
    }

    override fun onDateSet(
        dialog: DatePickerDialog?,
        year: Int,
        monthOfYear: Int,
        dayOfMonth: Int
    ) {
        var calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthOfYear)
            set(Calendar.DAY_OF_MONTH, dayOfMonth)
        }
        calendar = CalendarUtil.getWithoutTime(calendar)
        task.hasTime = true
        task.date = calendar
        refreshAllItems()
    }

    override fun onTimeSet(viewGroup: ViewGroup?, hourOfDay: Int, minute: Int) {
        if (isOpenTimePickerDialog4BeginTime) {
            task.beginTime.apply {
                set(Calendar.HOUR_OF_DAY, hourOfDay)
                set(Calendar.MINUTE, minute)
            }
            task.beginTime = CalendarUtil.getWithoutSecond(task.beginTime)
        } else {
            task.endTime.apply {
                set(Calendar.HOUR_OF_DAY, hourOfDay)
                set(Calendar.MINUTE, minute)
            }
            task.endTime = CalendarUtil.getWithoutSecond(task.endTime)
        }

        if (CalendarUtil.compareOnlyTime(task.beginTime, task.endTime) > 0) {
            if (isOpenTimePickerDialog4BeginTime) {
                task.endTime = task.beginTime
            } else {
                task.beginTime = task.endTime
            }
        }
        if (!task.hasTime) {
            Toasty.info(binding.root.context, "未设置日期无法添加时间", Toast.LENGTH_LONG, false).show()
        }
        refreshAllItems()
    }

    override fun onFinished(notifyTimesString: String) {
        task.notifyTimes.clear()
        for (timeString in notifyTimesString.split(",")) {
            try {
                val time = timeString.toInt()
                task.notifyTimes.add(time)
            } catch (e: NumberFormatException) {
            }
        }
        task.notifyTimes.distinct()
        task.notifyTimes.sort()
        if (!task.hasTime) {
            Toasty.info(binding.root.context, "未设置日期无法添加提醒", Toast.LENGTH_LONG, false).show()
        }
        refreshAllItems()
    }

}