package com.erha.calander.popup

import android.content.Context
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.NonNull
import com.erha.calander.R
import com.erha.calander.util.TinyDB
import com.lxj.xpopup.core.CenterPopupView
import com.qmuiteam.qmui.layout.QMUILinearLayout
import com.qmuiteam.qmui.util.QMUIDisplayHelper
import es.dmoral.toasty.Toasty

interface SingleTextInputPopupCallback {
    fun userFinished(input: String)
}


class SingleTextInputPopup  //注意：自定义弹窗本质是一个自定义View，但是只需重写一个参数的构造，其他的不要重写，所有的自定义弹窗都是这样。
    (@NonNull context: Context?) : CenterPopupView(context!!) {
    // 返回自定义弹窗的布局
    override fun getImplLayoutId(): Int {
        return R.layout.popup_single_text_input
    }

    var singleTextInputPopupCallback: SingleTextInputPopupCallback? = null

    private lateinit var store: TinyDB
    var title = "默认标题"
    var inputHint = "默认输入框提示文字"
    var inputDefault = "输入框默认内容"

    // 执行初始化操作，比如：findView，设置点击，或者任何你弹窗内的业务逻辑
    override fun onCreate() {
        super.onCreate()
        store = TinyDB(context)
        findViewById<TextView>(R.id.title).apply {
            this.text = title
        }
        var input = findViewById<EditText>(R.id.input).apply {
            this.hint = inputHint
            this.setText(inputDefault)
        }
        //取消按钮事件
        findViewById<View>(R.id.buttomCancle).setOnClickListener {
            dismiss() // 关闭弹窗
        }
        //完成按钮事件
        findViewById<View>(R.id.buttomFinish).setOnClickListener {
            if (input.text.toString().isBlank()) {
                Toasty.info(context, "不能为空").show()
                return@setOnClickListener
            }
            singleTextInputPopupCallback?.apply {
                userFinished(input.text.toString())
            }
            dismiss() // 关闭弹窗
        }
        //初始化圆角和阴影
        (findViewById<View>(R.id.QMUILinearLayout) as QMUILinearLayout).apply {
            setRadiusAndShadow(
                resources.getDimensionPixelSize(R.dimen.popup_radius),
                QMUIDisplayHelper.dp2px(context, 5),
                0.3F
            )
        }
    }

}