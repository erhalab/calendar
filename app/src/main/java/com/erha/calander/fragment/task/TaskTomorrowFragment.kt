package com.erha.calander.fragment.task

import com.erha.calander.dao.TaskDao
import com.erha.calander.model.SimpleTaskWithID
import com.erha.calander.model.TaskStatus
import com.erha.calander.util.CalendarUtil
import java.util.*

class TaskTomorrowFragment : TaskBaseFragment() {

    override fun getSmallTitleText(): String {
        return "明天,所有"
    }

    override fun getDate4ThisPage(): ArrayList<SimpleTaskWithID> {
        val list = ArrayList<SimpleTaskWithID>()
        val listUnDone = ArrayList<SimpleTaskWithID>()
        val listDone = ArrayList<SimpleTaskWithID>()
        val listCanceled = ArrayList<SimpleTaskWithID>()
        //过滤数据，只关心部分数据
        val now = CalendarUtil.getWithoutTime()
        now.apply {
            add(Calendar.DAY_OF_MONTH, 1)
        }
        for (i in TaskDao.getAllSimpleTasks()) {
            if (i.hasTime && CalendarUtil.getWithoutTime(i.date) == now) {
                when (i.status) {
                    TaskStatus.ONGOING -> listUnDone.add(i)
                    TaskStatus.FINISHED -> listDone.add(i)
                    TaskStatus.CANCELED -> listCanceled.add(i)
                }
            }
        }
        listUnDone.sortBy { i ->
            i.beginTime
        }
        listDone.sortBy { i ->
            i.beginTime
        }
        listCanceled.sortBy { i ->
            i.beginTime
        }
        list.addAll(listUnDone + listDone + listCanceled)
        return list
    }
}
