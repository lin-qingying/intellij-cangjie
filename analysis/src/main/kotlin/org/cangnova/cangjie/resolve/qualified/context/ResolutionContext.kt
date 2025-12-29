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

package org.cangnova.cangjie.resolve.qualified.context

import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.ModuleDescriptor
import org.cangnova.cangjie.descriptors.PackageFragmentDescriptor
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.module
import org.cangnova.cangjie.resolve.qualified.QualifierPosition
import org.cangnova.cangjie.resolve.scopes.LexicalScope

/**
 * 解析上下文 - 封装限定表达式解析过程中的所有必要信息
 *
 * ## 设计原则
 *
 * - **不可变性**: 所有属性都是只读的，通过 `with*` 方法创建新实例
 * - **统一参数传递**: 避免在方法间传递大量独立参数
 * - **上下文感知**: 提供位置相关的便捷方法
 *
 * ## 使用示例
 *
 * ```kotlin
 * // 创建解析上下文
 * val context = ResolutionContext(
 *     trace = bindingTrace,
 *     moduleDescriptor = module,
 *     position = QualifierPosition.TYPE,
 *     scopeForFirstPart = lexicalScope,
 *     shouldBeVisibleFrom = ownerDescriptor,
 *     languageVersionSettings = settings
 * )
 *
 * // 创建子上下文
 * val expressionContext = context.withPosition(QualifierPosition.EXPRESSION)
 * ```
 *
 * @property trace 绑定追踪器，用于记录解析结果和诊断信息
 * @property moduleDescriptor 当前模块描述符，提供模块上下文
 * @property position 解析位置，决定解析行为和可见性规则
 * @property scopeForFirstPart 用于解析第一部分的词法作用域
 * @property shouldBeVisibleFrom 可见性检查的起点描述符
 * @property languageVersionSettings 语言版本设置，控制语言特性
 *
 * @see QualifierPosition 解析位置枚举
 */
data class ResolutionContext(
    val trace: BindingTrace,
    val moduleDescriptor: ModuleDescriptor,
    val position: QualifierPosition,
    val scopeForFirstPart: LexicalScope?,
    val shouldBeVisibleFrom: DeclarationDescriptor?,
    val languageVersionSettings: LanguageVersionSettings
) {
    /**
     * 创建新上下文，使用指定的位置
     *
     * @param newPosition 新的解析位置
     * @return 新的解析上下文
     */
    fun withPosition(newPosition: QualifierPosition): ResolutionContext =
        copy(position = newPosition)

    /**
     * 创建新上下文，使用指定的作用域
     *
     * @param newScope 新的词法作用域
     * @return 新的解析上下文
     */
    fun withScope(newScope: LexicalScope): ResolutionContext =
        copy(scopeForFirstPart = newScope)

    /**
     * 创建新上下文，使用指定的可见性检查起点
     *
     * @param newVisibleFrom 新的可见性检查起点
     * @return 新的解析上下文
     */
    fun withVisibleFrom(newVisibleFrom: DeclarationDescriptor?): ResolutionContext =
        copy(shouldBeVisibleFrom = newVisibleFrom)

    /**
     * 获取用于可见性检查的包片段描述符
     *
     * 如果 [shouldBeVisibleFrom] 是包片段描述符，直接返回；
     * 否则返回 null。
     */
    val packageFragmentForVisibilityCheck: PackageFragmentDescriptor?
        get() = shouldBeVisibleFrom as? PackageFragmentDescriptor

    /**
     * 检查是否在 IDE 模式
     *
     * IDE 模式用于处理 `_root_ide_package_` 前缀，避免命名冲突。
     */
    val isIDEMode: Boolean
        get() = false // 可以根据需要实现

    /**
     * 是否允许模块名作为限定符
     *
     * 只有在导入位置才允许使用模块名作为限定符的第一部分。
     */
    val allowsModuleName: Boolean
        get() = position == QualifierPosition.IMPORT

    /**
     * 是否值优先于类型
     *
     * 在表达式位置，变量/函数优先于类/包进行解析。
     */
    val prefersValue: Boolean
        get() = position == QualifierPosition.EXPRESSION

    companion object {
        /**
         * 从词法作用域创建解析上下文
         *
         * @param trace 绑定追踪器
         * @param scope 词法作用域
         * @param position 解析位置
         * @param languageVersionSettings 语言版本设置
         * @param isDebuggerContext 是否在调试器上下文中
         * @return 解析上下文
         */
        fun fromScope(
            trace: BindingTrace,
            scope: LexicalScope,
            position: QualifierPosition,
            languageVersionSettings: LanguageVersionSettings,
            isDebuggerContext: Boolean = false
        ): ResolutionContext {
            return ResolutionContext(
                trace = trace,
                moduleDescriptor = scope.ownerDescriptor.module,
                position = position,
                scopeForFirstPart = scope,
                shouldBeVisibleFrom = if (isDebuggerContext) null else scope.ownerDescriptor,
                languageVersionSettings = languageVersionSettings
            )
        }

        /**
         * 从模块描述符创建解析上下文
         *
         * @param trace 绑定追踪器
         * @param module 模块描述符
         * @param position 解析位置
         * @param languageVersionSettings 语言版本设置
         * @param visibleFrom 可见性检查起点
         * @return 解析上下文
         */
        fun fromModule(
            trace: BindingTrace,
            module: ModuleDescriptor,
            position: QualifierPosition,
            languageVersionSettings: LanguageVersionSettings,
            visibleFrom: DeclarationDescriptor? = null
        ): ResolutionContext {
            return ResolutionContext(
                trace = trace,
                moduleDescriptor = module,
                position = position,
                scopeForFirstPart = null,
                shouldBeVisibleFrom = visibleFrom,
                languageVersionSettings = languageVersionSettings
            )
        }
    }
}

/**
 * 值检查函数类型
 *
 * 用于在表达式位置判断简单名称表达式是否表示值（变量/函数）而非类型。
 */
typealias IsValueChecker = (CjSimpleNameExpression) -> Boolean
