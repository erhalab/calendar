package com.erha.calander.activity


import android.app.NotificationManager
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_LOCKED_CLOSED
import androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_UNLOCKED
import androidx.fragment.app.Fragment
import cn.authing.guard.Authing
import cn.authing.guard.data.UserInfo
import cn.authing.guard.network.AuthClient
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.erha.calander.R
import com.erha.calander.dao.SecretKeyDao
import com.erha.calander.databinding.ActivityMainBinding
import com.erha.calander.fragment.AccountFragment
import com.erha.calander.fragment.CalendarFragment
import com.erha.calander.fragment.HomeFragment
import com.erha.calander.fragment.SettingsFragment
import com.erha.calander.service.NotificationService
import com.erha.calander.type.EventType
import com.erha.calander.type.LocalStorageKey
import com.erha.calander.util.TinyDB
import com.mikepenz.iconics.Iconics
import com.mikepenz.iconics.typeface.library.fontawesome.FontAwesome
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import com.mikepenz.materialdrawer.holder.ImageHolder
import com.mikepenz.materialdrawer.iconics.iconicsIcon
import com.mikepenz.materialdrawer.model.DividerDrawerItem
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.model.ProfileDrawerItem
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.descriptionText
import com.mikepenz.materialdrawer.model.interfaces.nameText
import com.mikepenz.materialdrawer.widget.AccountHeaderView
import com.qmuiteam.qmui.util.QMUIDisplayHelper
import com.tencent.smtt.export.external.TbsCoreSettings
import com.tencent.smtt.sdk.QbSdk
import com.tencent.smtt.sdk.QbSdk.*
import es.dmoral.toasty.Toasty
import github.com.st235.lib_expandablebottombar.MenuItem
import github.com.st235.lib_expandablebottombar.MenuItemDescriptor
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.*


class MainActivity : AppCompatActivity() {
    data class FragmentObject(
        var fragment: Fragment,
        var identity: Int,
        var frameLayout: FrameLayout
    ) {
        var showAddButton = true
        var allowSideDrawer = true
        var isDefault = false

        //        set(value){
//            field = value
//            //如果这个页面是默认页面，那么这个页面一定是当前fragment的默认页面
//            if (value){
//                isFragmentDefault = true
//            }
//        }
        var isFragmentDefault = false
    }

    //布局binding
    private lateinit var binding: ActivityMainBinding
    private val fragmentObjects = ArrayList<FragmentObject>()
    private lateinit var store: TinyDB
    private lateinit var nowDisplayFrameLayout: FrameLayout
    private var userInfo: UserInfo? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.e("onCreate Activity创建", this.javaClass.name)

        Iconics.registerFont(FontAwesome)
        Iconics.registerFont(GoogleMaterial)

        super.onCreate(savedInstanceState)
        //先保证服务启用了，他要负责数据的初始化
        initNotificationService()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        store = TinyDB(binding.root.context)

        window.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = getColor(R.color.default_background_color)
            decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        //注册事件，监听语言变化
        EventBus.getDefault().register(this)

        //通知管理器
        val notificationManager =
            binding.root.context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        //加载用户设置的语言或者默认语言
        var sta = store.getString("language")
        if (sta == null || sta.isBlank()) {
            store.putString("language", Locale.getDefault().language)
            sta = store.getString("language")
        }
        sta?.apply {
            when (this) {
                //只有支持的语言才能加载
                Locale.SIMPLIFIED_CHINESE.language, Locale.ENGLISH.language -> {
                    val myLocale = Locale(this)
                    val res = resources
                    val dm = res.displayMetrics
                    val conf: Configuration = res.configuration
                    conf.locale = myLocale
                    res.updateConfiguration(conf, dm)
                }
                else -> {
                    //不支持的语言把这个设置项清空
                    store.putString("language", "")
                }
            }
        }

        //初始化登录
        Authing.init(applicationContext, SecretKeyDao.AuthingAppID)
        Log.e("Authing.init", this.javaClass.name)

        initFragment()
        initExpandableBottomBar()
        initFloatButton()
        initToast()
        initDrawer()

        // 在调用TBS初始化、创建WebView之前进行如下配置
        val map = HashMap<String, Any>()
        map[TbsCoreSettings.TBS_SETTINGS_USE_SPEEDY_CLASSLOADER] = true
        map[TbsCoreSettings.TBS_SETTINGS_USE_DEXLOADER_SERVICE] = true
        map[TbsCoreSettings.TBS_SETTINGS_USE_PRIVATE_CLASSLOADER] = true
        QbSdk.initTbsSettings(map)
        QbSdk.unForceSysWebView()
        QbSdk.setDownloadWithoutWifi(true)

        if (store.getInt(LocalStorageKey.LAST_LAUNCH_WEBVIEW_SUCCESS) == 1) {
            initX5()
        }
    }

    private fun initX5() {
        // 在调用TBS初始化、创建WebView之前进行如下配置
        val map = HashMap<String, Any>()
        map[TbsCoreSettings.TBS_SETTINGS_USE_SPEEDY_CLASSLOADER] = true
        map[TbsCoreSettings.TBS_SETTINGS_USE_DEXLOADER_SERVICE] = true
        map[TbsCoreSettings.TBS_SETTINGS_USE_PRIVATE_CLASSLOADER] = true
        QbSdk.initTbsSettings(map)
        QbSdk.unForceSysWebView()
        QbSdk.setDownloadWithoutWifi(true)
        QbSdk.initX5Environment(applicationContext, object : PreInitCallback {
            override fun onCoreInitFinished() {
                // 内核初始化完成
                Log.e("X5", "内核初始化完成")
            }

            /**
             * 预初始化结束
             * 由于X5内核体积较大，需要依赖网络动态下发，所以当内核不存在的时候，默认会回调false，此时将会使用系统内核代替
             * @param isX5 是否使用X5内核
             */
            override fun onViewInitFinished(isX5: Boolean) {
                store.putBoolean(LocalStorageKey.X5_INIT, true)
                Log.e("X5", "onViewInitFinished -> ${isX5}")
                Log.e("X5", "getIsInitX5Environment -> ${getIsInitX5Environment()}")
                Log.e("X5", "getTBSInstalling -> ${getTBSInstalling()}")
                Log.e("X5", "getOnlyDownload -> ${getOnlyDownload()}")
            }
        })
    }

    private fun initNotificationService() {
        //启用通知Service
        var serviceIntent = Intent(this@MainActivity, NotificationService::class.java)
        startService(serviceIntent)
    }

    open override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == 1000) {
            data?.apply {
                val selectedImageUri = data.data
                val `in`: InputStream?
                try {
                    `in` = contentResolver.openInputStream(selectedImageUri!!)
                    AuthClient.uploadAvatar(
                        `in`
                    ) { code: Int, message: String?, userInfo: UserInfo? ->
                        runOnUiThread {}
                    }
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                }
            }

        }
    }


    private fun initToast() {
        Toasty.Config.getInstance()
            .allowQueue(false) // optional (prevents several Toastys from queuing)
            .apply() // required
    }


    //初始化fragment
    private fun initFragment(recreate: Boolean = false) {
        if (recreate) {
            TODO("实现设置里面的开关Model")
            return
        }
        fragmentObjects.addAll(
            listOf(
                FragmentObject(
                    fragment = HomeFragment(),
                    identity = R.id.menu_home,
                    frameLayout = binding.frameHome
                )
                    .apply {
                        isFragmentDefault = true
                        isDefault = true
                    },
                FragmentObject(
                    fragment = CalendarFragment(),
                    identity = R.id.menu_calendar,
                    frameLayout = binding.frameCalendar
                )
                    .apply {
                        allowSideDrawer = false
                        isFragmentDefault = true
                    },
                FragmentObject(
                    fragment = AccountFragment(),
                    identity = R.id.menu_account,
                    frameLayout = binding.frameHome
                )
                    .apply {
                        allowSideDrawer = false
                        showAddButton = false
                    },
                FragmentObject(
                    fragment = SettingsFragment(),
                    identity = R.id.menu_setting,
                    frameLayout = binding.frameHome
                )
                    .apply {
                        allowSideDrawer = false
                        showAddButton = false
                    }
            )
        )
        //加载fragment
        //if (savedInstanceState == null) {
        for (f in fragmentObjects) {
            if (f.isDefault) {
                nowDisplayFrameLayout = f.frameLayout
            }
        }
        for (f in fragmentObjects) {
            hasRecreatedFragments.add(f.fragment)
            if (f.isFragmentDefault) {
                supportFragmentManager
                    .beginTransaction()
                    .replace(f.frameLayout.id, f.fragment)
                    .commit()
            }
            if (nowDisplayFrameLayout != f.frameLayout) {
                f.frameLayout.visibility = View.INVISIBLE
            }
        }
        //}
    }

    //初始化侧边栏
    private fun initDrawer(recreate: Boolean = false) {
        binding.slider.apply {
            if (recreate) {
                this.headerAdapter.clear()
                this.itemAdapter.clear()
                this.footerAdapter.clear()
            }
            val i = ProfileDrawerItem().apply {
                nameText = userInfo?.run { this.nickname } ?: "未登录"
                descriptionText = userInfo?.run { this.email } ?: ""
                identifier = 102
            }
            val accountHeaderView = AccountHeaderView(binding.root.context).apply {
                attachToSliderView(binding.slider) // attach to the slider
                selectionListEnabledForSingleProfile = false
                closeDrawerOnProfileListClick = false
            }
            Glide.with(this@MainActivity)
                .asBitmap()
                .load("https://files.authing.co/user-contents/photos/52e998bf-a2b5-4db3-ae3d-71aea9b8314a")
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        i.icon = ImageHolder(resource)
                        accountHeaderView.addProfiles(i)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                    }
                })
            val item1 =
                PrimaryDrawerItem().apply {
                    nameText = "测试"
                    identifier = 1
                    isSelectable = false
                    iconicsIcon = FontAwesome.Icon.faw_star
                }
            val item2 =
                SecondaryDrawerItem().apply { nameText = "测试2"; identifier = 2 }

            // get the reference to the slider and add the items
            this.itemAdapter.add(
                item1,
                DividerDrawerItem(),
                item2,
                SecondaryDrawerItem().apply { nameText = "设置" }
            )

            // specify a click listener
            this.onDrawerItemClickListener = { v, drawerItem, position ->
                // do something with the clicked item :D
                Toast.makeText(
                    binding.root.context,
                    "onDrawerItemClickListener ${drawerItem.identifier}",
                    Toast.LENGTH_SHORT
                )
                    .show()
                Log.e("onDrawerItemClickListener", "ID = ${drawerItem.identifier}")
                false
            }
            closeOnClick = false
            scrollToTopAfterClick = false
        }
    }

    //初始化悬浮按钮
    private fun initFloatButton(recreate: Boolean = false) {
        binding.floatButtonShadowLayout.apply {
            setRadiusAndShadow(
                100, QMUIDisplayHelper.dp2px(binding.root.context, 5), 0.3F
            )
        }
        binding.floatButton.apply {
            if (recreate) {

            } else {
                setOnLongClickListener {
                    Toast.makeText(binding.root.context, "long", Toast.LENGTH_SHORT).show()
                    true
                }
                setOnClickListener {
                    Toast.makeText(binding.root.context, "short", Toast.LENGTH_SHORT).show()
                }
            }

        }
    }

    private var lastSelectedMenuId = -1

    //初始化底部菜单
    private fun initExpandableBottomBar(recreate: Boolean = false) {
        binding.expandableBottomBar.apply {
            if (recreate) {
                binding.expandableBottomBar.removeAllViews()
                menu.removeAll()
                onItemSelectedListener = null
            }
            var color = resources.getColor(R.color.default_active)

            menu.add(
                MenuItemDescriptor.Builder(
                    itemId = R.id.menu_home,
                    activeColor = color,
                    context = binding.root.context,
                    iconId = R.drawable.ic_baseline_home_24,
                    textId = R.string.menu_home_text
                ).build()
            )
            menu.add(
                MenuItemDescriptor.Builder(
                    itemId = R.id.menu_calendar,
                    activeColor = color,
                    context = binding.root.context,
                    iconId = R.drawable.ic_baseline_calendar_today_24,
                    textId = R.string.menu_calendar_text
                ).build()
            )
            menu.add(
                MenuItemDescriptor.Builder(
                    itemId = R.id.menu_account,
                    activeColor = color,
                    context = binding.root.context,
                    iconId = R.drawable.ic_baseline_account_circle_24,
                    textId = R.string.menu_account_text
                ).build()
            )
            menu.add(
                MenuItemDescriptor.Builder(
                    itemId = R.id.menu_setting,
                    activeColor = color,
                    context = binding.root.context,
                    iconId = R.drawable.ic_baseline_settings_24,
                    textId = R.string.menu_setting_text
                ).build()
            )
            if (lastSelectedMenuId > 0) {
                menu.select(lastSelectedMenuId)
            } else {
                for (f in fragmentObjects) {
                    if (f.isDefault) {
                        for (m in menu.items) {
                            if (m.id == f.identity) {
                                menu.select(m.id)
                                break
                            }

                        }
                        break
                    }
                }
            }

            //监听底部菜单点击事件（切换页面）
            onItemSelectedListener =
                { view: View, menuItem: MenuItem, b: Boolean ->
                    var i = 0
                    for (m in binding.expandableBottomBar.menu.items) {
                        if (m.id == menuItem.id) {
                            lastSelectedMenuId = m.id
                            //切换fragment
                            fragmentObjects[i].apply {

                                //如果需要重建的话，在切换语言之后
                                hasRecreatedFragments.run {
                                    if (!this.contains(fragmentObjects[i].fragment)) {
                                        //单独处理特殊的Fragment
                                        if (fragmentObjects[i].fragment is CalendarFragment) {
                                            fragmentObjects[i].fragment = CalendarFragment()
                                            // 修改BUG，如果这种类型的fragment（实例需要重建的），那么直接
                                            // 替换。不需要detach和attach
                                            hasRecreatedFragments.add(fragmentObjects[i].fragment)
                                        }
                                    }
                                    supportFragmentManager
                                        .beginTransaction()
                                        .replace(
                                            fragmentObjects[i].frameLayout.id,
                                            fragmentObjects[i].fragment
                                        )
                                        .commitNowAllowingStateLoss()
                                    if (!this.contains(fragmentObjects[i].fragment)) {
                                        recreateFragment(fragmentObjects[i].fragment)
                                    }
                                }

                                if (nowDisplayFrameLayout == this.frameLayout) {
                                    //是同一个框架，那么直接加载
                                } else {
                                    //不是同一个，隐藏当前这个
                                    nowDisplayFrameLayout.visibility = View.INVISIBLE
                                    //显示现在这个
                                    this.frameLayout.visibility = View.VISIBLE
                                    nowDisplayFrameLayout = this.frameLayout
                                }

                                binding.root.setDrawerLockMode(
                                    if (allowSideDrawer) LOCK_MODE_UNLOCKED else LOCK_MODE_LOCKED_CLOSED
                                )
                                binding.floatButtonShadowLayout.visibility =
                                    if (showAddButton) View.VISIBLE else View.INVISIBLE

                            }
                            break
                        }
                        i++
                    }
                }
        }
    }

    override fun onBackPressed() {
        val startMain = Intent(Intent.ACTION_MAIN)
        startMain.addCategory(Intent.CATEGORY_HOME)
        startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(startMain)
    }

    //已经重建过的fragment
    private var hasRecreatedFragments: ArrayList<Fragment> = ArrayList()
    private fun changeAppLanguage() {
        hasRecreatedFragments = ArrayList()
        initExpandableBottomBar(recreate = true)
        initFloatButton(recreate = true)
        initDrawer(recreate = true)
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onEvent(str: String?) {
        when (str) {
            EventType.LANGUAGE_CHANGE -> changeAppLanguage()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    //重建fragment
    fun recreateFragment(fragment: Fragment) {
        Log.e("重建fragment", fragment.javaClass.name)
        hasRecreatedFragments.add(fragment)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            supportFragmentManager.beginTransaction().detach(fragment).commitNowAllowingStateLoss()
            supportFragmentManager.beginTransaction().attach(fragment).commitNowAllowingStateLoss()
        } else {
            supportFragmentManager.beginTransaction().detach(fragment).attach(fragment)
                .commitNowAllowingStateLoss()
        }
    }

}