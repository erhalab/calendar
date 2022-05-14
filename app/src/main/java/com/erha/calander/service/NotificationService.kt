package com.erha.calander.service

import ando.file.core.FileOperator
import android.annotation.SuppressLint
import android.app.*
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.os.*
import android.provider.Settings
import android.util.Log
import cn.authing.guard.Authing
import com.erha.calander.BuildConfig
import com.erha.calander.R
import com.erha.calander.activity.HomeActivity
import com.erha.calander.activity.SplashActivity
import com.erha.calander.dao.*
import com.erha.calander.type.EventType
import com.erha.calander.type.LocalStorageKey
import com.erha.calander.type.MainActivityIntentType
import com.erha.calander.type.NotificationChannelType
import com.erha.calander.util.TinyDB
import io.karn.notify.Notify
import io.karn.notify.internal.utils.Action
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.text.SimpleDateFormat
import java.util.*


class NotificationService : Service() {
    private var mNM: NotificationManager? = null
    private var mStartForeground: Method? = null
    private var mStopForeground: Method? = null
    private val mStartForegroundArgs = arrayOfNulls<Any>(2)
    private val mStopForegroundArgs = arrayOfNulls<Any>(1)
    private lateinit var store: TinyDB
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.apply {
            intent.extras?.apply {
                if (this.getInt("clearOneTaskNotificationButtonClick") != 0) {
                    Log.e("用户点击了推送不再通知的按钮", this.getInt("userClearNotification").toString())
                    (applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancel(
                        this.getInt("clearOneTaskNotificationButtonClick")
                    )
                    NotificationDao.removeOneTaskAllNotificationsById4Service(this.getInt("clearOneTaskNotificationButtonClick"))
                }
                if (this.getInt("userClearNotification") != 0) {
                    Log.e("通知被用户划掉", this.getInt("userClearNotification").toString())
                    (applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancel(
                        this.getInt("userClearNotification")
                    )
                    if (store.getBoolean(LocalStorageKey.CLEAR_ONE_NOTIFICATION_EQUAL_CLEAR_TASK_NOTIFICATIONS)) {
                        NotificationDao.removeOneTaskAllNotificationsById4Service(this.getInt("userClearNotification"))
                    }
                }

            }
        }
        Log.e("onStartCommand 服务被访问", this.javaClass.name)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()

        notificationManager =
            applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        mNM = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        try {
            mStartForeground = NotificationService::class.java.getMethod(
                "startForeground",
                *mStartForegroundSignature
            )
            mStopForeground = NotificationService::class.java.getMethod(
                "stopForeground",
                *mStopForegroundSignature
            )
        } catch (e: NoSuchMethodException) {
            mStopForeground = null
            mStartForeground = mStopForeground
        }
        // 我们并不需要为 notification.flags 设置 FLAG_ONGOING_EVENT，因为
        // 前台服务的 notification.flags 总是默认包含了那个标志位
        val notification = Notification()
        // 注意使用 startForeground ，id 为 0 将不会显示 notification,1显示
        startForegroundCompat(0, notification)

        //看看服务什么时候被启用的
        Log.e("onCreate 服务被创建", this.javaClass.name)

        Log.e("ConfigDao.hadRunService", ConfigDao.hadRunService.toString())

        //初始化
        store = TinyDB(appContext = applicationContext)


        //加锁，避免Service被重复创建
        Thread {
            synchronized(ConfigDao) {
                if (!ConfigDao.hadRunService) {
                    ConfigDao.hadRunService = true

                    //初始化文件读取类
                    FileOperator.init(application, BuildConfig.DEBUG)
                    //初始化课程Dao
                    CourseDao.load(
                        application.filesDir,
                        store.getString(LocalStorageKey.COURSE_FIRST_DAY),
                        store.getString(LocalStorageKey.COURSE_NOTIFY_TIME)
                    )
                    //初始化Task Dao
                    TaskDao.load(applicationContext)
                    //初始化通知Dao
                    NotificationDao.repostOneTaskNotification =
                        store.getBoolean(LocalStorageKey.REPOST_ONE_TASK_NOTIFICATION)
                    //初始化登录
                    Authing.init(applicationContext, SecretKeyDao.AuthingAppID)
                    Log.e("Authing.init", this.javaClass.name)
                    //初始化渠道
                    initNotificationChannel()
                    //设置Notify默认渠道
                    Notify.defaultConfig {
                        header {
                            icon = R.mipmap.ic_launcher_round
                        }
                        alerting(NotificationChannelType.DEFAULT) {}
                    }
                    Thread(r).start()
                    //加载动态快捷方式
                    setupShortcuts()
                }
            }
        }.start()
    }

    private fun setupShortcuts() {
        getSystemService(ShortcutManager::class.java).addDynamicShortcuts(
            listOf(
                createCalendarShortcutInfo(),
                createDeadlineShortcutInfo()
            )
        )
    }

    private fun createCalendarShortcutInfo(): ShortcutInfo {
        val intent = Intent(this, SplashActivity::class.java)
        intent.action = Intent.ACTION_VIEW
        intent.putExtra("defaultPage", "日历")
        return ShortcutInfo.Builder(this, "calendarShortcut")
            .setShortLabel("日历")
            .setLongLabel("日历视图")
            .setIcon(Icon.createWithResource(this, R.drawable.ic_round_calendar_month_24))
            .setIntent(intent)
            .build()
    }

    private fun createDeadlineShortcutInfo(): ShortcutInfo {
        val intent = Intent(this, SplashActivity::class.java)
        intent.action = Intent.ACTION_VIEW
        intent.putExtra("defaultPage", "里程碑")
        return ShortcutInfo.Builder(this, "deadlineShortcut")
            .setShortLabel("DDL")
            .setLongLabel("DDL时间线")
            .setIcon(Icon.createWithResource(this, R.drawable.ic_round_flag_24))
            .setIntent(intent)
            .build()
    }

    private fun initNotificationChannel(
        notificationManager: NotificationManager = applicationContext.getSystemService(
            NOTIFICATION_SERVICE
        ) as NotificationManager
    ) {
        //创建通知渠道
        val channels: ArrayList<NotificationChannel> = ArrayList()
        channels.add(
            NotificationChannel(
                NotificationChannelType.DEFAULT,
                "默认通知",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "没有被分类的通知"
                //通知声音
                setSound(
                    Settings.System.DEFAULT_NOTIFICATION_URI,
                    Notification.AUDIO_ATTRIBUTES_DEFAULT
                )
                //呼吸灯
                enableLights(true)
                lightColor = Notification.DEFAULT_LIGHTS
                //震动
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
                setShowBadge(true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    this.setAllowBubbles(true)
                }
                //锁屏可见性
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        )
        channels.add(
            NotificationChannel(
                NotificationChannelType.COURSE,
                "课程提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "所有的课程提醒~"
                //通知声音
                setSound(
                    Settings.System.DEFAULT_NOTIFICATION_URI,
                    Notification.AUDIO_ATTRIBUTES_DEFAULT
                )
                //呼吸灯
                enableLights(true)
                lightColor = Notification.DEFAULT_LIGHTS
                //震动
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
                setShowBadge(true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    this.setAllowBubbles(true)
                }
                //锁屏可见性
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        )
        channels.add(
            NotificationChannel(
                NotificationChannelType.SIMPlE_EVENT,
                "普通任务提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "普通的任务会通过此渠道发布通知"
                //通知声音
                setSound(
                    Settings.System.DEFAULT_NOTIFICATION_URI,
                    Notification.AUDIO_ATTRIBUTES_DEFAULT
                )
                //呼吸灯
                enableLights(true)
                lightColor = Notification.DEFAULT_LIGHTS
                //震动
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
                setShowBadge(true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    this.setAllowBubbles(true)
                }
                //锁屏可见性
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        )
        notificationManager.createNotificationChannels(channels)
    }


    private val r = Runnable {
        while (true) {
            handler.sendEmptyMessage(0)
            try {

                val nextCalendar = Calendar.getInstance()
                nextCalendar.apply {
                    //下一分钟，秒和毫秒都为0
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    add(Calendar.MINUTE, 1)
                }
                val nowCalendar = Calendar.getInstance()
                val s = (nextCalendar.timeInMillis - nowCalendar.timeInMillis)
                Log.e("通知休眠 -> mills ", s.toString())
                Thread.sleep(s)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForegroundCompat(1)
        notificationManager.cancelAll()
        Log.e("onDestroy 服务被销毁", this.javaClass.name)
    }

    // 以兼容性方式开始前台服务
    private fun startForegroundCompat(id: Int, n: Notification) {
        if (mStartForeground != null) {
            mStartForegroundArgs[0] = id
            mStartForegroundArgs[1] = n
            try {
                mStartForeground!!.invoke(this, *mStartForegroundArgs)
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            } catch (e: InvocationTargetException) {
                e.printStackTrace()
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            }
            return
        }
        mNM!!.notify(id, n)
    }

    // 以兼容性方式停止前台服务
    private fun stopForegroundCompat(id: Int) {
        if (mStopForeground != null) {
            mStopForegroundArgs[0] = java.lang.Boolean.TRUE
            try {
                mStopForeground!!.invoke(this, *mStopForegroundArgs)
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            } catch (e: InvocationTargetException) {
                e.printStackTrace()
            }
            return
        }
        // 在 setForeground 之前调用 cancel，因为我们有可能在取消前台服务之后
        // 的那一瞬间被kill掉。这个时候 notification 便永远不会从通知一栏移除
        mNM!!.cancel(id)
    }

    var handler: Handler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            sendNotification()
        }
    }

    // Create an explicit intent for an Activity in your app
    private lateinit var notificationManager: NotificationManager

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onEvent(str: String?) {
        when (str) {
            EventType.NOTIFICATION_SERVICE_REMOVE_NOTIFICATIONS -> removeNotification()
        }
    }

    private fun removeNotification() {
        for (i in
        NotificationDao.getNowShouldDismissNotifications(notificationManager.activeNotifications)
        ) {
            notificationManager.cancel(i.id)
        }
    }


    private fun sendNotification() {
        Log.e("获取新通知前，当前展示的条数", notificationManager.activeNotifications.size.toString())
        Log.e(
            "获取通知",
            SimpleDateFormat.getDateTimeInstance().format(Calendar.getInstance().timeInMillis)
        )
        val notifications =
            NotificationDao.getNowShouldDisplayNotifications(notificationManager.activeNotifications)

        notifications["DISMISS"]?.apply {
            for (n in this) {
                notificationManager.cancel(n.notificationId)
            }
        }
        notifications["POST"]?.apply {
            for (n in this) {
                if (n is SimpleNotification) {
                    val intent = Intent(this@NotificationService, HomeActivity::class.java)
                    val bundle = Bundle()
                    bundle.putString("type", MainActivityIntentType.CLICK_EVENT_NOTIFICATION)
                    bundle.putInt("id", n.notificationId)
                    intent.putExtras(bundle)
                    val pendingIntent: PendingIntent =
                        PendingIntent.getActivity(
                            this@NotificationService,
                            ("9" + n.notificationId.toString()).toInt(),
                            intent,
                            FLAG_IMMUTABLE
                        )
                    val i = Notify
                        .with(this@NotificationService)
                        .meta {
                            clearIntent = PendingIntent.getService(
                                this@NotificationService,
                                "2${n.notificationId}".toInt(),
                                Intent(this@NotificationService, NotificationService::class.java)
                                    .putExtra("userClearNotification", n.notificationId),
                                FLAG_IMMUTABLE
                            )
                        }
                        .alerting(n.channel) {
                        }
                        .content { // this: Payload.Content.Default
                            title = n.title
                            text = n.text
                            //添加icon
                            largeIcon =
                                BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher_round)
                        }
                        .actions {
                            add(
                                Action(
                                    R.mipmap.ic_launcher_round,
                                    "不再提醒该事件",
                                    PendingIntent.getService(
                                        this@NotificationService,
                                        "1${n.notificationId}".toInt(),
                                        Intent(
                                            this@NotificationService,
                                            NotificationService::class.java
                                        )
                                            .putExtra(
                                                "clearOneTaskNotificationButtonClick",
                                                n.notificationId
                                            ),
                                        FLAG_IMMUTABLE
                                    )
                                )
                            )
                        }
                        .asBuilder().build()
                    i.contentIntent = pendingIntent
                    notificationManager.notify(n.notificationId, i)
                }
            }
        }

    }


    companion object {
        private val mStartForegroundSignature = arrayOf(
            Int::class.javaPrimitiveType, Notification::class.java
        )
        private val mStopForegroundSignature = arrayOf<Class<*>?>(Boolean::class.javaPrimitiveType)
    }
}