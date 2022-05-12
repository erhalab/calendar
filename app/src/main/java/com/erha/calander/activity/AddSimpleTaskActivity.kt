package com.erha.calander.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.erha.calander.R
import com.erha.calander.dao.SecretKeyDao
import com.erha.calander.dao.TaskDao
import com.erha.calander.databinding.ActivityAddSimpleTaskBinding
import com.erha.calander.model.SimpleTaskWithoutID
import com.erha.calander.model.TaskStatus
import com.erha.calander.type.EventType
import com.erha.calander.util.CalendarUtil
import com.erha.calander.util.FileUtil
import com.erha.calander.util.TinyDB
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.impl.LoadingPopupView
import com.squareup.okhttp.*
import com.yalantis.ucrop.UCrop
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
        binding.taskTime.apply {
            setOnClickListener {
                val i = Intent(this@AddSimpleTaskActivity, SelectSimpleTaskTimeActivity::class.java)
                taskTimeAndNotify.apply {
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
                }catch (e:Exception){}
                catch (e:java.lang.Exception){}
                val status = when (binding.checkbox.state){
                    null -> TaskStatus.CANCELED
                    true -> TaskStatus.FINISHED
                    else -> TaskStatus.ONGOING
                }
                val i = SimpleTaskWithoutID(
                    status = status,
                    title = binding.taskTitle.text.toString(),
                    detailHtml = html,
                    hashTime = taskTimeAndNotify.hasTime,
                    date = CalendarUtil.getWithoutTime(taskTimeAndNotify.date),
                    isAllDay = taskTimeAndNotify.isAllDay,
                    beginTime = CalendarUtil.getWithoutSecond(taskTimeAndNotify.beginTime),
                    endTime = CalendarUtil.getWithoutSecond(taskTimeAndNotify.endTime),
                    isDDL = taskTimeAndNotify.isDDL,
                    notifyTimes = taskTimeAndNotify.notifyTimes,
                    customColor = false,
                    color = "#FFFFFF"
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
                if (isIndeterminate){
                    isIndeterminate = false
                    isChecked = false
                }
            }
        }
        binding.checkbox.setOnLongClickListener {
            Toasty.info(binding.root.context,"任务状态 -> 已放弃",Toast.LENGTH_SHORT,false).show()
            binding.checkbox.isIndeterminate = true
            true
        }
        initLoadingPopup()
        progressUploadFile = ProgressUploadFile(binding.root.context, this)
        updateTimeTextview()
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