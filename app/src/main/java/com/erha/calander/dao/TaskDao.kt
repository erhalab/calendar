package com.erha.calander.dao

import ando.file.core.FileUtils
import android.graphics.Color
import android.util.Log
import androidx.core.net.toUri
import com.erha.calander.model.CalendarEntity
import com.erha.calander.model.SimpleTaskWithID
import com.erha.calander.model.SimpleTaskWithoutID
import com.erha.calander.model.TaskStatus
import com.erha.calander.type.NotificationChannelType
import com.erha.calander.util.HtmlUtil
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.ByteArrayInputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object TaskDao {

    private var hasInit = false
    private var simpleTaskList = ArrayList<SimpleTaskWithID>()
    private var simpleTaskCalendarEntityList = ArrayList<CalendarEntity.Event>()
    private val simpleTaskPostedNotifications = ArrayList<SimpleTaskNotification>()
    private val simpleTaskNewNotifications = ArrayList<SimpleTaskNotification>()

    //色板
    var colorMap = HashMap<Int, String>()

    //一共多少种颜色捏？
    private var colorQuantity = 0

    private lateinit var file: File
    private lateinit var simpleTaskFile: File

    fun load(file: File) {
        if (hasInit) {
            TODO("不能重复load")
        } else {
            hasInit = true
            this.file = file
            this.simpleTaskFile = File(File(file, "task"), "simple.json")
        }
        //初始化色板
        colorMap.clear()
        colorMap[++colorQuantity] = "#ec6666"
        colorMap[++colorQuantity] = "#f2b04b"
        colorMap[++colorQuantity] = "#ffd966"
        colorMap[++colorQuantity] = "#dde358"
        colorMap[++colorQuantity] = "#93c47d"
        colorMap[++colorQuantity] = "#5dd1a8"
        colorMap[++colorQuantity] = "#52b8d2"
        colorMap[++colorQuantity] = "#5992f8"
        colorMap[++colorQuantity] = "#9f4cef"
        colorMap[++colorQuantity] = "#d25294"
        simpleTaskList.clear()
        //读取到一个列表，然后循环添加他们
        try {
            Gson().fromJson<List<SimpleTaskWithID>?>(
                FileUtils.readFileText(simpleTaskFile.toUri()),
                (object : TypeToken<List<SimpleTaskWithID>>() {}.type)
            )?.apply {
                for (i in this) {
                    addSimpleTask(i.toSimpleTaskWithoutID(), false, i.id)
                }
            }
        } catch (e: Exception) {
        } catch (e: Exception) {
        }
    }

    private fun saveToLocal() {
        FileUtils.write2File(
            input = ByteArrayInputStream(Gson().toJson(simpleTaskList).toByteArray()),
            file = simpleTaskFile,
            overwrite = true
        )
    }

    fun updateSimpleTask(oldTask: SimpleTaskWithID) {
        //移除旧的通知
        removeSimpleTask(oldTask)
        //重新添加进去
        addSimpleTask(oldTask.toSimpleTaskWithoutID(), false, oldTask.id)
        Log.e(this.javaClass.name, "更新SimpleTask")
    }

    fun getSimpleTaskById(id: Int): SimpleTaskWithID? {
        for (i in simpleTaskList) {
            if (i.id == id) {
                return i
            }
        }
        return null
    }

    fun removeSimpleTask(oldTask: SimpleTaskWithID) {
        //先从任务列表中删除
        simpleTaskList.remove(oldTask)
        //从已发布的通知列表中删除
        val notificationTaskName = getSimpleTaskNotificationNameById(oldTask.id)
        for (i in ArrayList(simpleTaskPostedNotifications)) {
            if (i.taskName == notificationTaskName) {
                simpleTaskPostedNotifications.remove(i)
            }
        }
        //告诉通知Dao，我要删除这些已发布的通知
        val a = ArrayList<String>()
        a.add(notificationTaskName)
        NotificationDao.removeAllByTaskNames(a)
        //从日历实体集合中删除
        val calendarEntityId = getSimpleTaskCalendarEntityIdById(oldTask.id)
        for (i in ArrayList(simpleTaskCalendarEntityList)) {
            if (i.id == calendarEntityId) {
                simpleTaskCalendarEntityList.remove(i)
            }
        }
        saveToLocal()
    }

    fun addSimpleTask(
        simpleTaskWithoutID: SimpleTaskWithoutID,
        isNew: Boolean = true,
        id: Int = -1
    ) {
        var newId = -1
        if (isNew) {
            newId = 1
            if (simpleTaskList.isNotEmpty()) {
                newId = simpleTaskList.last().id + 1
            }
        } else {
            newId = id
        }
        val task = SimpleTaskWithID(
            id = newId,
            status = simpleTaskWithoutID.status,
            title = simpleTaskWithoutID.title,
            detailHtml = simpleTaskWithoutID.detailHtml,
            hashTime = simpleTaskWithoutID.hashTime,
            date = simpleTaskWithoutID.date,
            isAllDay = simpleTaskWithoutID.isAllDay,
            beginTime = simpleTaskWithoutID.beginTime,
            endTime = simpleTaskWithoutID.endTime,
            isDDL = simpleTaskWithoutID.isDDL,
            notifyTimes = simpleTaskWithoutID.notifyTimes,
            customColor = simpleTaskWithoutID.customColor,
            color = simpleTaskWithoutID.color
        )
        //添加到任务列表
        simpleTaskList.add(task)
        //创建对应的日历实体
        createSimpleTaskCalendarEntity(task)
        //创建通知
        createSimpleTaskNotification(task)
        //把所有新的通知传递给通知Dao
        postAllNewNotifications()
        //保存到本地
        saveToLocal()
    }

    private fun postAllNewNotifications() {
        NotificationDao.addAll(simpleTaskNewNotifications)
        simpleTaskPostedNotifications.addAll(simpleTaskNewNotifications)
        simpleTaskNewNotifications.clear()
    }

    private fun createSimpleTaskCalendarEntity(simpleTaskWithID: SimpleTaskWithID) {
        //获取起始时间
        val beginCalendar = simpleTaskWithID.date.clone() as Calendar
        if (simpleTaskWithID.isAllDay) {
            beginCalendar.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        } else {
            beginCalendar.apply {
                set(Calendar.HOUR_OF_DAY, simpleTaskWithID.beginTime.get(Calendar.HOUR_OF_DAY))
                set(Calendar.MINUTE, simpleTaskWithID.beginTime.get(Calendar.MINUTE))
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }
        val endCalendar = beginCalendar.clone() as Calendar
        if (simpleTaskWithID.isAllDay) {
            endCalendar.apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        } else {
            endCalendar.apply {
                set(Calendar.HOUR_OF_DAY, simpleTaskWithID.endTime.get(Calendar.HOUR_OF_DAY))
                set(Calendar.MINUTE, simpleTaskWithID.endTime.get(Calendar.MINUTE))
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }
        var color = colorMap[(simpleTaskWithID.id % colorQuantity) + 1]
        if (simpleTaskWithID.customColor) {
            color = simpleTaskWithID.color
        }
        if (simpleTaskWithID.status == TaskStatus.FINISHED && color != null) {
            color = color.replace("#", "#50")
        }
        simpleTaskCalendarEntityList.add(
            CalendarEntity.Event(
                id = getSimpleTaskCalendarEntityIdById(simpleTaskWithID.id),
                title = simpleTaskWithID.title,
                location = HtmlUtil.convertHtmlToOnlyText(simpleTaskWithID.detailHtml),
                startTime = beginCalendar,
                endTime = endCalendar,
                color = Color.parseColor(color), //是否完成会改变这个颜色
                isAllDay = simpleTaskWithID.isAllDay,
                isCanceled = (simpleTaskWithID.status == TaskStatus.CANCELED)
            )
        )
    }

    private fun createSimpleTaskNotification(simpleTaskWithID: SimpleTaskWithID) {
        //不是正在进行中的任务，不要发布通知
        if (simpleTaskWithID.status != TaskStatus.ONGOING) {
            return
        }
        //获取起始时间
        val calendar = simpleTaskWithID.date.clone() as Calendar
        if (simpleTaskWithID.isAllDay) {
            calendar.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        } else {
            calendar.apply {
                set(Calendar.HOUR_OF_DAY, simpleTaskWithID.beginTime.get(Calendar.HOUR_OF_DAY))
                set(Calendar.MINUTE, simpleTaskWithID.beginTime.get(Calendar.MINUTE))
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }
        val simpleTaskNotification = SimpleTaskNotification()
        simpleTaskNotification.apply {
            taskName = getSimpleTaskNotificationNameById(simpleTaskWithID.id)
            title = simpleTaskWithID.title
            text = HtmlUtil.convertHtmlToOnlyText(simpleTaskWithID.detailHtml)
            this.notifyTimes = simpleTaskWithID.notifyTimes
            beginTime =
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(
                    calendar.timeInMillis
                )
            channel = NotificationChannelType.SIMPlE_EVENT
        }
        simpleTaskNewNotifications.add(simpleTaskNotification)
    }

    fun getAllCalendarEntities(): ArrayList<CalendarEntity.Event> {
        if (!hasInit) {
            return ArrayList<CalendarEntity.Event>()
        }
        return simpleTaskCalendarEntityList
    }

    private fun getSimpleTaskNotificationNameById(id: Int): String {
        return "task${id}"
    }

    private fun getSimpleTaskCalendarEntityIdById(id: Int): Long {
        return "20${id}".toLong()
    }
}