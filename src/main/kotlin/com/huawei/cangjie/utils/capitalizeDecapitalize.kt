package com.huawei.cangjie.utils

fun String.toUpperCaseAsciiOnly(): String {
    val builder = StringBuilder(length)
    for (c in this) {
        builder.append(if (c in 'a'..'z') c.uppercaseChar() else c)
    }
    return builder.toString()
}


fun String.toLowerCaseAsciiOnly(): String {
    val builder = StringBuilder(length)
    for (c in this) {
        builder.append(if (c in 'A'..'Z') c.lowercaseChar() else c)
    }
    return builder.toString()
}
