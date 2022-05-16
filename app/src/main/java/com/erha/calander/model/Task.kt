package com.erha.calander.model

import com.erha.calander.util.CalendarUtil
import java.util.*


//i.putExtra("hasTime", taskTimeAndNotify.hasTime)
//i.putExtra("date", taskTimeAndNotify.date)
//i.putExtra("isAllDay", taskTimeAndNotify.isAllDay)
//i.putExtra("beginTime", taskTimeAndNotify.beginTime)
//i.putExtra("startTime", taskTimeAndNotify.endTime)
//i.putExtra("isDDL", taskTimeAndNotify.isDDL)
//i.putExtra("notifyTimes", taskTimeAndNotify.notifyTimes)
object TaskStatus {
    const val ONGOING = 0
    const val FINISHED = 1
    const val CANCELED = 2
}

data class SimpleTaskWithoutID(
    var status: Int,
    var title: String,
    var detailHtml: String,
    var hasTime: Boolean,
    var date: Calendar,
    var isAllDay: Boolean,
    var beginTime: Calendar,
    var endTime: Calendar,
    var isDDL: Boolean,
    var notifyTimes: ArrayList<Int>,
    var customColor: Boolean = false,
    var color: String = "#ec6666"
)

data class SimpleTaskWithID(
    var id: Int,
    var title: String,
    var detailHtml: String,
    var status: Int,
    var hasTime: Boolean,
    var date: Calendar,
    var isAllDay: Boolean,
    var beginTime: Calendar,
    var endTime: Calendar,
    var isDDL: Boolean,
    var notifyTimes: ArrayList<Int>,
    var customColor: Boolean,
    var color: String
) {
    companion object {
        fun copy(origin: SimpleTaskWithID): SimpleTaskWithID {
            return SimpleTaskWithID(
                id = origin.id,
                status = origin.status,
                title = origin.title,
                detailHtml = origin.detailHtml,
                hasTime = origin.hasTime,
                date = CalendarUtil.getWithoutTime(origin.date),
                isAllDay = origin.isAllDay,
                beginTime = CalendarUtil.alignDateAndTime(origin.date, origin.beginTime),
                endTime = CalendarUtil.alignDateAndTime(origin.date, origin.endTime),
                isDDL = origin.isDDL,
                notifyTimes = ArrayList(origin.notifyTimes),
                customColor = origin.customColor,
                color = origin.color
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other is SimpleTaskWithID) {
            return other.id == this.id
        }
        return super.equals(other)
    }

    fun toSimpleTaskWithoutID(): SimpleTaskWithoutID {
        return SimpleTaskWithoutID(
            status = status,
            title = title,
            detailHtml = detailHtml,
            hasTime = hasTime,
            date = date,
            isAllDay = isAllDay,
            beginTime = beginTime,
            endTime = endTime,
            isDDL = isDDL,
            notifyTimes = notifyTimes,
            customColor = customColor,
            color = color
        )
    }
}
