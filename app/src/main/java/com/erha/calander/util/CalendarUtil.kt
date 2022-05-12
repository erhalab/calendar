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
}