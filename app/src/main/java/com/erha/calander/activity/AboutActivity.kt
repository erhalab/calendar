package com.erha.calander.activity

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.recyclical.datasource.emptyDataSourceTyped
import com.afollestad.recyclical.setup
import com.afollestad.recyclical.withItem
import com.erha.calander.R
import com.erha.calander.databinding.ActivityAboutBinding
import com.erha.calander.model.RecyclerViewItem
import com.erha.calander.util.TinyDB
import com.qmuiteam.qmui.layout.QMUILayoutHelper
import com.qmuiteam.qmui.layout.QMUILinearLayout
import com.qmuiteam.qmui.util.QMUIDisplayHelper
import com.qmuiteam.qmui.widget.grouplist.QMUICommonListItemView


class AboutActivity : AppCompatActivity() {
    data class AboutItem(
        var key: String,
        var titleResId: Int,
        var subtitleResId: Int?,
        var isFirst: Boolean = false,
        var isLast: Boolean = false,
        var imageResId: Int
    ) : RecyclerViewItem() {
        class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val qmuiCommonListItemView: QMUICommonListItemView =
                itemView.findViewById(R.id.aboutListItemView)
            val qmuiLinearLayout: QMUILinearLayout = itemView.findViewById(R.id.QMUILinearLayout)
        }
    }

    data class AboutSimpleItem(
        var key: String,
        var titleResId: Int,
        var isFirst: Boolean = false,
        var isLast: Boolean = false
    ) : RecyclerViewItem() {
        class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val qmuiCommonListItemView: QMUICommonListItemView =
                itemView.findViewById(R.id.aboutListItemView)
            val qmuiLinearLayout: QMUILinearLayout = itemView.findViewById(R.id.QMUILinearLayout)
        }
    }

    data class SpaceItem(
        var key: String = "space"
    ) : RecyclerViewItem() {
        class Holder(itemView: View) : RecyclerView.ViewHolder(itemView)
    }

    //布局binding
    private lateinit var binding: ActivityAboutBinding
    private lateinit var store: TinyDB
    private val dataSource = emptyDataSourceTyped<RecyclerViewItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
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
        binding.aboutVersionTextView.text =
            resources.getString(R.string.about_version, "1.0.0 Alpha")
        binding.backButton.setOnClickListener { v ->
            run {
                setResult(Activity.RESULT_CANCELED, Intent())
                finish()
            }
        }
        dataSource.add(
            AboutItem(
                key = "site",
                titleResId = R.string.about_visit_site,
                subtitleResId = R.string.about_visit_site_link,
                isFirst = true,
                imageResId = R.mipmap.icon_web
            ),
            AboutItem(
                key = "rate",
                titleResId = R.string.about_rate_us,
                subtitleResId = null,
                isLast = true,
                imageResId = R.mipmap.icon_star
            ),
            SpaceItem(),
            AboutItem(
                key = "wechat",
                titleResId = R.string.about_wechat,
                subtitleResId = R.string.about_wechat_id,
                isFirst = true,
                imageResId = R.mipmap.icon_wechat
            ),
            AboutItem(
                key = "redBook",
                titleResId = R.string.about_red_book,
                subtitleResId = R.string.about_red_book_id,
                isLast = true,
                imageResId = R.mipmap.icon_small_red_bok
            ),
            SpaceItem(),
            AboutSimpleItem(
                key = "tern",
                titleResId = R.string.about_terms_of_use,
                isFirst = true
            ),
            AboutSimpleItem(
                key = "privacy",
                titleResId = R.string.about_privacy
            ),
            AboutSimpleItem(
                key = "licenses",
                titleResId = R.string.about_licenses
            ),
            AboutSimpleItem(
                key = "tern",
                titleResId = R.string.about_acknowledgements,
                isLast = true
            )
        )
        //初始化列表
        binding.aboutRecyclerView.setup {
            withDataSource(dataSource)
            withItem<SpaceItem, SpaceItem.Holder>(R.layout.item_list_space_20) {
                onBind(SpaceItem::Holder) { _, _ ->
                }
            }
            withItem<AboutItem, AboutItem.Holder>(R.layout.item_list_about) {
                onBind(AboutItem::Holder) { index, item ->
                    val paddingVer = QMUIDisplayHelper.dp2px(binding.root.context, 15)
                    this.qmuiCommonListItemView.apply {
                        text = getString(item.titleResId)
                        accessoryType = QMUICommonListItemView.ACCESSORY_TYPE_CHEVRON
                        item.subtitleResId?.apply {
                            detailText = getString(this)
                        }
                        orientation = QMUICommonListItemView.HORIZONTAL
                        setPadding(
                            paddingLeft, paddingVer,
                            paddingRight, paddingVer
                        )
                        val imageView = ImageView(binding.root.context)
                        val width = QMUIDisplayHelper.dp2px(binding.root.context, 25)
                        var bmp: Bitmap = BitmapFactory.decodeResource(
                            resources,
                            item.imageResId
                        )
                        bmp = Bitmap.createScaledBitmap(bmp, width, width, true)
                        imageView.setImageBitmap(bmp)
                        setImageDrawable(imageView.drawable)
                    }
                    val radius =
                        resources.getDimensionPixelSize(R.dimen.listview_radius)
                    if (item.isFirst && item.isLast) {
                        qmuiLinearLayout.radius = radius
                    } else if (item.isLast) {
                        qmuiLinearLayout.setRadius(radius, QMUILayoutHelper.HIDE_RADIUS_SIDE_TOP)
                    } else if (item.isFirst) {
                        qmuiLinearLayout.setRadius(radius, QMUILayoutHelper.HIDE_RADIUS_SIDE_BOTTOM)
                    }
                }
                onClick { index ->
                    when (item.key) {
                        "site" -> {
                            startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://qugeek.com/blog")
                                )
                            )
                        }
                        "rate" -> {
                            try {
                                startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("market://details?id=com.tencent.mobileqq")
                                    )
                                )
                            } catch (e: ActivityNotFoundException) {
                            }
                        }
                        else -> Toast.makeText(
                            binding.root.context,
                            R.string.text_developing,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            withItem<AboutSimpleItem, AboutSimpleItem.Holder>(R.layout.item_list_about) {
                onBind(AboutSimpleItem::Holder) { index, item ->
                    val paddingVer = QMUIDisplayHelper.dp2px(binding.root.context, 20)
                    this.qmuiCommonListItemView.apply {
                        text = getString(item.titleResId)
                        accessoryType = QMUICommonListItemView.ACCESSORY_TYPE_CHEVRON
                        orientation = QMUICommonListItemView.HORIZONTAL
                        setPadding(
                            paddingLeft, paddingVer,
                            paddingRight, paddingVer
                        )
                    }
                    val radius =
                        resources.getDimensionPixelSize(R.dimen.listview_radius)
                    if (item.isFirst && item.isLast) {
                        qmuiLinearLayout.radius = radius
                    } else if (item.isLast) {
                        qmuiLinearLayout.setRadius(radius, QMUILayoutHelper.HIDE_RADIUS_SIDE_TOP)
                    } else if (item.isFirst) {
                        qmuiLinearLayout.setRadius(radius, QMUILayoutHelper.HIDE_RADIUS_SIDE_BOTTOM)
                    }
                }
                onClick { index ->
                    when (item.key) {
                        else -> {
                            Toast.makeText(
                                binding.root.context,
                                R.string.text_developing,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }

    }

}