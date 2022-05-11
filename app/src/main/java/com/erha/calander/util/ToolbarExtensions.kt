@file:JvmName("ToolbarUtils")

package com.erha.calander.util

import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.alamkanak.weekview.WeekView
import com.erha.calander.R
import com.erha.calander.type.SettingType
import com.philliphsu.bottomsheetpickers.BottomSheetPickerDialog
import com.philliphsu.bottomsheetpickers.date.DatePickerDialog
import java.util.*


private enum class WeekViewType(val value: Int) {
    DayView(1),
    ThreeDayView(3),
    WeekView(7);

    companion object {
        fun of(days: Int): WeekViewType = values().first { it.value == days }
    }
}

fun Toolbar.setupWithWeekView(weekView: WeekView, fragment: Fragment) {
    var currentViewType = WeekViewType.of(weekView.numberOfVisibleDays)

    inflateMenu(R.menu.menu_main)
    setOnMenuItemClickListener { item ->
        when (item.itemId) {
            //响应 跳转到今天的按钮
            R.id.action_today -> {
                var time = Calendar.getInstance()
                if (weekView.numberOfVisibleDays == 7) {
                    time.add(
                        Calendar.DAY_OF_MONTH,
                        -1 * (time.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY)
                    )
                }
                weekView.scrollToDateTime(dateTime = time)
                true
            }
            R.id.action_time -> {
                val min = Calendar.getInstance()
                val max = Calendar.getInstance()
                max.add(Calendar.YEAR, 1)
                min.add(Calendar.YEAR, -1)
                val now = weekView.firstVisibleDate
                var builder: BottomSheetPickerDialog = DatePickerDialog.newInstance(
                    fragment as DatePickerDialog.OnDateSetListener,
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
                    when (TinyDB(context).getInt(SettingType.FIRSR_DAY_OF_WEEK)) {
                        Calendar.MONDAY -> Calendar.MONDAY
                        Calendar.SATURDAY -> Calendar.SATURDAY
                        Calendar.SUNDAY -> Calendar.SUNDAY
                        else -> Calendar.SUNDAY
                    }
                fragment.fragmentManager?.let { builder.show(it, "Time") }
                true
            }
            //响应下拉菜单，周视图、日视图切换
            else -> {
                val viewType = item.mapToWeekViewType()
                if (viewType != currentViewType) {
                    item.isChecked = !item.isChecked
                    currentViewType = viewType
                    weekView.numberOfVisibleDays = viewType.value
                    //如果是周视图，确保第一天还是星期天
                    if (viewType.value == 7) {
                        var c = weekView.firstVisibleDate.clone() as Calendar
                        c.add(
                            Calendar.DAY_OF_MONTH,
                            -1 * (c.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY)
                        )
                        weekView.scrollToDate(c)
                    }
                }
                true
            }
        }
    }

}


private fun MenuItem.mapToWeekViewType(): WeekViewType {
    return when (itemId) {
        R.id.action_day_view -> WeekViewType.DayView
        R.id.action_three_day_view -> WeekViewType.ThreeDayView
        R.id.action_week_view -> WeekViewType.WeekView
        else -> throw IllegalArgumentException("Invalid menu item ID $itemId")
    }
}
