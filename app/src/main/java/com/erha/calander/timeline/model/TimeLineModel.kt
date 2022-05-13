package com.erha.calander.timeline.model

import com.erha.calander.timeline.SingleTimeLineCallback

class TimeLineModel(
        var message: String,
        var date: String,
        var status: OrderStatus,
        var id: Int = -1,
        var callback: SingleTimeLineCallback? = null
)
