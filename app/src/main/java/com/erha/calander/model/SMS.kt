package com.erha.calander.data

import java.util.*

data class SingleSMS(
    var phone: String,
    var code: String,
    var createTime: Calendar,
    var expiredTime: Calendar,
    var used: Boolean = false
)