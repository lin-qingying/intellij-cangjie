package com.huawei.cangjie.ide.formatter.cjfmt

import com.huawei.cangjie.CangJieBundle
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.util.regex.Pattern

private val SUCCESS_MESSAGE: String = CangJieBundle.message("message.format_success")
private val FAIL_MESSAGE: String = CangJieBundle.message("message.format_fail")


enum class Type {
    INFO,
    WARN,
    ERROR
}

object FormatConstant {
    val AVAILABLE_LANGUAGE: Pattern = Pattern.compile("^(.+)\\.(cj)$")
    const val CJFMT_WINDOWS: String = "cjfmt.exe"
    const val CJFMT_MAC: String = "cjfmt"
    const val REFORMAT_CODE: String = "Reformat Code"
    const val REFORMAT_DIR: String = "-d"
}

fun notifyResult(isSucceed: Boolean, project: Project) {
    val message: String =
        if (isSucceed) SUCCESS_MESSAGE else FAIL_MESSAGE
    showWithProject(message, "Reformat Code", Type.INFO, project)
}

fun checkFile(file: VirtualFile): Boolean {
    return if (!file.exists()) false else FormatConstant.AVAILABLE_LANGUAGE.matcher(file.name).matches()
}
fun getFormatToolPath(executePath: String): MutableList<String> {
    val processArgs: MutableList<String> = mutableListOf()
    if (SystemInfo.isWindows) {
        processArgs.add(executePath + File.separator + "cjfmt.exe")
    } else {
        processArgs.add(executePath + File.separator + "cjfmt")
    }

    return processArgs
}
private fun show(
    message: String,
    title: String,
    type: Type,
    project: Project
) {
    val nType =
        if (type == Type.ERROR) NotificationType.ERROR else (if (type == Type.WARN) NotificationType.WARNING else (if (type == Type.INFO) NotificationType.INFORMATION else NotificationType.IDE_UPDATE))
    val notification = Notification("System Messages", title, message, nType)
    Notifications.Bus.notify(notification, project)
}

fun showWithProject(
    message: String?,
    title: String,
    type: Type,
    project: Project
) {
    if (message != null && "" != message) {
        show(message, title, type, project)
    }
}
