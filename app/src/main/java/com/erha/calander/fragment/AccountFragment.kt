package com.erha.calander.fragment

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import cn.authing.guard.network.AuthClient
import cn.hutool.core.util.IdUtil
import cn.hutool.http.HttpUtil
import com.afollestad.recyclical.datasource.emptyDataSourceTyped
import com.afollestad.recyclical.setup
import com.afollestad.recyclical.withItem
import com.bumptech.glide.Glide
import com.erha.calander.R
import com.erha.calander.activity.LoginActivity
import com.erha.calander.activity.UserCenterActivity
import com.erha.calander.databinding.FragmentAccountBinding
import com.erha.calander.model.RecyclerViewItem
import com.erha.calander.type.EventType
import com.erha.calander.type.LocalStorageKey
import com.erha.calander.util.TinyDB
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.typeface.library.materialdesigniconic.MaterialDesignIconic
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizeDp
import com.qmuiteam.qmui.layout.QMUILayoutHelper
import com.qmuiteam.qmui.layout.QMUILinearLayout
import com.qmuiteam.qmui.util.QMUIDisplayHelper
import com.qmuiteam.qmui.widget.grouplist.QMUICommonListItemView
import es.dmoral.toasty.Toasty
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File


class AccountFragment : Fragment(R.layout.fragment_account) {

    data class AccountItem(
        var title: String,
        var icon: Icon,
        var isFirst: Boolean = false,
        var isLast: Boolean = false,
        var requireLogin: Boolean = true,
        var activity: Class<out AppCompatActivity>? = null,
        var key: String = "default"
    ) : RecyclerViewItem() {
        class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val qmuiCommonListItemView: QMUICommonListItemView =
                itemView.findViewById(R.id.settingListItemView)
            val qmuiLinearLayout: QMUILinearLayout = itemView.findViewById(R.id.QMUILinearLayout)
        }
    }

    data class SimpleItem(
        var titleResId: Int,
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

    data class Icon(
        var key: IIcon,
        var color: Int,
        var sizeCorrect: Int = 0
    )

    private lateinit var binding: FragmentAccountBinding
    private var isLogin = false
    private lateinit var store: TinyDB
    var loadUserInfoRunnable = Runnable {
        try {
            AuthClient.getCurrentUser { code, message, data ->
                if (code == 200) {
                    isLogin = true
                    activity?.runOnUiThread {
                        Glide.with(this)
                            .load(data.photo)
                            .circleCrop()
                            .error(R.mipmap.image_default_avator)
                            .into(binding.avatorImageView)
                        store.putBoolean(LocalStorageKey.USER_IS_LOGIN, true)
                        data.nickname.apply {
                            store.putString(LocalStorageKey.USER_NICKNAME, this)
                            binding.userNicknameTextView.text = this
                        }
                        data.email.split("@")[0].replaceRange(3, 7, "****").apply {
                            store.putString(LocalStorageKey.USER_PHONE, this)
                            binding.userPhoneTextvView.text = this
                        }
                    }
                    //缓存用户的头像
                    data.photo.apply {
                        var toCache = true
                        store.getString(LocalStorageKey.USER_PHOTO_CACHE_URL)?.apply {
                            if (isNotBlank() && this == data.photo) toCache = false
                        }
                        Log.e("新增头像缓存", toCache.toString())
                        if (toCache) {
                            var id = IdUtil.simpleUUID()
                            var file = File(File(binding.root.context.filesDir, "avator"), id)
                            try {
                                HttpUtil.downloadFileFromUrl(this, file)
                                store.putString(LocalStorageKey.USER_PHOTO_CACHE_URL, this)
                                store.putString(LocalStorageKey.USER_PHOTO_CACHE_ID, id)
                            } catch (e: Exception) {
                                store.putBoolean(LocalStorageKey.USER_IS_LOGIN, false)
                            }
                        }

                    }

                } else {
                    isLogin = false
                    activity?.runOnUiThread {
                        store.putBoolean(LocalStorageKey.USER_IS_LOGIN, false)
                        binding.userNicknameTextView.text = "未登录"
                        binding.userPhoneTextvView.text = "轻触快速登录"
                        Glide.with(this)
                            .load(R.mipmap.image_default_avator)
                            .circleCrop()
                            .into(binding.avatorImageView)
                    }
                }
            }
        } catch (e: java.lang.Exception) {
        } catch (e: Exception) {
        }
    }
    private lateinit var loadUserInfoThread: Thread

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    private val dataSource = emptyDataSourceTyped<RecyclerViewItem>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        saveInstanceState: Bundle?
    ): View {
        binding = FragmentAccountBinding.inflate(inflater, container, false)
        store = TinyDB(binding.root.context)
        store.apply {
            //加载用户信息缓存
            var loadPhoto = false
            if (getBoolean(LocalStorageKey.USER_IS_LOGIN)) {
                isLogin = true
                getString(LocalStorageKey.USER_NICKNAME)?.apply {
                    binding.userNicknameTextView.text = this
                }
                getString(LocalStorageKey.USER_PHONE)?.apply {
                    binding.userPhoneTextvView.text = this
                }
                getString(LocalStorageKey.USER_PHOTO_CACHE_ID)?.apply {
                    if (isNotBlank()) {
                        loadPhoto = true
                        Glide.with(this@AccountFragment)
                            .load(File(File(binding.root.context.filesDir, "avator"), this))
                            .error(R.mipmap.image_default_avator)
                            .circleCrop()
                            .into(binding.avatorImageView)
                    }
                }

            } else {
                binding.userNicknameTextView.text = "未登录"
                binding.userPhoneTextvView.text = "轻触快速登录"
            }
            if (!loadPhoto) {
                Glide.with(this@AccountFragment)
                    .load(R.mipmap.image_default_avator)
                    .circleCrop()
                    .into(binding.avatorImageView)
            }
        }
        dataSource.clear()
        dataSource.add(
            AccountItem(
                title = "个人中心",
                icon = Icon(
                    key = MaterialDesignIconic.Icon.gmi_account,
                    color = Color.parseColor("#4d70fa"),
                    sizeCorrect = 0
                ),
                isFirst = true,
                key = "userCenter"
            ),
            AccountItem(
                title = "登录设备管理",
                icon = Icon(
                    key = MaterialDesignIconic.Icon.gmi_devices,
                    color = Color.parseColor("#4d70fa"),
                    sizeCorrect = 0
                ),
                isFirst = false,
                isLast = false
            ),
            AccountItem(
                title = "账户安全",
                icon = Icon(
                    key = MaterialDesignIconic.Icon.gmi_shield_check,
                    color = Color.parseColor("#4d70fa"),
                    sizeCorrect = 0
                ),
                isLast = true
            ),
            SpaceItem(),
            AccountItem(
                title = "备份至云端",
                icon = Icon(
                    key = MaterialDesignIconic.Icon.gmi_cloud_upload,
                    color = Color.parseColor("#0ccd9c"),
                    sizeCorrect = 0
                ),
                isFirst = true
            ),
            AccountItem(
                title = "从云端恢复",
                icon = Icon(
                    key = MaterialDesignIconic.Icon.gmi_cloud_download,
                    color = Color.parseColor("#0ccd9c"),
                    sizeCorrect = 0
                ),
                isLast = true,
            ),
            SpaceItem(),
            AccountItem(
                title = "校园论坛",
                icon = Icon(
                    key = MaterialDesignIconic.Icon.gmi_comments,
                    color = Color.parseColor("#ffb001"),
                    sizeCorrect = 0
                ),
                isFirst = true,
                isLast = true,
                key = "discuss",
                requireLogin = false
            )
        )

        //创建ActivityResultLauncher
        val resultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                Log.e("registerForActivityResult", it.resultCode.toString())
            }

        binding.userInfoConstraintLayout.setOnClickListener { v ->
            if (isLogin) {
                resultLauncher.launch(Intent(activity, UserCenterActivity::class.java))
            } else {
                resultLauncher.launch(Intent(activity, LoginActivity::class.java))
            }
        }

        EventBus.getDefault().register(this)
        loadUserInfoThread = Thread(loadUserInfoRunnable).apply {
            start()
        }

        binding.recyclerView.setup {
            withDataSource(dataSource)
            withItem<SpaceItem, SpaceItem.Holder>(R.layout.item_list_space_20) {
                onBind(SpaceItem::Holder) { _, _ ->
                }
            }
            withItem<SimpleItem, SimpleItem.Holder>(R.layout.item_list_simple) {
                onBind(SimpleItem::Holder) { _, item ->
                    textView.apply {
                        text = getString(item.titleResId)
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
                }
            }
            withItem<AccountItem, AccountItem.Holder>(R.layout.item_list_setting) {
                onBind(AccountItem::Holder) { index, item ->
                    qmuiCommonListItemView.text = item.title
                    qmuiCommonListItemView.setImageDrawable(
                        IconicsDrawable(
                            binding.root.context,
                            item.icon.key
                        ).apply {
                            colorInt = item.icon.color
                            sizeDp = 19 + item.icon.sizeCorrect
                        })
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
                    if (item.requireLogin && !isLogin) {
                        Toasty.info(binding.root.context, "请先登录").show()
                        resultLauncher.launch(Intent(activity, LoginActivity::class.java))
                        return@onClick
                    }
                    when (item.key) {
                        "userCenter" -> {
                            resultLauncher.launch(Intent(activity, UserCenterActivity::class.java))
                        }
                        "discuss" -> {
                            startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://idiscuz.qugeek.com/")
                                )
                            )
                        }
                        else -> {
                            Toasty.info(binding.root.context, R.string.text_developing).show()
                        }
                    }
                }
                onLongClick { index ->
                }
            }
        }

        return binding.root
    }


    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onEvent(str: String?) {
        when (str) {
            EventType.USER_INFO_CHANGE -> {
                loadUserInfoThread = Thread(loadUserInfoRunnable).apply {
                    start()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
        try {
            loadUserInfoThread.interrupt()
            loadUserInfoThread.stop()
        } catch (e: Exception) {
        } catch (e: java.lang.Exception) {
        }
    }

}