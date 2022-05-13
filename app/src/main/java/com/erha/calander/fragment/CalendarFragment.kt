package com.erha.calander.fragment

import android.content.Intent
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.alamkanak.weekview.WeekView
import com.alamkanak.weekview.WeekViewEntity
import com.erha.calander.R
import com.erha.calander.activity.ModifySimpleTaskActivity
import com.erha.calander.dao.CourseDao
import com.erha.calander.dao.TaskDao
import com.erha.calander.databinding.FragmentCalenderBinding
import com.erha.calander.model.CalendarEntity
import com.erha.calander.model.toWeekViewEntity
import com.erha.calander.type.EventType
import com.erha.calander.type.LocalStorageKey
import com.erha.calander.util.CalendarUtil
import com.erha.calander.util.TinyDB
import com.erha.calander.util.setupWithWeekView
import com.google.android.material.appbar.MaterialToolbar
import com.philliphsu.bottomsheetpickers.date.DatePickerDialog
import es.dmoral.toasty.Toasty
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.text.SimpleDateFormat
import java.util.*

class CalendarFragment : Fragment(R.layout.fragment_calender), DatePickerDialog.OnDateSetListener {
    private lateinit var binding: FragmentCalenderBinding

    //    private val viewModel by genericViewModel()
    private lateinit var store: TinyDB
    private var locale = Locale.getDefault()
    private lateinit var weekViewAdapter: WeekViewSimpleAdapter

    init {
        Log.e("onCreate Fragment", this.javaClass.name)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        saveInstanceState: Bundle?
    ): View? {
        binding = FragmentCalenderBinding.inflate(inflater, container, false)
        store = TinyDB(binding.root.context)
        EventBus.getDefault().register(this)
        return binding.root
    }

    var isPostEventChangeMyself = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolbarContainer.toolbar.setupWithWeekView(
            binding.weekView,
            this@CalendarFragment
        )
        weekViewAdapter = WeekViewSimpleAdapter(
            calendarFragment = this,
            locale = this.locale,
            toolbar = binding.toolbarContainer.toolbar,
            store = store
        )
        binding.weekView.adapter = weekViewAdapter
        weekViewAdapter.submitList(CourseDao.getAllCalendarEntities() + TaskDao.getAllCalendarEntities())
        //支持本地语言
        binding.weekView.setDateFormatter { date ->
            defaultDateFormatter(binding.weekView.numberOfVisibleDays).format(date.time)
        }
        binding.weekView.setTimeFormatter { hour ->
            val date = Calendar.getInstance().withTime(hour = hour, minutes = 0)
            when (hour) {
                7 -> SimpleDateFormat("破晓", locale).format(date.time)
                12 -> SimpleDateFormat("正午", locale).format(date.time)
                19 -> SimpleDateFormat("日落", locale).format(date.time)
                else -> defaultTimeFormatter().format(date.time)
            }
        }
        //滚动到上次的位置（记忆）
        store.getString(LocalStorageKey.CALENDAR_LAST_FIRST_DAY)?.apply {
            if (this.isBlank()) return@apply
            var calendar = Calendar.getInstance()
            var s = this.split("/")
            try {
                calendar.apply {
                    set(Calendar.YEAR, s[0].toInt())
                    set(Calendar.MONTH, s[1].toInt() - 1)
                    set(Calendar.DAY_OF_MONTH, s[2].toInt())
                    set(Calendar.HOUR_OF_DAY, 8)
                    set(Calendar.MINUTE, 0)
                    if (binding.weekView.numberOfVisibleDays == 7) {
                        add(
                            Calendar.DAY_OF_MONTH,
                            -1 * (get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY)
                        )
                    }
                }
                Log.e("自动跳转到 ->", this)
                binding.weekView.scrollToDateTime(calendar)
            } catch (e: NumberFormatException) {
                Log.e("滚动到上次的位置（记忆）", "NumberFormatException")
            }


        }
        //初始化标题日期
        binding.toolbarContainer.toolbar.apply {
            title = SimpleDateFormat("MMMM", locale).format(binding.weekView.firstVisibleDate.time)
            subtitle =
                SimpleDateFormat("yyyy", locale).format(binding.weekView.firstVisibleDate.time)
        }
        binding.weekView.apply {
            hourHeight = 200
            minHourHeight = 200
            maxHourHeight = 200
            stickToActualWeek = true
        }
        initFirstWeek()
    }

    //初始化第一周的日期
    private fun initFirstWeek() {
        var weekDate = store.getString(LocalStorageKey.FIRST_WEEK).toString()
        if (weekDate.isNotBlank()) {
            var calendar = Calendar.getInstance().apply {
                set(Calendar.MONTH, weekDate.split(",")[0].toInt() - 1)
                set(Calendar.DAY_OF_MONTH, weekDate.split(",")[1].toInt())
            }
            binding.weekView.setWeekNumber(calendar)
        }
    }

    //顶部日期格式
    private fun defaultDateFormatter(
        numberOfDays: Int
    ) = when (numberOfDays) {
        1 -> SimpleDateFormat("EEEE yyyy/MM/dd", locale)
        in 2..6 -> SimpleDateFormat("EEE d", locale)
        else -> SimpleDateFormat("EEEEE d", locale)
    }

    //侧边栏格式
    private fun defaultTimeFormatter(): SimpleDateFormat = SimpleDateFormat("HH:00", locale)

    // Calendar特定时间
    private fun Calendar.withTime(hour: Int, minutes: Int): Calendar {
        return (clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minutes)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onEvent(str: String?) {
        when (str) {
            EventType.FIRST_WEEK_CHANGE -> initFirstWeek()
            EventType.EVENT_CHANGE -> {
                //如果是自己发布了事件变化Event，不要刷新视图，减少开销。
                if (isPostEventChangeMyself) {
                    isPostEventChangeMyself = false
                } else {
                    weekViewAdapter.submitList(CourseDao.getAllCalendarEntities() + TaskDao.getAllCalendarEntities())
                }
            }
            EventType.LANGUAGE_CHANGE -> updateLanguage()
        }
    }

    private fun updateLanguage() {
        locale = Locale.getDefault()
        binding.weekView.setDateFormatter { date ->
            defaultDateFormatter(binding.weekView.numberOfVisibleDays).format(date.time)
        }
        binding.toolbarContainer.toolbar.apply {
            title = SimpleDateFormat("MMMM", locale).format(binding.weekView.firstVisibleDate.time)
            subtitle =
                SimpleDateFormat("yyyy", locale).format(binding.weekView.firstVisibleDate.time)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.e("onDestroy", this.javaClass.name)
        EventBus.getDefault().unregister(this)
    }

    override fun onDateSet(p0: DatePickerDialog?, p1: Int, p2: Int, p3: Int) {
        var calendar = Calendar.getInstance()
        calendar.apply {
            set(Calendar.YEAR, p1)
            set(Calendar.MONTH, p2)
            set(Calendar.DAY_OF_MONTH, p3)
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            if (binding.weekView.numberOfVisibleDays == 7) {
                add(Calendar.DAY_OF_MONTH, -1 * (get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY))
            }

        }
        binding.weekView.scrollToDateTime(calendar)
    }

    private class WeekViewSimpleAdapter(
        private val calendarFragment: CalendarFragment,
        private val store: TinyDB,
        private val toolbar: MaterialToolbar? = null,
        private val locale: Locale
    ) : WeekView.SimpleAdapter<CalendarEntity>() {
        override fun onCreateEntity(item: CalendarEntity): WeekViewEntity {
            return item.toWeekViewEntity()
        }

        //响应非全天事件的普通事件的拖放
        override fun onDragAndDropFinished(
            data: CalendarEntity,
            newStartTime: Calendar,
            newEndTime: Calendar
        ) {
            if (data is CalendarEntity.Event) {
                if (data.id.toString().startsWith("20")) {
                    TaskDao.getSimpleTaskById(data.id.toString().substring(2).toInt())?.apply {
                        this.date = CalendarUtil.getWithoutTime(newStartTime)
                        this.beginTime = CalendarUtil.getWithoutSecond(newStartTime)
                        this.endTime = CalendarUtil.getWithoutSecond(newEndTime)
                        calendarFragment.isPostEventChangeMyself = true
                        Thread {
                            TaskDao.updateSimpleTask(this)
                            EventBus.getDefault().post(EventType.EVENT_CHANGE)
                        }.start()
                        Toasty.info(
                            calendarFragment.binding.root.context,
                            "移动成功",
                            Toast.LENGTH_SHORT,
                            false
                        ).show()
                    }
                }
            }
            super.onDragAndDropFinished(data, newStartTime, newEndTime)
        }

        override fun onEventClick(data: CalendarEntity, bounds: RectF) {
            super.onEventClick(data, bounds)
            if (data is CalendarEntity.Event) {
                if (data.id.toString().startsWith("20")) {
                    var i = Intent(
                        calendarFragment.binding.root.context,
                        ModifySimpleTaskActivity::class.java
                    )
                    i.putExtra("simpleTaskId", data.id.toString().substring(2).toInt())
                    calendarFragment.startActivity(i)
                }
            }
        }

        override fun onEventLongClick(data: CalendarEntity, bounds: RectF): Boolean {
            //禁止课程被滑动，禁止全天事件被移动
            if (data is CalendarEntity.Event) {
                if (data.id.toString().startsWith("20")) {
                    TaskDao.getSimpleTaskById(data.id.toString().substring(2).toInt())?.apply {
                        return isAllDay
                    }
                }
                if (data.id.toString().startsWith("10")) {
                    return true
                }
            } else {
                return true
            }
            return super.onEventLongClick(data, bounds)
        }

        override fun onRangeChanged(firstVisibleDate: Calendar, lastVisibleDate: Calendar) {
            super.onRangeChanged(firstVisibleDate, lastVisibleDate)
            store.putString(
                LocalStorageKey.CALENDAR_LAST_FIRST_DAY,
                SimpleDateFormat("yyyy/MM/dd", locale).format(firstVisibleDate.timeInMillis)
            )
            Log.e("日历被滑动了，现在显示的第一天是 ->", firstVisibleDate.toString())
            toolbar?.apply {
                title = SimpleDateFormat("MMMM", locale).format(firstVisibleDate.timeInMillis)
                subtitle = SimpleDateFormat("yyyy", locale).format(firstVisibleDate.timeInMillis)
            }
        }
    }
}

