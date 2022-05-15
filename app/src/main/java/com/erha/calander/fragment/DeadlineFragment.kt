package com.erha.calander.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.erha.calander.R
import com.erha.calander.activity.ModifySimpleTaskActivity
import com.erha.calander.dao.TaskDao
import com.erha.calander.databinding.FragmentDeadlineBinding
import com.erha.calander.model.TaskStatus
import com.erha.calander.timeline.SimpleTimeLineAdapter
import com.erha.calander.timeline.SingleTimeLineCallback
import com.erha.calander.timeline.model.OrderStatus
import com.erha.calander.timeline.model.TimeLineModel
import com.erha.calander.type.LocalStorageKey
import com.erha.calander.util.CalendarUtil
import com.erha.calander.util.TinyDB
import java.text.SimpleDateFormat
import java.util.*


class DeadlineFragment : Fragment(R.layout.fragment_deadline), SingleTimeLineCallback {
    private lateinit var binding: FragmentDeadlineBinding

    private lateinit var mAdapter: SimpleTimeLineAdapter
    private val mDataList = ArrayList<TimeLineModel>()
    private lateinit var mLayoutManager: LinearLayoutManager
    private lateinit var store: TinyDB
    private var simpleDateFormat = SimpleDateFormat("yy/MM/dd EEE HH:mm", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        saveInstanceState: Bundle?
    ): View {
        binding = FragmentDeadlineBinding.inflate(inflater, container, false)
        store = TinyDB(binding.root.context)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mLayoutManager = LinearLayoutManager(binding.root.context, RecyclerView.VERTICAL, false)
        binding.recyclerView.layoutManager = mLayoutManager
    }

    override fun onResume() {
        super.onResume()
        Log.e(this.javaClass.name, "onResume")
        reloadDataListItems()
        mAdapter = SimpleTimeLineAdapter(mDataList)
        binding.recyclerView.adapter = mAdapter
    }

    private fun reloadDataListItems() {
        var hasInsertNow = false
        mDataList.clear()
        val weekDate = store.getString(LocalStorageKey.FIRST_WEEK).toString()
        var firstWeekCalendar: Calendar? = null
        if (weekDate.isNotEmpty()) {
            firstWeekCalendar = CalendarUtil.getWithoutTime().apply {
                set(Calendar.MONTH, weekDate.split(",")[0].toInt() - 1)
                set(Calendar.DAY_OF_MONTH, weekDate.split(",")[1].toInt())
            }
        }
        for (i in TaskDao.getAllDeadlines()) {
            val beginCalendar = CalendarUtil.getWithoutTime(i.date).apply {
                set(Calendar.HOUR_OF_DAY, i.beginTime.get(Calendar.HOUR_OF_DAY))
                set(Calendar.MINUTE, i.beginTime.get(Calendar.MINUTE))
            }
            var simpleTaskId = i.id
            val isPassed = CalendarUtil.getWithoutSecond() > beginCalendar

            val weekNumber = if (firstWeekCalendar != null) {
                CalendarUtil.getWeekNumber(beginCalendar, firstWeekCalendar = firstWeekCalendar)
            } else {
                CalendarUtil.getWeekNumber(beginCalendar)
            }

            if (isPassed && i.status == TaskStatus.ONGOING) {
                //任务过去了，但是还正在进行中
                mDataList.add(
                    TimeLineModel(
                        i.title.ifBlank { "无标题的DDL" },
                        "第${weekNumber}周 ${simpleDateFormat.format(beginCalendar.timeInMillis)}",
                        OrderStatus.INACTIVE,
                        simpleTaskId,
                        this
                    )
                )
                continue
            }
            if (isPassed && i.status != TaskStatus.ONGOING) {
                continue
            }
            if (beginCalendar > Calendar.getInstance() && !hasInsertNow) {
                hasInsertNow = true
                mDataList.add(
                    TimeLineModel(
                        "现在",
                        "${simpleDateFormat.format(CalendarUtil.getWithoutSecond().timeInMillis)}",
                        OrderStatus.INACTIVE
                    )
                )
            }
            if (!isPassed) {
                mDataList.add(
                    TimeLineModel(
                        i.title.ifBlank { "无标题的DDL" },
                        "第${weekNumber}周 ${
                            simpleDateFormat.format(beginCalendar.timeInMillis)
                        }",
                        if (i.status == TaskStatus.ONGOING) {
                            OrderStatus.ACTIVE
                        } else {
                            OrderStatus.COMPLETED
                        },
                        simpleTaskId,
                        this
                    )
                )
            }
        }
        if (mDataList.isEmpty()) {
            mDataList.add(
                TimeLineModel(
                    "现在",
                    "${simpleDateFormat.format(CalendarUtil.getWithoutSecond().timeInMillis)}",
                    OrderStatus.INACTIVE
                )
            )
            hasInsertNow = true
            mDataList.add(TimeLineModel("你没有DEADLINE呀~", "-", OrderStatus.COMPLETED))
        } else if (!hasInsertNow) {
            mDataList.add(
                TimeLineModel(
                    "现在",
                    "${simpleDateFormat.format(CalendarUtil.getWithoutSecond().timeInMillis)}",
                    OrderStatus.INACTIVE
                )
            )
            hasInsertNow = true
        }
    }

    override fun onclick(id: Int) {
        Log.e(this.javaClass.name, "onclick ${id}")
        val i = Intent(binding.root.context, ModifySimpleTaskActivity::class.java)
        i.putExtra("simpleTaskId", id)
        startActivity(i)
    }
}