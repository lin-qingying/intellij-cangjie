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

package org.cangnova.cangjie.resolve.qualified.validation

import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.DescriptorVisibilityUtils.isVisibleIgnoringReceiver
import org.cangnova.cangjie.resolve.qualified.QualifierPosition
import org.cangnova.cangjie.resolve.qualified.context.ResolutionContext

/**
 * 可见性检查器
 *
 * 负责检查符号在当前解析位置的可见性。
 *
 * ## 可见性规则
 *
 * ### 导入位置（IMPORT）
 * - **private** 符号：只能在同一文件内导入
 * - **其他可见性**：如果需要在导入中检查，则执行标准检查
 *
 * ### 其他位置（TYPE, EXPRESSION, PACKAGE_HEADER）
 * - 使用标准可见性规则：
 *   - **public**：所有地方可见
 *   - **protected**：子类和同一包中可见
 *   - **internal**：同一模块中可见
 *   - **private**：同一文件或同一类中可见
 *
 * ## 使用示例
 *
 * ```kotlin
 * val checker = VisibilityChecker(languageVersionSettings)
 *
 * val error = checker.check(descriptor, context)
 * if (error != null) {
 *     context.trace.report(error)
 * }
 * ```
 */
class VisibilityChecker(
    private val languageVersionSettings: LanguageVersionSettings
) {
    /**
     * 检查描述符的可见性
     *
     * @param descriptor 要检查的描述符
     * @param context 解析上下文
     * @return 如果不可见返回错误信息，可见返回 null
     */
    fun check(
        descriptor: DeclarationDescriptor,
        context: ResolutionContext
    ): VisibilityError? {
        return checkVisibility(
            descriptor,
            context.shouldBeVisibleFrom,
            context.position,
            languageVersionSettings
        )
    }

    /**
     * 检查描述符是否可见
     *
     * @param descriptor 要检查的描述符
     * @param context 解析上下文
     * @return 是否可见
     */
    fun isVisible(
        descriptor: DeclarationDescriptor,
        context: ResolutionContext
    ): Boolean = check(descriptor, context) == null

    /**
     * 内部可见性检查实现
     */
    private fun checkVisibility(
        descriptor: DeclarationDescriptor,
        shouldBeVisibleFrom: DeclarationDescriptor?,
        position: QualifierPosition,
        languageVersionSettings: LanguageVersionSettings
    ): VisibilityError? {
        // 不是带可见性的描述符，或没有检查起点，默认可见
        if (descriptor !is DeclarationDescriptorWithVisibility || shouldBeVisibleFrom == null) {
            return null
        }

        val visibility = descriptor.visibility

        // 导入位置的特殊规则
        if (position == QualifierPosition.IMPORT) {
            // private 符号只能在同一文件内导入
            if (DescriptorVisibilities.isPrivate(visibility)) {
                if (!DescriptorVisibilities.inSameFile(descriptor, shouldBeVisibleFrom)) {
                    return VisibilityError.PrivateNotInSameFile(descriptor, visibility)
                }
                return null
            }
            // 如果可见性不要求在导入中检查，直接返回 null
            if (!visibility.mustCheckInImports()) {
                return null
            }
        }

        // 标准可见性检查（忽略接收器）
        val isVisible = isVisibleIgnoringReceiver(
            descriptor,
            shouldBeVisibleFrom,
            languageVersionSettings
        )

        return if (isVisible) {
            null
        } else {
            VisibilityError.NotVisible(descriptor, visibility)
        }
    }

    companion object {
        /**
         * 快速可见性检查（静态方法）
         */
        fun isVisible(
            descriptor: DeclarationDescriptor,
            shouldBeVisibleFrom: DeclarationDescriptor?,
            position: QualifierPosition,
            languageVersionSettings: LanguageVersionSettings
        ): Boolean {
            if (descriptor !is DeclarationDescriptorWithVisibility || shouldBeVisibleFrom == null) {
                return true
            }

            val visibility = descriptor.visibility

            if (position == QualifierPosition.IMPORT) {
                if (DescriptorVisibilities.isPrivate(visibility)) {
                    return DescriptorVisibilities.inSameFile(descriptor, shouldBeVisibleFrom)
                }
                if (!visibility.mustCheckInImports()) return true
            }

            return isVisibleIgnoringReceiver(descriptor, shouldBeVisibleFrom, languageVersionSettings)
        }
    }
}

/**
 * 可见性错误
 */
sealed class VisibilityError {
    /**
     * 获取涉及的描述符
     */
    abstract val descriptor: DeclarationDescriptor

    /**
     * 获取可见性
     */
    abstract val visibility: DescriptorVisibility

    /**
     * 不可见错误
     */
    data class NotVisible(
        override val descriptor: DeclarationDescriptor,
        override val visibility: DescriptorVisibility
    ) : VisibilityError()

    /**
     * private 符号不在同一文件
     */
    data class PrivateNotInSameFile(
        override val descriptor: DeclarationDescriptor,
        override val visibility: DescriptorVisibility
    ) : VisibilityError()
}
