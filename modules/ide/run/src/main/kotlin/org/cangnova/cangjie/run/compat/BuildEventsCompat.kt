/*
 * Copyright 2025 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cangnova.cangjie.run.compat

import com.intellij.build.FilePosition
import com.intellij.build.events.BuildEvents
import com.intellij.build.events.BuildEventsNls
import com.intellij.build.events.MessageEvent
import com.intellij.build.events.StartEvent
import org.jetbrains.annotations.Nls

/**
 * BuildEvents 兼容层 - Builder 版本 (253+)
 *
 * 此文件用于 IntelliJ Platform 2025.3 及以后版本。
 * 在这些版本中，使用 BuildEvents.getInstance() 的 Builder 模式创建事件对象。
 */

/**
 * 创建 StartEvent
 */
fun createStartEvent(
    id: Any,
    parentId: Any?,
    eventTime: Long,
    @BuildEventsNls.Message message: String
): StartEvent {
    return BuildEvents.getInstance()
        .start()
        .withId(id)
        .withParentId(parentId)
        .withTime(eventTime)
        .withMessage(message)
        .build()
}

/**
 * 创建 FinishEvent
 */
fun createFinishEvent(
    id: Any,
    parentId: Any?,
    eventTime: Long,
    @BuildEventsNls.Message message: String,
    result: com.intellij.build.events.EventResult
): com.intellij.build.events.FinishEvent {
    return BuildEvents.getInstance()
        .finish()
        .withStartId(id)
        .withParentId(parentId)
        .withTime(eventTime)
        .withMessage(message)
        .withResult(result)
        .build()
}

/**
 * 创建 FileMessageEvent
 */
fun createFileMessageEvent(
    parentId: Any,
    kind: MessageEvent.Kind,
    @BuildEventsNls.Title group: String,
    @BuildEventsNls.Message message: String,
    @Nls detailedMessage: String?,
    filePosition: FilePosition
): MessageEvent {
    return BuildEvents.getInstance()
        .fileMessage()
        .withParentId(parentId)
        .withKind(kind)
        .withGroup(group)
        .withMessage(message)
        .withDescription(detailedMessage)
        .withFilePosition(filePosition)
        .build()
}

/**
 * 创建 OutputBuildEvent
 */
fun createOutputBuildEvent(
    parentId: Any,
    @BuildEventsNls.Message message: String,
    stdOut: Boolean
): com.intellij.build.events.BuildEvent {
    return BuildEvents.getInstance()
        .output()
        .withParentId(parentId)
        .withMessage(message)
        .build()
}
