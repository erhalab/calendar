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
import com.erha.calander.activity.AddSimpleTaskActivity
import com.erha.calander.activity.ModifySimpleTaskActivity
import com.erha.calander.dao.ConfigDao
import com.erha.calander.dao.CourseDao
import com.erha.calander.dao.TaskDao
import com.erha.calander.databinding.FragmentCalenderBinding
import com.erha.calander.model.CalendarEntity
import com.erha.calander.model.SimpleTaskWithoutID
import com.erha.calander.model.TaskStatus
import com.erha.calander.model.toWeekViewEntity
import com.erha.calander.type.EventType
import com.erha.calander.type.LocalStorageKey
import com.erha.calander.util.*
import com.google.android.material.appbar.MaterialToolbar
import com.philliphsu.bottomsheetpickers.date.DatePickerDialog
import es.dmoral.toasty.Toasty
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.text.SimpleDateFormat
import java.util.*

class CalendarFragment : Fragment(R.layout.fragment_calender), DatePickerDialog.OnDateSetListener {
    lateinit var binding: FragmentCalenderBinding

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
    ): View {
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
            val calendar = CalendarUtil.getWithoutSecond()
            val s = this.split("/")
            try {
                calendar.apply {
                    set(Calendar.YEAR, s[0].toInt())
                    set(Calendar.MONTH, s[1].toInt() - 1)
                    set(Calendar.DAY_OF_MONTH, s[2].toInt())
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
            hourHeight = 300
            minHourHeight = 200
            maxHourHeight = 500
            stickToActualWeek = true
        }
        initFirstWeek()
        updateTimeRangeText()
    }

    private fun loadGuide() {
        val guideVersion = 1
        if (!ConfigDao.isDisplayingAnyGuide && !GuideUtil.getGuideStatus(
                binding.root.context,
                this.javaClass.name,
                guideVersion
            )
        ) {
            ConfigDao.isDisplayingAnyGuide = true
            val list = listOf(
                GuideEntity(
                    view = binding.toolbarContainer.toolbar,
                    title = "日历视图",
                    text = "快速跳转到指定日期、今天，\n以及设置日历视图的可见天数"
                ),
                GuideEntity(
                    view = binding.weekViewWeekNumberGuideZone,
                    title = "当前周数",
                    text = "你可以在设置中自定义起始周的日期"
                ),
                GuideEntity(
                    view = binding.weekViewGuideZone,
                    title = "快捷操作",
                    text = "点击任务 -> 查看详情\n长按拖动任务 -> 改变时间\n空白区域长按 -> 在目标时间添加任务"
                )
            )
            val demoCalendar = CalendarUtil.getWithoutSecond().apply {
                set(Calendar.HOUR_OF_DAY, 12)
            }
            val nextHour = CalendarUtil.getWithoutSecond(demoCalendar).apply {
                add(Calendar.HOUR_OF_DAY, 1)
            }
            binding.weekView.scrollToDateTime((demoCalendar.clone() as Calendar).apply {
                add(Calendar.HOUR_OF_DAY, 1)
            })
            val demoTaskId = TaskDao.addSimpleTask(
                SimpleTaskWithoutID(
                    status = TaskStatus.ONGOING,
                    title = "演示任务",
                    detailHtml = "程序自动创建",
                    hashTime = true,
                    date = demoCalendar,
                    isAllDay = false,
                    beginTime = demoCalendar,
                    endTime = nextHour,
                    isDDL = true,
                    notifyTimes = ArrayList()
                )
            )
            var i = 0
            GuideUtil.getDefaultBuilder(requireActivity(), list[i++])
                .setGuideListener {
                    GuideUtil.getDefaultBuilder(requireActivity(), list[i++])
                        .setGuideListener {
                            GuideUtil.getDefaultBuilder(requireActivity(), list[i++])
                                .setGuideListener {
                                    TaskDao.getSimpleTaskById(demoTaskId)
                                        ?.let { it1 ->
                                            TaskDao.removeSimpleTask(it1)
                                            reloadAllCalendarEvents()
                                        }
                                    ConfigDao.isDisplayingAnyGuide = false
                                    GuideUtil.updateGuideStatus(
                                        binding.root.context,
                                        this.javaClass.name,
                                        guideVersion
                                    )
                                }.build().show()
                        }.build().show()
                }.build().show()
        }
    }

    override fun onResume() {
        super.onResume()
        loadGuide()
        reloadAllCalendarEvents()
    }

    fun updateTimeRangeText(numberOfVisibleDays: Int = binding.weekView.numberOfVisibleDays) {
        Log.e("updateTimeRangeText", "numberOfVisibleDays=${numberOfVisibleDays}")
        val firstVisibleDate = binding.weekView.firstVisibleDate
        val lastVisibleDate = firstVisibleDate.clone() as Calendar
        lastVisibleDate.apply {
            add(Calendar.DAY_OF_YEAR, numberOfVisibleDays)
        }
        binding.timeRangeText.text = when (numberOfVisibleDays) {
            1 -> "${
                SimpleDateFormat.getDateInstance()
                    .format(firstVisibleDate.timeInMillis)
            }"
            else -> "${
                SimpleDateFormat.getDateInstance()
                    .format(firstVisibleDate.timeInMillis)
            } - ${
                SimpleDateFormat.getDateInstance()
                    .format(lastVisibleDate.timeInMillis)
            }"
        }
    }

    //更新日历视图的所有事件
    private fun reloadAllCalendarEvents() {
        weekViewAdapter.submitList(CourseDao.getAllCalendarEntities() + TaskDao.getAllCalendarEntities())
    }

    //初始化第一周的日期
    private fun initFirstWeek() {
        val weekDate = store.getString(LocalStorageKey.FIRST_WEEK).toString()
        if (weekDate.isNotBlank()) {
            val calendar = CalendarUtil.getWithoutTime().apply {
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
        1 -> SimpleDateFormat("EEEE", locale)
        in 2..6 -> SimpleDateFormat("EEE", locale)
        else -> SimpleDateFormat("EEEEE", locale)
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
                    reloadAllCalendarEvents()
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
        val calendar = CalendarUtil.getWithoutSecond()
        calendar.apply {
            set(Calendar.YEAR, p1)
            set(Calendar.MONTH, p2)
            set(Calendar.DAY_OF_MONTH, p3)
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

        override fun onEmptyViewLongClick(time: Calendar) {
            super.onEmptyViewLongClick(time)
            val i = Intent(calendarFragment.binding.root.context, AddSimpleTaskActivity::class.java)
            i.putExtra("calendarLongClickAddTask", true)
            i.putExtra("date", time)
            calendarFragment.startActivity(i)
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
            calendarFragment.updateTimeRangeText()
            toolbar?.apply {
                title = SimpleDateFormat("MMMM", locale).format(firstVisibleDate.timeInMillis)
                subtitle = SimpleDateFormat("yyyy", locale).format(firstVisibleDate.timeInMillis)
            }
        }
    }
}

