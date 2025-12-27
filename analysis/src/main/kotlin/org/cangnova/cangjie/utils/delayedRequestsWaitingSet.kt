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

import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.UserDataProperty
import org.cangnova.cangjie.resolve.caches.unsafeResolveToDescriptor
import org.cangnova.cangjie.resolve.lazy.BodyResolveMode
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.*
import org.cangnova.cangjie.psi.CjElement
import java.util.ArrayList
import java.util.LinkedHashSet


/**
 * 延迟重构请求接口
 *
 * 表示需要延迟执行的重构操作。
 * IntelliJ 中的某些重构操作不能立即执行（例如在代码分析过程中），
 * 需要等待当前操作完成后再执行。
 *
 * **设计模式**：
 * - 命令模式（Command Pattern）：将重构操作封装为对象
 * - 队列模式（Queue Pattern）：延迟执行，批量处理
 *
 * **典型实现**：
 * - [ImportRequest] - 延迟导入请求
 * - 其他重构请求（如重命名、移动文件等）
 */
interface DelayedRefactoringRequest

/**
 * 导入请求
 *
 * 表示一个需要延迟执行的自动导入操作。
 * 当代码补全或快速修复建议添加导入时，可能无法立即修改文件
 * （例如在只读模式下或正在进行其他操作时），此时创建一个 [ImportRequest]
 * 记录需要导入的元素和目标文件，稍后执行。
 *
 * **使用场景**：
 * - 代码补全后的自动导入
 * - 快速修复（Quick Fix）建议的导入
 * - 批量导入优化
 *
 * **智能指针的作用**：
 * 使用 [SmartPsiElementPointer] 而非直接引用 PSI 元素，是因为：
 * - PSI 元素可能在等待期间被修改或删除
 * - 智能指针可以跟踪元素的移动和修改
 * - 如果元素已被删除，智能指针返回 null，避免操作无效元素
 *
 * @property elementToImportPointer 要导入的元素的智能指针（如类、函数、变量）
 * @property filePointer 目标文件的智能指针（导入语句将添加到此文件）
 */
class ImportRequest(
    val elementToImportPointer: SmartPsiElementPointer<PsiElement>,
    val filePointer: SmartPsiElementPointer<CjFile>
) : DelayedRefactoringRequest

/**
 * 项目的延迟重构请求集合
 *
 * 每个项目维护一个延迟重构请求的集合。
 * 使用 [UserDataProperty] 将请求集合附加到项目对象上，
 * 确保项目生命周期内请求的持久化。
 *
 * @receiver Project IntelliJ 项目对象
 */
private var Project.delayedRefactoringRequests: MutableSet<DelayedRefactoringRequest>?
        by UserDataProperty(Key.create("DELAYED_REFACTORING_REQUESTS"))

/**
 * 添加延迟导入请求
 *
 * 将一个导入请求添加到延迟执行队列中。
 * 此函数必须在写访问（Write Access）中调用，以确保线程安全。
 *
 * **执行时机**：
 * - 延迟请求会在合适的时机执行（通常是当前操作完成后）
 * - IntelliJ 会自动合并重复的请求，避免多次导入相同的符号
 *
 * **写访问要求**：
 * IntelliJ 的 PSI 修改必须在写访问中进行，这是平台的基本安全机制。
 * 如果在非写访问中调用此函数，断言会失败。
 *
 * **使用示例**：
 * ```kotlin
 * ApplicationManager.getApplication().runWriteAction {
 *     addDelayedImportRequest(classDescriptor, currentFile)
 * }
 * ```
 *
 * @param elementToImport 要导入的 PSI 元素（如类、函数、变量）
 * @param file 目标文件（导入语句将添加到此文件）
 * @throws AssertionError 如果不在写访问中调用
 */
fun addDelayedImportRequest(elementToImport: PsiElement, file: CjFile) {
    assert(ApplicationManager.getApplication().isWriteAccessAllowed) { "Write access needed" }
    file.project.getOrCreateRefactoringRequests() += ImportRequest(
        elementToImport.createSmartPointer(),
        file.createSmartPointer()
    )
}



/**
 * 获取或创建延迟重构请求集合
 *
 * 懒加载项目的延迟重构请求集合。
 * 如果集合不存在，创建一个新的 [LinkedHashSet]（保持插入顺序）。
 *
 * **为什么使用 LinkedHashSet**：
 * - 去重：避免重复的请求
 * - 有序：保持请求的添加顺序，确保执行的确定性
 * - 性能：常数时间的查找和插入
 *
 * @receiver Project IntelliJ 项目对象
 * @return MutableSet<DelayedRefactoringRequest> 延迟重构请求集合（非空）
 */
private fun Project.getOrCreateRefactoringRequests(): MutableSet<DelayedRefactoringRequest> {
    var requests = delayedRefactoringRequests
    if (requests == null) {
        requests = LinkedHashSet()
        delayedRefactoringRequests = requests
    }

    return requests
}