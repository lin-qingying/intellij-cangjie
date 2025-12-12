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
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.utils

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import java.lang.Exception
@Suppress("NOTHING_TO_INLINE")
inline fun isApplicationInternalMode(): Boolean = ApplicationManager.getApplication().isInternal

val isUnitTestMode: Boolean get() = ApplicationManager.getApplication().isUnitTestMode
val isDispatchThread: Boolean get() = ApplicationManager.getApplication().isDispatchThread
val isInternal: Boolean get() = ApplicationManager.getApplication().isInternal


fun <T> invokeAndWaitIfNeeded(modalityState: ModalityState? = null, runnable: () -> T): T {
    val app = ApplicationManager.getApplication()
    if (app.isDispatchThread) {
        return runnable()
    } else {
        var resultRef: T? = null
        app.invokeAndWait({ resultRef = runnable() }, modalityState ?: ModalityState.defaultModalityState())
        @Suppress("UNCHECKED_CAST")
        return resultRef as T
    }
}

/**
 * 在一个带有取消进度的项目中计算给定的任务
 *
 * 此函数用于在可能需要长时间运行的任务中显示一个带有取消功能的进度条
 * 它根据环境是否是单元测试模式来决定任务的执行方式
 *
 * @param title 进度条的标题，用于向用户显示当前任务的简短描述
 * @param supplier 一个提供任务结果的lambda表达式，它会在一个带有进度条的环境中执行
 * @return 任务的结果，类型由调用者指定
 *
 * 注意：此函数使用了`ProgressManager`来管理进度条的显示和取消逻辑，这是JetBrains IDEA和其他IntelliJ平台产品中的一个组件
 * 在单元测试模式下，进度条将被跳过，任务将直接执行，以避免在测试环境中显示UI元素
 */
fun <T> Project.computeWithCancelableProgress(
    @Suppress("UnstableApiUsage") @NlsContexts.ProgressTitle title: String,
    supplier: () -> T
): T {
    // 如果是单元测试模式，直接执行任务并返回结果，不显示进度条
    if (isUnitTestMode) {
        return supplier()
    }
    // 在非单元测试模式下，使用ProgressManager运行任务，并显示带有取消功能的进度条
    return ProgressManager.getInstance().runProcessWithProgressSynchronously<T, Exception>(supplier, title, true, this)
}

