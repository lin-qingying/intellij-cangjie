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

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.moduleinfo.util.isUnderCangJieSourceRootTypes
import org.cangnova.cangjie.resolve.lazy.NoDescriptorForDeclarationException

/**
 * 在安全分析块下执行操作
 *
 * 此函数提供了一种安全的方式来执行可能抛出异常的分析操作。
 * 对于不同作用域的文件采用不同的异常处理策略：
 *
 * **处理策略**：
 * - 源码根目录外的文件：尽力而为的分析，可以吞掉 NoDescriptorForDeclarationException 异常
 * - 源码根目录内的文件：不吞掉 NoDescriptorForDeclarationException 异常，直接抛出以暴露问题
 *
 * **使用场景**：
 * - 对不确定来源的 PSI 元素进行分析
 * - 需要降级处理（fallback）的分析操作
 * - 跨模块或跨项目的代码分析
 *
 * **异常处理逻辑**：
 * 1. 尝试执行 [action] 操作
 * 2. 如果抛出异常，检查是否为 NoDescriptorForDeclarationException
 * 3. 如果是且文件不在源码根目录下（或非物理文件），执行 fallback
 * 4. 否则重新抛出异常
 *
 * @receiver PsiElement 要分析的 PSI 元素
 * @param T 返回值类型
 * @param action 主要的分析操作
 * @param fallback 异常时的降级操作
 * @return T action 或 fallback 的执行结果
 */
inline fun <T> PsiElement.actionUnderSafeAnalyzeBlock(
    crossinline action: () -> T,
    crossinline fallback: () -> T
): T = try {
    action()
} catch (e: Exception) {
    e.returnIfNoDescriptorForDeclarationException(condition = {
        val file = containingFile
        it && (!file.isPhysical || !file.isUnderCangJieSourceRootTypes())
    }) { fallback() }
}


/**
 * 判断异常是否为 NoDescriptorForDeclarationException
 *
 * 递归检查异常链，确定当前异常或其根本原因是否为 NoDescriptorForDeclarationException。
 * 这种异常通常发生在以下情况：
 * - 声明没有对应的描述符（descriptor）
 * - 分析过程中描述符构建失败
 * - 跨模块引用时缺少依赖
 *
 * @receiver Exception 要检查的异常
 * @return Boolean true 表示是 NoDescriptorForDeclarationException，false 表示不是
 */
val Exception.isItNoDescriptorForDeclarationException: Boolean
    get() = this is NoDescriptorForDeclarationException || cause?.safeAs<Exception>()?.isItNoDescriptorForDeclarationException == true

/**
 * 如果异常是 NoDescriptorForDeclarationException，则执行回调函数
 *
 * 根据条件判断是否应该处理 NoDescriptorForDeclarationException 异常：
 * - 如果条件满足且异常是 NoDescriptorForDeclarationException，执行 [computable] 并返回结果
 * - 否则重新抛出异常，让上层调用者处理
 *
 * **典型使用场景**：
 * - 对不在源码根目录下的文件提供降级分析
 * - 在代码补全时忽略某些无法解析的声明
 * - 在批量分析时跳过有问题的元素
 *
 * @receiver Exception 要处理的异常
 * @param T 返回值类型
 * @param condition 判断条件，接收 isItNoDescriptorForDeclarationException 的结果
 * @param computable 条件满足时执行的计算函数
 * @return T computable 的执行结果
 * @throws Exception 如果条件不满足，重新抛出原异常
 */
inline fun <T> Exception.returnIfNoDescriptorForDeclarationException(
    crossinline condition: (Boolean) -> Boolean = { v -> v },
    crossinline computable: () -> T
): T =
    if (condition(this.isItNoDescriptorForDeclarationException)) {
        computable()
    } else {
        throw this
    }

