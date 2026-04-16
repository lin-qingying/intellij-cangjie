/*
 * Copyright 2026 LinQingYing. and contributors.
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
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectEx
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.psi.PsiElement
import com.intellij.util.application
import java.lang.Exception
/**
 * 在智能模式下执行读操作
 *
 * 该函数确保在项目中执行的读操作符合智能模式的要求，以避免在读操作期间对项目文件进行修改
 * 如果当前应用已经允许读访问，则直接执行提供的操作；否则，通过DumbService在智能模式下执行
 *
 * @param action 要执行的读操作，以lambda表达式形式提供
 * @return 执行操作的结果，类型为泛型T
 */
fun <T> Project.runReadActionInSmartMode(action: () -> T): T {
    // 检查当前应用是否已经允许读访问
    if (ApplicationManager.getApplication().isReadAccessAllowed) return action()

    // 使用DumbService在智能模式下执行读操作


    return DumbService.getInstance(this).runReadActionInSmartMode(
        Computable(
            action
        )
    )
//    return DumbService.getInstance(this).runReadActionInSmartMode(Computable {
//        action()
//    })
//    val future = CompletableFuture<T>()
//    DumbService.getInstance(this).runWhenSmart {
//        try {
//            // 执行你想要的操作并获取返回值
//            future.complete(action())
//        } catch (e: Exception) {
//            future.completeExceptionally(e)
//        }
//    }
//    return future.get()
}

@Suppress("NOTHING_TO_INLINE")
inline fun isApplicationInternalMode(): Boolean = ApplicationManager.getApplication().isInternal
inline fun <T> runAction(runImmediately: Boolean, crossinline action: () -> T): T {
    if (runImmediately) {
        return action()
    }

    var result: T? = null
    ApplicationManager.getApplication().invokeAndWait {
        CommandProcessor.getInstance().runUndoTransparentAction {
            result = ApplicationManager.getApplication().runWriteAction<T> { action() }
        }
    }
    return result!!
}

fun <T> executeInBackgroundWithProgress(
    project: Project? = null,
    @NlsContexts.ProgressTitle title: String,
    block: () -> T
): T {
    assert(!ApplicationManager.getApplication().isWriteAccessAllowed) {
        "Rescheduling computation into the background is impossible under the write lock"
    }
    return ProgressManager.getInstance().runProcessWithProgressSynchronously(
        ThrowableComputable { block() }, title, true, project
    )
}

fun Project.executeWriteCommand(@NlsContexts.Command name: String, command: () -> Unit) {
    CommandProcessor.getInstance().executeCommand(this, { runWriteAction(command) }, name, null)
}
fun <T> Project.executeCommand(@NlsContexts.Command name: String, groupId: Any? = null, command: () -> T): T {
    @Suppress("UNCHECKED_CAST") var result: T = null as T
    CommandProcessor.getInstance().executeCommand(this, { result = command() }, name, groupId)
    @Suppress("USELESS_CAST")
    return result as T
}

fun <T> Project.executeWriteCommand(@NlsContexts.Command name: String, groupId: Any? = null, command: () -> T): T {
    return executeCommand(name, groupId) { runWriteAction(command) }
}
val isUnitTestMode: Boolean get() = ApplicationManager.getApplication().isUnitTestMode
val isDispatchThread: Boolean get() = ApplicationManager.getApplication().isDispatchThread
val isInternal: Boolean get() = ApplicationManager.getApplication().isInternal
fun assertIsNonDispatchThread() {
    application.assertIsNonDispatchThread()
}

/**
 * 在非轻量级项目上执行操作
 * 轻量级项目通常用于单元测试，需要特殊处理
 *
 * @param project 当前项目实例
 * @param action 要执行的操作
 */
inline fun runWithNonLightProject(project: Project, action: () -> Unit) {
    if ((project as? ProjectEx)?.isLight != true) {
        action()
    } else {
        check(isUnitTestMode)
    }
}

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

/**
 * Run [action] under a write action if needed, and run outside an action otherwise.
 */
fun <T> runWriteActionIfNeeded(isNeeded: Boolean, action: () -> T): T {
    if (isNeeded) {
        return ApplicationManager.getApplication().runWriteAction<T>(action)
    }
    return action()
}
fun <T> runWriteActionIfPhysical(e: PsiElement, action: () -> T): T = runWriteActionIfNeeded(e.isPhysical, action)
