package com.erha.calander.popup

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.*
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.recyclical.datasource.emptyDataSourceTyped
import com.afollestad.recyclical.setup
import com.afollestad.recyclical.withItem
import com.erha.calander.R
import com.erha.calander.dao.CourseDao
import com.erha.calander.model.RecyclerViewItem
import com.erha.calander.type.LocalStorageKey
import com.erha.calander.util.TinyDB
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.CenterPopupView
import com.mikepenz.iconics.Iconics
import com.mikepenz.iconics.typeface.library.fontawesome.FontAwesome
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import com.qmuiteam.qmui.layout.QMUILinearLayout
import com.qmuiteam.qmui.util.QMUIDisplayHelper
import es.dmoral.toasty.Toasty


class SelectCourseNotifyTimesPopup(@NonNull context: Context?) : CenterPopupView(context!!),
    AddNotifyTimeCallBack {

    data class NotifyTimeItem(
        var minutes: Int,
        var select: Boolean
    ) : RecyclerViewItem() {
        class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)
        }
    }

    // 返回自定义弹窗的布局
    override fun getImplLayoutId(): Int {
        return R.layout.popup_select_notify_times
    }

    private lateinit var store: TinyDB

    var notifyTimeItems = ArrayList<NotifyTimeItem>()

    init {
        notifyTimeItems.apply {
            //默认的时间节点
            add(
                NotifyTimeItem(
                    minutes = 0,
                    select = false
                )
            )
            add(
                NotifyTimeItem(
                    minutes = 20,
                    select = false
                )
            )
        }
    }

    private val dataSource = emptyDataSourceTyped<RecyclerViewItem>()

    var recentNotifyTimeItems = ArrayList<NotifyTimeItem>()
    private val recentDataSource = emptyDataSourceTyped<RecyclerViewItem>()

    lateinit var recyclerView: RecyclerView
    lateinit var recentRecyclerView: RecyclerView

    // 执行初始化操作，比如：findView，设置点击，或者任何你弹窗内的业务逻辑
    override fun onCreate() {
        Iconics.registerFont(GoogleMaterial)
        Iconics.registerFont(FontAwesome)
        super.onCreate()
        store = TinyDB(context)

        recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recentRecyclerView = findViewById<RecyclerView>(R.id.recentRecyclerView)
        //取消按钮事件
        findViewById<View>(R.id.buttomCancle).setOnClickListener {
            dismiss() // 关闭弹窗
        }
        //完成按钮事件
        findViewById<View>(R.id.buttomFinish).setOnClickListener {
            var finalList = ArrayList<NotifyTimeItem>(
                ArrayList<NotifyTimeItem>(recentNotifyTimeItems) + ArrayList<NotifyTimeItem>(
                    notifyTimeItems
                )
            )
            finalList.sortBy { i -> i.minutes }
            //再次去重，避免遗落
            finalList.distinctBy { i -> i.minutes }
            var s = ""
            var isFirst = true
            for (i in finalList) {
                if (i.select) {
                    if (isFirst) {
                        isFirst = false
                        s = "${i.minutes}"
                    } else {
                        s = "${s},${i.minutes}"
                    }
                }
            }
            store.putString(LocalStorageKey.COURSE_NOTIFY_TIME, s)
            Toasty.success(context, "更新通知成功", Toast.LENGTH_SHORT).show()
            Log.e("save ${LocalStorageKey.COURSE_NOTIFY_TIME}", s)
            Thread {
                CourseDao.reload(
                    store.getString(LocalStorageKey.COURSE_FIRST_DAY),
                    store.getString(LocalStorageKey.COURSE_NOTIFY_TIME)
                )
            }.start()
            dismiss() // 关闭弹窗
        }
        //初始化圆角和阴影
        (findViewById<View>(R.id.QMUILinearLayout) as QMUILinearLayout).apply {
            setRadiusAndShadow(
                resources.getDimensionPixelSize(R.dimen.popup_radius),
                QMUIDisplayHelper.dp2px(context, 5),
                0.3F
            )
        }


        //加载用户设置的选项，注意去重！
        store.getString(LocalStorageKey.COURSE_NOTIFY_TIME)?.apply {
            if (this.isNotBlank()) {
                for (i in this.split(",")) {
                    try {
                        var t = i.toInt()
                        var find = false
                        for (n in notifyTimeItems) {
                            if (n.minutes == t) {
                                find = true
                                n.select = true
                                break
                            }
                        }
                        if (!find) {
                            notifyTimeItems.add(NotifyTimeItem(minutes = t, select = true))
                        }
                    } catch (e: NumberFormatException) {
                    }
                }
            }
        }
        //对所有要显示的进行排序
        notifyTimeItems.sortBy { item -> item.minutes }
        Log.e("notifyTimeItems", notifyTimeItems.size.toString())

        //拿到最近自定义的时间
        store.getInt(LocalStorageKey.ADD_TIME_HISTORY_1).apply {
            if (this != -1) {
                recentNotifyTimeItems.add(NotifyTimeItem(minutes = this, select = false))
            }
        }
        store.getInt(LocalStorageKey.ADD_TIME_HISTORY_2).apply {
            if (this != -1) {
                recentNotifyTimeItems.add(NotifyTimeItem(minutes = this, select = false))
            }
        }
        //两个列表去重
        for (t in ArrayList<NotifyTimeItem>(recentNotifyTimeItems)) {
            for (i in notifyTimeItems) {
                if (i.minutes == t.minutes) {
                    recentNotifyTimeItems.remove(t)
                    break
                }
            }
        }
        //也要排序
        recentNotifyTimeItems.sortBy { item -> item.minutes }
        recentDataSource.addAll(recentNotifyTimeItems)

        //初始化列表
        recyclerView.setup {
            withDataSource(dataSource)
            withItem<NotifyTimeItem, NotifyTimeItem.Holder>(R.layout.item_list_notify_time) {
                onBind(NotifyTimeItem::Holder) { index, item ->
                    this.checkBox.apply {
                        text = getTimeText(item.minutes)
                        this.isChecked = item.select
                        setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
                            item.select = isChecked
                            updateTip()
                            updateCancelAllCheckBox()
                        })
                        setOnLongClickListener {
                            true
                        }
                    }
                }
                onClick { index ->
                }
                onLongClick { index ->
                    // item is a `val` in `this` here

                }
            }
        }
        recentRecyclerView.setup {
            withDataSource(recentDataSource)
            withItem<NotifyTimeItem, NotifyTimeItem.Holder>(R.layout.item_list_notify_time) {
                onBind(NotifyTimeItem::Holder) { index, item ->
                    this.checkBox.apply {
                        text = getTimeText(item.minutes)
                        this.isChecked = item.select
                        setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
                            item.select = isChecked
                            updateTip()
                            updateCancelAllCheckBox()
                        })
                    }
                }
                onClick { index ->
                }
                onLongClick { index ->
                    // item is a `val` in `this` here
                }
            }
        }

        var addMoreClickZone = findViewById<LinearLayout>(R.id.addTimeClickZone) as LinearLayout
        addMoreClickZone.setOnClickListener {
            AddNotifyTimePopup(context).apply {
                addNotifyTimeCallBack = this@SelectCourseNotifyTimesPopup
                XPopup.Builder(context)
                    .dismissOnBackPressed(false)
                    .dismissOnTouchOutside(false)
                    .asCustom(this)
                    .show()
            }

        }

        updateTip()
        updateCancelAllCheckBox()
        var cancelAllCheckBox = findViewById<CheckBox>(R.id.cancleAllCheckBox)
        cancelAllCheckBox.setOnClickListener {
            if (cancelAllCheckBox.isChecked) {
                cancelAll()
                cancelAllCheckBox.isClickable = false
                cancelAllCheckBox.isEnabled = false
            }
        }

    }

    private fun addOneTime(minutes: Int) {
        Log.e("addTime", minutes.toString())
        var n = NotifyTimeItem(minutes = minutes, select = true)
        var findA = false
        var indexA = 0
        for (i in notifyTimeItems) {
            if (i.minutes == n.minutes) {
                findA = true
                break
            }
            indexA++
        }

        var findB = false
        var indexB = 0
        for (i in recentNotifyTimeItems) {
            if (i.minutes == n.minutes) {
                findB = true
                break
            }
            indexB++
        }

        Log.e("addTime", "findA = ${findA}, findB = ${findB}")
        Log.e("addTime", "indexA = ${indexA}, size = ${dataSource.size()}")


        try {
            if (findA) {
                notifyTimeItems[indexA].select = true
                dataSource.removeAt(indexA)
                dataSource.apply {
                    var tempIndex = indexA
                    var i = notifyTimeItems[tempIndex]
                    if (tempIndex == this.size()) {
                        add(i)
                    } else {
                        insert(tempIndex, i)
                    }
                }
            } else if (findB) {
                recentNotifyTimeItems[indexB].select = true
                recentDataSource.removeAt(indexB)
                recentDataSource.apply {
                    var tempIndex = indexB
                    var i = recentNotifyTimeItems[tempIndex]
                    if (tempIndex == this.size()) {
                        add(i)
                    } else {
                        insert(tempIndex, i)
                    }
                }
            } else {
                //之前没有过，那怎么办？
                notifyTimeItems.add(n)
                notifyTimeItems.sortBy { item -> item.minutes }
                dataSource.apply {
                    var tempIndex = notifyTimeItems.indexOf(n)
                    var i = n
                    if (tempIndex == this.size()) {
                        add(i)
                    } else {
                        insert(tempIndex, i)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun cancelAll() {
        for (i in notifyTimeItems) {
            if (i.select) {
                i.select = false
            }
        }
        for (i in recentNotifyTimeItems) {
            if (i.select) {
                i.select = false
            }
        }
        dataSource.clear()
        dataSource.addAll(notifyTimeItems)
        recentDataSource.clear()
        recentDataSource.addAll(recentNotifyTimeItems)
        updateTip()
    }

    private fun updateTip() {
        var textView = findViewById<TextView>(R.id.tipCanScrollTextView)
        var count = 0
        for (i in notifyTimeItems) {
            if (i.select) count++
        }
        for (i in recentNotifyTimeItems) {
            if (i.select) count++
        }
        var s = ""
        if (count == 0) {
            s = "没有选择任何提醒"
        } else {
            s = "已选${count}个时间点"
        }
        if (notifyTimeItems.size > 5) {
            if (s.isNotBlank()) {
                s += ";"
            }
            s += "下方列表可滚动"
        }
        textView.text = s
    }

    private fun updateCancelAllCheckBox() {
        var count = 0
        for (i in notifyTimeItems) {
            if (i.select) count++
        }
        for (i in recentNotifyTimeItems) {
            if (i.select) count++
        }
        var checkBox = findViewById<CheckBox>(R.id.cancleAllCheckBox)
        checkBox.isChecked = count == 0
        if (!checkBox.isChecked) {
            checkBox.isClickable = true
            checkBox.isEnabled = true
        }
    }


    private fun getTimeText(minutesTotal: Int): String {
        if (minutesTotal == 0) {
            return "准时提醒"
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

    override fun addNotifyTimeFinished(minutes: Int) {
        addOneTime(minutes = minutes)
        updateTip()
        updateCancelAllCheckBox()
    }


}