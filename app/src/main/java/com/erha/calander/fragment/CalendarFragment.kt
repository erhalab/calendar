package com.erha.calander.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.alamkanak.weekview.WeekView
import com.alamkanak.weekview.WeekViewEntity
import com.erha.calander.R
import com.erha.calander.dao.CourseDao
import com.erha.calander.databinding.FragmentCalenderBinding
import com.erha.calander.model.CalendarEntity
import com.erha.calander.model.toWeekViewEntity
import com.erha.calander.type.EventType
import com.erha.calander.type.LocalStorageKey
import com.erha.calander.util.TinyDB
import com.erha.calander.util.setupWithWeekView
import com.google.android.material.appbar.MaterialToolbar
import com.philliphsu.bottomsheetpickers.date.DatePickerDialog
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.text.SimpleDateFormat
import java.util.*

class CalendarFragment : Fragment(R.layout.fragment_calender), DatePickerDialog.OnDateSetListener {
    private lateinit var binding: FragmentCalenderBinding

    //    private val viewModel by genericViewModel()
    private lateinit var store: TinyDB
    private lateinit var locale: Locale
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
        locale = Locale(store.getString(LocalStorageKey.LANGUAGE))
        EventBus.getDefault().register(this)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolbarContainer.toolbar.setupWithWeekView(
            binding.weekView,
            this@CalendarFragment
        )
//        val adapter = FragmentWeekViewAdapter(
//            loadMoreHandler = viewModel::fetchEvents,
//            locale = this.locale,
//            toolbar = binding.toolbarContainer.toolbar,
//            store = store
//        )
        weekViewAdapter = WeekViewSimpleAdapter(
            locale = this.locale,
            toolbar = binding.toolbarContainer.toolbar,
            store = store
        )
        binding.weekView.adapter = weekViewAdapter
        // Limit WeekView to the current month
        //binding.weekView.minDateAsLocalDate = YearMonth.now().atDay(1)
        weekViewAdapter.submitList(CourseDao.getAll())
//        viewModel.viewState.observe(viewLifecycleOwner) { viewState ->
//            adapter.submitList(viewState.entities)
//        }
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
        //禁止查看的时间范围
        binding.weekView.apply {
            minHour = 6
            maxHour = 23
            hourHeight = 200
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
        1 -> SimpleDateFormat("EEEE dd", locale)
        in 2..6 -> SimpleDateFormat("EEE dd", locale)
        else -> SimpleDateFormat("EEEEE dd", locale)
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
            EventType.FIRST_WEEK_CHANGE -> {
                initFirstWeek()
            }
            EventType.EVENT_CHANGE -> {
                weekViewAdapter.submitList(CourseDao.getAll())
            }
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
}

private class WeekViewSimpleAdapter(
    private val store: TinyDB,
    private val toolbar: MaterialToolbar? = null,
    private val locale: Locale
) : WeekView.SimpleAdapter<CalendarEntity>() {
    override fun onCreateEntity(item: CalendarEntity): WeekViewEntity {
        return item.toWeekViewEntity()
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