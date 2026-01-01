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
import com.intellij.build.events.BuildEventsNls
import com.intellij.build.events.MessageEvent
import com.intellij.build.events.StartEvent
import com.intellij.build.events.impl.*
import com.intellij.build.issue.BuildIssue
import org.jetbrains.annotations.Nls

/**
 * BuildEvents 兼容层 - Legacy 版本 (242-252)
 *
 * 此文件用于 IntelliJ Platform 2024.2 到 2025.2 版本。
 * 在这些版本中，使用具体的实现类创建 BuildEvent 对象。
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
    return StartEventImpl(id, parentId, eventTime, message)
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
    return FinishEventImpl(id, parentId, eventTime, message, result)
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
    return FileMessageEventImpl(
        parentId,
        kind,
        group,
        message,
        detailedMessage,
        filePosition
    )
}

/**
 * 创建 OutputBuildEvent
 */
fun createOutputBuildEvent(
    parentId: Any,
    @BuildEventsNls.Message message: String,
    stdOut: Boolean
): com.intellij.build.events.BuildEvent {
    return OutputBuildEventImpl(parentId, message, stdOut)
}
