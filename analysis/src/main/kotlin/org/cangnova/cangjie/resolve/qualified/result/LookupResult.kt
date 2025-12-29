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

package org.cangnova.cangjie.resolve.qualified.result

import org.cangnova.cangjie.descriptors.ClassifierDescriptor
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.name.Name

/**
 * 查找结果 - 限定符查找策略的返回类型
 *
 * ## 设计说明
 *
 * 使用密封类模式，表示查找的三种可能结果：
 * - 找到唯一匹配
 * - 未找到匹配
 * - 存在歧义（多个匹配）
 *
 * ## 使用示例
 *
 * ```kotlin
 * val result = lookupStrategy.lookup(name, context)
 * when (result) {
 *     is LookupResult.Found -> {
 *         val descriptor = result.descriptor
 *         // 使用找到的描述符
 *     }
 *     is LookupResult.NotFound -> {
 *         // 继续尝试下一个策略
 *     }
 *     is LookupResult.Ambiguous -> {
 *         // 报告歧义错误
 *     }
 * }
 * ```
 */
sealed class LookupResult {
    /**
     * 找到描述符
     *
     * @property descriptor 找到的描述符
     * @property continueSearch 是否继续搜索（用于收集多个结果的场景）
     */
    data class Found(
        val descriptor: DeclarationDescriptor,
        val continueSearch: Boolean = false
    ) : LookupResult()

    /**
     * 未找到，继续尝试下一个策略
     */
    object NotFound : LookupResult()

    /**
     * 存在歧义 - 找到多个候选符号
     *
     * @property candidates 候选描述符列表
     */
    data class Ambiguous(
        val candidates: List<DeclarationDescriptor>
    ) : LookupResult()
}

/**
 * 类型解析结果
 *
 * 专门用于类型引用解析的结果类型。
 */
sealed class TypeResolutionResult {
    /**
     * 成功解析到类型
     *
     * @property descriptor 类型描述符（类、接口、类型别名）
     */
    data class Success(val descriptor: ClassifierDescriptor) : TypeResolutionResult()

    /**
     * 未找到指定名称的类型
     *
     * @property name 未找到的类型名称
     */
    data class NotFound(val name: Name) : TypeResolutionResult()

    /**
     * 存在歧义 - 找到多个匹配的类型
     *
     * @property candidates 候选类型列表
     */
    data class Ambiguous(val candidates: List<DeclarationDescriptor>) : TypeResolutionResult()

    /**
     * 解析错误
     *
     * @property message 错误消息
     * @property name 相关的名称（可选）
     */
    data class Error(val message: String, val name: Name? = null) : TypeResolutionResult()

    /**
     * 是否成功解析
     */
    val isSuccess: Boolean
        get() = this is Success

    /**
     * 获取成功解析的描述符，如果未成功则返回 null
     */
    fun getOrNull(): ClassifierDescriptor? = (this as? Success)?.descriptor
}

/**
 * 限定符前缀解析结果
 *
 * 表示 `resolveToPackageOrClassPrefix` 的结果。
 *
 * @property descriptor 解析到的描述符（包或类），null 表示未解析成功
 * @property nextIndex 解析到的路径索引（下一个未解析部分的位置）
 */
data class QualifierPrefixResult(
    val descriptor: DeclarationDescriptor?,
    val nextIndex: Int
) {
    /**
     * 是否成功解析
     */
    val isResolved: Boolean
        get() = descriptor != null && nextIndex > 0

    /**
     * 是否完全解析（所有部分都已解析）
     */
    fun isFullyResolved(pathSize: Int): Boolean =
        descriptor != null && nextIndex == pathSize

    companion object {
        /**
         * 未解析的结果
         */
        val UNRESOLVED = QualifierPrefixResult(null, 0)
    }
}
