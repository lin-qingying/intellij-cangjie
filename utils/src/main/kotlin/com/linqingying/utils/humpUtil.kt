package com.linqingying.utils

/**
 * 将下划线命名转换为大驼峰命名
 */

fun String.toCamelCase( ): String {
    return this.split('_')
        .joinToString("") { it.capitalize() }
}
