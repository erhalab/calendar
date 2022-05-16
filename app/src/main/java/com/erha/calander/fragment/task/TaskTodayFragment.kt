package com.erha.calander.fragment.task

import com.erha.calander.dao.TaskDao
import com.erha.calander.model.SimpleTaskWithID
import com.erha.calander.model.TaskStatus
import com.erha.calander.util.CalendarUtil

class TaskTodayFragment : TaskBaseFragment() {

    override fun getSmallTitleText(): String {
        return "今天,未完成"
    }

    override fun getDate4ThisPage(): ArrayList<SimpleTaskWithID> {
        val list = ArrayList<SimpleTaskWithID>()
        val listNoDate = ArrayList<SimpleTaskWithID>()
        val listPassUndone = ArrayList<SimpleTaskWithID>()
        val listUnDone = ArrayList<SimpleTaskWithID>()
        val listDone = ArrayList<SimpleTaskWithID>()
        val listCanceled = ArrayList<SimpleTaskWithID>()
        //过滤数据，只关心部分数据
        //今日视图，会关心今天的所有已完成、未完成、取消的事项，以及过期未完成的事项，以及没有日期的事项
        val now = CalendarUtil.getWithoutTime()
        for (i in TaskDao.getAllSimpleTasks()) {
            if (i.hasTime && i.date < now && i.status == TaskStatus.ONGOING) {
                listPassUndone.add(i)
                continue
            }
            if (!i.hasTime && i.status == TaskStatus.ONGOING) {
                listNoDate.add(i)
            }
            if (i.hasTime && CalendarUtil.getWithoutTime(i.date) == now) {
                when (i.status) {
                    TaskStatus.ONGOING -> listUnDone.add(i)
                    TaskStatus.FINISHED -> listDone.add(i)
                    TaskStatus.CANCELED -> listCanceled.add(i)
                }
            }
        }
        listNoDate.sortBy { i ->
            i.id
        }
        listNoDate.reverse()
        listPassUndone.sortBy { i ->
            i.beginTime
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
        list.addAll(listNoDate + listPassUndone + listUnDone + listDone + listCanceled)
        return list
    }
}
