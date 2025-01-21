package com.linqingying.utils

import java.util.*

/**
 * 将下划线命名转换为大驼峰命名
 */

fun String.toCamelCase( ): String {
    return this.split('_')
        .joinToString("") { it.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } }
}
