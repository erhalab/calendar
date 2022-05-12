package com.erha.calander.dao

import android.service.notification.StatusBarNotification
import android.util.Log
import com.erha.calander.type.EventType
import com.erha.calander.type.NotificationChannelType
import org.greenrobot.eventbus.EventBus
import java.text.SimpleDateFormat
import java.util.*

//注意，这是一条通知。所以只能有一个推送时间
open class BaseNotification {
    var notificationId: Int = -1
    var taskName: String = ""
    var notifyTime: Calendar = Calendar.getInstance()
    var channel: String = NotificationChannelType.DEFAULT

    init {
        notifyTime.add(Calendar.YEAR, -1)
    }

    override fun toString(): String {
        return "$notificationId ${taskName} ${
            SimpleDateFormat(
                "yyyy-MM-dd HH:mm",
                Locale.getDefault()
            ).format(notifyTime.timeInMillis)
        }"
    }
}

//最普通的通知
open class SimpleNotification : BaseNotification() {
    var title: String = "默认Title"
    var text: String = "默认Text"
}

//可以展开细节的通知
open class BigTextNotification : SimpleNotification() {
    var expandedText: String = "默认Text"
    var bigText: String = "默认细节文字"
}

//这是一个任务的通知，可以有若干的推送时间
open class SimpleTaskNotification {
    var taskName: String = ""
    var title: String = "默认Title"
    var text: String = "默认Text"
    var notifyTimes: ArrayList<Int> = ArrayList()
    var beginTime: String = "2022-04-10 04:10"
    var channel: String = NotificationChannelType.DEFAULT

    override fun equals(other: Any?): Boolean {
        if (other is SimpleTaskNotification) {
            return other.taskName == this.taskName
        }
        return super.equals(other)
    }
}

object NotificationDao {
    private var counter = 0

    //默认下关闭。关闭意味着，如果一个任务的通知已经发布且用户没有划掉，新的通知不会发布，不会再次提醒用户
    var repostOneTaskNotification = false
    private var notifications: ArrayList<BaseNotification> = ArrayList()

    //用来！暂时！保存被移除的通知
    private var removedNotifications: ArrayList<BaseNotification> = ArrayList()

    //所有的通知，都是应用启动时动态获取的，不是通过存储实现的
    //队列要按照时间排序
    private fun sort() {
        notifications.sortBy { notification -> notification.notifyTime }
    }

    //添加一个常规任务的通知
    fun addOne(simpleTaskNotification: SimpleTaskNotification) {
        add(simpleTaskNotification)
        sort()
    }

    fun addAll(simpleTaskNotifications: List<SimpleTaskNotification>) {
        for (s in simpleTaskNotifications) {
            add(s)
        }
        sort()
    }
    //删除通知的流程
    //有人发起删除通知请求 -> Dao根据任务名加入到删除队列 -> 发送通知给Service
    // -> Service调用Dao，获取它应该在通知栏中移除的通知，然后移除他们 -> Dao清空删除队列

    fun removeAllByTaskNames(names: ArrayList<String>) {
        Log.e("removeAllByTaskNames", names.size.toString())
        //拷贝一份，用来遍历。不能遍历A又移除A中的元素
        var list = ArrayList<BaseNotification>(notifications)
        for (l in list) {
            for (name in names) {
                if (name == l.taskName) {
                    removedNotifications.add(l)
                    notifications.remove(l)
                    //Log.e("从通知队列中移除，加入到删除队列", l.toString())
                    break
                }
            }
        }
        //告诉通知服务，你要删除一些通知了
        EventBus.getDefault().post(EventType.NOTIFICATION_SERVICE_REMOVE_NOTIFICATIONS)
    }

    //处理用户点击某个通知的“不再提醒该事件”通知按钮
    fun removeOneTaskAllNotificationsById4Service(id: Int) {
        Log.e("removeOneTaskAllNotificationsById4Service", id.toString())
        var list = ArrayList<BaseNotification>(notifications)
        var taskName = ""
        for (n in notifications) {
            if (n.notificationId == id) {
                taskName = n.taskName
                break
            }
        }
        for (n in list) {
            if (n.taskName == taskName) {
                notifications.remove(n)
            }
        }
    }

    fun getNowShouldDismissNotifications(statusBarNotifications: Array<StatusBarNotification>): ArrayList<StatusBarNotification> {
        var list = ArrayList<StatusBarNotification>()
        for (l in statusBarNotifications) {
            //对于每条正在显示的通知l来说
            //如果它的id和删除队列中的id一样
            for (m in removedNotifications) {
                if (m.notificationId == l.id) {
                    Log.e("删除这条已经发布的通知", m.toString())
                    //这条已经发布的通知，在删除队列中找到了
                    list.add(l)
                    break
                }
            }
        }
        //清楚删除队列
        removedNotifications.clear()
        return list
    }


    private fun add(simpleTaskNotification: SimpleTaskNotification) {
        if (simpleTaskNotification.notifyTimes.size == 0) {
            Log.e("添加通知错误", simpleTaskNotification.taskName)
            return
        }
        var times = simpleTaskNotification.notifyTimes.toIntArray()
        //最大的值在前面呢
        times.sortDescending()
        for (t in times) {
            //通知时刻不允许是负数，不允许事件发生后才通知
            if (t < 0) {
                continue
            }
            var notifyTime = getCalendarByString(simpleTaskNotification.beginTime, t)
            //拿到了这个通知的时间
            if (getCalendarByNow() >= notifyTime) {
                //现在的时间，晚过了这个通知的时间。所以这个通知不可能被触发
//                Log.e("通知已过时", simpleTaskNotification.taskName + "${
//                    SimpleDateFormat(
//                        "yyyy-MM-dd HH:mm",
//                        Locale.getDefault()
//                    ).format(notifyTime.timeInMillis)
//                }")
                continue
            }
            //这个通知可以被添加进去了
            //开始构造通知
            var simpleNotification = SimpleNotification()
            simpleNotification.apply {
                notificationId = ++counter
                taskName = simpleTaskNotification.taskName
                title = simpleTaskNotification.title
                text = simpleTaskNotification.text
                this.notifyTime = notifyTime
                channel = simpleTaskNotification.channel
            }
            //添加到总队列中
            notifications.add(simpleNotification)
            //
            simpleNotification.apply {
//                Log.e(
//                    "创建新的通知",
//                    this.toString()
//                )
            }

        }
    }

    //获取这个时间点应该显示的通知
    //传入已经显示的通知，如果同属于一个TASK，那么不应该被显示
    fun getNowShouldDisplayNotifications(statusBarNotifications: Array<StatusBarNotification>): HashMap<String, ArrayList<BaseNotification>> {
        //方法内部千万不要改变其中的元素，这里是浅拷贝
        //拷贝一份总列表
        //TODO 提高通知去重的计算效率
        var rst = HashMap<String, ArrayList<BaseNotification>>()
        if (!repostOneTaskNotification) {
            var list = ArrayList<BaseNotification>(notifications)
            for (l in notifications) {
                // l是当前通知
                //先看看这个通知是不是当前时间要发布的
                if (l.notifyTime != getCalendarByNow()) {
                    list.remove(l)
                    continue
                }
                //判断这个通知所属的task有没有被发布过通知
                var taskName = l.taskName
                for (s in statusBarNotifications) {
                    //注意，你只能拿到这个通知的id，当时这个通知对应的task，依旧需要遍历查找
                    var findTaskName = "ERROR"
                    for (g in notifications) {
                        if (g.notificationId == s.id) {
                            findTaskName = g.taskName
                            Log.e("findTaskName", findTaskName)
                            //找到这条通知的任务了
                            break
                        }
                    }
                    if (findTaskName == taskName) {
                        //这条已经发布的通知所属task和当前一致，不能再发布一次
                        list.remove(l)
                        break
                    }
                }
            }
            rst["POST"] = list
            rst["DISMISS"] = ArrayList<BaseNotification>()
        } else {
            var list = ArrayList<BaseNotification>(notifications)
            var disMissList = ArrayList<BaseNotification>()
            for (l in notifications) {
                // l是当前通知
                //先看看这个通知是不是当前时间要发布的
                if (l.notifyTime != getCalendarByNow()) {
                    list.remove(l)
                    continue
                }
                //判断这个通知所属的task有没有被发布过通知
                var taskName = l.taskName
                for (s in statusBarNotifications) {
                    //注意，你只能拿到这个通知的id，当时这个通知对应的task，依旧需要遍历查找
                    var findTaskName = "ERROR"
                    for (g in notifications) {
                        if (g.notificationId == s.id) {
                            findTaskName = g.taskName
                            Log.e("findTaskName", findTaskName)
                            //找到这条通知的任务了
                            break
                        }
                    }
                    if (findTaskName == taskName) {
                        //这条已经发布的通知所属task和当前一致，那么，撤回已经发布的通知，发布当前这条
                        val b = BaseNotification()
                        b.apply {
                            notificationId = s.id
                        }
                        disMissList.add(b)
                        break
                    }
                }
            }
            rst["POST"] = list
            rst["DISMISS"] = disMissList
        }
        return rst
    }

    private fun getCalendarByNow(): Calendar {
        var calendar = Calendar.getInstance()
        calendar.apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar
    }

    private fun getCalendarByString(timeString: String, minutesOffset: Int = 0): Calendar {
        var calendar = Calendar.getInstance()
        calendar.apply {
            time = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(timeString)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, minutesOffset * -1)
        }
        return calendar
    }
}