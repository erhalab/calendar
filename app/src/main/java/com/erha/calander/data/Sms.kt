package com.erha.calander.data

import java.util.*

object Sms {
    var list = ArrayList<SingleSms>()
}

data class SingleSms(
    var phone: String,
    var code: String,
    var createTime: Calendar,
    var expiredTime: Calendar,
    var used: Boolean = false
)