package com.erha.calander.popup

import android.content.Context
import android.view.View
import androidx.annotation.NonNull
import cn.carbswang.android.numberpickerview.library.NumberPickerView
import com.erha.calander.R
import com.erha.calander.type.LocalStorageKey
import com.erha.calander.util.TinyDB
import com.lxj.xpopup.core.CenterPopupView
import com.qmuiteam.qmui.layout.QMUILinearLayout
import com.qmuiteam.qmui.util.QMUIDisplayHelper


interface AddNotifyTimeCallBack {
    fun addNotifyTimeFinished(minutes: Int)
}

class AddNotifyTimePopup  //注意：自定义弹窗本质是一个自定义View，但是只需重写一个参数的构造，其他的不要重写，所有的自定义弹窗都是这样。
    (@NonNull context: Context?) : CenterPopupView(context!!) {

    var addNotifyTimeCallBack: AddNotifyTimeCallBack? = null

    // 返回自定义弹窗的布局
    override fun getImplLayoutId(): Int {
        return R.layout.popup_add_notify_time
    }

    // 执行初始化操作，比如：findView，设置点击，或者任何你弹窗内的业务逻辑
    override fun onCreate() {
        super.onCreate()
        //初始化圆角和阴影
        (findViewById<View>(R.id.QMUILinearLayout) as QMUILinearLayout).apply {
            setRadiusAndShadow(
                resources.getDimensionPixelSize(R.dimen.popup_radius),
                QMUIDisplayHelper.dp2px(context, 5),
                0.3F
            )
        }
        //取消按钮事件
        findViewById<View>(R.id.buttomCancle).setOnClickListener {
            dismiss() // 关闭弹窗
        }
        //完成按钮事件
        findViewById<View>(R.id.buttomFinish).setOnClickListener {
            val minutes =
                (findViewById<View>(R.id.daysPicker) as NumberPickerView).value * 60 * 24 + (findViewById<View>(
                    R.id.hoursPicker
                ) as NumberPickerView).value * 60 + (findViewById<View>(R.id.minutesPicker) as NumberPickerView).value
            addNotifyTimeCallBack?.apply {
                this.addNotifyTimeFinished(minutes)
            }
            var store = TinyDB(appContext = context)
            if (store.getInt(LocalStorageKey.ADD_TIME_HISTORY_1) == -1) {
                store.putInt(LocalStorageKey.ADD_TIME_HISTORY_1, minutes)
            } else if (store.getInt(LocalStorageKey.ADD_TIME_HISTORY_2) == -1) {
                store.putInt(LocalStorageKey.ADD_TIME_HISTORY_2, minutes)
            } else {
                store.putInt(
                    LocalStorageKey.ADD_TIME_HISTORY_1,
                    store.getInt(LocalStorageKey.ADD_TIME_HISTORY_2)
                )
                store.putInt(LocalStorageKey.ADD_TIME_HISTORY_2, minutes)
            }
            dismiss() // 关闭弹窗
        }

        val days = arrayOfNulls<String>(61)
        for (i in days.indices) {
            days[i] = "$i"
        }
        (findViewById<View>(R.id.daysPicker) as NumberPickerView).apply {
            refreshByNewDisplayedValues(days)
        }

        val hours = arrayOfNulls<String>(24)
        for (i in hours.indices) {
            hours[i] = "$i"
        }
        (findViewById<View>(R.id.hoursPicker) as NumberPickerView).apply {
            refreshByNewDisplayedValues(hours)
        }

        val minutes = arrayOfNulls<String>(60)
        for (i in minutes.indices) {
            minutes[i] = "$i"
        }
        (findViewById<View>(R.id.minutesPicker) as NumberPickerView).apply {
            refreshByNewDisplayedValues(minutes)
        }

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