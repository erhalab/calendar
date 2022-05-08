package com.alamkanak.weekview

import android.os.Parcel
import android.os.Parcelable
import android.view.View.BaseSavedState
import java.util.*

internal class SavedState : BaseSavedState {

    var numberOfVisibleDays: Int = 0
    var firstVisibleDate: Calendar = today()
    var customFirstWeekEnable = false
    var firstWeekCalendar: Calendar = Calendar.getInstance()

    constructor(superState: Parcelable) : super(superState)

    constructor(
        superState: Parcelable,
        numberOfVisibleDays: Int,
        firstVisibleDate: Calendar,
        customFirstWeekEnable: Boolean,
        firstWeekCalendar: Calendar
    ) : super(superState) {
        this.numberOfVisibleDays = numberOfVisibleDays
        this.firstVisibleDate = firstVisibleDate
        this.customFirstWeekEnable = customFirstWeekEnable
        this.firstWeekCalendar = firstWeekCalendar
    }

    constructor(source: Parcel) : super(source) {
        numberOfVisibleDays = source.readInt()
        firstVisibleDate = source.readSerializable() as Calendar
        customFirstWeekEnable = source.readSerializable() as Boolean
        firstWeekCalendar = source.readSerializable() as Calendar
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
        super.writeToParcel(out, flags)
        out.writeInt(numberOfVisibleDays)
        out.writeSerializable(firstVisibleDate)
        out.writeSerializable(customFirstWeekEnable)
        out.writeSerializable(firstWeekCalendar)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
            override fun createFromParcel(source: Parcel) = SavedState(source)
            override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
        }
    }
}
