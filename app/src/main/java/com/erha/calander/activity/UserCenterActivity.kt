package com.erha.calander.activity

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import cn.authing.guard.data.UserInfo
import cn.authing.guard.network.AuthClient
import cn.hutool.core.util.IdUtil
import com.afollestad.recyclical.datasource.emptyDataSourceTyped
import com.afollestad.recyclical.setup
import com.afollestad.recyclical.withItem
import com.bumptech.glide.Glide
import com.erha.calander.R
import com.erha.calander.databinding.ActivityUserCenterBinding
import com.erha.calander.model.RecyclerViewItem
import com.erha.calander.popup.SingleTextInputPopup
import com.erha.calander.popup.SingleTextInputPopupCallback
import com.erha.calander.type.EventType
import com.erha.calander.type.LocalStorageKey
import com.erha.calander.util.TinyDB
import com.lxj.xpopup.XPopup
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.typeface.library.materialdesigniconic.MaterialDesignIconic
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizeDp
import com.qmuiteam.qmui.layout.QMUILayoutHelper
import com.qmuiteam.qmui.layout.QMUILinearLayout
import com.qmuiteam.qmui.util.QMUIDisplayHelper
import com.qmuiteam.qmui.widget.grouplist.QMUICommonListItemView
import com.yalantis.ucrop.UCrop
import es.dmoral.toasty.Toasty
import org.greenrobot.eventbus.EventBus
import org.json.JSONObject
import java.io.File
import java.io.InputStream


class UserCenterActivity : AppCompatActivity(), SingleTextInputPopupCallback {
    data class UserInfoItem(
        var key: String,
        var title: String,
        var detail: String,
        var accessoryType: Int,
        var iconKey: IIcon,
        var isFirst: Boolean = false,
        var isLast: Boolean = false
    ) : RecyclerViewItem() {
        class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val qmuiCommonListItemView: QMUICommonListItemView =
                itemView.findViewById(R.id.modelListItemView)
            val qmuiLinearLayout: QMUILinearLayout = itemView.findViewById(R.id.QMUILinearLayout)
        }
    }

    data class SimpleItem(
        var title: String,
        var key: String,
        var isFirst: Boolean = false,
        var isLast: Boolean = false,
        var textColor: Int,
    ) : RecyclerViewItem() {
        class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val textView: TextView = itemView.findViewById(R.id.textView)
            val qmuiLinearLayout: QMUILinearLayout = itemView.findViewById(R.id.QMUILinearLayout)
        }
    }

    data class SpaceItem(
        var key: String = "space"
    ) : RecyclerViewItem() {
        class Holder(itemView: View) : RecyclerView.ViewHolder(itemView)
    }

    private var userNickname = ""

    //布局binding
    private lateinit var binding: ActivityUserCenterBinding
    private lateinit var store: TinyDB

    private lateinit var nicknameQMUICommonListItemView: QMUICommonListItemView

    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            Log.e("registerForActivityResult", it.resultCode.toString())
        }

    private var loadUserInfoThread = Thread {
        AuthClient.getCurrentUser { code, message, data ->
            if (code == 200) {
                //加载头像
                runOnUiThread {
                    if (data.photo.isNotBlank()) {
                        Glide.with(this)
                            .load(data.photo)
                            .circleCrop()
                            .error(R.mipmap.image_default_avator)
                            .into(binding.avatorImageView)
                    } else {
                        Glide.with(this)
                            .load(R.mipmap.image_default_avator)
                            .circleCrop()
                            .into(binding.avatorImageView)
                    }
                    userNickname = data.nickname
                    dataSource.insert(
                        0, UserInfoItem(
                            key = "nickname",
                            title = "昵称",
                            detail = data.nickname,
                            accessoryType = QMUICommonListItemView.ACCESSORY_TYPE_CHEVRON,
                            iconKey = MaterialDesignIconic.Icon.gmi_account
                        )
                    )
                    dataSource.insert(
                        0, UserInfoItem(
                            isFirst = true,
                            key = "phone",
                            title = "手机号",
                            detail = data.email.split("@")[0].replaceRange(3, 7, "****"),
                            accessoryType = QMUICommonListItemView.ACCESSORY_TYPE_NONE,
                            iconKey = MaterialDesignIconic.Icon.gmi_phone
                        )
                    )
                }
            } else {
                runOnUiThread {
                    Toasty.info(this, "登录状态失效", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == 1000) {
            //用户选择头像后需要裁剪
            data?.apply {
                var id = IdUtil.simpleUUID()
                var file = File(File(binding.root.context.filesDir, "avator"), id)
                val selectedImageUri: Uri? = data.data
                selectedImageUri?.apply {
                    UCrop.of(selectedImageUri, file.toUri())
                        .withAspectRatio(1F, 1F)
                        .withMaxResultSize(512, 512)
                        .start(this@UserCenterActivity);
                }
            }
        }
        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            //用户裁剪完成头像后
            Log.e("用户裁剪完成头像", "${UCrop.REQUEST_CROP}")
            data?.apply {
                val selectedImageUri: Uri? = UCrop.getOutput(data)
                val `in`: InputStream?
                try {
                    selectedImageUri?.apply {
                        `in` = contentResolver.openInputStream(selectedImageUri)
                        AuthClient.uploadAvatar(
                            `in`
                        ) { code: Int, message: String?, userInfo: UserInfo? ->
                            runOnUiThread {
                                if (code == 200) {
                                    //更新头像成功
                                    EventBus.getDefault().post(EventType.USER_INFO_CHANGE)
                                    Glide.with(this@UserCenterActivity)
                                        .load(selectedImageUri)
                                        .circleCrop()
                                        .into(binding.avatorImageView)
                                } else {
                                    Toasty.error(this@UserCenterActivity, "头像更新失败:${message}")
                                        .show()
                                }
                            }
                        }
                    }
                } catch (e: java.lang.Exception) {
                } catch (e: Exception) {
                }
            }
        }
    }


    private val dataSource = emptyDataSourceTyped<RecyclerViewItem>()
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityUserCenterBinding.inflate(layoutInflater)
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
        loadUserInfoThread.start()

        binding.backButton.setOnClickListener { v ->
            run {
                setResult(Activity.RESULT_CANCELED, Intent())
                finish()
            }
        }
        dataSource.add(
            UserInfoItem(
                key = "sex",
                title = "性别",
                detail = "男♂",
                accessoryType = QMUICommonListItemView.ACCESSORY_TYPE_CHEVRON,
                iconKey = MaterialDesignIconic.Icon.gmi_male_female
            )
        )
        dataSource.add(
            UserInfoItem(
                key = "email",
                title = "邮箱",
                detail = "20202020@stu.neu.edu.cn",
                accessoryType = QMUICommonListItemView.ACCESSORY_TYPE_CHEVRON,
                iconKey = MaterialDesignIconic.Icon.gmi_email
            )
        )
        dataSource.add(
            UserInfoItem(
                key = "location",
                title = "地区",
                detail = "中国大陆",
                accessoryType = QMUICommonListItemView.ACCESSORY_TYPE_CHEVRON,
                iconKey = MaterialDesignIconic.Icon.gmi_pin
            )
        )
        dataSource.add(
            UserInfoItem(
                isLast = true,
                key = "birthday",
                title = "生日",
                detail = "2022年5月12日",
                accessoryType = QMUICommonListItemView.ACCESSORY_TYPE_CHEVRON,
                iconKey = MaterialDesignIconic.Icon.gmi_cake
            )
        )
        dataSource.add(SpaceItem())
        dataSource.add(
            SimpleItem(
                title = "退出登录",
                key = "logout",
                isFirst = true,
                isLast = true,
                textColor = resources.getColor(R.color.qmui_config_color_red)
            )
        )
        binding.avatorImageView.setOnClickListener {
            //修改头像开始
            try {
                val i = Intent()
                i.addCategory(Intent.CATEGORY_OPENABLE)
                i.type = "image/*"
                i.action = Intent.ACTION_GET_CONTENT
                startActivityForResult(
                    Intent.createChooser(
                        i,
                        "选择头像图片"
                    ), 1000
                )
            } catch (e: java.lang.Exception) {
            } catch (e: Exception) {
            }

        }
        //初始化列表
        binding.userCenterRecyclerView.setup {
            withDataSource(dataSource)
            withItem<UserInfoItem, UserInfoItem.Holder>(R.layout.item_list_model) {
                onBind(UserInfoItem::Holder) { index, item ->
                    qmuiCommonListItemView.apply {
                        text = item.title
                        detailText = item.detail
                        accessoryType = item.accessoryType
                        orientation = QMUICommonListItemView.HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        setImageDrawable(
                            IconicsDrawable(
                                binding.root.context,
                                item.iconKey
                            ).apply {
                                colorInt = Color.BLACK
                                sizeDp = 15
                            })
                        val paddingVer = QMUIDisplayHelper.dp2px(binding.root.context, 15)
                        setPadding(
                            paddingLeft, paddingVer,
                            paddingRight, paddingVer
                        )
                        when (item.key) {
                            "nickname" -> nicknameQMUICommonListItemView = qmuiCommonListItemView
                        }
                    }
                    val radius =
                        resources.getDimensionPixelSize(R.dimen.listview_radius)
                    if (item.isFirst && item.isLast) {
                        qmuiLinearLayout.radius = radius
                    } else if (item.isLast && !item.isFirst) {
                        qmuiLinearLayout.setRadius(radius, QMUILayoutHelper.HIDE_RADIUS_SIDE_TOP)
                    } else if (item.isFirst && !item.isLast) {
                        qmuiLinearLayout.setRadius(radius, QMUILayoutHelper.HIDE_RADIUS_SIDE_BOTTOM)
                    } else {
                        qmuiLinearLayout.radius = 0
                    }
                }
                onClick { index ->
                    when (this.item.key) {
                        "nickname" -> {
                            SingleTextInputPopup(this@UserCenterActivity).apply {
                                singleTextInputPopupCallback = this@UserCenterActivity
                                title = "修改昵称"
                                inputDefault = userNickname
                                inputHint = "请输入昵称"
                                XPopup.Builder(context)
                                    .dismissOnBackPressed(false)
                                    .dismissOnTouchOutside(false)
                                    .asCustom(this)
                                    .show()
                            }
                        }
                    }

                }
                onLongClick { index ->

                }
            }
            withItem<SimpleItem, SimpleItem.Holder>(R.layout.item_list_simple) {
                onBind(SimpleItem::Holder) { _, item ->
                    textView.apply {
                        text = item.title
                        setTextColor(item.textColor)
                    }
                    qmuiLinearLayout.apply {
                        val paddingVer = QMUIDisplayHelper.dp2px(binding.root.context, 15)
                        setPadding(
                            paddingLeft, paddingVer,
                            paddingRight, paddingVer
                        )
                    }

                    val radius =
                        resources.getDimensionPixelSize(R.dimen.listview_radius)
                    if (item.isFirst && item.isLast) {
                        qmuiLinearLayout.radius = radius
                    } else if (item.isLast && !item.isFirst) {
                        qmuiLinearLayout.setRadius(radius, QMUILayoutHelper.HIDE_RADIUS_SIDE_TOP)
                    } else if (item.isFirst && !item.isLast) {
                        qmuiLinearLayout.setRadius(radius, QMUILayoutHelper.HIDE_RADIUS_SIDE_BOTTOM)
                    } else {
                        qmuiLinearLayout.radius = 0
                    }
                }
                onClick { index ->
                    when (item.key) {
                        "logout" -> logout()
                    }
                }
            }
            withItem<SpaceItem, SpaceItem.Holder>(R.layout.item_list_space_20) {
                onBind(SpaceItem::Holder) { _, _ ->
                }
            }
        }


    }

    private fun logout() {
        Thread {
            AuthClient.logout { code, _, _ ->
                runOnUiThread {
                    if (code == 200) {
                        store.putBoolean(LocalStorageKey.USER_IS_LOGIN, false)
                        EventBus.getDefault().post(EventType.USER_INFO_CHANGE)
                        finish()
                    } else {
                        Toasty.error(this, "无法登出").show()
                    }
                }
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            loadUserInfoThread.interrupt()
            loadUserInfoThread.stop()
        } catch (e: Exception) {
        } catch (e: java.lang.Exception) {
        }
    }

    override fun userFinished(input: String) {
        Thread {
            var jsonObject = JSONObject()
            jsonObject.put("nickname", input)
            AuthClient.updateProfile(jsonObject) { code, message, _ ->
                runOnUiThread {
                    if (code == 200) {
                        userNickname = input
                        EventBus.getDefault().post(EventType.USER_INFO_CHANGE)
                        nicknameQMUICommonListItemView.detailText = input
                    } else {
                        Toasty.error(this, "更新昵称失败").show()
                    }
                }

            }
        }.start()
    }

}