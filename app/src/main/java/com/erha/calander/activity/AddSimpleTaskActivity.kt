package com.erha.calander.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.erha.calander.R
import com.erha.calander.dao.ConfigDao
import com.erha.calander.dao.SecretKeyDao
import com.erha.calander.dao.TaskDao
import com.erha.calander.databinding.ActivityAddSimpleTaskBinding
import com.erha.calander.model.SimpleTaskWithoutID
import com.erha.calander.model.TaskStatus
import com.erha.calander.type.EventType
import com.erha.calander.util.*
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.impl.LoadingPopupView
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.contourColorInt
import com.qmuiteam.qmui.util.QMUIDisplayHelper
import com.squareup.okhttp.*
import com.yalantis.ucrop.UCrop
import dev.sasikanth.colorsheet.ColorSheet
import es.dmoral.toasty.Toasty
import okio.Buffer
import okio.BufferedSink
import okio.Source
import okio.source
import org.greenrobot.eventbus.EventBus
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

data class TaskTimeAndNotify(
    var hasTime: Boolean = false,
    var date: Calendar = CalendarUtil.getWithoutTime(),
    var isAllDay: Boolean = false,
    var beginTime: Calendar = CalendarUtil.getWithoutSecond(),
    var endTime: Calendar = CalendarUtil.getWithoutSecond(),
    var isDDL: Boolean = false,
    var notifyTimes: ArrayList<Int> = ArrayList()
)

class AddSimpleTaskActivity : AppCompatActivity() {

    //布局binding
    private lateinit var binding: ActivityAddSimpleTaskBinding
    private lateinit var store: TinyDB
    private var taskTimeAndNotify = TaskTimeAndNotify()

    private lateinit var progressUploadFile: ProgressUploadFile

    private var isCustomColor = false
    private var customColor = "#FFFFFF"
    private val colorStrings = listOf(
        "#ec6666",
        "#f2b04b",
        "#ffd966",
        "#dde358",
        "#93c47d",
        "#5dd1a8",
        "#52b8d2",
        "#5992f8",
        "#9f4cef",
        "#d25294"
    )
    private var editorTextColorInt = Color.parseColor("#000000")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddSimpleTaskBinding.inflate(layoutInflater)
        store = TinyDB(binding.root.context)
        setContentView(binding.root)

        window.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = getColor(R.color.default_background_color)
            decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        binding.backButton.setOnClickListener { v ->
            run {
                setResult(Activity.RESULT_CANCELED, Intent())
                finish()
            }
        }
        intent?.apply {
            if (getBooleanExtra("calendarLongClickAddTask", false)) {
                var calendar = getSerializableExtra("date") as Calendar
                var date = CalendarUtil.getWithoutTime(calendar)
                var beginTime = CalendarUtil.getWithoutSecond(calendar)
                var endTime = CalendarUtil.getWithoutSecond(calendar)
                taskTimeAndNotify.hasTime = true
                taskTimeAndNotify.date = date
                endTime.apply {
                    add(Calendar.MINUTE, 120)
                }
                if (CalendarUtil.compareOnlyTime(beginTime, endTime) > 0) {
                    endTime = beginTime
                }
                taskTimeAndNotify.beginTime = beginTime
                taskTimeAndNotify.endTime = endTime
            }
        }
        binding.editor.apply {
            setPadding(5, 10, 10, 10)
            setTextColor(resources.getColor(R.color.black))
            setPlaceholder("任务说明")
            setOnTextChangeListener { html ->
                Log.e("editor", html)
            }
            binding.boldButton.setOnClickListener {
                this.setBold()
            }
            binding.italicButton.setOnClickListener {
                this.setItalic()
            }
            binding.redoButton.setOnClickListener {
                this.redo()
            }
            binding.undoButton.setOnClickListener {
                this.undo()
            }
            binding.todoButton.setOnClickListener {
                this.insertTodo()
            }
        }
        binding.imageButton.apply {
            setOnClickListener {
                try {
                    val i = Intent()
                    i.addCategory(Intent.CATEGORY_OPENABLE)
                    i.type = "image/*"
                    i.action = Intent.ACTION_GET_CONTENT
                    startActivityForResult(
                        Intent.createChooser(
                            i,
                            "选择图片"
                        ), 1000
                    )
                } catch (e: java.lang.Exception) {
                } catch (e: Exception) {
                }
            }
        }
        binding.editorColorButton.apply {
            setOnClickListener {
                val colors = IntArray(colorStrings.size) { index ->
                    when (index) {
                        0 -> Color.BLACK
                        else -> Color.parseColor(colorStrings[index - 1])
                    }
                }
                ColorSheet().colorPicker(
                    colors = colors,
                    selectedColor = editorTextColorInt,
                    noColorOption = false,
                    listener = { color ->
                        binding.editor.setTextColor(color)
                        editorTextColorInt = color
                        binding.editorColorButtonIcon.apply {
                            this.icon?.apply {
                                this.colorInt = editorTextColorInt
                                this.contourColorInt = editorTextColorInt
                            }
                        }
                    })
                    .show(supportFragmentManager)
            }
        }
        binding.editorColorButtonIcon.apply {
            this.icon?.apply {
                this.colorInt = editorTextColorInt
                this.contourColorInt = editorTextColorInt
            }
        }
        binding.taskTime.apply {
            setOnClickListener {
                val i = Intent(this@AddSimpleTaskActivity, SelectSimpleTaskTimeActivity::class.java)
                taskTimeAndNotify.apply {
                    i.putExtra("fromAddTask", true)
                    i.putExtra("hasTime", taskTimeAndNotify.hasTime)
                    i.putExtra("date", taskTimeAndNotify.date)
                    i.putExtra("isAllDay", taskTimeAndNotify.isAllDay)
                    i.putExtra("beginTime", taskTimeAndNotify.beginTime)
                    i.putExtra("endTime", taskTimeAndNotify.endTime)
                    i.putExtra("isDDL", taskTimeAndNotify.isDDL)
                    i.putExtra("notifyTimes", taskTimeAndNotify.notifyTimes)
                }
                startActivityForResult(i, SelectSimpleTaskTime.requestCode)
            }
        }
        binding.submitZone.apply {
            setOnClickListener {
                var html = ""
                try {
                    html = binding.editor.html
                } catch (e: Exception) {
                } catch (e: java.lang.Exception) {
                }
                val status = when (binding.checkbox.state) {
                    null -> TaskStatus.CANCELED
                    true -> TaskStatus.FINISHED
                    else -> TaskStatus.ONGOING
                }
                val i = SimpleTaskWithoutID(
                    status = status,
                    title = binding.taskTitle.text.toString(),
                    detailHtml = html,
                    hasTime = taskTimeAndNotify.hasTime,
                    date = CalendarUtil.getWithoutTime(taskTimeAndNotify.date),
                    isAllDay = taskTimeAndNotify.isAllDay,
                    beginTime = CalendarUtil.getWithoutSecond(taskTimeAndNotify.beginTime),
                    endTime = CalendarUtil.getWithoutSecond(taskTimeAndNotify.endTime),
                    isDDL = taskTimeAndNotify.isDDL,
                    notifyTimes = taskTimeAndNotify.notifyTimes,
                    customColor = isCustomColor,
                    color = customColor
                )
                Thread {
                    TaskDao.addSimpleTask(i)
                    EventBus.getDefault().post(EventType.EVENT_CHANGE)
                }.start()
                Toasty.success(binding.root.context, "添加成功", Toast.LENGTH_SHORT, false).show()
                finish()
            }
        }
        binding.checkbox.setOnClickListener {
            binding.checkbox.apply {
                if (isIndeterminate) {
                    isIndeterminate = false
                    isChecked = false
                }
            }
        }
        binding.checkbox.setOnLongClickListener {
            Toasty.info(binding.root.context, "任务状态 -> 已放弃", Toast.LENGTH_SHORT, false).show()
            binding.checkbox.isIndeterminate = true
            true
        }
        binding.colorQMUILinearLayout.apply {
            setOnClickListener {
                val colors = IntArray(colorStrings.size) { index ->
                    Color.parseColor(colorStrings[index])
                }
                ColorSheet().colorPicker(
                    colors = colors,
                    noColorOption = true,
                    selectedColor = if (isCustomColor) Color.parseColor(customColor) else null,
                    listener = { color ->
                        if (color == ColorSheet.NO_COLOR) {
                            isCustomColor = false
                        } else {
                            isCustomColor = true
                            customColor = java.lang.String.format("#%06X", 0xFFFFFF and color)
                        }
                        if (isCustomColor) {
                            binding.colorQMUILinearLayout.setBackgroundColor(
                                Color.parseColor(
                                    customColor
                                )
                            )
                            radius = QMUIDisplayHelper.dp2px(binding.root.context, 30)
                            binding.colorIcon.visibility = View.INVISIBLE
                        } else {
                            binding.colorQMUILinearLayout.setBackgroundColor(resources.getColor(R.color.white))
                            radius = QMUIDisplayHelper.dp2px(binding.root.context, 30)
                            binding.colorIcon.visibility = View.VISIBLE
                        }
                    })
                    .show(supportFragmentManager)
            }
            if (isCustomColor) {
                setBackgroundColor(Color.parseColor(customColor))
                radius = QMUIDisplayHelper.dp2px(binding.root.context, 30)
                binding.colorIcon.visibility = View.INVISIBLE
            } else {
                binding.colorQMUILinearLayout.setBackgroundColor(resources.getColor(R.color.white))
                radius = QMUIDisplayHelper.dp2px(binding.root.context, 30)
                binding.colorIcon.visibility = View.VISIBLE
            }
        }
        initLoadingPopup()
        progressUploadFile = ProgressUploadFile(binding.root.context, this)
        updateTimeTextview()
        loadGuide()
    }

    private fun loadGuide() {
        val guideVersion = 1
        if (!ConfigDao.isDisplayingAnyGuide && !GuideUtil.getGuideStatus(
                binding.root.context,
                this.javaClass.name,
                guideVersion
            )
        ) {
            ConfigDao.isDisplayingAnyGuide = true
            val list = listOf(
                GuideEntity(
                    view = binding.taskTitle,
                    title = "创建任务",
                    text = "从填写任务标题开始\n它可以为空"
                ),
                GuideEntity(
                    view = binding.editorGuideZone,
                    title = "任务说明",
                    text = "在这里添加任务说明、细节等"
                ),
                GuideEntity(
                    view = binding.editorLeftButtonsLinearLayout,
                    title = "任务说明",
                    text = "支持富文本编辑~"
                ),
                GuideEntity(
                    view = binding.checkbox,
                    title = "任务状态",
                    text = "点按 -> 改变完成状态\n长按 -> 取消（放弃）状态"
                ),
                GuideEntity(
                    view = binding.taskTime,
                    title = "任务时间与提醒",
                    text = "你可以设置它的时间\n以及此任务的提醒时机等"
                ),
                GuideEntity(
                    view = binding.colorConstraintLayout,
                    title = "颜色",
                    text = "这个任务在日历中的颜色\n默认为随机抽取"
                ),
                GuideEntity(
                    view = binding.submitZone,
                    title = "大功告成",
                    text = "创建完不要忘记点它哟~"
                )
            )
            ConfigDao.isDisplayingAnyGuide = false
            var i = 0
            GuideUtil.getDefaultBuilder(this, list[i++])
                .setGuideListener {
                    GuideUtil.getDefaultBuilder(this, list[i++])
                        .setGuideListener {
                            GuideUtil.getDefaultBuilder(this, list[i++])
                                .setGuideListener {
                                    GuideUtil.getDefaultBuilder(this, list[i++])
                                        .setGuideListener {
                                            GuideUtil.getDefaultBuilder(this, list[i++])
                                                .setGuideListener {
                                                    GuideUtil.getDefaultBuilder(this, list[i++])
                                                        .setGuideListener {
                                                            GuideUtil.getDefaultBuilder(
                                                                this,
                                                                list[i++]
                                                            )
                                                                .setGuideListener {
                                                                    GuideUtil.updateGuideStatus(
                                                                        binding.root.context,
                                                                        this.javaClass.name,
                                                                        guideVersion
                                                                    )
                                                                }.build().show()

                                                        }.build().show()
                                                }.build().show()
                                        }.build().show()
                                }.build().show()
                        }.build().show()
                }.build().show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == SelectSimpleTaskTime.requestCode) {
            data?.apply {
                extras?.apply {
                    taskTimeAndNotify.hasTime = getBoolean("hasTime")
                    taskTimeAndNotify.date = getSerializable("date") as Calendar
                    taskTimeAndNotify.isAllDay = getBoolean("isAllDay")
                    taskTimeAndNotify.beginTime = getSerializable("beginTime") as Calendar
                    taskTimeAndNotify.endTime = getSerializable("endTime") as Calendar
                    taskTimeAndNotify.isDDL = getBoolean("isDDL")
                    taskTimeAndNotify.notifyTimes = getSerializable("notifyTimes") as ArrayList<Int>
                }
                Log.e("回传成功", "${taskTimeAndNotify.date}")
                updateTimeTextview()
            }
        }
        if (resultCode == RESULT_OK && requestCode == 1000) {
            data?.apply {
                val id = "cropTemp"
                val dir = File(binding.root.context.filesDir, "images")
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                val file = File(dir, id)
                val selectedImageUri: Uri? = data.data
                selectedImageUri?.apply {
                    UCrop.of(selectedImageUri, file.toUri())
                        .start(this@AddSimpleTaskActivity);
                }
            }
        }
        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            //用户裁剪图片后
            Log.e("用户裁剪完成", "${UCrop.REQUEST_CROP}")
            data?.apply {
                val selectedImageUri: Uri? = UCrop.getOutput(data)
                try {
                    if (selectedImageUri != null) {
                        progressUploadFile.fileUri = selectedImageUri
                        progressUploadFile.start()
                    }
                } catch (e: java.lang.Exception) {
                } catch (e: Exception) {
                }
            }
        }
    }

    private fun updateTimeTextview() {
        binding.taskTime.apply {
            if (taskTimeAndNotify.hasTime) {
                taskTimeAndNotify.beginTime.apply {
                    set(Calendar.YEAR, taskTimeAndNotify.date.get(Calendar.YEAR))
                    set(Calendar.DAY_OF_YEAR, taskTimeAndNotify.date.get(Calendar.DAY_OF_YEAR))
                }
                taskTimeAndNotify.endTime.apply {
                    set(Calendar.YEAR, taskTimeAndNotify.date.get(Calendar.YEAR))
                    set(Calendar.DAY_OF_YEAR, taskTimeAndNotify.date.get(Calendar.DAY_OF_YEAR))
                }
                if (taskTimeAndNotify.isAllDay) {
                    taskTimeAndNotify.beginTime =
                        CalendarUtil.getWithoutTime(taskTimeAndNotify.date)
                    taskTimeAndNotify.endTime =
                        CalendarUtil.getWithoutTime(taskTimeAndNotify.date).apply {
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                        }
                }
                text = CalendarUtil.getClearDateTimeText(
                    taskTimeAndNotify.beginTime,
                    taskTimeAndNotify.endTime
                )
                if (taskTimeAndNotify.isDDL) {
                    setTextColor(resources.getColor(R.color.dark_orange))
                } else {
                    setTextColor(resources.getColor(R.color.default_active))
                }
            } else {
                text = "设置任务时间与提醒"
                setTextColor(resources.getColor(R.color.default_active))
            }
        }
    }

    lateinit var loadingPopup: LoadingPopupView
    var loadingPopupInit = false
    private fun initLoadingPopup() {
        if (loadingPopupInit) {
            return
        }
        loadingPopup = XPopup.Builder(binding.root.context)
            .autoDismiss(false)
            .asLoading("加载中")
        loadingPopupInit = true
    }

    class ProgressUploadFile(
        private var context: Context,
        private var addSimpleTaskActivity: AddSimpleTaskActivity
    ) {
        lateinit var fileUri: Uri
        fun start() {
            var thread = Thread {
                Log.e("upload", "begin Upload")
                run()
                addSimpleTaskActivity.runOnUiThread {
                    addSimpleTaskActivity.loadingPopup.show()
                }
            }.start()
        }

        private fun run() {
            val builder = MultipartBuilder().type(MultipartBuilder.FORM)
            Log.e("upload", FileUtil.getFilePathByUri(context, fileUri))
            val file = File(FileUtil.getFilePathByUri(context, fileUri))
            builder.addFormDataPart(
                "file",
                file.name,
                createCustomRequestBody(MultipartBuilder.FORM, file, object : ProgressListener {
                    override fun onProgress(totalBytes: Long, remainingBytes: Long, done: Boolean) {
                        var percent = (totalBytes - remainingBytes) * 100 / totalBytes
                        addSimpleTaskActivity.runOnUiThread {
                            if (percent >= 99) {
                                addSimpleTaskActivity.loadingPopup.setTitle("等待服务器响应")
                            } else {
                                addSimpleTaskActivity.loadingPopup.setTitle("上传中 ${percent}%")
                            }
                        }
                        Log.e(
                            "upload",
                            ((totalBytes - remainingBytes) * 100 / totalBytes).toString() + "%"
                        )
                    }
                })
            )
            val requestBody = builder.build()
            val request = Request.Builder()
                .url("https://api.superbed.cn/upload?token=${SecretKeyDao.SuperbedImageUploadToken}") //地址
                .post(requestBody)
                .build()
            okHttpClient.apply {
                setConnectTimeout(50, TimeUnit.MINUTES)
                setWriteTimeout(50, TimeUnit.MINUTES)
                setReadTimeout(50, TimeUnit.MINUTES)
            }
            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(request: Request, e: IOException) {
                    Log.e("upload", "fail")
                }

                @Throws(IOException::class)
                override fun onResponse(response: Response) {
                    try {
                        val json = response.body().string()
                        Log.e("upload", "response.body().string() = $json")
                        val rst = JSONObject(json)
                        val code = rst.getInt("err")
                        if (code == 1) {
                            addSimpleTaskActivity.runOnUiThread {
                                Toasty.success(
                                    addSimpleTaskActivity.binding.root.context,
                                    "添加失败",
                                    Toast.LENGTH_LONG
                                ).show()
                                addSimpleTaskActivity.loadingPopup.dismiss()
                            }
                        } else if (code == 0 && rst.getJSONObject("urls").length() != 0) {
                            addSimpleTaskActivity.runOnUiThread {
                                Toasty.success(
                                    addSimpleTaskActivity.binding.root.context,
                                    "添加成功",
                                    Toast.LENGTH_LONG
                                ).show()
                                addSimpleTaskActivity.loadingPopup.dismiss()
                            }
                            val data = rst.getJSONObject("urls")
                            val keys: Iterator<String> = data.keys()
                            while (keys.hasNext()) {
                                val key = keys.next()
                                var url = data.getString(key)
                                //拿到图片的URL了
                                addSimpleTaskActivity.runOnUiThread {
                                    val displayMetrics =
                                        context.resources.displayMetrics
                                    val dpWidth: Float =
                                        displayMetrics.widthPixels / displayMetrics.density
                                    addSimpleTaskActivity.binding.editor.insertImage(
                                        url,
                                        "用户上传的图片",
                                        dpWidth.toInt() - 60
                                    )
                                }
                                break
                            }
                        }
                    } catch (e: java.lang.Exception) {
                        addSimpleTaskActivity.runOnUiThread {
                            Toasty.success(
                                addSimpleTaskActivity.binding.root.context,
                                "添加失败",
                                Toast.LENGTH_LONG
                            ).show()
                            addSimpleTaskActivity.loadingPopup.dismiss()
                        }
                    }
                }
            })
        }

        interface ProgressListener {
            fun onProgress(totalBytes: Long, remainingBytes: Long, done: Boolean)
        }

        companion object {
            private val okHttpClient = OkHttpClient()
            fun createCustomRequestBody(
                contentType: MediaType,
                file: File,
                listener: ProgressListener
            ): RequestBody {
                return object : RequestBody() {
                    override fun contentType(): MediaType {
                        return contentType
                    }

                    override fun contentLength(): Long {
                        return file.length()
                    }

                    @Throws(IOException::class)
                    override fun writeTo(sink: BufferedSink) {
                        val source: Source
                        try {
                            source = file.source()
                            val buf = Buffer()
                            var remaining = contentLength()
                            var readCount: Long
                            while (source.read(buf, 2048).also { readCount = it } != -1L) {
                                sink.write(buf, readCount)
                                listener.onProgress(
                                    contentLength(),
                                    readCount.let { remaining -= it; remaining },
                                    remaining == 0L
                                )
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }
}