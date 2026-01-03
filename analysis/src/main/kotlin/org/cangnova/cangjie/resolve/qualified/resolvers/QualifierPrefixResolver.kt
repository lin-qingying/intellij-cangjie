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

package org.cangnova.cangjie.resolve.qualified.resolvers

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.diagnostics.reportInvisibleReference
import org.cangnova.cangjie.diagnostics.reportUnresolvedReference
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.qualified.QualifiedExpressionResolverFacade.Companion.ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE
import org.cangnova.cangjie.resolve.qualified.QualifierPart
import org.cangnova.cangjie.resolve.qualified.QualifierPosition
import org.cangnova.cangjie.resolve.qualified.context.IsValueChecker
import org.cangnova.cangjie.resolve.qualified.context.ResolutionContext
import org.cangnova.cangjie.resolve.qualified.result.QualifierPrefixResult
import org.cangnova.cangjie.resolve.qualified.validation.ModuleNameValidator
import org.cangnova.cangjie.resolve.qualified.validation.VisibilityChecker
import org.cangnova.cangjie.resolve.scopes.LexicalScope
import org.cangnova.cangjie.resolve.scopes.findClassifier
import org.cangnova.cangjie.resolve.scopes.findPackage

/**
 * 限定符前缀解析器
 *
 * 负责将限定名称路径（如 `a.b.c`）解析到包或类前缀。
 * 这是名称解析的核心算法，采用贪心策略从左到右尽可能多地解析路径。
 *
 * ## 解析流程
 *
 * 1. 处理 IDE 模式前缀 (`_root_ide_package_`)
 * 2. 检查空路径，返回根包
 * 3. 在表达式位置，检查第一部分是否为值（值优先于类型）
 * 4. 尝试将第一部分解析为类型
 * 5. 尝试从导入作用域查找包
 * 6. 快速解析包前缀（批量查找）
 * 7. 逐个解析剩余部分（类成员或嵌套包）
 *
 * ## 使用示例
 *
 * ```kotlin
 * val resolver = QualifierPrefixResolver(lookupStrategies, visibilityChecker)
 *
 * // 解析 a.b.c.d
 * val result = resolver.resolve(path, context)
 * // result.descriptor = PackageViewDescriptor("a.b.c")
 * // result.nextIndex = 3
 * // 剩余 "d" 作为成员访问
 * ```
 */
class QualifierPrefixResolver(
    private val visibilityChecker: VisibilityChecker,
    private val moduleNameValidator: ModuleNameValidator = ModuleNameValidator()
) {
    companion object {

    }

    /**
     * 解析限定符前缀
     *
     * @param path 限定符路径
     * @param context 解析上下文
     * @param isValue 判断表达式是否为值的谓词（仅表达式位置使用）
     * @return 解析结果
     */
    fun resolve(
        path: List<QualifierPart>,
        context: ResolutionContext,
        isValue: IsValueChecker? = null
    ): QualifierPrefixResult {
        // 处理 IDE 模式前缀
        if (isIDEModePrefix(path)) {
            val subResult = resolve(
                path.subList(1, path.size),
                context.withScope(null as LexicalScope?),
                isValue = null
            )
            return QualifierPrefixResult(subResult.descriptor, subResult.nextIndex + 1)
        }

        if (path.isEmpty()) {
            return QualifierPrefixResult(context.moduleDescriptor.getPackage(FqName.ROOT), 0)
        }

        val firstPart = path.first()

        // 在表达式位置，值优先于类型
        if (context.position == QualifierPosition.EXPRESSION) {
            if (context.scopeForFirstPart != null && isValue != null &&
                firstPart.expression != null && isValue(firstPart.expression!!)) {
                return QualifierPrefixResult.UNRESOLVED
            }
        }

        // 尝试从当前作用域查找分类器
        val classifierDescriptor = context.scopeForFirstPart?.findClassifier(
            firstPart.name,
            NoLookupLocation.FOR_DEFAULT_IMPORTS
        )

        if (classifierDescriptor != null) {
            storeResult(firstPart, classifierDescriptor, context)
            return QualifierPrefixResult(classifierDescriptor, 1)
        }

        // 尝试从导入作用域查找包
        val importedPackage = context.scopeForFirstPart?.findPackage(firstPart.name)
        if (importedPackage != null && !importedPackage.isEmpty()) {
            storeResult(firstPart, importedPackage, context)
            return QualifierPrefixResult(importedPackage, 1)
        }

        // 快速解析包前缀
        val (prefixDescriptor, nextIndexAfterPrefix) = quickResolveToPackage(path, context)

        // 如果第一部分完全解析失败且不在表达式位置(或在类型位置),报告错误
        if (nextIndexAfterPrefix == 0 && context.position != QualifierPosition.EXPRESSION) {
            storeResult(firstPart, null, context)
            return QualifierPrefixResult.UNRESOLVED
        }

        // 继续解析剩余部分
        var currentDescriptor: DeclarationDescriptor? = prefixDescriptor

        for (qualifierPartIndex in nextIndexAfterPrefix until path.size) {
            val qualifierPart = path[qualifierPartIndex]

            val nextDescriptor = resolveNextPart(currentDescriptor, qualifierPart, context)

            // 存储结果并报告错误(如果有)
            // 在表达式位置且解析失败时,该名称可能是值,因此不报告错误
            if (context.position != QualifierPosition.EXPRESSION || nextDescriptor != null) {
                storeResult(qualifierPart, nextDescriptor, context)
            }

            if (nextDescriptor == null) {
                return QualifierPrefixResult(currentDescriptor, qualifierPartIndex)
            }

            currentDescriptor = nextDescriptor
        }

        // 检查是否在表达式位置使用了模块名
        if (context.position == QualifierPosition.EXPRESSION && currentDescriptor != null && path.isNotEmpty()) {
            val error = moduleNameValidator.validateForExpression(path, currentDescriptor, context)
            if (error != null) {
                moduleNameValidator.reportError(error, context)
            }
        }

        return QualifierPrefixResult(currentDescriptor, path.size)
    }

    /**
     * 快速解析到包（贪心匹配最长包前缀）
     */
    private fun quickResolveToPackage(
        path: List<QualifierPart>,
        context: ResolutionContext
    ): Pair<PackageViewDescriptor, Int> {
        val possiblePackagePrefixSize = path.indexOfFirst { it.typeArguments != null }
            .let { if (it == -1) path.size else it + 1 }

        var fqName = FqName.fromSegments(
            path.subList(0, possiblePackagePrefixSize).map { it.name.asString() }
        )

        var prefixSize = possiblePackagePrefixSize

        while (!fqName.isRoot) {
            val packageDescriptor = context.moduleDescriptor.getPackage(fqName)
            if (!packageDescriptor.isEmpty()) {
                // 记录包路径中的所有绑定
                recordPackageViews(path.subList(0, prefixSize), packageDescriptor, context)
                return Pair(packageDescriptor, prefixSize)
            }
            fqName = fqName.parent()
            prefixSize--
        }

        return Pair(context.moduleDescriptor.getPackage(FqName.ROOT), 0)
    }

    /**
     * 解析下一部分
     */
    private fun resolveNextPart(
        current: DeclarationDescriptor?,
        part: QualifierPart,
        context: ResolutionContext
    ): DeclarationDescriptor? {
        return when (current) {
            is TypeAliasDescriptor -> null // 类型别名不能作为限定符

            is ClassDescriptor -> {
                current.unsubstitutedMemberScope.getContributedClassifier(
                    part.name,
                    NoLookupLocation.FOR_DEFAULT_IMPORTS
                )
            }

            is PackageViewDescriptor -> {
                // 先尝试子包（如果没有类型参数）
                val packageView = if (part.typeArguments == null) {
                    context.moduleDescriptor.getPackage(current.fqName.child(part.name))
                } else null

                if (packageView != null && !packageView.isEmpty()) {
                    packageView
                } else {
                    current.memberScope.getContributedClassifier(
                        part.name,
                        NoLookupLocation.FOR_DEFAULT_IMPORTS
                    )
                }
            }

            else -> null
        }
    }

    /**
     * 记录包路径中的所有绑定
     */
    private fun recordPackageViews(
        path: List<QualifierPart>,
        packageView: PackageViewDescriptor,
        context: ResolutionContext
    ) {
        path.foldRight(packageView) { qualifierPart, currentView ->
            storeResult(qualifierPart, currentView, context)
            currentView.containingDeclaration ?: error(
                "Containing Declaration must be not null for package with fqName: ${currentView.fqName}"
            )
        }
    }

    /**
     * 存储解析结果
     */
    private fun storeResult(
        part: QualifierPart,
        descriptor: DeclarationDescriptor?,
        context: ResolutionContext
    ) {
        val expression = part.expression ?: return

        if (descriptor == null) {
            // 使用统一的错误报告方法
            context.trace.reportUnresolvedReference(expression)
            return
        }

        context.trace.record(BindingContext.REFERENCE_TARGET, expression, descriptor)

        // 检查可见性
        if (descriptor is DeclarationDescriptorWithVisibility) {
            if (!visibilityChecker.isVisible(descriptor, context)) {
                // 使用统一的错误报告方法
                context.trace.reportInvisibleReference(expression, descriptor)
            }
        }
    }

    /**
     * 检查是否是 IDE 模式前缀
     */
    private fun isIDEModePrefix(path: List<QualifierPart>): Boolean {
        return path.size > 1 && path.first().name.asString() == ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE
    }

    /**
     * 创建不带作用域的新上下文
     */
    private fun ResolutionContext.withScope(scope: org.cangnova.cangjie.resolve.scopes.LexicalScope?): ResolutionContext {
        return copy(scopeForFirstPart = scope)
    }
}
