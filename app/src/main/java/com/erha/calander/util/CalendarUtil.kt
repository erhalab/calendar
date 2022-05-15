package com.erha.calander.util

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

object CalendarUtil {

    //返回更加清晰的时间格式
    fun getClearTimeText(time: Calendar): String {
        val t = getWithoutSecond(time)
        var timeString = when (t.get(Calendar.HOUR_OF_DAY)) {
            in 0..3, 23 -> "深夜 "
            in 4..6 -> "清晨 "
            in 7..10 -> "上午 "
            in 11..13 -> "中午 "
            in 14..18 -> "下午 "
            in 19..22 -> "晚上 "
            else -> "ERROR "
        }
        timeString += SimpleDateFormat(
            "H:mm",
            Locale.getDefault()
        ).format(t.timeInMillis)
        return timeString
    }

    //返回更加清晰的日期时间格式（起始日期必须是同一天）
    fun getClearDateTimeText(begin: Calendar, end: Calendar): String {
        val now = getWithoutSecond()
        val beginCalendar = getWithoutSecond(begin)
        val endCalendar = getWithoutSecond(end)

        var dateString: String
        if (now.get(Calendar.YEAR) == beginCalendar.get(Calendar.YEAR)) {
            //是同一年的
            val dayDiffs = now.get(Calendar.DAY_OF_YEAR) - beginCalendar.get(Calendar.DAY_OF_YEAR)
            Log.e("Calendar Util", "daydiffs = ${dayDiffs}")
            dateString = when (dayDiffs) {
                0 -> "今天,"
                1 -> "昨天,"
                2 -> "前天,"
                -1 -> "明天,"
                -2 -> "后天,"
                else -> SimpleDateFormat(
                    "M月d日 ",
                    Locale.getDefault()
                ).format(beginCalendar.timeInMillis)
            }
        } else {
            //不是同一年的
            dateString =
                SimpleDateFormat("yy年M月d日 ", Locale.getDefault()).format(beginCalendar.timeInMillis)
        }
        val timeString = when {
            compareOnlyTime(beginCalendar, endCalendar) == 0 -> {
                //只有一个时间呢
                getClearTimeText(beginCalendar)
            }
            (endCalendar.timeInMillis - beginCalendar.timeInMillis) == (1000 * 60 * 60 * 24 - 1000 * 60).toLong() -> {
                //相差1天少1分钟
                "全天"
            }
            else -> {
                SimpleDateFormat(
                    "H:mm",
                    Locale.getDefault()
                ).format(beginCalendar.timeInMillis) + "-" + SimpleDateFormat(
                    "H:mm",
                    Locale.getDefault()
                ).format(endCalendar.timeInMillis)
            }
        }
        return dateString + "" + timeString
    }

    //返回时间差的容易辨识的文本
    fun getTimeDiffClearText(
        calendar: Calendar,
        calendar1: Calendar = Calendar.getInstance()
    ): String {
        val millsDiff = calendar.timeInMillis - calendar1.timeInMillis
        if (millsDiff < 0) {
            //时间已过去
            return "已过期"
        } else {
            var i: Long = millsDiff / (1000 * 60 * 60 * 24)
            if (i > 0) {
                if (i > 364) {
                    return "1年以后"
                }
                return "${i}天"
            } else {
                i = millsDiff / (1000 * 60 * 60)
                if (i > 0) {
                    return "${i}小时"
                } else {
                    i = millsDiff / (1000 * 60)
                    if (i > 0) {
                        return "${i}分钟"
                    } else {
                        return "现在"
                    }
                }
            }
        }
    }

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
        val a = Calendar.getInstance()
        val b = calendar1.clone() as Calendar
        val c = calendar2.clone() as Calendar
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