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
import com.erha.calander.databinding.ActivityModifySimpleTaskBinding
import com.erha.calander.model.SimpleTaskWithID
import com.erha.calander.model.TaskStatus
import com.erha.calander.type.EventType
import com.erha.calander.util.*
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.impl.LoadingPopupView
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.contourColorInt
import com.qmuiteam.qmui.skin.QMUISkinManager
import com.qmuiteam.qmui.util.QMUIDisplayHelper
import com.qmuiteam.qmui.widget.dialog.QMUIDialog.MessageDialogBuilder
import com.qmuiteam.qmui.widget.dialog.QMUIDialogAction
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
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class ModifySimpleTaskActivity : AppCompatActivity() {

    //布局binding
    private lateinit var binding: ActivityModifySimpleTaskBinding
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

    private var simpleTaskId = -1

    private var isLoadFinished = false //如果用户快速打开页面（还没加载完成任务详情），然后又快速关闭，会导致关闭时自动保存了新任务

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityModifySimpleTaskBinding.inflate(layoutInflater)
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
            getIntExtra("simpleTaskId", -1).apply {
                if (this == -1) {
                    Log.e(this.javaClass.name, "谁打开了这个界面？没有传入普通任务ID")
                    finish()
                }
                val simpleTaskWithID = TaskDao.getSimpleTaskById(this)
                if (simpleTaskWithID == null) {
                    Log.e(this.javaClass.name, "谁打开了这个界面？不存在这个id的普通任务")
                    finish()
                }
                simpleTaskWithID?.apply {
                    simpleTaskId = id
                    binding.checkbox.state = when (status) {
                        TaskStatus.ONGOING -> false
                        TaskStatus.FINISHED -> true
                        else -> null
                    }
                    if (title.isBlank()) {
                        binding.taskTitle.hint = "无标题"
                    } else {
                        binding.taskTitle.setText(title)
                    }
                    try {
                        binding.editor.html = detailHtml
                    } catch (e: java.lang.Exception) {
                        Toasty.error(binding.root.context, "加载Editor失败", Toast.LENGTH_SHORT, false)
                            .show()
                    }
                    taskTimeAndNotify.hasTime = hasTime
                    taskTimeAndNotify.date = CalendarUtil.getWithoutTime(date)
                    taskTimeAndNotify.isAllDay = isAllDay
                    taskTimeAndNotify.beginTime = CalendarUtil.getWithoutSecond(beginTime)
                    taskTimeAndNotify.endTime = CalendarUtil.getWithoutSecond(endTime)
                    taskTimeAndNotify.isDDL = isDDL
                    taskTimeAndNotify.notifyTimes = notifyTimes
                    isCustomColor = customColor
                    this@ModifySimpleTaskActivity.customColor = this.color
                }
                isLoadFinished = true
            }
        }
        binding.editor.apply {
            setPadding(5, 10, 10, 10)
            setTextColor(resources.getColor(R.color.black))
            setPlaceholder("可以在此添加任务说明")
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
                val i =
                    Intent(this@ModifySimpleTaskActivity, SelectSimpleTaskTimeActivity::class.java)
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
        binding.deleteZone.apply {
            setOnLongClickListener {
                //打开确认删除弹窗
                MessageDialogBuilder(this@ModifySimpleTaskActivity)
                    .setTitle("提醒")
                    .setMessage("确定要删除吗？")
                    .setSkinManager(QMUISkinManager.defaultInstance(context))
                    .addAction(
                        "取消"
                    ) { dialog, index -> dialog.dismiss() }
                    .addAction(
                        0, "删除", QMUIDialogAction.ACTION_PROP_NEGATIVE
                    ) { dialog, index ->
                        Thread {
                            TaskDao.getSimpleTaskById(simpleTaskId)
                                ?.let { it1 -> TaskDao.removeSimpleTask(it1) }
                            EventBus.getDefault().post(EventType.EVENT_CHANGE)
                        }.start()
                        dialog.dismiss()
                        finish()
                    }
                    .create(com.qmuiteam.qmui.R.style.QMUI_Dialog).show()
                true
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
    }

    override fun onStart() {
        super.onStart()
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
                    view = binding.modifyTaskGuideZone,
                    title = "任务详情",
                    text = "退出此页面时，\n所有修改会自动保存"
                ),
                GuideEntity(
                    view = binding.deleteZone,
                    title = "删除任务",
                    text = "长按触发"
                )
            )
            ConfigDao.isDisplayingAnyGuide = false
            var i = 0
            GuideUtil.getDefaultBuilder(this, list[i++])
                .setGuideListener {
                    GuideUtil.getDefaultBuilder(this, list[i++])
                        .setGuideListener {
                            GuideUtil.updateGuideStatus(
                                binding.root.context,
                                this.javaClass.name,
                                guideVersion
                            )
                        }.build().show()
                }.build().show()
        }
    }

    override fun onBackPressed() {
        if (!isLoadFinished) {
            Log.e(this.javaClass.name, "用户快速打开又快速关闭了页面")
            finish()
            return
        }
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
        val i = SimpleTaskWithID(
            id = simpleTaskId,
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
            //更新普通事件
            TaskDao.updateSimpleTask(i)
            EventBus.getDefault().post(EventType.EVENT_CHANGE)
        }.start()
        finish()
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
                        .start(this@ModifySimpleTaskActivity);
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
                if (taskTimeAndNotify.isAllDay) {
                    text = "${
                        SimpleDateFormat.getDateInstance()
                            .format(taskTimeAndNotify.date.timeInMillis)
                    } 全天"
                } else {
                    var timeSimpleDateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    text = "${
                        SimpleDateFormat.getDateInstance()
                            .format(taskTimeAndNotify.date.timeInMillis)
                    } ${timeSimpleDateFormat.format(taskTimeAndNotify.beginTime.timeInMillis)}-${
                        timeSimpleDateFormat.format(
                            taskTimeAndNotify.endTime.timeInMillis
                        )
                    }"
                }
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
        private var modifySimpleTaskActivity: ModifySimpleTaskActivity
    ) {
        lateinit var fileUri: Uri
        fun start() {
            var thread = Thread {
                Log.e("upload", "begin Upload")
                run()
                modifySimpleTaskActivity.runOnUiThread {
                    modifySimpleTaskActivity.loadingPopup.show()
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
                        modifySimpleTaskActivity.runOnUiThread {
                            if (percent >= 99) {
                                modifySimpleTaskActivity.loadingPopup.setTitle("等待服务器响应")
                            } else {
                                modifySimpleTaskActivity.loadingPopup.setTitle("上传中 ${percent}%")
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
                            modifySimpleTaskActivity.runOnUiThread {
                                Toasty.success(
                                    modifySimpleTaskActivity.binding.root.context,
                                    "添加失败",
                                    Toast.LENGTH_LONG
                                ).show()
                                modifySimpleTaskActivity.loadingPopup.dismiss()
                            }
                        } else if (code == 0 && rst.getJSONObject("urls").length() != 0) {
                            modifySimpleTaskActivity.runOnUiThread {
                                Toasty.success(
                                    modifySimpleTaskActivity.binding.root.context,
                                    "添加成功",
                                    Toast.LENGTH_LONG
                                ).show()
                                modifySimpleTaskActivity.loadingPopup.dismiss()
                            }
                            val data = rst.getJSONObject("urls")
                            val keys: Iterator<String> = data.keys()
                            while (keys.hasNext()) {
                                val key = keys.next()
                                var url = data.getString(key)
                                //拿到图片的URL了
                                modifySimpleTaskActivity.runOnUiThread {
                                    val displayMetrics =
                                        context.resources.displayMetrics
                                    val dpWidth: Float =
                                        displayMetrics.widthPixels / displayMetrics.density
                                    modifySimpleTaskActivity.binding.editor.insertImage(
                                        url,
                                        "用户上传的图片",
                                        dpWidth.toInt() - 60
                                    )
                                }
                                break
                            }
                        }
                    } catch (e: java.lang.Exception) {
                        modifySimpleTaskActivity.runOnUiThread {
                            Toasty.success(
                                modifySimpleTaskActivity.binding.root.context,
                                "添加失败",
                                Toast.LENGTH_LONG
                            ).show()
                            modifySimpleTaskActivity.loadingPopup.dismiss()
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