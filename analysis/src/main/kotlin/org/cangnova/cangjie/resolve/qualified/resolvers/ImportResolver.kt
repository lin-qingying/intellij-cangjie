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

import com.intellij.util.SmartList
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.diagnostics.infos.errors.*
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.codeFragmentUtil.suppressDiagnosticsInDebugMode
import org.cangnova.cangjie.psi.psiUtil.getParentOfType
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
import org.cangnova.cangjie.resolve.AllUnderImportScope
import org.cangnova.cangjie.resolve.LazyExplicitImportScope
import org.cangnova.cangjie.resolve.importVisibility
import org.cangnova.cangjie.resolve.isReexport
import org.cangnova.cangjie.resolve.qualified.PackageFragmentWithCustomSource
import org.cangnova.cangjie.resolve.qualified.QualifierPart
import org.cangnova.cangjie.resolve.qualified.QualifierPosition
import org.cangnova.cangjie.resolve.qualified.asQualifierPartList
import org.cangnova.cangjie.resolve.qualified.context.ResolutionContext
import org.cangnova.cangjie.resolve.qualified.validation.ReexportValidator
import org.cangnova.cangjie.resolve.qualified.validation.VisibilityChecker
import org.cangnova.cangjie.resolve.scopes.ImportingScope
import org.cangnova.cangjie.resolve.scopes.PackageReexportScope
import org.cangnova.cangjie.resolve.source.CangJieSourceElement
import org.cangnova.cangjie.utils.CallOnceFunction

/**
 * 导入解析器
 *
 * 专门处理 import 语句的解析，将导入语句转换为导入作用域。
 *
 * ## 支持的导入形式
 *
 * 1. **单一导入**: `import a.b.C` 或 `import a.b.foo as bar`
 * 2. **全导入**: `import a.b.*`
 * 3. **重导出**: `public import a.b.C`
 *
 * ## 使用示例
 *
 * ```kotlin
 * val resolver = ImportResolver(languageVersionSettings, prefixResolver, visibilityChecker, reexportValidator)
 *
 * val scope = resolver.processImport(importDirective, context)
 * // scope 现在可以用于查找导入的符号
 * ```
 *
 * @see AllUnderImportScope 全导入作用域
 * @see LazyExplicitImportScope 显式导入作用域
 */
class ImportResolver(
    private val languageVersionSettings: LanguageVersionSettings,
    private val prefixResolver: QualifierPrefixResolver,
    private val visibilityChecker: VisibilityChecker,
    private val reexportValidator: ReexportValidator
) {
    /**
     * 处理导入引用
     *
     * 将导入指令解析为导入作用域，用于后续的名称解析。
     *
     * @param importDirective 导入指令
     * @param context 解析上下文
     * @param excludedImportNames 要排除的导入名称（避免循环导入）
     * @return 导入作用域，解析失败时返回 null
     */
    fun processImport(
        importDirective: CjImportInfo,
        context: ResolutionContext,
        excludedImportNames: Collection<FqName> = emptyList()
    ): ImportingScope? {
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        val importedReference = importDirective.importContent ?: return null
        val path = importedReference.asQualifierPartList()
        val lastPart = path.lastOrNull() ?: return null

        val packageFragmentForCheck = computePackageFragmentToCheck(importDirective, context)

        return if (importDirective.isAllUnder) {
            processAllUnderImport(path, importDirective, context, excludedImportNames, packageFragmentForCheck)
        } else {
            processSingleImport(path, lastPart, importDirective, context, packageFragmentForCheck)
        }
    }

    /**
     * 处理全导入（import a.b.*）
     */
    private fun processAllUnderImport(
        path: List<QualifierPart>,
        importDirective: CjImportInfo,
        context: ResolutionContext,
        excludedImportNames: Collection<FqName>,
        packageFragmentForCheck: PackageFragmentDescriptor?
    ): ImportingScope? {
        // 解析包或类
        val result = prefixResolver.resolve(path, context.copy(scopeForFirstPart = null))
        val packageOrClassDescriptor = result.descriptor?.classDescriptorFromTypeAlias() ?: return null

        val lastPart = path.lastOrNull() ?: return null

        // 不能从类进行全导入
        if (packageOrClassDescriptor is ClassDescriptor && lastPart.expression != null) {
            context.trace.report(
                CANNOT_ALL_UNDER_IMPORT_FROM_ENUM.on(
                    lastPart.expression ?: return null,
                    packageOrClassDescriptor
                )
            )
            return null
        }


        return AllUnderImportScope.create(packageOrClassDescriptor, excludedImportNames)
    }

    /**
     * 处理单一导入（import a.b.C）
     */
    private fun processSingleImport(
        path: List<QualifierPart>,
        lastPart: QualifierPart,
        importDirective: CjImportInfo,
        context: ResolutionContext,
        packageFragmentForCheck: PackageFragmentDescriptor?
    ): ImportingScope? {
        val aliasName = importDirective.importedName
        if (aliasName == null) {
            // 没有别名，直接解析整个路径（可能是包导入）
            prefixResolver.resolve(path, context.copy(scopeForFirstPart = null))
            return null
        }

        // 解析限定符前缀
        val result = prefixResolver.resolve(
            path.subList(0, path.size - 1),
            context.copy(scopeForFirstPart = null)
        )
        val resolvedDescriptor = result.descriptor ?: return null

        val packageOrClassDescriptor =
            (resolvedDescriptor as? TypeAliasDescriptor)?.let { it.classDescriptor ?: return null }
                ?: resolvedDescriptor


        return LazyExplicitImportScope(
            languageVersionSettings,
            packageOrClassDescriptor,
            packageFragmentForCheck,
            lastPart.name,
            aliasName,
            CallOnceFunction(Unit) { candidates ->
                tryResolveDescriptorsWhichCannotBeImported(
                    packageOrClassDescriptor,
                    lastPart,
                    candidates,
                    context,
                    packageFragmentForCheck
                )
            },

        )
    }

    /**
     * 尝试解析不能被导入的描述符（用于错误报告）
     */
    private fun tryResolveDescriptorsWhichCannotBeImported(
        packageOrClassDescriptor: DeclarationDescriptor,
        lastPart: QualifierPart,
        candidates: Collection<DeclarationDescriptor>,
        context: ResolutionContext,
        packageFragmentForCheck: PackageFragmentDescriptor?
    ) {
        val lastPartExpression = lastPart.expression ?: return

        val descriptors = SmartList<DeclarationDescriptor>().apply {
            addAll(candidates)
        }
        val lastName = lastPart.name

        when (packageOrClassDescriptor) {
            is PackageViewDescriptor -> {
                val packageDescriptor = context.moduleDescriptor.getPackage(packageOrClassDescriptor.fqName.child(lastName))
                if (!packageDescriptor.isEmpty()) {
                    // 检查是否尝试重导出包
                    val importDirective = lastPartExpression.getParentOfType<CjImportItem>(true)
                    if (importDirective != null && importDirective.isReexport) {
                        context.trace.report(
                            PACKAGE_CANNOT_BE_REEXPORTED.on(
                                importDirective,
                                packageDescriptor.fqName,
                                importDirective.getStrictParentOfType<CjImportDirective>()?.importVisibility
                                    ?: DescriptorVisibilities.PRIVATE
                            )
                        )
                    }
                    descriptors.add(packageDescriptor)
                }
            }

            is ClassDescriptor -> {
                val memberScope = packageOrClassDescriptor.unsubstitutedMemberScope
                descriptors.addAll(memberScope.getContributedFunctions(lastName, lastPart.location))
                descriptors.addAll(memberScope.getContributedVariables(lastName, lastPart.location))
                if (descriptors.isNotEmpty()) {
                    context.trace.report(CANNOT_BE_IMPORTED.on(lastPartExpression, lastName))
                }
            }

            else -> throw IllegalStateException("Should be class or package: $packageOrClassDescriptor")
        }

        storeImportResult(descriptors, lastPart.expression, context, packageFragmentForCheck)
    }

    /**
     * 存储导入解析结果
     */
    private fun storeImportResult(
        descriptors: Collection<DeclarationDescriptor>,
        expression: CjSimpleNameExpression?,
        context: ResolutionContext,
        packageFragmentForCheck: PackageFragmentDescriptor?
    ) {
        expression ?: return

        if (descriptors.isEmpty()) {
            context.trace.report(UNRESOLVED_REFERENCE.on(expression, expression))
            return
        }

        if (descriptors.size == 1) {
            val descriptor = descriptors.single()
            context.trace.record(
                org.cangnova.cangjie.resolve.binding.BindingContext.REFERENCE_TARGET,
                expression,
                descriptor
            )
            return
        }

        // 多个候选，检查可见性
        val visibleDescriptors = descriptors.filter {
            visibilityChecker.isVisible(it as? DeclarationDescriptorWithVisibility ?: return@filter true, context)
        }

        when {
            visibleDescriptors.isEmpty() -> {
                val descriptor = descriptors.first() as? DeclarationDescriptorWithVisibility ?: return
                context.trace.report(
                    INVISIBLE_REFERENCE.on(expression, descriptor, descriptor.visibility, descriptor)
                )
            }
            visibleDescriptors.size > 1 -> {
                context.trace.record(
                    org.cangnova.cangjie.resolve.binding.BindingContext.AMBIGUOUS_REFERENCE_TARGET,
                    expression,
                    visibleDescriptors
                )
            }
            else -> {
                context.trace.record(
                    org.cangnova.cangjie.resolve.binding.BindingContext.REFERENCE_TARGET,
                    expression,
                    visibleDescriptors.single()
                )
            }
        }
    }



    /**
     * 计算用于可见性检查的包片段描述符
     */
    private fun computePackageFragmentToCheck(
        importDirective: CjImportInfo,
        context: ResolutionContext
    ): PackageFragmentDescriptor? {
        if (importDirective !is CjImportItem) return null

        val containingFile = importDirective.getContainingCjFile()
        if (containingFile.suppressDiagnosticsInDebugMode) return null

        val packageFragment = context.packageFragmentForVisibilityCheck
        if (packageFragment is DeclarationDescriptorWithSource &&
            packageFragment.source == SourceElement.NO_SOURCE) {
            return PackageFragmentWithCustomSource(
                packageFragment,
                CangJieSourceElement(containingFile)
            )
        }

        return packageFragment
    }

    /**
     * 将类型别名解析为实际的类描述符
     */
    private fun DeclarationDescriptor?.classDescriptorFromTypeAlias(): DeclarationDescriptor? {
        return if (this is TypeAliasDescriptor) classDescriptor else this
    }

    /**
     * 将导入内容转换为限定符路径
     */
    private fun CjImportInfo.ImportContent.asQualifierPartList(): List<QualifierPart> =
        when (this) {
            is CjImportInfo.ImportContent.ExpressionBased -> expression.asQualifierPartList()
            is CjImportInfo.ImportContent.FqNameBased -> fqName.pathSegments().map { QualifierPart(it) }
        }
}
