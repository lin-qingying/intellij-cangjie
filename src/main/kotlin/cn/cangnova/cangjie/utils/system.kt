package cn.cangnova.cangjie.utils

import com.intellij.openapi.util.SystemInfo

fun String.toSystemIndependentPath(): String {
    return if (SystemInfo.isWindows) {
        "$this.exe"
    } else {
        this
    }
}
