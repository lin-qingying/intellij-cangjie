package com.huawei.cangjie.cjpm.project.model

import com.intellij.build.events.BuildEventsNls
import com.intellij.execution.process.ProcessListener

@Suppress("UnstableApiUsage")
interface ProcessProgressListener : ProcessListener {
    fun error(@BuildEventsNls.Title title: String, @BuildEventsNls.Message message: String)
    fun warning(@BuildEventsNls.Title title: String, @BuildEventsNls.Message message: String)
}
