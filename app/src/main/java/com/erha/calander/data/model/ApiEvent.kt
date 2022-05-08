package com.erha.calander.data.model

import android.graphics.Color
import com.google.gson.annotations.SerializedName
import java.time.DateTimeException
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

interface ApiResult {
    fun toCalendarEntity(yearMonth: YearMonth, index: Int): CalendarEntity?
}

data class ApiEvent(
    @SerializedName("title") val title: String,
    @SerializedName("location") val location: String,
    var year: Int = -1,
    val month: Int = -1,
    @SerializedName("day_of_month") val dayOfMonth: Int,
    @SerializedName("start_time") val startTime: String,
    @SerializedName("duration") val duration: Int,
    @SerializedName("color") val color: String,
    @SerializedName("is_canceled") val isCanceled: Boolean,
    @SerializedName("is_all_day") val isAllDay: Boolean,
) : ApiResult {

    override fun toCalendarEntity(yearMonth: YearMonth, index: Int): CalendarEntity? {
        return try {
            val startTime = LocalTime.parse(startTime)
            var startDateTime = yearMonth.atDay(dayOfMonth).atTime(startTime)
            if (month != -1 && year != -1) {
                startDateTime = LocalDateTime.of(
                    year,
                    month,
                    dayOfMonth,
                    this.startTime.split(":")[0].toInt(),
                    this.startTime.split(":")[1].toInt(),
                    0
                )
            }
            if (yearMonth.year != startDateTime.year || yearMonth.month != startDateTime.month) {
                return null
            }
            //Log.e("time test for :","startDateTime = {$startDateTime}")
            val endDateTime = startDateTime.plusMinutes(duration.toLong())
            CalendarEntity.Event(
                id = "${startDateTime.year}${startDateTime.monthValue}${startDateTime.dayOfMonth}${startDateTime.hour}${startDateTime.minute}${index}".toLong(),
                title = title,
                location = location,
                startTime = startDateTime,
                endTime = endDateTime,
                color = Color.parseColor(color),
                isAllDay = isAllDay,
                isCanceled = isCanceled
            )
        } catch (e: DateTimeException) {
            null
        }
    }
}

data class ApiBlockedTime(
    @SerializedName("day_of_month") val dayOfMonth: Int,
    @SerializedName("start_time") val startTime: String,
    @SerializedName("duration") val duration: Int
) : ApiResult {

    override fun toCalendarEntity(yearMonth: YearMonth, index: Int): CalendarEntity? {
        return try {
            val startTime = LocalTime.parse(startTime)
            val startDateTime = yearMonth.atDay(dayOfMonth).atTime(startTime)
            val endDateTime = startDateTime.plusMinutes(duration.toLong())
            CalendarEntity.BlockedTimeSlot(
                id = "200${yearMonth.year}00${yearMonth.monthValue}00$index".toLong(),
                startTime = startDateTime,
                endTime = endDateTime
            )
        } catch (e: DateTimeException) {
            null
        }
    }
}
