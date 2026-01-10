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

package org.cangnova.cangjie.progress

import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicatorProvider

/**
 * 编译取消异常
 *
 * 该异常用于表示编译过程被取消。
 * 继承自 [ProcessCanceledException]，与 IntelliJ 平台的取消机制集成。
 */
open class CompilationCanceledException : ProcessCanceledException()

/**
 * 增量编译下一轮异常
 *
 * 在增量编译过程中，当需要开始下一轮编译时抛出此异常。
 */
class IncrementalNextRoundException : CompilationCanceledException()

/**
 * 编译取消状态接口
 *
 * 该接口定义了检查编译是否被取消的方法。
 */
interface CompilationCanceledStatus {
    /**
     * 检查编译是否被取消
     *
     * 如果编译被取消，该方法应抛出 [CompilationCanceledException]。
     *
     * @throws CompilationCanceledException 如果编译被取消
     */
    fun checkCanceled()
}

/**
 * 进度指示器和编译取消状态
 *
 * 该对象提供了统一的取消检查机制，结合了：
 * - IntelliJ 平台的进度指示器取消检查
 * - 自定义的编译取消状态检查
 *
 * 使用场景：
 * - 在长时间运行的编译任务中定期检查取消状态
 * - 响应用户的取消请求
 * - 支持自定义的取消逻辑
 */
object ProgressIndicatorAndCompilationCanceledStatus {
    /**
     * 当前设置的取消状态检查器
     */
    private var canceledStatus: CompilationCanceledStatus? = null

    /**
     * 设置编译取消状态检查器
     *
     * 该方法是线程安全的。
     *
     * @param newCanceledStatus 新的取消状态检查器，传入 null 表示清除
     */
    @JvmStatic
    @Synchronized
    fun setCompilationCanceledStatus(newCanceledStatus: CompilationCanceledStatus?) {
        canceledStatus = newCanceledStatus
    }

    /**
     * 检查是否取消
     *
     * 该方法会依次检查：
     * 1. IntelliJ 平台的进度指示器
     * 2. 自定义的编译取消状态（如果已设置）
     *
     * 如果任何一个检查表明应该取消，则抛出相应的异常。
     *
     * 使用示例：
     * ```kotlin
     * fun longRunningTask() {
     *     for (item in items) {
     *         ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
     *         // 处理 item
     *     }
     * }
     * ```
     *
     * @throws ProcessCanceledException 如果进度指示器取消
     * @throws CompilationCanceledException 如果编译被取消
     */
    @JvmStatic
    fun checkCanceled() {
        ProgressIndicatorProvider.checkCanceled()
        canceledStatus?.checkCanceled()
    }
}
