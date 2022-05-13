package com.erha.calander.util

import java.util.*

object CalendarUtil {
    //返回只包含日期的，其他设置为0
    fun getWithoutTime(calendar: Calendar = Calendar.getInstance()): Calendar {
        val r = calendar.clone() as Calendar
        r.apply {
            set(Calendar.HOUR, 0)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return r
    }

    //返回只包含日期和小时、分钟，其他为0
    fun getWithoutSecond(calendar: Calendar = Calendar.getInstance()): Calendar {
        val r = calendar.clone() as Calendar
        r.apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return r
    }

    //只比较时间，包括小时...毫秒
    fun compareOnlyTime(calendar1: Calendar, calendar2: Calendar): Int {
        var a = Calendar.getInstance()
        var b = calendar1.clone() as Calendar
        var c = calendar2.clone() as Calendar
        b.apply {
            set(Calendar.YEAR, a.get(Calendar.YEAR))
            set(Calendar.DAY_OF_YEAR, a.get(Calendar.DAY_OF_YEAR))
        }
        c.apply {
            set(Calendar.YEAR, a.get(Calendar.YEAR))
            set(Calendar.DAY_OF_YEAR, a.get(Calendar.DAY_OF_YEAR))
        }
        return b.compareTo(c)
    }

    //返回周数
    fun getWeekNumber(calendar: Calendar): Int {
        return calendar.get(Calendar.WEEK_OF_YEAR)
    }

    fun getWeekNumber(calendar: Calendar, firstWeekCalendar: Calendar): Int {
        // 自己适配周数
        firstWeekCalendar.apply {
            val now = getWithoutTime(calendar)
            val begin2 = getWithoutTime()
            begin2.apply {
                set(Calendar.YEAR, now.get(Calendar.YEAR))
                set(Calendar.DAY_OF_MONTH, firstWeekCalendar.get(Calendar.DAY_OF_MONTH))
                set(Calendar.MONTH, firstWeekCalendar.get(Calendar.MONTH))
                add(Calendar.DAY_OF_YEAR, (get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY) * -1)
            }
            if (begin2 > now) {
                begin2.apply {
                    set(Calendar.YEAR, get(Calendar.YEAR) - 1)
                }
            }
            val betweenDays: Long =
                (now.timeInMillis - begin2.timeInMillis) / (1000 * 3600 * 24)
            return (betweenDays / 7).toInt() + 1
        }
    }
}