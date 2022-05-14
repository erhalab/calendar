package com.erha.calander.util

import android.app.Activity
import android.content.Context
import android.view.View
import com.erha.calander.type.LocalStorageKey
import smartdevelop.ir.eram.showcaseviewlib.GuideView
import smartdevelop.ir.eram.showcaseviewlib.config.DismissType
import smartdevelop.ir.eram.showcaseviewlib.config.Gravity
import smartdevelop.ir.eram.showcaseviewlib.config.PointerType

data class GuideEntity(val view: View, val title: String, val text: String)

object GuideUtil {
    const val contentTextSize: Int = 17
    const val titleTextSize: Int = 20

    fun getGuideStatus(context: Context, guideName: String, guideVersion: Int): Boolean {
        return TinyDB(context).getBoolean("${LocalStorageKey.USER_GUIDE_STATE_PREFIX}${guideName}_${guideVersion}")
    }

    fun updateGuideStatus(context: Context, guideName: String, guideVersion: Int) {
        TinyDB(context).putBoolean(
            "${LocalStorageKey.USER_GUIDE_STATE_PREFIX}${guideName}_${guideVersion}",
            true
        )
    }

    fun getDefaultBuilder(activity: Activity, guideEntity: GuideEntity): GuideView.Builder {
        return GuideView.Builder(activity)
            .setGravity(Gravity.auto)
            .setContentTextSize(contentTextSize)
            .setTitleTextSize(titleTextSize)
            .setDismissType(DismissType.outside)
            .setPointerType(PointerType.arrow)
            .setTitle(guideEntity.title)
            .setContentText(guideEntity.text)
            .setTargetView(guideEntity.view)
    }
}