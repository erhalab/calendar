package com.erha.calander.model

class SimpleNEUClass {
    var day //星期几？
            : Int = 1
    var name //课程名字？
            : String = ""
    var position //教室？ 体育课没有教室的哦~~~~
            : String = ""
    var sections //第几节？ 这是一个数组哦，数组里面有1和2说明这节课在第1节和第2节
            : ArrayList<Int> = ArrayList()
    var teacher //授课老师？ 这里不是课程老师，而是这一次课的授课老师哦~ 两者不一样
            : String = ""
    var weeks //第几周有？ 一个数组，有1,3,5,7说明就是1,3,5,7周有
            : ArrayList<Int> = ArrayList()

    override fun toString(): String {
        return "-------- 课程信息 --------" + System.lineSeparator() +
                "星期几：" + day + System.lineSeparator() +
                "名称：" + name + System.lineSeparator() +
                "教室：" + (if (position.isEmpty()) "无教室信息" else position) + System.lineSeparator() +
                "节数：" + sections.size + System.lineSeparator() +
                "授课教师：" + teacher + System.lineSeparator() +
                "周数：" + weeks + System.lineSeparator()
    }
}