package com.erha.calander.fragment.task

import com.erha.calander.dao.TaskDao
import com.erha.calander.model.SimpleTaskWithID
import com.erha.calander.model.TaskStatus

class TaskAllInboxFragment : TaskBaseFragment() {

    override fun getSmallTitleText(): String {
        return "收集箱,所有"
    }

    override fun getDate4ThisPage(): ArrayList<SimpleTaskWithID> {
        val list = ArrayList<SimpleTaskWithID>()
        val listUnDone = ArrayList<SimpleTaskWithID>()
        val listDone = ArrayList<SimpleTaskWithID>()
        val listCanceled = ArrayList<SimpleTaskWithID>()
        //过滤数据，只关心部分数据
        for (i in TaskDao.getAllSimpleTasks()) {
            //找所有没有时间的
            if (!i.hasTime) {
                when (i.status) {
                    TaskStatus.ONGOING -> listUnDone.add(i)
                    TaskStatus.FINISHED -> listDone.add(i)
                    TaskStatus.CANCELED -> listCanceled.add(i)
                }
            }
        }
        listUnDone.sortBy { i ->
            i.id
        }
        listUnDone.reverse()
        listDone.sortBy { i ->
            i.id
        }
        listUnDone.reverse()
        listCanceled.sortBy { i ->
            i.id
        }
        listCanceled.reverse()
        list.addAll(listUnDone + listDone + listCanceled)
        return list
    }
}
