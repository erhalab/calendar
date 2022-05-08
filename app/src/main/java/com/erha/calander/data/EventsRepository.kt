package com.erha.calander.data

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import com.erha.calander.dao.CourseDao
import com.erha.calander.data.model.ApiBlockedTime
import com.erha.calander.data.model.ApiEvent
import com.erha.calander.data.model.ApiResult
import com.erha.calander.data.model.CalendarEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.YearMonth

class EventsRepository(private val context: Context) {

    private val eventResponseType = object : TypeToken<List<ApiEvent>>() {}.type
    private val blockedTimeResponseType = object : TypeToken<List<ApiBlockedTime>>() {}.type

    private val gson = Gson()

    fun fetch(
        yearMonths: List<YearMonth>,
        onSuccess: (List<CalendarEntity>) -> Unit
    ) {
        val handlerThread = HandlerThread("events-fetching")
        handlerThread.start()

        val backgroundHandler = Handler(handlerThread.looper)
        val mainHandler = Handler(Looper.getMainLooper())

        backgroundHandler.post {
            val apiEntities = fetchEvents() + fetchBlockedTimes()

            val calendarEntities = yearMonths.flatMap { yearMonth ->
                apiEntities.mapIndexedNotNull { index, apiResult ->
                    apiResult.toCalendarEntity(yearMonth, index)
                }
            }

            mainHandler.post {
                onSuccess(calendarEntities)
            }
        }
    }

    private fun fetchEvents(): List<ApiResult> {
//        val inputStream = context.assets.open("courseTest.json")
//        Log.e("fetchEvents", "fetchEvents json")
//        val json = inputStream.reader().readText()

//        val store = TinyDB(context)
//        var begin = Calendar.getInstance();
//        begin.apply {
//            set(Calendar.MONTH, 2 - 1)
//            set(Calendar.DAY_OF_MONTH, 27)
//        }
//        var i = store.getString(SettingType.COURSE_FIRST_DAY)
//        i?.apply {
//            if (this.isNotBlank()) {
//                begin.apply {
//                    set(Calendar.YEAR, i.split(",")[0].toInt())
//                    set(Calendar.MONTH, i.split(",")[1].toInt() - 1)
//                    set(Calendar.DAY_OF_MONTH, i.split(",")[2].toInt())
//                }
//            }
//        }
//        begin.apply {
//            add(Calendar.DAY_OF_MONTH, (get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY) * -1)
//        }
//        //现在拿到了，这学期的第一天
        var courseList = CourseDao.getAll()

//        var timeMap = HashMap<Int, String>()
//        timeMap[1] = "08:30"
//        timeMap[2] = "09:30"
//        timeMap[3] = "10:40"
//        timeMap[4] = "11:40"
//        timeMap[5] = "14:00"
//        timeMap[6] = "15:00"
//        timeMap[7] = "16:10"
//        timeMap[8] = "17:10"
//        timeMap[9] = "18:30"
//        timeMap[10] = "19:30"
//        timeMap[11] = "20:30"
//        timeMap[12] = "21:30"

//        var colorMap = HashMap<Int, String>()
//        colorMap[1] = "#ec6666"
//        colorMap[2] = "#f2b04b"
//        colorMap[3] = "#ffd966"
//        colorMap[4] = "#dde358"
//        colorMap[5] = "#93c47d"
//        colorMap[6] = "#5dd1a8"
//        colorMap[7] = "#52b8d2"
//        colorMap[8] = "#5992f8"
//        colorMap[9] = "#9f4cef"
//        colorMap[10] = "#d25294"


//        try {
//            var courseJson =
//                FileUtils.readFileText(File(File(context.filesDir, "course"), "data.json").toUri())
//            if (courseJson != null) {
//                //Log.e("courseJson", courseJson)
//                var classList: List<SimpleNEUClass>? = null
//
//                try {
//                    classList =
//                        Gson().fromJson(
//                            courseJson,
//                            (object : TypeToken<List<SimpleNEUClass>>() {}.type)
//                        )
//                    //拿到了我写的课程格式文件
//                    //转换成他写的格式
//
//                    var courseColorMap = HashMap<String, String>()
//
//                    if (classList != null) {
//                        var colorCounter = 1;
//                        for (neuClass in classList) {
//                        }
//                    }
//
//                } catch (e: JsonParseException) {
//                }
//            }
//        } catch (e: FileNotFoundException) {
//        }


        return courseList
    }

    private fun fetchBlockedTimes(): List<ApiResult> {
        val inputStream = context.assets.open("blocked_times.json")
        val json = inputStream.reader().readText()
        return gson.fromJson(json, blockedTimeResponseType)
    }


}
