package com.erha.calander.popup;

import android.content.Context;

import androidx.annotation.NonNull;

import com.erha.calander.R;
import com.lxj.xpopup.core.AttachPopupView;
import com.qmuiteam.qmui.layout.QMUILinearLayout;

public class NotificationHelpPopup extends AttachPopupView {

    public NotificationHelpPopup(@NonNull Context context) {
        super(context);
    }

    @Override
    protected int getImplLayoutId() {
        return R.layout.popup_notification_help;
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        QMUILinearLayout i = findViewById(R.id.QMUILinearLayout);
        i.setRadius(getResources().getDimensionPixelSize(R.dimen.listview_radius));
    }
}