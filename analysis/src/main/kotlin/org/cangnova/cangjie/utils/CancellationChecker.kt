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

import com.intellij.openapi.progress.ProgressManager

/**
 * 取消检查器接口
 *
 * 提供统一的任务取消检查机制。
 * IntelliJ 平台中的长时间运行任务（如代码分析、索引构建）需要定期检查是否被用户取消，
 * 以便及时响应用户的中断请求。
 *
 * **设计模式**：
 * - 策略模式（Strategy Pattern）：不同的取消检查策略可以互换
 * - 责任链模式（Chain of Responsibility）：多个检查器可以组合使用
 *
 * **典型使用场景**：
 * - 代码分析：在分析每个文件前检查是否被取消
 * - 代码补全：在计算补全项时定期检查
 * - 重构操作：在修改文件前检查用户是否取消
 * - 索引构建：在索引大量文件时检查
 *
 * **实现要求**：
 * - [check] 方法必须是轻量级的（执行时间 < 1ms）
 * - 如果任务被取消，抛出 [ProcessCanceledException]
 * - 不应该修改任何状态，只负责检查
 */
interface CancellationChecker {
    /**
     * 检查当前任务是否被取消
     *
     * 如果任务被取消，此方法会抛出 [ProcessCanceledException]，
     * 导致当前任务立即终止。
     *
     * @throws com.intellij.openapi.progress.ProcessCanceledException 如果任务被取消
     */
    fun check()
}

/**
 * 基于 ProgressManager 的取消检查器
 *
 * IntelliJ 平台的标准取消检查实现，使用 [ProgressManager.checkCanceled] 检查任务状态。
 *
 * **工作原理**：
 * 1. IntelliJ 为每个后台任务分配一个 [ProgressIndicator]
 * 2. 用户点击"取消"按钮时，[ProgressIndicator.isCanceled] 被设置为 true
 * 3. [ProgressManager.checkCanceled] 检查当前线程的 [ProgressIndicator]
 * 4. 如果被取消，抛出 [ProcessCanceledException]
 *
 * **使用示例**：
 * ```kotlin
 * fun analyzeFile(file: CjFile) {
 *     for (declaration in file.declarations) {
 *         ProgressManagerBasedCancellationChecker.check()  // 定期检查取消状态
 *         analyzeDeclaration(declaration)
 *     }
 * }
 * ```
 *
 * **注意事项**：
 * - 必须在 [ProgressManager.run] 或 [ProgressManager.runProcess] 上下文中使用
 * - 在主线程外的后台线程中调用
 * - 不要在持有锁的情况下调用（可能导致死锁）
 *
 * @see com.intellij.openapi.progress.ProgressManager
 * @see com.intellij.openapi.progress.ProgressIndicator
 */
object ProgressManagerBasedCancellationChecker : CancellationChecker {
    override fun check() {
        ProgressManager.checkCanceled()
    }
}
