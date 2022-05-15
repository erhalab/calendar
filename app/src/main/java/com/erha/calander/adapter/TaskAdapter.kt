package com.erha.calander.adapter

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.erha.calander.R
import com.erha.calander.activity.ModifySimpleTaskActivity
import com.erha.calander.dao.TaskDao
import com.erha.calander.model.SimpleTaskWithID
import com.erha.calander.model.TaskStatus
import com.erha.calander.timeline.utils.VectorDrawableUtils
import com.erha.calander.type.EventType
import com.erha.calander.util.CalendarUtil
import com.mikepenz.iconics.view.IconicsImageView
import com.qmuiteam.qmui.recyclerView.QMUISwipeAction
import com.qmuiteam.qmui.recyclerView.QMUISwipeViewHolder
import com.qmuiteam.qmui.util.QMUIDisplayHelper
import org.greenrobot.eventbus.EventBus
import java.util.*

class TaskAdapter(val context: Context) :
    RecyclerView.Adapter<QMUISwipeViewHolder>() {
    private val mData: MutableList<SimpleTaskWithID> = ArrayList()
    val mFinishAction: QMUISwipeAction
    val mDeleteAction: QMUISwipeAction
    val mCalendarAction: QMUISwipeAction
    fun setData(list: List<SimpleTaskWithID>?) {
        mData.clear()
        list?.apply {
            mData.addAll(this)
        }
        notifyDataSetChanged()
    }

    fun getData(): Array<SimpleTaskWithID> {
        return this.mData.toTypedArray()
    }

    fun remove(pos: Int) {
        mData.removeAt(pos)
        notifyItemRemoved(pos)
    }

    fun add(pos: Int, item: SimpleTaskWithID) {
        mData.add(pos, item)
        notifyItemInserted(pos)
    }

    fun prepend(items: List<SimpleTaskWithID>) {
        mData.addAll(0, items)
        notifyDataSetChanged()
    }

    fun append(items: List<SimpleTaskWithID>) {
        mData.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QMUISwipeViewHolder {
        val view: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_list_task, parent, false)
        val vh = QMUISwipeViewHolder(view)
        vh.addSwipeAction(mDeleteAction)
        vh.addSwipeAction(mCalendarAction)
        vh.addSwipeAction(mFinishAction)
        view.setOnClickListener {

        }
        return vh
    }

    override fun onBindViewHolder(holder: QMUISwipeViewHolder, position: Int) {
        holder.itemView.apply {
            val task = mData[position]
            //点击这一项打开详情页
            findViewById<ConstraintLayout>(R.id.constraintLayout).apply {
                setOnClickListener {
                    Log.e(this.javaClass.name, "onclick ${task.id}")
                    val i = Intent(context, ModifySimpleTaskActivity::class.java)
                    i.putExtra("simpleTaskId", task.id)
                    context.startActivity(i)
                }
            }
            val checkBox =
                findViewById<com.buildware.widget.indeterm.IndeterminateCheckBox>(R.id.checkBox).apply {
                    if (task.status == TaskStatus.CANCELED) {
                        isIndeterminate = true
                    } else {
                        isChecked = when (task.status) {
                            TaskStatus.FINISHED -> true
                            else -> false
                        }
                    }
                    setOnClickListener {
                        task.status = when (state) {
                            null -> TaskStatus.CANCELED
                            true -> TaskStatus.FINISHED
                            false -> TaskStatus.ONGOING
                        }
                        pushTask(task)
                    }
                    setOnLongClickListener {
                        if (isIndeterminate) {
                            isChecked = false
                            task.status = TaskStatus.ONGOING
                            pushTask(task)
                        } else {
                            isIndeterminate = true
                            task.status = TaskStatus.CANCELED
                            pushTask(task)
                        }
                        true
                    }
                }
            val title = findViewById<TextView>(R.id.taskTitle).apply {
                val maxLength = 15
                text = if (task.title.length > maxLength) {
                    "${task.title.substring(0, maxLength)}..."
                } else if (task.title.isBlank()) "无标题" else task.title
                if (task.title.isBlank()) {
                    setTextColor(resources.getColor(R.color.gray_500))
                }
            }
            val time = findViewById<TextView>(R.id.taskTime).apply {
                text = if (task.hashTime) {
                    val beginCalendar = CalendarUtil.getWithoutSecond(task.beginTime).apply {
                        set(Calendar.YEAR, task.date.get(Calendar.YEAR))
                        set(Calendar.MONTH, task.date.get(Calendar.MONTH))
                        set(Calendar.DAY_OF_MONTH, task.date.get(Calendar.DAY_OF_MONTH))
                    }
                    val endCalendar = CalendarUtil.getWithoutSecond(task.endTime).apply {
                        set(Calendar.YEAR, task.date.get(Calendar.YEAR))
                        set(Calendar.MONTH, task.date.get(Calendar.MONTH))
                        set(Calendar.DAY_OF_MONTH, task.date.get(Calendar.DAY_OF_MONTH))
                    }
                    CalendarUtil.getClearTimeText(beginCalendar, endCalendar)
                } else {
                    "无时间"
                }
                if (!task.hashTime) {
                    setTextColor(resources.getColor(R.color.gray_300))
                }
            }
            val taskNotifyIcon = findViewById<IconicsImageView>(R.id.taskNotifyIcon).apply {
                val taskNotifyNumber =
                    holder.itemView.findViewById<TextView>(R.id.taskNotifyNumber)

                val beginCalendar = CalendarUtil.getWithoutSecond(task.beginTime).apply {
                    set(Calendar.YEAR, task.date.get(Calendar.YEAR))
                    set(Calendar.MONTH, task.date.get(Calendar.MONTH))
                    set(Calendar.DAY_OF_MONTH, task.date.get(Calendar.DAY_OF_MONTH))
                }
                var notifyNumber = 0
                if (task.hashTime) {
                    for (i in task.notifyTimes) {
                        val notifyCalendar = (beginCalendar.clone() as Calendar).apply {
                            add(Calendar.MINUTE, -1 * i)
                        }
                        //现在的时间要晚于通知的时间了，也就是没办法再提醒了
                        if (Calendar.getInstance() <= notifyCalendar) {
                            notifyNumber++
                        }
                    }
                }
                if (notifyNumber == 0) {
                    this.visibility = View.GONE
                    taskNotifyNumber.visibility = View.GONE
                } else {
                    this.visibility = View.VISIBLE
                    taskNotifyNumber.text = notifyNumber.toString()
                }
            }

            val taskDDLIcon = findViewById<IconicsImageView>(R.id.taskDDLIcon).apply {
                val taskDDLLeftTime =
                    holder.itemView.findViewById<TextView>(R.id.taskDDLLeftTime)
                if (!task.isDDL || !task.hashTime) {
                    this.visibility = View.GONE
                    taskDDLLeftTime.visibility = View.GONE
                } else {
                    this.visibility = View.VISIBLE
                    taskDDLLeftTime.visibility = View.VISIBLE

                    val beginCalendar = CalendarUtil.getWithoutSecond(task.beginTime).apply {
                        set(Calendar.YEAR, task.date.get(Calendar.YEAR))
                        set(Calendar.MONTH, task.date.get(Calendar.MONTH))
                        set(Calendar.DAY_OF_MONTH, task.date.get(Calendar.DAY_OF_MONTH))
                    }
                    taskDDLLeftTime.text = CalendarUtil.getTimeDiffClearText(beginCalendar)
                }
            }

        }

    }

    private fun pushTask(task: SimpleTaskWithID) {
        Thread {
            TaskDao.updateSimpleTask(task)
            EventBus.getDefault().post(EventType.EVENT_CHANGE)
        }.start()
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    init {
        val builder = QMUISwipeAction.ActionBuilder()
            .textSize(QMUIDisplayHelper.sp2px(context, 14))
            .textColor(Color.WHITE)
            .paddingStartEnd(QMUIDisplayHelper.dp2px(context, 14))
        mFinishAction = builder
            .backgroundColor(context.resources.getColor(R.color.success))
            .icon(
                VectorDrawableUtils.getDrawable(
                    context,
                    R.drawable.ic_round_check_24,
                    Color.WHITE
                )
            )
            .orientation(QMUISwipeAction.ActionBuilder.VERTICAL)
            .build()
        mDeleteAction = builder
            .backgroundColor(context.resources.getColor(R.color.danger))
            .icon(
                VectorDrawableUtils.getDrawable(
                    context,
                    R.drawable.ic_outline_delete_24,
                    Color.WHITE
                )
            )
            .orientation(QMUISwipeAction.ActionBuilder.VERTICAL)
            .build()
        mCalendarAction = builder
            .backgroundColor(context.resources.getColor(R.color.warning))
            .icon(
                VectorDrawableUtils.getDrawable(
                    context,
                    R.drawable.ic_round_calendar_month_24,
                    Color.WHITE
                )
            )
            .orientation(QMUISwipeAction.ActionBuilder.VERTICAL)
            .build()
    }
}