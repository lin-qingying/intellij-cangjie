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

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.diagnostics.infos.errors.MODULE_CANNOT_BE_USED_AS_TYPE
import org.cangnova.cangjie.diagnostics.infos.errors.MODULE_CANNOT_BE_USED_IN_EXPRESSION
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.resolve.fqNameSafe
import org.cangnova.cangjie.resolve.qualified.QualifierPosition
import org.cangnova.cangjie.resolve.qualified.context.ResolutionContext
import org.cangnova.cangjie.resolve.qualified.QualifierPart

/**
 * 模块名验证器
 *
 * 验证模块名是否在正确的位置使用。
 * 在仓颉语言中，模块名（如 `std`）只能在导入语句中使用，
 * 不能在类型或表达式位置使用。
 *
 * ## 规则
 *
 * - **导入位置**: 允许使用模块名，如 `import std.core.String`
 * - **类型位置**: 不允许，如 `var x: std.core.String` 是错误的
 * - **表达式位置**: 不允许，如 `std.core.String()` 是错误的
 *
 * ## 使用示例
 *
 * ```kotlin
 * val validator = ModuleNameValidator()
 *
 * // 在类型位置检查
 * val error = validator.validateForType(qualifierParts, resolvedDescriptor, context)
 * if (error != null) {
 *     context.trace.report(error)
 * }
 * ```
 */
class ModuleNameValidator {
    /**
     * 验证类型位置的模块名使用
     *
     * @param path 限定符路径
     * @param resolvedDescriptor 解析到的描述符
     * @param context 解析上下文
     * @return 如果使用了模块名返回错误信息，否则返回 null
     */
    fun validateForType(
        path: List<QualifierPart>,
        resolvedDescriptor: DeclarationDescriptor,
        context: ResolutionContext
    ): ModuleNameError? {
        if (path.isEmpty()) return null
        if (context.position != QualifierPosition.TYPE) return null

        val firstPart = path.first()
        val resolvedFqName = resolvedDescriptor.fqNameSafe

        // 检查第一部分是否匹配模块名
        val firstSegment = resolvedFqName.firstSegment()
        if (firstSegment != null && firstPart.name == firstSegment) {
            return ModuleNameError.UsedInType(
                moduleName = firstPart.name,
                expression = firstPart.expression as? CjSimpleNameExpression
            )
        }

        return null
    }

    /**
     * 验证表达式位置的模块名使用
     *
     * @param path 限定符路径
     * @param resolvedDescriptor 解析到的描述符
     * @param context 解析上下文
     * @return 如果使用了模块名返回错误信息，否则返回 null
     */
    fun validateForExpression(
        path: List<QualifierPart>,
        resolvedDescriptor: DeclarationDescriptor,
        context: ResolutionContext
    ): ModuleNameError? {
        if (path.isEmpty()) return null
        if (context.position != QualifierPosition.EXPRESSION) return null

        val firstPart = path.first()
        val resolvedFqName = when (resolvedDescriptor) {
            is PackageViewDescriptor -> resolvedDescriptor.fqName
            is ClassifierDescriptor -> resolvedDescriptor.fqNameSafe
            is CallableDescriptor -> resolvedDescriptor.fqNameSafe
            else -> return null
        }

        // 检查第一部分是否匹配模块名
        val firstSegment = resolvedFqName.firstSegment()
        if (firstSegment != null && firstPart.name == firstSegment) {
            return ModuleNameError.UsedInExpression(
                moduleName = firstPart.name,
                expression = firstPart.expression as? CjSimpleNameExpression
            )
        }

        return null
    }

    /**
     * 报告模块名错误
     *
     * @param error 模块名错误
     * @param context 解析上下文
     */
    fun reportError(error: ModuleNameError, context: ResolutionContext) {
        val expression = error.expression ?: return

        when (error) {
            is ModuleNameError.UsedInType -> {
                context.trace.report(MODULE_CANNOT_BE_USED_AS_TYPE.on(expression, error.moduleName))
            }
            is ModuleNameError.UsedInExpression -> {
                context.trace.report(MODULE_CANNOT_BE_USED_IN_EXPRESSION.on(expression, error.moduleName))
            }
        }
    }
}

/**
 * 模块名错误
 */
sealed class ModuleNameError {
    /**
     * 模块名
     */
    abstract val moduleName: Name

    /**
     * 相关的表达式
     */
    abstract val expression: CjSimpleNameExpression?

    /**
     * 在类型位置使用模块名
     */
    data class UsedInType(
        override val moduleName: Name,
        override val expression: CjSimpleNameExpression?
    ) : ModuleNameError()

    /**
     * 在表达式位置使用模块名
     */
    data class UsedInExpression(
        override val moduleName: Name,
        override val expression: CjSimpleNameExpression?
    ) : ModuleNameError()
}
