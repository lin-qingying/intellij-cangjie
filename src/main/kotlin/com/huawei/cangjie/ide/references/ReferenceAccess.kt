package com.huawei.cangjie.ide.references


enum class ReferenceAccess(
    val isRead: Boolean,
    val isWrite: Boolean
) {
    READ(true, false),
    WRITE(false, true),
    READ_WRITE(true, true)
}
