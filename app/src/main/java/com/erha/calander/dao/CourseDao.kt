package com.erha.calander.dao

import ando.file.core.FileUtils
import android.graphics.Color
import androidx.core.net.toUri
import com.erha.calander.model.CalendarEntity
import com.erha.calander.model.SimpleNEUClass
import com.erha.calander.type.NotificationChannelType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object CourseDao {
    private lateinit var file: File
    private var beginDayCalendar = Calendar.getInstance()
    private var notifyTimes = ArrayList<Int>()

    //每节课的起始时间
    private var timeMap = HashMap<Int, String>()

    //色板
    private var colorMap = HashMap<Int, String>()

    //已加入的课程的色值
    private var courseColorMap = HashMap<String, String>()

    //一共多少种颜色捏？
    private var colorQuantity = 0

    //现在用到哪一个颜色了？
    private var colorCounter = 1

    //每节课多久？
    private var classDuration = 50

    private var isInit = false

    private val courseEvents = ArrayList<CalendarEntity>()
    private val coursePostedNotifications = ArrayList<SimpleTaskNotification>()
    private val courseNewNotifications = ArrayList<SimpleTaskNotification>()

    fun load(file: File, courseFirstWeek: String?, notifyTimesString: String?) {
        this.file = file

        //一些默认值
        //默认起始日期
        beginDayCalendar.set(Calendar.YEAR, 2022)
        beginDayCalendar.set(Calendar.MONTH, 2 - 1)
        beginDayCalendar.set(Calendar.DAY_OF_MONTH, 27)
        beginDayCalendar.add(
            Calendar.DAY_OF_MONTH,
            (beginDayCalendar.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY) * -1
        )
        //清空通知时间数组
        notifyTimes.clear()
        //初始化每节课的时间
        timeMap.clear()
        timeMap[1] = "08:30"
        timeMap[2] = "09:30"
        timeMap[3] = "10:40"
        timeMap[4] = "11:40"
        timeMap[5] = "14:00"
        timeMap[6] = "15:00"
        timeMap[7] = "16:10"
        timeMap[8] = "17:10"
        timeMap[9] = "18:30"
        timeMap[10] = "19:30"
        timeMap[11] = "20:30"
        timeMap[12] = "21:30"
        //初始化颜色数量
        colorQuantity = 0
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
        //初始化课程色值对应表
        courseColorMap.clear()
        //初始化颜色计数器
        colorCounter = 1
        //初始化一节课的时长
        classDuration = 50
        //初始化几个列表
        coursePostedNotifications.clear()
        courseNewNotifications.clear()
        courseEvents.clear()

        courseFirstWeek?.apply {
            if (this.isNotBlank()) {
                beginDayCalendar.apply {
                    set(Calendar.YEAR, courseFirstWeek.split(",")[0].toInt())
                    set(Calendar.MONTH, courseFirstWeek.split(",")[1].toInt() - 1)
                    set(Calendar.DAY_OF_MONTH, courseFirstWeek.split(",")[2].toInt())
                    add(Calendar.DAY_OF_MONTH, (get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY) * -1)
                }
            }
        }
        notifyTimesString?.apply {
            if (this.isNotBlank()) {
                for (i in this.split(",")) {
                    try {
                        notifyTimes.add(i.toInt())
                    } catch (e: NumberFormatException) {
                    }
                }
            }
        }
        isInit = true
        readFromLocal()
    }

    fun reload(courseFirstWeek: String?, notifyTimesString: String?) {
        var taskNames = ArrayList<String>()
        for (n in coursePostedNotifications) {
            //把之前发布的通知撤销掉先
            taskNames.add(n.taskName)
        }
        //让通知管理者去处理通知的事情，我已经告诉它移除那些通知了
        NotificationDao.removeAllByTaskNames(taskNames)
        load(file = file, courseFirstWeek = courseFirstWeek, notifyTimesString = notifyTimesString)
    }

    private fun readFromLocal() {
        if (!isInit) {
            return
        }
        try {
            var json = FileUtils.readFileText(File(File(file, "course"), "data.json").toUri())
            //var json = "[{\"name\":\"体育(四)(A047047)\",\"position\":\"\",\"teacher\":\"任可\",\"weeks\":[3,4,5,6,7,8,10,11,12,13,14,15],\"day\":5,\"sections\":[5,6]},{\"name\":\"Web开发技术(A045603)\",\"position\":\"信息A101(浑南校区)\",\"teacher\":\"于鲲鹏\",\"weeks\":[11,12,13,16,17],\"day\":6,\"sections\":[5,6,7,8]},{\"name\":\"Web开发技术(A045603)\",\"position\":\"信息A101(浑南校区)\",\"teacher\":\"于鲲鹏\",\"weeks\":[11,12,13,14,16],\"day\":7,\"sections\":[5,6,7,8]},{\"name\":\"嵌入式软件开发技术(A045602)\",\"position\":\"1号A201(浑南校区)\",\"teacher\":\"姜琳颖\",\"weeks\":[1,2,3,4,5,6,7,8],\"day\":4,\"sections\":[5,6]},{\"name\":\"嵌入式软件开发技术(A045602)\",\"position\":\"信息A101(浑南校区)\",\"teacher\":\"姜琳颖\",\"weeks\":[1,2,3,4,5,6,7,8],\"day\":1,\"sections\":[7,8]},{\"name\":\"思想政治理论实践课(A045564)\",\"position\":\"文管A447(浑南校区)\",\"teacher\":\"王喆\",\"weeks\":[5,6,7,8],\"day\":5,\"sections\":[3,4]},{\"name\":\"软件创新方法与实例(A045590)\",\"position\":\"文管A447(浑南校区)\",\"teacher\":\"王蓓蕾\",\"weeks\":[2,3,4,5,7,8,11,12],\"day\":7,\"sections\":[1,2]},{\"name\":\"软件创新方法与实例(A045590)\",\"position\":\"建筑A402(浑南校区)\",\"teacher\":\"王蓓蕾\",\"weeks\":[1,2,3,4,5,6,7,8],\"day\":5,\"sections\":[7,8]},{\"name\":\"软件构造与测试(A045599)\",\"position\":\"信息A101(浑南校区)\",\"teacher\":\"于海\",\"weeks\":[9,10,11,12,13,14,15,16],\"day\":1,\"sections\":[3,4]},{\"name\":\"软件构造与测试(A045599)\",\"position\":\"信息A101(浑南校区)\",\"teacher\":\"于海\",\"weeks\":[10,11,12,13,14,15,16,17],\"day\":5,\"sections\":[1,2]},{\"name\":\"软件构造与测试(A045599)\",\"position\":\"1号A204(浑南校区)\",\"teacher\":\"于海\",\"weeks\":[10,11,12,13,14,15,16,17],\"day\":3,\"sections\":[3,4]},{\"name\":\"数据库概论(A045593)\",\"position\":\"1号A304(浑南校区)\",\"teacher\":\"佟强\",\"weeks\":[1,2,3,4,5,6,7,8],\"day\":3,\"sections\":[1,2]},{\"name\":\"数据库概论(A045593)\",\"position\":\"1号A304(浑南校区)\",\"teacher\":\"佟强\",\"weeks\":[1,2,3,4,5,6,7,8],\"day\":1,\"sections\":[1,2]},{\"name\":\"数据库概论(A045593)\",\"position\":\"信息A101(浑南校区)\",\"teacher\":\"佟强\",\"weeks\":[5,6,7,8],\"day\":2,\"sections\":[5,6]},{\"name\":\"计算机网络(A045594)\",\"position\":\"1号A202(浑南校区)\",\"teacher\":\"刘益先\",\"weeks\":[1,2,3,4,5,6,7,8],\"day\":3,\"sections\":[3,4]},{\"name\":\"计算机网络(A045594)\",\"position\":\"1号A202(浑南校区)\",\"teacher\":\"刘益先\",\"weeks\":[1,2,3,4,5,6,7,8],\"day\":1,\"sections\":[3,4]},{\"name\":\"计算机网络(A045594)\",\"position\":\"1号A202(浑南校区)\",\"teacher\":\"刘益先\",\"weeks\":[1,2,3,4],\"day\":2,\"sections\":[5,6]},{\"name\":\"数值分析(A045587)\",\"position\":\"1号A105(浑南校区)\",\"teacher\":\"冯男\",\"weeks\":[1,2,3,4,5,6,7,8,10,11,12,13],\"day\":4,\"sections\":[1,2]},{\"name\":\"数值分析(A045587)\",\"position\":\"1号A207(浑南校区)\",\"teacher\":\"冯男\",\"weeks\":[1,2,3,4,5,6,7,8,9,10,11,12],\"day\":2,\"sections\":[1,2]},{\"name\":\"操作系统(A045595)\",\"position\":\"1号A202(浑南校区)\",\"teacher\":\"张伟\",\"weeks\":[10,11,12,13,14,15,16,17],\"day\":3,\"sections\":[1,2]},{\"name\":\"操作系统(A045595)\",\"position\":\"1号A202(浑南校区)\",\"teacher\":\"张伟\",\"weeks\":[9,10,11,12,13,14,15,16],\"day\":1,\"sections\":[1,2]},{\"name\":\"形势与政策(2)(A045563)\",\"position\":\"文管A447(浑南校区)\",\"teacher\":\"钱丽丽\",\"weeks\":[1,2],\"day\":5,\"sections\":[5,6]},{\"name\":\"大国航母与舰载机（在线式）(A045969)\",\"position\":\"\",\"teacher\":\"学堂云平台\",\"weeks\":[6,16],\"day\":3,\"sections\":[5,6,7,8]},{\"name\":\"软件工程经济与软件生态(A046118)\",\"position\":\"1号A204(浑南校区)\",\"teacher\":\"吴辰铌\",\"weeks\":[10,11,12,13,14,15,16,17],\"day\":4,\"sections\":[5,6]},{\"name\":\"Android开发技术(A045600)\",\"position\":\"信息A101(浑南校区)\",\"teacher\":\"于鲲鹏\",\"weeks\":[1,2,3,4,6],\"day\":6,\"sections\":[1,2,3,4]},{\"name\":\"Android开发技术(A045600)\",\"position\":\"信息A101(浑南校区)\",\"teacher\":\"于鲲鹏\",\"weeks\":[3],\"day\":6,\"sections\":[5,6,7,8]}]"
            var classList: List<SimpleNEUClass>? = null
            classList = Gson().fromJson(
                json,
                (object : TypeToken<List<SimpleNEUClass>>() {}.type)
            )
            if (classList != null) {
                for (i in classList) {
                    add(neuClass = i)
                }
            }

        } catch (e: Exception) {
        }
    }

    //添加课程
    fun add(neuClass: SimpleNEUClass) {
        if (!isInit) {
            return
        }
        var fullName = neuClass.name
        var fullPosition = neuClass.position
        //去掉课程编号
        if (neuClass.name.lastIndexOf("(") != -1) {
            neuClass.name = neuClass.name.substring(
                0,
                neuClass.name.lastIndexOf("(")
            )
        }
        //去掉教师的校区
        if (neuClass.position.lastIndexOf("(") != -1) {
            neuClass.position = neuClass.position.substring(
                0,
                neuClass.position.lastIndexOf("(")
            )
        }

        for (week in neuClass.weeks) {
            var nowIndex = -1
            for (startSectionIndex in 0 until neuClass.sections.size) {
                //注意，要处理连续起来的课程
                //找到最后一个连续的位置
                if (startSectionIndex <= nowIndex) {
                    continue
                }
                var endSectionIndex = startSectionIndex
                for (findLast in startSectionIndex + 1 until neuClass.sections.size) {
                    if (neuClass.sections[findLast] == (neuClass.sections[endSectionIndex] + 1)) {
                        endSectionIndex = findLast
                    } else {
                        break
                    }
                }

                //Log.e( "连续节数测试", neuClass.sections[startSectionIndex].toString() + "--->" + neuClass.sections[endSectionIndex].toString())

                //现在起始就是一个单独的连续课
                //首先，根据第几周，星期几来计算当天日期
                var courseCalendar = beginDayCalendar.clone() as Calendar
                var startTime = "12:00"

                courseCalendar.add(
                    Calendar.DAY_OF_MONTH,
                    (week - 1) * 7 + (if (neuClass.day + 1 > 7) 1 else (neuClass.day + 1)) - 1
                )

                var duration = classDuration //一节课多长？
                var endCalendar = Calendar.getInstance()
                timeMap[neuClass.sections[startSectionIndex]]?.apply {
                    courseCalendar.set(
                        Calendar.HOUR_OF_DAY,
                        this.split(":")[0].toInt()
                    )
                    courseCalendar.set(
                        Calendar.MINUTE,
                        this.split(":")[1].toInt()
                    )
                    courseCalendar.set(Calendar.MILLISECOND, 0)

                    //先计算出下课的确切时间
                    endCalendar = courseCalendar.clone() as Calendar
                    timeMap[neuClass.sections[endSectionIndex]]?.apply {
                        endCalendar.set(
                            Calendar.HOUR_OF_DAY,
                            this.split(":")[0].toInt()
                        )
                        endCalendar.set(
                            Calendar.MINUTE,
                            this.split(":")[1].toInt()
                        )
                        endCalendar.set(
                            Calendar.MILLISECOND,
                            0
                        )
                        endCalendar.add(
                            Calendar.MINUTE,
                            duration
                        )
                    }

                    duration =
                        ((endCalendar.timeInMillis - courseCalendar.timeInMillis) / 1000 / 60).toInt()

                    startTime = this
                }


                //获取课程颜色，同一个课程只有一个颜色
                var color = "#87D288"

                if (courseColorMap[neuClass.name] == null) {
                    colorMap[colorCounter]?.apply {
                        courseColorMap[neuClass.name] = this
                    }
                    colorCounter++
                    if (colorCounter > 10) {
                        colorCounter = 1
                    }

                }
                courseColorMap[neuClass.name]?.apply {
                    color = this
                }


                var month = courseCalendar.get(Calendar.MONTH) + 1
                var dayOfMonth = courseCalendar.get(Calendar.DAY_OF_MONTH)
                var isPass =
                    (Calendar.getInstance().compareTo(endCalendar) == 1)


                if (isPass) {
                    color = color.replace("#", "#50")
                }


//                var apiEvent = ApiEvent(
//                    title = neuClass.name,
//                    location = neuClass.position,
//                    year = courseCalendar.get(Calendar.YEAR),
//                    month = month,
//                    dayOfMonth = dayOfMonth,
//                    startTime = startTime,
//                    duration = duration,
//                    color = color,
//                    isCanceled = false,
//                    isAllDay = false
//                )


                courseEvents.add(
                    CalendarEntity.Event(
                        id = "10${courseEvents.size + 1}".toLong(),
                        title = neuClass.name,
                        location = neuClass.position,
                        startTime = courseCalendar,
                        endTime = endCalendar,
                        color = Color.parseColor(color),
                        isAllDay = false,
                        isCanceled = false
                    )
                )

                if (!isPass) {
                    //开始创建通知
                    var simpleTaskNotification = SimpleTaskNotification()
                    var notifyTimes = ArrayList<Int>()
                    for (i in this.notifyTimes) {
                        notifyTimes.add(i)
                    }
                    simpleTaskNotification.apply {
                        taskName =
                            "${fullName}-${week}-${neuClass.day}-${neuClass.sections[startSectionIndex]}"
                        title = fullName
                        text = fullPosition
                        this.notifyTimes = notifyTimes
                        beginTime =
                            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(
                                courseCalendar.timeInMillis
                            )
                        channel = NotificationChannelType.COURSE
                    }
                    courseNewNotifications.add(simpleTaskNotification)
                }

                nowIndex = endSectionIndex
            }
        }
        postNotification()
    }

    private fun postNotification() {
        NotificationDao.addAll(this.courseNewNotifications)
        this.coursePostedNotifications.addAll(this.courseNewNotifications)
        this.courseNewNotifications.clear()
    }

    fun getAllCalendarEntities(): ArrayList<CalendarEntity> {
        if (!isInit) {
            return ArrayList<CalendarEntity>()
        }
        return courseEvents
    }
}