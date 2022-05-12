package com.erha.calander.model

import android.graphics.Color
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.text.style.TypefaceSpan
import com.alamkanak.weekview.WeekViewEntity
import com.erha.calander.R
import java.util.*

sealed class CalendarEntity {

    data class Event(
        val id: Long,
        val title: CharSequence,
        val startTime: Calendar,
        val endTime: Calendar,
        val location: CharSequence,
        val color: Int,
        val isAllDay: Boolean,
        val isCanceled: Boolean
    ) : CalendarEntity() {
        override fun equals(other: Any?): Boolean {
            if (other is Event) {
                return other.id == this.id
            }
            return super.equals(other)
        }
    }

    data class BlockedTimeSlot(
        val id: Long,
        val startTime: Calendar,
        val endTime: Calendar
    ) : CalendarEntity() {
        override fun equals(other: Any?): Boolean {
            if (other is BlockedTimeSlot) {
                return other.id == this.id
            }
            return super.equals(other)
        }
    }
}

fun CalendarEntity.toWeekViewEntity(): WeekViewEntity {
    return when (this) {
        is CalendarEntity.Event -> toWeekViewEntity()
        is CalendarEntity.BlockedTimeSlot -> toWeekViewEntity()
    }
}

fun CalendarEntity.Event.toWeekViewEntity(): WeekViewEntity {
    val backgroundColor = if (!isCanceled) color else Color.WHITE
    val textColor = if (!isCanceled) Color.WHITE else color
    val borderWidthResId = if (!isCanceled) R.dimen.no_border_width else R.dimen.border_width

    val style = WeekViewEntity.Style.Builder()
        .setTextColor(textColor)
        .setBackgroundColor(backgroundColor)
        .setBorderWidthResource(borderWidthResId)
        .setBorderColor(color)
        .build()

    val title = SpannableStringBuilder(title).apply {
        val titleSpan = TypefaceSpan("sans-serif-medium")
        setSpan(titleSpan, 0, title.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        if (isCanceled) {
            setSpan(StrikethroughSpan(), 0, title.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    val subtitle = SpannableStringBuilder(location).apply {
        if (isCanceled) {
            setSpan(StrikethroughSpan(), 0, location.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    return WeekViewEntity.Event.Builder(this)
        .setId(id)
        .setTitle(title)
        .setStartTime(startTime)
        .setEndTime(endTime)
        .setSubtitle(subtitle)
        .setAllDay(isAllDay)
        .setStyle(style)
        .build()
}

fun CalendarEntity.BlockedTimeSlot.toWeekViewEntity(): WeekViewEntity {
    val style = WeekViewEntity.Style.Builder()
        .setBackgroundColorResource(R.color.gray_alpha10)
        .setCornerRadius(0)
        .build()

    return WeekViewEntity.BlockedTime.Builder()
        .setId(id)
        .setStartTime(startTime)
        .setEndTime(endTime)
        .setStyle(style)
        .build()
}
