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

import org.cangnova.cangjie.descriptors.DescriptorVisibilities
import org.cangnova.cangjie.descriptors.DescriptorVisibility
import org.cangnova.cangjie.descriptors.PackageViewDescriptor
import org.cangnova.cangjie.diagnostics.infos.errors.PACKAGE_CANNOT_BE_REEXPORTED
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjImportDirective
import org.cangnova.cangjie.psi.CjImportItem
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
import org.cangnova.cangjie.resolve.importVisibility
import org.cangnova.cangjie.resolve.isReexport
import org.cangnova.cangjie.resolve.qualified.context.ResolutionContext

/**
 * 重导出验证器
 *
 * 验证重导出语句的有效性。在仓颉语言中，包不能被重导出，
 * 只有具体的声明（类、函数等）可以被重导出。
 *
 * ## 规则
 *
 * - **包不能重导出**: `public import std.core` 是错误的
 * - **声明可以重导出**: `public import std.core.String` 是正确的
 *
 * ## 使用示例
 *
 * ```kotlin
 * val validator = ReexportValidator()
 *
 * if (importDirective.isReexport) {
 *     val error = validator.validate(qualifier, lastName, importDirective, context)
 *     if (error != null) {
 *         context.trace.report(error)
 *     }
 * }
 * ```
 */
class ReexportValidator {
    /**
     * 验证重导出
     *
     * @param qualifier 限定符描述符（包或类）
     * @param importedName 导入的名称
     * @param importDirective 导入指令
     * @param context 解析上下文
     * @return 如果是无效的重导出返回错误信息，否则返回 null
     */
    fun validate(
        qualifier: org.cangnova.cangjie.descriptors.DeclarationDescriptor,
        importedName: Name,
        importDirective: CjImportItem,
        context: ResolutionContext
    ): ReexportError? {
        // 只检查重导出语句
        if (!importDirective.isReexport) return null

        // 检查是否导入的是包（包不能重导出）
        if (qualifier is PackageViewDescriptor) {
            val childPackage = context.moduleDescriptor.getPackage(qualifier.fqName.child(importedName))
            if (!childPackage.isEmpty()) {
                return ReexportError.PackageCannotBeReexported(
                    packageFqName = childPackage.fqName,
                    visibility = importDirective.getStrictParentOfType<CjImportDirective>()?.importVisibility
                        ?: DescriptorVisibilities.PRIVATE,
                    importDirective = importDirective
                )
            }
        }

        return null
    }

    /**
     * 报告重导出错误
     *
     * @param error 重导出错误
     * @param context 解析上下文
     */
    fun reportError(error: ReexportError, context: ResolutionContext) {
        when (error) {
            is ReexportError.PackageCannotBeReexported -> {
                context.trace.report(
                    PACKAGE_CANNOT_BE_REEXPORTED.on(
                        error.importDirective,
                        error.packageFqName,
                        error.visibility
                    )
                )
            }
        }
    }
}

/**
 * 重导出错误
 */
sealed class ReexportError {
    /**
     * 包不能被重导出
     *
     * @property packageFqName 包的完全限定名
     * @property visibility 导入语句的可见性修饰符
     * @property importDirective 导入指令
     */
    data class PackageCannotBeReexported(
        val packageFqName: FqName,
        val visibility: DescriptorVisibility,
        val importDirective: CjImportItem
    ) : ReexportError()
}
