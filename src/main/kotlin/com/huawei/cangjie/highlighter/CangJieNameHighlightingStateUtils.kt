
@file:JvmName("CangJieNameHighlightingStateUtils")

package com.huawei.cangjie.highlighter

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.TestOnly

private val NAME_HIGHLIGHTING_STATE_KEY = Key<Boolean>("CANGJIE_NAME_HIGHLIGHTING_STATE")


var Project.isNameHighlightingEnabled: Boolean
    get() = getUserData(NAME_HIGHLIGHTING_STATE_KEY) ?: true
    internal set(value) {

        val valueToPut = if (!value) false else null
        putUserData(NAME_HIGHLIGHTING_STATE_KEY, valueToPut)
    }

@TestOnly

fun Project.withNameHighlightingDisabled(block: () -> Unit) {
    val oldValue = isNameHighlightingEnabled
    try {
        isNameHighlightingEnabled = false
        block()
    } finally {
        isNameHighlightingEnabled = oldValue
    }
}
