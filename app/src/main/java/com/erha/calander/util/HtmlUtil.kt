package com.erha.calander.util

object HtmlUtil {
    fun convertHtmlToOnlyText(html: String): String {
        var s = html.replace("<.{1,10}?>", "")
        s = s.trim()
        s = s.replace("&nbsp;", "")
        s = s.replace(
            "<input type=\\\"checkbox\\\" name=\\\"[0-9]{12,14}\\\" value=\\\"[0-9]{12,14}\\\">",
            ""
        )
        s = s.replace(
            "<img src=\\\".{0,200}?\\\" alt=\\\".{0,30}\\\" width=\\\"[0-9]{0,5}\\\">",
            ""
        )
        return s
    }
}