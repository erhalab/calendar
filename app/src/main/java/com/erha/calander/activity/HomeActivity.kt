package com.erha.calander.activity


import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import cn.authing.guard.Authing
import cn.authing.guard.data.UserInfo
import cn.authing.guard.network.AuthClient
import com.erha.calander.R
import com.erha.calander.dao.ConfigDao
import com.erha.calander.dao.SecretKeyDao
import com.erha.calander.databinding.ActivityHomeBinding
import com.erha.calander.fragment.*
import com.erha.calander.service.NotificationService
import com.erha.calander.timeline.utils.VectorDrawableUtils
import com.erha.calander.type.EventType
import com.erha.calander.type.LocalStorageKey
import com.erha.calander.util.CalendarUtil
import com.erha.calander.util.GuideEntity
import com.erha.calander.util.GuideUtil
import com.erha.calander.util.TinyDB
import com.mikepenz.iconics.Iconics
import com.mikepenz.iconics.typeface.library.fontawesome.FontAwesome
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.model.ProfileDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.iconBitmap
import com.mikepenz.materialdrawer.model.interfaces.iconDrawable
import com.qmuiteam.qmui.util.QMUIDisplayHelper
import com.tencent.smtt.export.external.TbsCoreSettings
import com.tencent.smtt.sdk.QbSdk
import com.tencent.smtt.sdk.QbSdk.*
import devlight.io.library.ntb.NavigationTabBar
import es.dmoral.toasty.Toasty
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.*


class HomeActivity : AppCompatActivity(), NavigationTabBar.OnTabBarSelectedIndexListener,
    MenuEventCallback {
    data class FragmentObject(
        var fragment: Fragment,
        var identity: String
    ) {
        var showAddButton = true
        var allowSideDrawer = true
    }

    //??????binding
    private lateinit var binding: ActivityHomeBinding
    private val fragmentObjects = ArrayList<FragmentObject>()
    private lateinit var store: TinyDB
    private var userInfo: UserInfo? = null

    private val depthPageTransformer = DepthPageTransformer()

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.e("onCreate Activity??????", this.javaClass.name)

        Iconics.registerFont(FontAwesome)
        Iconics.registerFont(GoogleMaterial)
        window.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = getColor(R.color.default_background_color)
            decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        super.onCreate(savedInstanceState)
        //?????????????????????????????????????????????????????????
        initNotificationService()

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        store = TinyDB(binding.root.context)

        //?????????????????????????????????
        EventBus.getDefault().register(this)

        //?????????????????????????????????????????????
        var sta = store.getString(LocalStorageKey.LANGUAGE)
        if (sta == null || sta.isBlank()) {
            store.putString(LocalStorageKey.LANGUAGE, Locale.getDefault().language)
            sta = store.getString(LocalStorageKey.LANGUAGE)
        }
        sta?.apply {
            when (this) {
                //?????????????????????????????????
                Locale.SIMPLIFIED_CHINESE.language, Locale.ENGLISH.language -> {
                    val locale = Locale(this)
                    val res: Resources = resources
                    val dm: DisplayMetrics = res.displayMetrics
                    val conf: Configuration = res.configuration
                    conf.locale = locale
                    res.updateConfiguration(conf, dm)
                    Locale.setDefault(locale)
                    onConfigurationChanged(conf)
                }
                else -> {
                    //??????????????????????????????????????????
                    store.putString("language", "")
                }
            }
        }

        //???????????????
        Authing.init(applicationContext, SecretKeyDao.AuthingAppID)
        Log.e("Authing.init", this.javaClass.name)

        initFragment()
        initNavigation()
        initFloatButton()
        initToast()
        initDrawer()

        // ?????????TBS??????????????????WebView????????????????????????
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
        val list = ArrayList<Fragment>()
        for (i in fragmentObjects) {
            list.add(i.fragment)
        }
        binding.viewPager2.adapter = MonitorPagerAdapter(this, list)
        binding.viewPager2.isUserInputEnabled = false
        binding.viewPager2.setPageTransformer(depthPageTransformer)

        intent?.apply {
            getStringExtra("defaultPage")?.apply {
                gotoTab(this)
            }
        }
    }

    private fun loadGuide() {
        val guideVersion = 1
        if (!ConfigDao.isDisplayingAnyGuide && !GuideUtil.getGuideStatus(
                binding.root.context,
                this.javaClass.name,
                guideVersion
            ) && binding.floatButton.visibility == View.VISIBLE && binding.ntb.visibility == View.VISIBLE
        ) {
            ConfigDao.isDisplayingAnyGuide = true
            val list = listOf(
                GuideEntity(
                    view = binding.floatButton,
                    title = "????????????",
                    text = "?????????????????????????????????"
                ),
                GuideEntity(
                    view = binding.ntb,
                    title = "?????????",
                    text = "???????????????????????????????????????"
                )
            )
            var i = 0
            GuideUtil.getDefaultBuilder(this, list[i++])
                .setGuideListener {
                    GuideUtil.getDefaultBuilder(this, list[i++])
                        .setGuideListener {
                            ConfigDao.isDisplayingAnyGuide = false
                            GuideUtil.updateGuideStatus(
                                binding.root.context,
                                this.javaClass.name,
                                guideVersion
                            )
                        }.build().show()
                }.build().show()
        }
    }

    override fun onStart() {
        super.onStart()
        loadGuide()
    }

    private fun gotoTab(tabName: String) {
        for (i in 0 until fragmentObjects.size) {
            if (fragmentObjects[i].identity == tabName) {
                binding.viewPager2.setCurrentItem(i, false)
                binding.root.setDrawerLockMode(
                    if (fragmentObjects[i].allowSideDrawer) DrawerLayout.LOCK_MODE_UNLOCKED else DrawerLayout.LOCK_MODE_LOCKED_CLOSED
                )
                binding.floatButtonShadowLayout.visibility =
                    if (fragmentObjects[i].showAddButton) View.VISIBLE else View.INVISIBLE
                break
            }
        }
        for (i in 0 until binding.ntb.models.size) {
            if (binding.ntb.models[i].title == tabName) {
                binding.ntb.modelIndex = i
                break
            }
        }
    }

    private fun initX5() {
        // ?????????TBS??????????????????WebView????????????????????????
        val map = HashMap<String, Any>()
        map[TbsCoreSettings.TBS_SETTINGS_USE_SPEEDY_CLASSLOADER] = true
        map[TbsCoreSettings.TBS_SETTINGS_USE_DEXLOADER_SERVICE] = true
        map[TbsCoreSettings.TBS_SETTINGS_USE_PRIVATE_CLASSLOADER] = true
        QbSdk.initTbsSettings(map)
        QbSdk.unForceSysWebView()
        QbSdk.setDownloadWithoutWifi(true)
        QbSdk.initX5Environment(applicationContext, object : PreInitCallback {
            override fun onCoreInitFinished() {
                // ?????????????????????
                Log.e("X5", "?????????????????????")
            }

            /**
             * ??????????????????
             * ??????X5?????????????????????????????????????????????????????????????????????????????????????????????????????????false???????????????????????????????????????
             * @param isX5 ????????????X5??????
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
        //????????????Service
        val serviceIntent = Intent(this@HomeActivity, NotificationService::class.java)
        startService(serviceIntent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
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

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.apply {
            getStringExtra("defaultPage")?.apply {
                gotoTab(this)
            }
        }
    }


    private fun initToast() {
        Toasty.Config.getInstance()
            .allowQueue(false) // optional (prevents several Toastys from queuing)
            .apply() // required
    }


    //?????????fragment
    private fun initFragment() {
        fragmentObjects.addAll(
            listOf(
                FragmentObject(
                    fragment = TaskPaneFragment().apply {
                        menuEventCallback = this@HomeActivity
                    },
                    identity = "??????"
                ),
                FragmentObject(
                    fragment = CalendarFragment(),
                    identity = "??????"
                )
                    .apply {
                        allowSideDrawer = false
                    },
                FragmentObject(
                    fragment = DeadlineFragment(),
                    identity = "?????????"
                )
                    .apply {
                        allowSideDrawer = false
                        showAddButton = true
                    },
                FragmentObject(
                    fragment = AccountFragment(),
                    identity = "??????"
                )
                    .apply {
                        allowSideDrawer = false
                        showAddButton = false
                    },
                FragmentObject(
                    fragment = SettingsFragment(),
                    identity = "??????"
                )
                    .apply {
                        allowSideDrawer = false
                        showAddButton = false
                    }
            )
        )
    }

    //??????????????????
    private fun initDrawer(recreate: Boolean = false) {
        binding.crossFadeLargeView.apply {
            var correctIndex = 0
            if (store.getBoolean(LocalStorageKey.USER_IS_LOGIN)) {
                store.getString(LocalStorageKey.USER_PHOTO_CACHE_ID)?.apply {
                    if (this.isNotEmpty()) {
                        var id = this
                        itemAdapter.add(
                            ProfileDrawerItem().apply {
                                iconBitmap = BitmapFactory.decodeFile(
                                    File(
                                        File(
                                            binding.root.context.filesDir,
                                            "avator"
                                        ), id
                                    ).path
                                )
                            }
                        )
                        correctIndex++
                    }
                }
            }
            itemAdapter.add(
                PrimaryDrawerItem().apply {
                    iconDrawable = VectorDrawableUtils.getDrawable(
                        binding.root.context, R.drawable.ic_baseline_wb_sunny_24,
                        Color.BLACK
                    )
                },
                PrimaryDrawerItem().apply {
                    iconDrawable = VectorDrawableUtils.getDrawable(
                        binding.root.context,
                        R.drawable.ic_baseline_inbox_24,
                        Color.BLACK
                    )
                },
                PrimaryDrawerItem().apply {
                    iconDrawable = VectorDrawableUtils.getDrawable(
                        binding.root.context,
                        R.drawable.ic_baseline_playlist_play_24,
                        Color.BLACK
                    )
                },
                PrimaryDrawerItem().apply {
                    iconDrawable = VectorDrawableUtils.getDrawable(
                        binding.root.context,
                        R.drawable.ic_baseline_playlist_add_check_24,
                        Color.BLACK
                    )
                },
                PrimaryDrawerItem().apply {
                    iconDrawable = VectorDrawableUtils.getDrawable(
                        binding.root.context,
                        R.drawable.ic_baseline_playlist_remove_24,
                        Color.BLACK
                    )
                },
            )
            onDrawerItemClickListener = { v, drawerItem, position ->
                for (i in fragmentObjects) {
                    var g = i.fragment
                    if (g is TaskPaneFragment) {
                        g.gotoPage(position - correctIndex)
                    }
                }
                binding.root.close()
                true
            }
        }
        binding.crossFadeSmallView.drawer = binding.crossFadeLargeView
        binding.crossFadeSmallView.background = binding.crossFadeLargeView.background
    }

    //?????????????????????
    private fun initFloatButton(recreate: Boolean = false) {
        if (!recreate) {
            binding.floatButtonShadowLayout.apply {
                setRadiusAndShadow(
                    100, QMUIDisplayHelper.dp2px(binding.root.context, 5), 0.1F
                )
            }
        }
        binding.floatButton.apply {
            if (recreate) {

            } else {

                setOnClickListener {
                    val i = Intent(this@HomeActivity, AddSimpleTaskActivity::class.java)
                    if (binding.ntb.models[binding.ntb.modelIndex].title == "?????????") {
                        i.putExtra("isDDL", true)
                        i.putExtra("date", CalendarUtil.getWithoutTime())
                        i.putExtra("hasDefaultDate", true)
                    }
                    for (g in fragmentObjects) {
                        val fragment = g.fragment
                        if (fragment is TaskPaneFragment) {
                            if (fragment.isDisplayingToday() && binding.ntb.models[binding.ntb.modelIndex].title == "??????") {
                                i.putExtra("date", CalendarUtil.getWithoutTime())
                                i.putExtra("hasDefaultDate", true)
                            }
                        }
                    }
                    startActivity(i)
                }
            }

        }
    }

    //?????????????????????
    private fun initNavigation() {
        val navigationTabBar = binding.ntb
        val models: ArrayList<NavigationTabBar.Model> = ArrayList()
        models.add(
            NavigationTabBar.Model.Builder(
                resources.getDrawable(R.drawable.ic_baseline_check_box_24),
                resources.getColor(R.color.default_background_color)
            ).title("??????")
                .build()
        )
        models.add(
            NavigationTabBar.Model.Builder(
                resources.getDrawable(R.drawable.ic_round_calendar_month_24),
                resources.getColor(R.color.default_background_color)
            ).title("??????")
                .build()
        )
        models.add(
            NavigationTabBar.Model.Builder(
                resources.getDrawable(R.drawable.ic_round_flag_24),
                resources.getColor(R.color.default_background_color)
            ).title("?????????")
                .build()
        )
        models.add(
            NavigationTabBar.Model.Builder(
                resources.getDrawable(R.drawable.ic_baseline_account_circle_24),
                resources.getColor(R.color.default_background_color)
            ).title("??????")
                .build()
        )
        models.add(
            NavigationTabBar.Model.Builder(
                resources.getDrawable(R.drawable.ic_baseline_settings_24),
                resources.getColor(R.color.default_background_color)
            ).title("??????")
                .build()
        )
        navigationTabBar.models = models
        navigationTabBar.modelIndex = 0
        navigationTabBar.onTabBarSelectedIndexListener = this@HomeActivity
    }

    //????????????????????????????????????????????????
    override fun onStartTabSelected(model: NavigationTabBar.Model?, index: Int) {
        model?.apply {
            for (i in 0 until fragmentObjects.size) {
                fragmentObjects[i].apply {
                    if (model.title == this.identity) {
                        binding.viewPager2.apply {
                            binding.viewPager2.setCurrentItem(
                                i,
                                (i == currentItem + 1 || i == currentItem - 1)
                            )
                        }
                        binding.root.setDrawerLockMode(
                            if (allowSideDrawer) DrawerLayout.LOCK_MODE_UNLOCKED else DrawerLayout.LOCK_MODE_LOCKED_CLOSED
                        )
                        binding.floatButtonShadowLayout.visibility =
                            if (showAddButton) View.VISIBLE else View.INVISIBLE
                    }
                }
            }
        }
    }

    override fun onEndTabSelected(model: NavigationTabBar.Model?, index: Int) {
    }

    override fun onBackPressed() {
        val startMain = Intent(Intent.ACTION_MAIN)
        startMain.addCategory(Intent.CATEGORY_HOME)
        startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(startMain)
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onEvent(str: String?) {
        when (str) {
            EventType.LANGUAGE_CHANGE -> updateLanguage()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    private fun updateLanguage() {
    }

    override fun menuOnclick() {
        binding.root.open()
    }

}

class MonitorPagerAdapter(context: FragmentActivity, fragments: List<Fragment>) :
    FragmentStateAdapter(context) {
    var context: Context = context
    var fragments: List<Fragment> = ArrayList()

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }

    fun getFragment(position: Int): Fragment {
        return fragments[position]
    }

    override fun getItemCount(): Int {
        return fragments.size
    }

    init {
        this.fragments = fragments
    }
}

@RequiresApi(21)
class DepthPageTransformer : ViewPager2.PageTransformer {
    private val MIN_SCALE = 0.75f
    override fun transformPage(view: View, position: Float) {
        view.apply {
            val pageWidth = width
            when {
                position < -1 -> { // [-Infinity,-1)
                    // This page is way off-screen to the left.
                    alpha = 0f
                }
                position <= 0 -> { // [-1,0]
                    // Use the default slide transition when moving to the left page
                    alpha = 1f
                    translationX = 0f
                    translationZ = 0f
                    scaleX = 1f
                    scaleY = 1f
                }
                position <= 1 -> { // (0,1]
                    // Fade the page out.
                    alpha = 1 - position

                    // Counteract the default slide transition
                    translationX = pageWidth * -position
                    // Move it behind the left page
                    translationZ = -1f

                    // Scale the page down (between MIN_SCALE and 1)
                    val scaleFactor = (MIN_SCALE + (1 - MIN_SCALE) * (1 - Math.abs(position)))
                    scaleX = scaleFactor
                    scaleY = scaleFactor
                }
                else -> { // (1,+Infinity]
                    // This page is way off-screen to the right.
                    alpha = 0f
                }
            }
        }
    }
}
