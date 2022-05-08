package com.erha.calander.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

/**
 * 自定义 广播接收者 开机自动启动应用
 * 继承 android.content.BroadcastReceiver
 */
public class AutoStartReceiver extends BroadcastReceiver {
    private final String TAG = "AutoStartReceiver";
    private final String ACTION_BOOT = "android.intent.action.BOOT_COMPLETED";

    /**
     * 接收广播消息后都会进入 onReceive 方法，然后要做的就是对相应的消息做出相应的处理
     *
     * @param context 表示广播接收器所运行的上下文
     * @param intent  表示广播接收器收到的Intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, intent.getAction());
        Toast.makeText(context, intent.getAction(), Toast.LENGTH_LONG).show();
        /**
         * 如果 系统 启动的消息，则启动 APP 的服务，确保通知能够被推送
         */
        if (ACTION_BOOT.equals(intent.getAction())) {
            Intent intentService = new Intent(context, NotificationService.class);
            context.startService(intentService);
        }
    }
}