package com.erha.calander.fragment.task

import com.erha.calander.dao.TaskDao
import com.erha.calander.fragment.TaskFragment
import com.erha.calander.model.SimpleTaskWithID
import com.erha.calander.model.TaskStatus
import java.util.*

class TaskAllDoneFragment : TaskFragment() {

    override fun getSmallTitleText(): String {
        return "所有,已完成"
    }

    override fun getDate4ThisPage(): ArrayList<SimpleTaskWithID> {
        val list = ArrayList<SimpleTaskWithID>()
        val listDone = ArrayList<SimpleTaskWithID>()
        val listNoDate = ArrayList<SimpleTaskWithID>()
        //过滤数据，只关心部分数据
        for (i in TaskDao.getAllSimpleTasks()) {
            if (i.status == TaskStatus.FINISHED) {
                if (i.hashTime) {
                    SimpleTaskWithID.copy(i).apply {
                        this.beginTime.apply {
                            set(Calendar.YEAR, i.date.get(Calendar.YEAR))
                            set(Calendar.MONTH, i.date.get(Calendar.MONTH))
                            set(Calendar.DAY_OF_MONTH, i.date.get(Calendar.DAY_OF_MONTH))
                        }
                        listDone.add(this)
                    }
                } else {
                    listNoDate.add(SimpleTaskWithID.copy(i))
                }
            }
        }
        listDone.sortBy { i ->
            i.beginTime
        }
        listNoDate.sortBy { i ->
            i.id
        }
        listNoDate.reverse()
        list.addAll(listNoDate + listDone)
        return list
    }
}