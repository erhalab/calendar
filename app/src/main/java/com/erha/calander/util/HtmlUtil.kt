package com.erha.calander.util

object HtmlUtil {
    fun convertHtmlToOnlyText(html: String): String {
        var s = html.replace(Regex("<.{1,10}?>"), "")
        s = s.trim()
        s = s.replace("&nbsp;", "")
        s = s.replace(
            Regex("<input type=\"checkbox\" name=\"[0-9]{12,14}\" value=\"[0-9]{12,14}\">"),
            ""
        )
        s = s.replace(
            Regex("<img src=\".{0,200}?\" alt=\".{0,30}\" width=\"[0-9]{0,5}\">"),
            ""
        )
        s = s.replace(
            Regex("<font color=\"#[0-9A-Za-z]{6}\">"),
            ""
        )
        return s
    }

    fun getHtmlFirstLineOnlyText(html: String): String {
        var i = html.indexOf("<br>")
        return if (i == -1) {
            convertHtmlToOnlyText(html)
        } else {
            convertHtmlToOnlyText(html.substring(0, i))
        }
    }
}