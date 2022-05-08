# 日程管理

为什么创造了它：安卓课的作业。

## 应用预览图

|  日历视图   | 设置界面  | 通知管理 | 提醒时机 | 自定义提醒 | 提醒方式 |
|  ----  | ----  | ----  | ----  | ----  | ----  |
| ![](https://pic.imgdb.cn/item/6277723f0947543129f4a8f3.jpg) | ![](https://pic.imgdb.cn/item/627772590947543129f51060.jpg) | ![](https://pic.imgdb.cn/item/627772680947543129f54a8c.jpg) | ![](https://pic.imgdb.cn/item/627774e00947543129fdf99e.jpg) | ![](https://pic.imgdb.cn/item/627773a40947543129f9c944.jpg) | ![](https://pic.imgdb.cn/item/627772df0947543129f706be.jpg) |

## 下载安卓安装包

地址一：https://raw.fastgit.org/erhalab/calendar/master/app/release/app-release.apk

地址二：https://ghproxy.fsofso.com/https://github.com/erhalab/calendar/blob/master/app/release/app-release.apk

地址三：https://raw.iqiq.io/erhalab/calendar/master/app/release/app-release.apk

地址四：https://hk1.monika.love/erhalab/calendar/master/app/release/app-release.apk

## 功能描述

### 日历视图

魔改自一个库。

自主实现设置第一周所在日期，修改后的组件支持状态保存（重写onSaveInstanceState和onRestoreInstanceState）。

### 通知管理

自主实现复杂的通知队列，使用Service实现实时推送。

推送支持：通知互斥、渠道、取消订阅某事项。

队列支持：相对完善的增删改查，异步移除已发布的通知。

### 自动导入课程

仅支持我所在的学校，实现方式参考小爱课程表。

实现复杂的导入机制，相对完善的用户引导。

兼容各种机型的各种魔改Webview，若默认Webview启动失败，则启用X5。

## 库

基础UI：https://github.com/Tencent/QMUI_Android

底层日历视图：https://github.com/ArleAndino/WeekView




