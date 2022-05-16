package com.erha.calander.fragment.task

import com.erha.calander.dao.TaskDao
import com.erha.calander.model.SimpleTaskWithID
import com.erha.calander.model.TaskStatus

class TaskAllCancelFragment : TaskBaseFragment() {

    override fun getSmallTitleText(): String {
        return "所有,已放弃"
    }

    override fun getDate4ThisPage(): ArrayList<SimpleTaskWithID> {
        val list = ArrayList<SimpleTaskWithID>()
        val listCancel = ArrayList<SimpleTaskWithID>()
        val listNoDate = ArrayList<SimpleTaskWithID>()
        //过滤数据，只关心部分数据
        for (i in TaskDao.getAllSimpleTasks()) {
            if (i.status == TaskStatus.CANCELED) {
                if (i.hasTime) {
                    listCancel.add(i)
                } else {
                    listNoDate.add(i)
                }
            }
        }
        listCancel.sortBy { i ->
            i.beginTime
        }
        listNoDate.sortBy { i ->
            i.id
        }
        listNoDate.reverse()
        list.addAll(listNoDate + listCancel)
        return list
    }
}
