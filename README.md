# 日程管理

为什么创造了它：安卓课的作业。

## 应用预览图

![](https://wkphoto.cdn.bcebos.com/7af40ad162d9f2d30f6eaf8ab9ec8a136327cc5c.jpg)

![](https://wkphoto.cdn.bcebos.com/7a899e510fb30f245bd06766d895d143ad4b0308.jpg)

![](https://wkphoto.cdn.bcebos.com/e7cd7b899e510fb31753fcf2c933c895d1430c08.jpg)

![](https://wkphoto.cdn.bcebos.com/a8014c086e061d95582ebc5d6bf40ad162d9ca6f.jpg)

![](https://wkphoto.cdn.bcebos.com/b999a9014c086e064cc0b42e12087bf40ad1cb6f.jpg)

## 下载安卓安装包

推荐：https://www.pgyer.com/erhacalendar2

![](https://pic.imgdb.cn/item/6283274609475431290a8747.png)

地址一：https://raw.fastgit.org/erhalab/calendar/master/app/release/app-release.apk

地址二：https://ghproxy.fsofso.com/https://github.com/erhalab/calendar/blob/master/app/release/app-release.apk

地址三：https://raw.iqiq.io/erhalab/calendar/master/app/release/app-release.apk

地址四：https://hk1.monika.love/erhalab/calendar/master/app/release/app-release.apk

## 功能概述

【任务看板】 查看今日任务等

【日历视图】 清晰地展现任务，支持课表、自定义起始周、手势操作

【DDL视图】 展现DDL时间线，纵览大局，把握剩余时间

【任务细节】 支持三种状态：完成、正在进行、放弃；支持富文本编辑：插入图片等

【任务、课程提醒】 支持若干个自定义提醒时间；丰富的提醒方式：重复提醒、震动、铃声、锁屏显示等

【课表导入】 支持教务处课表自动导入

【隐私保护】 只在登录、导入课表时需要联网，所有数据仅在本地存储。不需要任何权限，零权限运行。

## 核心代码

### 日历视图

修改自开源库。

实现：设置第一周所在日期，组件支持状态保存（重写onSaveInstanceState和onRestoreInstanceState）。

### 通知管理

实现复杂的通知队列，使用Service实现实时推送。

推送支持：通知互斥、渠道、取消订阅某事项。

队列支持：相对完善的增删改查，异步移除已发布的通知。

### 自动导入课程

仅支持我所在的学校，实现方式参考了小爱课程表。

实现复杂的导入机制，相对完善的用户引导。

兼容各种机型的各种魔改Webview，若默认Webview启动失败，则启用X5。

## 致谢&开源库

提出意见的朋友们：小陆、爱挑毛病的杜同学

基础UI：https://github.com/Tencent/QMUI_Android

底层日历视图：https://github.com/ArleAndino/WeekView




