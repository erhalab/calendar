package com.erha.calander.activity

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.CompoundButton
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.recyclical.datasource.emptyDataSourceTyped
import com.afollestad.recyclical.setup
import com.afollestad.recyclical.withItem
import com.erha.calander.R
import com.erha.calander.databinding.ActivitySettingModelBinding
import com.erha.calander.model.RecyclerViewItem
import com.erha.calander.util.TinyDB
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizeDp
import com.qmuiteam.qmui.layout.QMUILinearLayout
import com.qmuiteam.qmui.util.QMUIDisplayHelper
import com.qmuiteam.qmui.widget.grouplist.QMUICommonListItemView


class SettingModelActivity : AppCompatActivity() {
    data class ModelItem(
        var key: String,
        var iconKey: IIcon,
        var titleResId: Int,
        var subtitleResId: Int
    ) : RecyclerViewItem() {
        class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val qmuiCommonListItemView: QMUICommonListItemView =
                itemView.findViewById(R.id.modelListItemView)
        }
    }

    //布局binding
    private lateinit var binding: ActivitySettingModelBinding
    private lateinit var store: TinyDB
    private val dataSource = emptyDataSourceTyped<RecyclerViewItem>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingModelBinding.inflate(layoutInflater)
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
        binding.modelRecyclerViewQMUILinearLayout.radius =
            resources.getDimensionPixelSize(R.dimen.listview_radius)
        dataSource.add(
            ModelItem(
                key = "calendar",
                titleResId = R.string.setting_model_calendar,
                subtitleResId = R.string.setting_model_calendar_subtitle,
                iconKey = GoogleMaterial.Icon.gmd_today
            ),
            ModelItem(
                key = "search",
                titleResId = R.string.setting_model_search,
                subtitleResId = R.string.setting_model_search_subtitle,
                iconKey = GoogleMaterial.Icon.gmd_search
            ),
            ModelItem(
                key = "focus",
                titleResId = R.string.setting_model_focus,
                subtitleResId = R.string.setting_model_focus_subtitle,
                iconKey = GoogleMaterial.Icon.gmd_adjust
            )
        )
        //初始化列表
        binding.modelRecyclerView.setup {
            withDataSource(dataSource)
            withItem<ModelItem, ModelItem.Holder>(R.layout.item_list_model) {
                onBind(ModelItem::Holder) { index, item ->
                    this.qmuiCommonListItemView.apply {
                        text = getString(item.titleResId)
                        detailText = getString(item.subtitleResId)
                        accessoryType = QMUICommonListItemView.ACCESSORY_TYPE_SWITCH
                        orientation = QMUICommonListItemView.VERTICAL
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
                                sizeDp = 20
                            })
                        val paddingVer = QMUIDisplayHelper.dp2px(binding.root.context, 20)
                        setPadding(
                            paddingLeft, paddingVer,
                            paddingRight, paddingVer
                        )
                        switch.isChecked = store.getBoolean("setting_model_${item.key}")
                        switch
                            .setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
                                Log.e(
                                    "OnCheckedChangeListener",
                                    "key=${item.key} isSelect=$isChecked"
                                )
                                store.putBoolean("setting_model_${item.key}", isChecked)
                                var i = store.getBoolean("setting_model_${item.key}")
                                Log.e("Store", "key=${item.key} isSelect=$i")
                            })
                    }
                }
                onClick { index ->
                    var t = binding.modelRecyclerView.getChildAt(index) as QMUILinearLayout
                    var i = t.getChildAt(0) as QMUICommonListItemView
                    i.switch.isChecked = !(i.switch.isChecked)
                }
                onLongClick { index ->
                }
            }
        }


    }

}