package com.linqingying.cangjie.ide.stubindex.resolve

import com.intellij.openapi.util.IntellijInternalApi
import com.intellij.util.messages.Topic

@IntellijInternalApi
interface CangJieCorruptedIndexListener {
    fun corruptionDetected()

    companion object {
        @Topic.ProjectLevel
        val TOPIC = Topic(CangJieCorruptedIndexListener::class.java)
    }
}
