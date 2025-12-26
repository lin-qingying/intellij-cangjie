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

package org.cangnova.cangjie.resolve.qualified



import com.intellij.codeInsight.completion.CompletionUtilCore
import com.intellij.psi.impl.source.DummyHolder
import com.intellij.util.SmartList
import jakarta.inject.Inject
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.impl.LazyPackageViewDescriptorImpl
import org.cangnova.cangjie.diagnostics.infos.errors.*
import org.cangnova.cangjie.incremental.CangJieLookupLocation
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.getParentOfType
import org.cangnova.cangjie.psi.psiUtil.getTopmostParentQualifiedExpressionForSelector
import org.cangnova.cangjie.resolve.AllUnderImportScope
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.resolve.LazyExplicitImportScope
import org.cangnova.cangjie.resolve.TypeResolver
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.binding.DelegatingBindingTrace
import org.cangnova.cangjie.resolve.calls.CallExpressionElement
import org.cangnova.cangjie.resolve.calls.unrollToLeftMostQualifiedExpression
import org.cangnova.cangjie.resolve.fqNameSafe
import org.cangnova.cangjie.resolve.getResolutionAnchorIfAny
import org.cangnova.cangjie.resolve.importVisibility
import org.cangnova.cangjie.resolve.isReexport
import org.cangnova.cangjie.resolve.module
import org.cangnova.cangjie.resolve.scopes.*
import org.cangnova.cangjie.resolve.scopes.PackageReexportScope
import org.cangnova.cangjie.resolve.scopes.receivers.*
import org.cangnova.cangjie.resolve.source.CangJieSourceElement
import org.cangnova.cangjie.types.expressions.ExpressionTypingContext
import org.cangnova.cangjie.utils.CallOnceFunction


/**
 * 限定表达式解析器
 *
 * 负责将限定名称（如 `a.b.c`）解析为对应的描述符（包、类、类型别名等）。
 * 这是仓颉语言语义分析的核心组件之一。
 *
 * ## 核心职责
 *
 * 1. **包声明解析**：验证包路径的有效性
 * 2. **导入解析**：将导入语句转换为导入作用域
 * 3. **类型解析**：将类型引用解析为类型描述符
 * 4. **表达式解析**：解析表达式中的限定符
 * 5. **可见性检查**：确保符号在当前上下文中可访问
 *
 * ## 解析策略
 *
 * 不同位置应用不同的解析策略：
 *
 * | 位置 | 策略 | 示例 |
 * |------|------|------|
 * | 包声明 | 只验证路径 | `package com.example` |
 * | 导入 | 解析包/类/类型别名 | `import std.core.String` |
 * | 类型 | 只解析类型 | `var x: String` |
 * | 表达式 | 值优先于类型 | `Foo.bar()` |
 *
 * ## 特殊处理
 *
 * ### IDE 模式
 * 支持 `_root_ide_package_` 前缀以避免命名冲突：
 * ```kotlin
 * // a 是变量名，与包名冲突
 * a.A()  // 错误
 * _root_ide_package_.a.A()  // 正确
 * ```
 *
 * ### 模块名限制
 * 模块名（如 `std`）只能在导入语句中使用，不能在表达式或类型位置使用：
 * ```kotlin
 * import std.core.String  // 正确
 * var x: std.core.String  // 错误：模块名不能用作类型限定符
 * std.core.String()       // 错误：模块名不能在表达式中使用
 * ```
 *
 * ### 重导出限制
 * 包不能被重导出，只有具体的声明（类、函数等）可以被重导出：
 * ```kotlin
 * public import std.core       // 错误：包不能被重导出
 * public import std.core.String // 正确
 * ```
 *
 * @param languageVersionSettings 语言版本设置，用于控制语言特性和可见性检查
 *
 * @see QualifierPosition 解析位置枚举
 * @see QualifierPart 限定符部分
 * @see TypeQualifierResolutionResult 类型解析结果
 * @see QualifiedExpressionResolveResult 表达式解析结果
 */
class QualifiedExpressionResolver(
    val languageVersionSettings: LanguageVersionSettings

) {
    @set:Inject
    private lateinit var typeResolver: TypeResolver


    companion object {
        /**
         * IDE 解析模式的根前缀
         *
         * 用户不应直接使用此前缀。
         * 在 IDE 中作为非根路径的前缀，避免解析时的冲突。
         *
         * 示例：
         * ---------
         * package a
         *
         * class A
         *
         * fun test(a: Any) {
         *     a.A() // 无效代码 -> 导致错误的导入/补全等
         *     _root_ide_package_.a.A() // 正确
         * }
         * ---------
         */
        const val ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE = "_root_ide_package_"
        const val ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE_WITH_DOT = "$ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE."
    }





    /**
     * 解析限定表达式中的类或包
     *
     * 从左到右解析限定表达式，尽可能多地识别出类或包的前缀部分。
     * 例如：a.b.c.foo() 可能解析为 包a.b.c + 成员foo
     *
     * @param expression 要解析的限定表达式
     * @param scope 解析的词法作用域
     * @param context 绑定上下文，用于查询已解析的信息
     * @return 解析结果，包含识别出的类/包描述符和剩余的成员名
     */
    fun resolveClassOrPackageInQualifiedExpression(
        expression: CjQualifiedExpression,
        scope: LexicalScope,
        context: BindingContext
    ): QualifiedExpressionResolveResult {
        // 将嵌套的限定表达式展开为线性列表：a.b.c -> [a.b, a.b.c]
        val qualifiedExpressions = unrollToLeftMostQualifiedExpression(expression)
        // 提取限定符路径部分，跳过最后一个（可能是值）
        val path = mapToQualifierParts(qualifiedExpressions, 0)
        val trace = DelegatingBindingTrace(context, "Temp trace for resolving qualified expression")

        // 解析到包或类前缀
        val (result, index) = resolveToPackageOrClassPrefix(
            path = path,
            moduleDescriptor = scope.ownerDescriptor.module,
            trace = trace,
            shouldBeVisibleFrom = scope.ownerDescriptor,
            scopeForFirstPart = scope,
            position = QualifierPosition.EXPRESSION
        )

        if (result == null) return QualifiedExpressionResolveResult.UNRESOLVED
        return when (index) {
            path.size -> QualifiedExpressionResolveResult(result, null)
            path.size - 1 -> QualifiedExpressionResolveResult(result, path[index].name)
            else -> QualifiedExpressionResolveResult.UNRESOLVED
        }
    }

    /**
     * 在作用域中查找类型，并报告废弃状态（如果需要）
     *
     * @param name 要查找的类型名称
     * @param lookupLocation 查找位置信息
     * @param reportOn 如果类型已废弃，在此表达式上报告
     * @param trace 用于记录绑定信息和诊断的 trace
     * @return 找到的类型描述符，未找到时返回 null
     */
    private fun LexicalScope.findClassifierAndReportDeprecationIfNeeded(
        name: Name,
        lookupLocation: CangJieLookupLocation,
        reportOn: CjExpression?,
        trace: BindingTrace
    ): ClassifierDescriptor? {
        val (classifier, isDeprecated) = findFirstClassifierWithDeprecationStatus(name, lookupLocation) ?: return null

        if (isDeprecated && reportOn != null) {
            trace.record(BindingContext.DEPRECATED_SHORT_NAME_ACCESS, reportOn) // 用于 IDE

            // 慢路径：我们知道最近的类型是通过废弃路径导入的，但在报告废弃之前，
            // 需要重新检查是否存在其他未废弃的导入路径（例如显式导入）
//            if (!classifier.canBeResolvedWithoutDeprecation(this, lookupLocation)) {
//                trace.report(DEPRECATED_ACCESS_BY_SHORT_NAME.on(reportOn, classifier))
//            }
        }

        return classifier
    }


    /**
     * 将限定表达式列表映射为限定符部分列表
     *
     * 从嵌套的限定表达式中提取出符号名称路径。
     * 例如：a.b.c.d 会被解析为 [a, b, c, d] 或根据 skipLast 跳过最后几个
     *
     * 关键逻辑：
     * - 限定符部分包括最左侧的接收器名称
     * - 以及除最右侧之外的所有选择器名称
     * - 最右侧的选择器可能表示值，因此不作为限定符
     *
     * @param qualifiedExpressions 限定表达式列表（从左到右）
     * @param skipLast 跳过最后几个表达式（通常跳过1个，因为最后可能是值）
     * @return 限定符部分列表
     */
    private fun mapToQualifierParts(
        qualifiedExpressions: List<CjQualifiedExpression>,
        skipLast: Int
    ): List<QualifierPart> {
        if (qualifiedExpressions.isEmpty()) return emptyList()

        val first = qualifiedExpressions.first()
        if (first !is CjDotQualifiedExpression) return emptyList()
        val firstReceiver = first.receiverExpression
        if (firstReceiver !is CjSimpleNameExpression) return emptyList()

        // 限定符部分包括：
        // - 最左侧表达式的接收器名称
        // - 除最右侧之外的所有限定表达式的选择器名称
        //   （因为最右侧的选择器在表达式位置应该表示一个值，因此不能作为限定符部分）
        // 例如：
        //  限定表达式 'a.b'：限定符部分 == ['a']
        //  限定表达式 'a.b.c.d'：限定符部分 == ['a', 'b', 'c']

        val qualifierParts = arrayListOf<QualifierPart>()
        qualifierParts.add(ExpressionQualifierPart(firstReceiver))

        for (qualifiedExpression in qualifiedExpressions.dropLast(skipLast)) {
            if (qualifiedExpression !is CjDotQualifiedExpression) break
            val selector = qualifiedExpression.selectorExpression
            if (selector !is CjSimpleNameExpression) break
            qualifierParts.add(ExpressionQualifierPart(selector))
        }

        return qualifierParts
    }

    /**
     * 解析表达式中的限定符并展开调用链
     *
     * 在表达式位置解析限定名称，识别哪部分是类型限定符，哪部分是成员调用链。
     * 这对于正确类型推导和重载解析至关重要。
     *
     * 解析策略：
     * - 首先尝试将尽可能长的前缀解析为包或类
     * - 如果失败，尝试通过导入路径解析
     * - 剩余部分作为成员访问链返回
     *
     * 例如：com.example.MyClass.companion.foo()
     * -> 限定符：com.example.MyClass.companion
     * -> 调用链：[foo()]
     *
     * @param expression 限定表达式
     * @param context 表达式类型推导上下文
     * @param isValue 判断简单名称表达式是否表示值的谓词（用于区分类型和值）
     * @return 调用表达式元素列表，表示成员访问和调用链
     */
    fun resolveQualifierInExpressionAndUnroll(
        expression: CjQualifiedExpression,
        context: ExpressionTypingContext,
        isValue: (CjSimpleNameExpression) -> Boolean
    ): List<CallExpressionElement> {
        val qualifiedExpressions = unrollToLeftMostQualifiedExpression(expression)
        val maxPossibleQualifierPrefix = mapToQualifierParts(qualifiedExpressions, 1)

        var (declarationDescriptor, nextIndexAfterPrefix) = resolveToPackageOrClassPrefix(
            path = maxPossibleQualifierPrefix,
            moduleDescriptor = context.scope.ownerDescriptor.module,
            trace = context.trace,
            shouldBeVisibleFrom = context.scope.ownerDescriptor,
            scopeForFirstPart = context.scope,
            position = QualifierPosition.EXPRESSION,
            isValue = isValue,

            )
        if (nextIndexAfterPrefix == 0) {
            var (declarationDescriptor1, nextIndexAfterPrefix1) = resolveToPackageOrClassPrefixByImport(
                path = maxPossibleQualifierPrefix,
                moduleDescriptor = context.scope.ownerDescriptor.module,
                trace = context.trace,
                shouldBeVisibleFrom = context.scope.ownerDescriptor,
                scopeForFirstPart = context.scope,
                position = QualifierPosition.EXPRESSION,
                isValue = isValue,

                )
            if (nextIndexAfterPrefix1 != 0) {
                declarationDescriptor = declarationDescriptor1
                nextIndexAfterPrefix = nextIndexAfterPrefix1
            }
        }

        val nextExpressionIndexAfterQualifier =
            if (nextIndexAfterPrefix == 0) 0 else nextIndexAfterPrefix - 1

        return qualifiedExpressions
            .subList(nextExpressionIndexAfterQualifier, qualifiedExpressions.size)
            .map(::CallExpressionElement)
    }

    private fun CjUserType.asQualifierPartList(): Pair<List<ExpressionQualifierPart>, Boolean> {
        var hasError = false
        val result = SmartList<ExpressionQualifierPart>()
        var userType: CjUserType? = this
        while (userType != null) {
            val referenceExpression = userType.referenceExpression
            if (referenceExpression != null) {
                result.add(
                    ExpressionQualifierPart(
                        referenceExpression.referencedNameAsName,
                        referenceExpression,
                        userType.typeArgumentList
                    )
                )
            } else {
                hasError = true
            }
            userType = userType.qualifier
        }
        return result.asReversed() to hasError
    }

    /**
     * 解析用户类型（UserType）的描述符
     *
     * 将源码中的类型引用（如 com.example.MyClass<T>）解析为对应的类型描述符。
     * 这是类型系统的核心入口之一。
     *
     * 处理逻辑：
     * 1. 无限定名：直接在当前作用域查找类型
     * 2. 有限定名：
     *    - 先解析限定符部分（包或外部类）
     *    - 再在限定符的作用域中查找类型名称
     *    - 处理泛型参数
     *
     * 错误检查：
     * - 检查是否错误地将枚举条目用作类型
     * - 检查是否使用了模块名限定（不允许）
     * - 检查类型的可见性
     *
     * @param userType PSI 中的用户类型节点
     * @param scope 解析的词法作用域
     * @param trace 用于记录绑定信息和诊断
     * @param isDebuggerContext 是否在调试器上下文中（调试器上下文禁用某些检查）
     * @return 类型解析结果，包含解析路径和最终的类型描述符
     */
    fun resolveDescriptorForType(
        userType: CjUserType,
        scope: LexicalScope,
        trace: BindingTrace,
        isDebuggerContext: Boolean
    ): TypeQualifierResolutionResult {
        val ownerDescriptor = if (!isDebuggerContext) scope.ownerDescriptor else null
        if (userType.qualifier == null) {
//      如果没有使用限定名称
            val descriptor = userType.referenceExpression?.let { expression ->
                val classifier = scope.findClassifierAndReportDeprecationIfNeeded(
                    expression.referencedNameAsName,
                    CangJieLookupLocation(expression),
                    expression,
                    trace
                )

                checkNotEnumEntry(classifier, trace, expression)
                storeResult(
                    trace,

                    expression,
                    classifier,
                    ownerDescriptor,
                    position = QualifierPosition.TYPE,
                    isQualifier = false
                )
                classifier
            }

            return TypeQualifierResolutionResult(userType.asQualifierPartList().first, descriptor)
        }
//        val a = userType.referenceExpression?.let { expression ->
//            val classifier = scope.findClassifierAndReportDeprecationIfNeeded(
//                expression.referencedNameAsName,
//                CangJieLookupLocation(expression),
//                expression,
//                trace
//            )
//        }


//如果使用了限定名称
//  1 如果使用了包含模块名称的限定名称 报错 快速修复
//  2 如果使用了包名限定名称，则查找包
        val (qualifierPartList, hasError) = userType.asQualifierPartList()
        if (hasError) {
            val descriptor = resolveToPackageOrClass(
                qualifierPartList,
                scope.ownerDescriptor.module,
                trace,
                ownerDescriptor,
                scope,
                position = QualifierPosition.TYPE
            ) as? ClassifierDescriptor
            return TypeQualifierResolutionResult(qualifierPartList, descriptor)
        }

        return resolveQualifierPartListForType(qualifierPartList, ownerDescriptor, scope, trace, isQualifier = false)
    }

    private fun checkNotEnumEntry(
        descriptor: DeclarationDescriptor?,
        trace: BindingTrace,
        expression: CjSimpleNameExpression?
    ) {
        expression ?: return
        if (descriptor != null && DescriptorUtils.isEnumConstructor(descriptor)) {
            val qualifiedParent = expression.getTopmostParentQualifiedExpressionForSelector()
            if (qualifiedParent == null) {
                trace.report(ENUM_ENTRY_AS_TYPE.on(expression))
            }
        }
    }

    private fun resolveQualifierPartListForType(
        qualifierPartList: List<ExpressionQualifierPart>,
        ownerDescriptor: DeclarationDescriptor?,
        scope: LexicalScope,
        trace: BindingTrace,
        isQualifier: Boolean
    ): TypeQualifierResolutionResult {
        assert(qualifierPartList.isNotEmpty()) { "Qualifier list should not be empty" }

//     查找包是否声明
        val qualifier = resolveToPackageOrClass(
            qualifierPartList.subList(0, qualifierPartList.size - 1),
            scope.ownerDescriptor.module, trace, ownerDescriptor, scope,
            position = QualifierPosition.TYPE
        ) ?: return TypeQualifierResolutionResult(qualifierPartList, null)
// 该包的模块名

        val lastPart = qualifierPartList.last()
        val classifier = when (qualifier) {
            is PackageViewDescriptor -> qualifier.memberScope.getContributedClassifier(lastPart.name, lastPart.location)
            is ClassDescriptor -> {
                val descriptor =
                    qualifier.unsubstitutedMemberScope.getContributedClassifier(lastPart.name, lastPart.location)
                checkNotEnumEntry(descriptor, trace, lastPart.expression)
                descriptor
            }

            else -> null
        }

        val moduleName = qualifier.fqNameSafe.moduleName
        if (classifier != null && qualifierPartList[0].name == moduleName) {
            // 在类型位置使用模块名限定符，使用更准确的错误消息
            val firstPartExpr = qualifierPartList[0].expression
            if (firstPartExpr is CjSimpleNameExpression) {
                trace.report(MODULE_CANNOT_BE_USED_AS_TYPE.on(firstPartExpr, moduleName))
            } else {
                trace.report(MODULE_PACKAGE_CANNOT_BE_IMPORTED.on(qualifierPartList[0].expression))
            }
        }

        storeResult(
            trace,

            lastPart.expression,
            classifier,
            ownerDescriptor,
            position = QualifierPosition.TYPE,
            isQualifier = isQualifier,

            packageView = qualifier
        )
        return TypeQualifierResolutionResult(qualifierPartList, classifier)
    }


    /**
     * 解析包声明（package directive）
     *
     * 验证并记录包声明中的每个名称部分，确保包路径的每一级都有效。
     * 例如：package com.example.myapp
     * -> 会依次解析 com、com.example、com.example.myapp
     *
     * @param packageDirective 包声明 PSI 节点
     * @param module 模块描述符
     * @param trace 用于记录绑定信息
     */
    fun resolvePackageHeader(
        packageDirective: CjPackageDirective,
        module: ModuleDescriptor,
        trace: BindingTrace
    ) {
        val packageNames = packageDirective.packageNames
        for ((index, nameExpression) in packageNames.withIndex()) {
            storeResult(
                trace,
                nameExpression,
                module.getPackage(packageDirective.getFqName(nameExpression)),
                shouldBeVisibleFrom = null,
                position = QualifierPosition.PACKAGE_HEADER,
                isQualifier = index != packageNames.lastIndex
            )
        }
    }



    fun CjImportInfo.ImportContent.asQualifierPartList(): List<QualifierPart> =
        when (this) {
            is CjImportInfo.ImportContent.ExpressionBased -> expression.asQualifierPartList()
            is CjImportInfo.ImportContent.FqNameBased -> fqName.pathSegments().map { QualifierPart(it) }
        }


    /**
     * 为诊断目的解析简单名称表达式为限定符接收器
     *
     * 在表达式位置尝试将简单名称解析为限定符（包、类、枚举类等），
     * 用于提供代码补全、导航等 IDE 功能。
     *
     * 解析顺序：
     * - 有接收器：在接收器的作用域中查找
     * - 无接收器：在当前词法作用域中查找
     *
     * @param expression 简单名称表达式
     * @param receiver 接收器（如果有），例如 a.B 中的 a
     * @param context 表达式类型推导上下文
     * @return 限定符接收器，如果无法解析为限定符则返回 null
     */
    fun resolveNameExpressionAsQualifierForDiagnostics(
        expression: CjSimpleNameExpression,
        receiver: Receiver?,
        context: ExpressionTypingContext
    ): QualifierReceiver? {

        val name = expression.referencedNameAsName
        if (!expression.isPhysical && !name.isSpecial && name.asString()
                .endsWith(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED)
        ) {
            return null
        }

        val location = CangJieLookupLocation(expression)
        val qualifierDescriptor = when (receiver) {
            is PackageQualifier -> {
                val childPackageFQN = receiver.descriptor.fqName.child(name)
                receiver.descriptor.module.getPackage(childPackageFQN).takeUnless { it.isEmpty() }
                    ?: receiver.descriptor.memberScope.getContributedClassifier(name, location)
            }

            is EnumClassQualifier -> receiver.staticScope.getContributedClassifier(name, location)
            is ClassQualifier -> receiver.staticScope.getContributedClassifier(name, location)
//            无接收器时，直接获取
            null -> context.scope.findClassifier(name, location)
                ?: context.scope.getPackageView(name, location)

            is ReceiverValue -> receiver.type.memberScope.memberScopeAsImportingScope().findClassifier(name, location)
            else -> null
        }

        if (qualifierDescriptor != null) {
            typeResolver.resolveTypeForClass(expression, context.scope, context.trace, qualifierDescriptor)
            return storeResult(
                context.trace,
                expression,
                qualifierDescriptor,
                context.scope.ownerDescriptor,
                QualifierPosition.EXPRESSION
            )
        }

        return null
    }

    private fun computePackageFragmentToCheck(
        containingFile: CjFile,
        packageFragmentForVisibilityCheck: PackageFragmentDescriptor?
    ): PackageFragmentDescriptor? =
        when {
            containingFile.suppressDiagnosticsInDebugMode -> null

            packageFragmentForVisibilityCheck is DeclarationDescriptorWithSource &&
                    packageFragmentForVisibilityCheck.source == SourceElement.NO_SOURCE -> {

                PackageFragmentWithCustomSource(
                    packageFragmentForVisibilityCheck,
                    CangJieSourceElement(containingFile)
                )
            }

            else -> packageFragmentForVisibilityCheck
        }

    /**
     * 处理导入引用的核心实现
     *
     * 将导入语句（import xxx）解析为导入作用域（ImportingScope），
     * 该作用域提供导入的符号供后续名称解析使用。
     *
     * 支持两种导入形式：
     * 1. 单一导入：import a.b.C 或 import a.b.foo as bar
     * 2. 全导入：import a.b.*
     *
     * 特殊检查：
     * - 禁止从单例对象全导入（import Object.*）
     * - 检查可见性（私有符号不能导入）
     * - 检查是否导入了不能导入的成员（如成员函数）
     *
     * @param importDirective 导入指令 PSI 节点
     * @param moduleDescriptor 当前模块描述符
     * @param trace 用于记录绑定信息和诊断
     * @param excludedImportNames 要排除的导入名称（避免循环导入）
     * @param packageFragmentForVisibilityCheck 用于可见性检查的包片段
     * @return 导入作用域，解析失败时返回 null
     */
    private fun doProcessImportReference(
        importDirective: CjImportInfo,
        moduleDescriptor: ModuleDescriptor,
        trace: BindingTrace,
        excludedImportNames: Collection<FqName>,
        packageFragmentForVisibilityCheck: PackageFragmentDescriptor?
    ): ImportingScope? { // null if some error happened
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        val importedReference = importDirective.importContent ?: return null
        val path = importedReference.asQualifierPartList()
        val lastPart = path.lastOrNull() ?: return null
        val packageFragmentForCheck =
            if (importDirective is CjImportDirectiveItem)
                computePackageFragmentToCheck(importDirective.getContainingCjFile(), packageFragmentForVisibilityCheck)
            else
                null




        if (importDirective.isAllUnder) {
            val packageOrClassDescriptor = resolveToPackageOrClass(
                path, moduleDescriptor, trace, packageFragmentForCheck,
                scopeForFirstPart = null, position = QualifierPosition.IMPORT
            ).classDescriptorFromTypeAlias() ?: return null

            if (packageOrClassDescriptor is ClassDescriptor  /* && packageOrClassDescriptor.kind.isObject */ && lastPart.expression != null) {
                trace.report(
                    CANNOT_ALL_UNDER_IMPORT_FROM_SINGLETON.on(
                        lastPart.expression ?: return null,
                        packageOrClassDescriptor
                    )
                ) // todo report on star
                return null
            }

            // 如果是包导入，创建重导出作用域
            val reexportScope = if (packageOrClassDescriptor is PackageViewDescriptor && importDirective is CjImportDirectiveItem) {
                val project = importDirective.getContainingCjFile().project
                PackageReexportScope(
                    packageFqName = packageOrClassDescriptor.fqName,
                    project = project,
                    moduleDescriptor = moduleDescriptor,
                    fromPackage = packageFragmentForCheck
                )
            } else null

            return AllUnderImportScope.create(packageOrClassDescriptor, excludedImportNames, reexportScope)
        } else {
            return processSingleImport(
                moduleDescriptor,
                trace,
                importDirective,
                path,
                lastPart,
                packageFragmentForCheck
            )
        }
    }

    private fun processSingleImport(
        moduleDescriptor: ModuleDescriptor,
        trace: BindingTrace,
        importDirective: CjImportInfo,
        path: List<QualifierPart>,
        lastPart: QualifierPart,
        packageFragmentForVisibilityCheck: PackageFragmentDescriptor?
    ): ImportingScope? {

//        importDirective.modifierVisibility
        val aliasName = importDirective.importedName
        if (aliasName == null) {

            resolveToPackageOrClass(
                path,
                moduleDescriptor,
                trace,
                packageFragmentForVisibilityCheck,
                scopeForFirstPart = null,
                position = QualifierPosition.IMPORT
            )
            return null
        }

        val resolvedDescriptor = resolveToPackageOrClass(
            path.subList(0, path.size - 1), moduleDescriptor, trace,
            packageFragmentForVisibilityCheck, scopeForFirstPart = null, position = QualifierPosition.IMPORT
        ) ?: return null

        val packageOrClassDescriptor =
            (resolvedDescriptor as? TypeAliasDescriptor)?.let { it.classDescriptor ?: return null }
                ?: resolvedDescriptor

        // 如果是从包中导入，创建重导出作用域用于查找被重导出的声明
        val reexportScope = if (packageOrClassDescriptor is PackageViewDescriptor && importDirective is CjImportDirectiveItem) {
            val project = importDirective.getContainingCjFile().project
            PackageReexportScope(
                packageFqName = packageOrClassDescriptor.fqName,
                project = project,
                moduleDescriptor = moduleDescriptor,
                fromPackage = packageFragmentForVisibilityCheck
            )
        } else null

        return LazyExplicitImportScope(
            languageVersionSettings,

            packageOrClassDescriptor,
            packageFragmentForVisibilityCheck,
            lastPart.name,
            aliasName,
            CallOnceFunction(Unit) { candidates ->
//                if (candidates.isNotEmpty()) {
//                    storeResult(
//                        trace,
//                        lastPart.expression,
//                        candidates,
//                        packageFragmentForVisibilityCheck,
//                        position = IMPORT,
//                        isQualifier = false
//                    )

                tryResolveDescriptorsWhichCannotBeImported(
                    trace,
                    moduleDescriptor,
                    packageOrClassDescriptor,
                    lastPart, candidates, packageFragmentForVisibilityCheck
                )
//                } else {
//                    tryResolveDescriptorsWhichCannotBeImported(
//                        trace,
//                        moduleDescriptor,
//                        packageOrClassDescriptor,
//                        lastPart
//                    )
//                }
            },
            reexportScope = reexportScope
        )
    }

    private fun tryResolveDescriptorsWhichCannotBeImported(
        trace: BindingTrace,
        moduleDescriptor: ModuleDescriptor,
        packageOrClassDescriptor: DeclarationDescriptor,
        lastPart: QualifierPart,
        candidates: Collection<DeclarationDescriptor> = emptyList(),
        packageFragmentForVisibilityCheck: PackageFragmentDescriptor?

    ) {
        val lastPartExpression = lastPart.expression ?: return

        val descriptors = SmartList<DeclarationDescriptor>().apply {
            addAll(candidates)
        }
        val lastName = lastPart.name

        when (packageOrClassDescriptor) {
            is PackageViewDescriptor -> {
                val packageDescriptor = moduleDescriptor.getPackage(packageOrClassDescriptor.fqName.child(lastName))
                if (!packageDescriptor.isEmpty()) {
                    // 检查是否尝试重导出包（包不能被重导出）
                    val importDirective = lastPartExpression.getParentOfType<CjImportDirectiveItem>(true)
                    if (importDirective != null && importDirective.isReexport) {
                        // 使用新的错误：包不能被重导出
                        trace.report(
                            PACKAGE_CANNOT_BE_REEXPORTED.on(
                                importDirective,
                                packageDescriptor.fqName,
                                importDirective.importVisibility
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
                    trace.report(CANNOT_BE_IMPORTED.on(lastPartExpression, lastName))
                }
            }

            else -> throw IllegalStateException("Should be class or package: $packageOrClassDescriptor")
        }
        storeResult(
            trace,
            lastPart.expression,
            descriptors,
            shouldBeVisibleFrom = packageFragmentForVisibilityCheck,
            position = QualifierPosition.IMPORT,
            isQualifier = false
        )
    }

    private fun DeclarationDescriptor?.classDescriptorFromTypeAlias(): DeclarationDescriptor? {
        return if (this is TypeAliasDescriptor) classDescriptor else this
    }

    private fun resolveInIDEMode(path: List<QualifierPart>): Boolean =
        path.size > 1 && path.first().name.asString() == ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE

    fun resolveDescriptorForDoubleColonLHS(
        expression: CjExpression,
        scope: LexicalScope,
        trace: BindingTrace,
        isDebuggerContext: Boolean
    ): TypeQualifierResolutionResult {
        val ownerDescriptor = if (!isDebuggerContext) scope.ownerDescriptor else null

        val qualifierPartList = expression.asQualifierPartList(doubleColonLHS = true)
        if (qualifierPartList.isEmpty()) {
            return TypeQualifierResolutionResult(qualifierPartList, null)
        }

        if (qualifierPartList.size == 1) {
            val (name, simpleNameExpression) = qualifierPartList.single()
            val descriptor = scope.findClassifierAndReportDeprecationIfNeeded(
                name,
                CangJieLookupLocation(simpleNameExpression),
                simpleNameExpression,
                trace
            )
            storeResult(trace, simpleNameExpression, descriptor, ownerDescriptor, position = QualifierPosition.TYPE, isQualifier = true)
            return TypeQualifierResolutionResult(qualifierPartList, descriptor)
        }

        return resolveQualifierPartListForType(qualifierPartList, ownerDescriptor, scope, trace, isQualifier = true)
    }

    private fun resolveToPackageOrClassPrefixByImport(
        path: List<QualifierPart>,
        moduleDescriptor: ModuleDescriptor,
        trace: BindingTrace,
        shouldBeVisibleFrom: DeclarationDescriptor?,
        scopeForFirstPart: LexicalScope?,
        position: QualifierPosition,
        isValue: ((CjSimpleNameExpression) -> Boolean)? = null
    ): Pair<DeclarationDescriptor?, Int> {
        if (resolveInIDEMode(path)) {
            return resolveToPackageOrClassPrefixByImport(
                path.subList(1, path.size),
                moduleDescriptor,
                trace,
                shouldBeVisibleFrom,
                scopeForFirstPart = null,
                position = position,
                isValue = null
            ).let { it.first to it.second + 1 }
        }

        if (path.isEmpty()) {
            return Pair(moduleDescriptor.getPackage(FqName.ROOT), 0)
        }

        val firstPart = path.first()

        if (position == QualifierPosition.EXPRESSION) {
            // 在表达式位置，值（变量/函数）优先于类型（类/包）。
            // 如果看到函数或变量（可能有歧义），
            // 告诉解析器没有限定符，让它执行依赖上下文的解析。
            if (scopeForFirstPart != null && isValue != null && firstPart.expression != null && isValue(firstPart.expression!!)) {
                return Pair(null, 0)
            }
        }

        val classifierDescriptor = scopeForFirstPart?.findClassifier(firstPart.name, firstPart.location)

        if (classifierDescriptor != null) {
//            typeResolver.resolveTypeForClass()
            storeResult(
                trace, firstPart.expression, classifierDescriptor, shouldBeVisibleFrom, position,
                scope = scopeForFirstPart
            )
            return Pair(classifierDescriptor, 1)
        }


        val (prefixDescriptor, nextIndexAfterPrefix) = moduleDescriptor.quickResolveToPackageByImport(
            scopeForFirstPart,
            shouldBeVisibleFrom,
            path,
            trace,
            position
        )

        var currentDescriptor: DeclarationDescriptor? = prefixDescriptor
        for (qualifierPartIndex in nextIndexAfterPrefix until path.size) {
            val qualifierPart = path[qualifierPartIndex]

            val nextPackageOrClassDescriptor =
                when (currentDescriptor) {
                    is TypeAliasDescriptor -> // TODO 类型别名作为限定符？（可能会破坏 TypeResolver 中的某些假设）
                        null

                    is ClassDescriptor ->
                        currentDescriptor.getContributedClassifier(qualifierPart)

                    is PackageViewDescriptor -> {
                        val packageView =
                            if (qualifierPart.typeArguments == null) {
                                moduleDescriptor.getPackage(currentDescriptor.fqName.child(qualifierPart.name))
                            } else null

                        if (packageView != null && !packageView.isEmpty()   ) {
                            packageView
                        } else {
                            currentDescriptor.memberScope.getContributedClassifier(
                                qualifierPart.name,
                                qualifierPart.location
                            )
                        }
                    }

                    else ->
                        null
                }

            // 如果在表达式位置，该名称可能表示一个值（而不是包或类）。
            if (!(position == QualifierPosition.EXPRESSION && nextPackageOrClassDescriptor == null)) {
                storeResult(
                    trace,
                    qualifierPart.expression,
                    nextPackageOrClassDescriptor,
                    shouldBeVisibleFrom,
                    position
                )
            }

            if (nextPackageOrClassDescriptor == null) {
                return Pair(currentDescriptor, qualifierPartIndex)
            }

            currentDescriptor = nextPackageOrClassDescriptor
        }

        return Pair(currentDescriptor, path.size)
    }

    /**
     * 将限定名称路径解析到包或类前缀
     *
     * 这是名称解析的核心算法，采用贪心策略从左到右尽可能多地解析路径。
     *
     * 解析流程：
     * 1. 处理 IDE 模式前缀 (_root_ide_package_)
     * 2. 检查空路径，返回根包
     * 3. 在表达式位置，检查第一部分是否为值（值优先于类型）
     * 4. 尝试将第一部分解析为类型
     * 5. 快速解析包前缀（批量查找）
     * 6. 逐个解析剩余部分（类成员或嵌套包）
     *
     * @param path 限定符路径
     * @param moduleDescriptor 模块描述符
     * @param trace 用于记录绑定信息
     * @param shouldBeVisibleFrom 可见性检查的起点描述符
     * @param scopeForFirstPart 用于解析第一部分的词法作用域
     * @param position 解析位置（包声明/导入/类型/表达式）
     * @param isValue 判断表达式是否为值的谓词（仅表达式位置使用）
     * @return (解析到的描述符, 解析到的索引位置)
     */
    private fun resolveToPackageOrClassPrefix(
        path: List<QualifierPart>,
        moduleDescriptor: ModuleDescriptor,
        trace: BindingTrace,
        shouldBeVisibleFrom: DeclarationDescriptor?,
        scopeForFirstPart: LexicalScope?,
        position: QualifierPosition,
        isValue: ((CjSimpleNameExpression) -> Boolean)? = null
    ): Pair<DeclarationDescriptor?, Int> {
        if (resolveInIDEMode(path)) {
            return resolveToPackageOrClassPrefix(
                path.subList(1, path.size),
                moduleDescriptor,
                trace,
                shouldBeVisibleFrom,
                scopeForFirstPart = null,
                position = position,
                isValue = null
            ).let { it.first to it.second + 1 }
        }

        if (path.isEmpty()) {
            return Pair(moduleDescriptor.getPackage(FqName.ROOT), 0)
        }

        val firstPart = path.first()

        if (position == QualifierPosition.EXPRESSION) {
            // 在表达式位置，值（变量/函数）优先于类型（类/包）。
            // 如果看到函数或变量（可能有歧义），
            // 告诉解析器没有限定符，让它执行依赖上下文的解析。
            if (scopeForFirstPart != null && isValue != null && firstPart.expression != null && isValue(firstPart.expression!!)) {
                return Pair(null, 0)
            }
        }

        val classifierDescriptor = scopeForFirstPart?.findClassifier(firstPart.name, firstPart.location)

        if (classifierDescriptor != null) {
//            typeResolver.resolveTypeForClass()
            storeResult(
                trace, firstPart.expression, classifierDescriptor, shouldBeVisibleFrom, position,
                scope = scopeForFirstPart
            )
            return Pair(classifierDescriptor, 1)
        }


        val (prefixDescriptor, nextIndexAfterPrefix) = moduleDescriptor.quickResolveToPackage(
            shouldBeVisibleFrom,
            path,
            trace,
            position
        )

        var currentDescriptor: DeclarationDescriptor? = prefixDescriptor
        for (qualifierPartIndex in nextIndexAfterPrefix until path.size) {
            val qualifierPart = path[qualifierPartIndex]

            val nextPackageOrClassDescriptor =
                when (currentDescriptor) {
                    is TypeAliasDescriptor -> // TODO 类型别名作为限定符？（可能会破坏 TypeResolver 中的某些假设）
                        null

                    is ClassDescriptor ->
                        currentDescriptor.getContributedClassifier(qualifierPart)

                    is PackageViewDescriptor -> {
                        val packageView =
                            if (qualifierPart.typeArguments == null) {
                                moduleDescriptor.getPackage(currentDescriptor.fqName.child(qualifierPart.name))
                            } else null
                        if (packageView != null && !packageView.isEmpty()) {
                            packageView
                        } else {
                            currentDescriptor.memberScope.getContributedClassifier(
                                qualifierPart.name,
                                qualifierPart.location
                            )
                        }
                    }

                    else ->
                        null
                }

            // 如果在表达式位置，该名称可能表示一个值（而不是包或类）。
            if (!(position == QualifierPosition.EXPRESSION && nextPackageOrClassDescriptor == null)) {
                storeResult(
                    trace,
                    qualifierPart.expression,
                    nextPackageOrClassDescriptor,
                    shouldBeVisibleFrom,
                    position
                )
            }

            if (nextPackageOrClassDescriptor == null) {
                return Pair(currentDescriptor, qualifierPartIndex)
            }

            currentDescriptor = nextPackageOrClassDescriptor
        }

        // 错误恢复：检查是否在表达式位置使用了模块名作为限定符
        // 例如：std.core.String() 是非法的，但应该解析到 String 并报错
        if (position == QualifierPosition.EXPRESSION && currentDescriptor != null && path.isNotEmpty()) {
            checkModuleNameInExpression(path, currentDescriptor, trace)
        }

        return Pair(currentDescriptor, path.size)
    }

    fun ClassDescriptor.getContributedClassifier(qualifierPart: QualifierPart) =
        unsubstitutedMemberScope.getContributedClassifier(qualifierPart.name, qualifierPart.location)

    /**
     * 检查是否在表达式位置使用了模块名作为限定符
     *
     * 在仓颉语言中，模块名（如 std）只能在导入语句中使用。
     * 如果在表达式位置使用了模块名限定符（如 std.core.String()），
     * 应该报告错误但仍然返回解析结果（错误恢复）。
     *
     * @param path 限定符路径
     * @param resolvedDescriptor 解析到的描述符
     * @param trace 用于记录错误
     */
    private fun checkModuleNameInExpression(
        path: List<QualifierPart>,
        resolvedDescriptor: DeclarationDescriptor,
        trace: BindingTrace
    ) {
        if (path.isEmpty()) return

        val firstPart = path.first()
        val firstPartExpression = firstPart.expression as? CjSimpleNameExpression ?: return

        // 获取解析到的描述符的完全限定名
        val resolvedFqName = when (resolvedDescriptor) {
            is PackageViewDescriptor -> resolvedDescriptor.fqName
            is ClassifierDescriptor -> resolvedDescriptor.fqNameSafe
            is CallableDescriptor -> resolvedDescriptor.fqNameSafe
            else -> return
        }

        // 检查第一部分是否匹配模块名
        if (!resolvedFqName.isRoot && firstPart.name == resolvedFqName.moduleName) {
            trace.report(MODULE_CANNOT_BE_USED_IN_EXPRESSION.on(firstPartExpression, firstPart.name))
        }
    }

    private fun ModuleDescriptor.quickResolveToPackageByImport(
        scopeForFirstPart: LexicalScope?,
        shouldBeVisibleFrom: DeclarationDescriptor?,
        path: List<QualifierPart>,
        trace: BindingTrace,
        position: QualifierPosition
    ): Pair<PackageViewDescriptor, Int> {
        val possiblePackagePrefixSize =
            path.indexOfFirst { it.typeArguments != null }.let { if (it == -1) path.size else it + 1 }
        val firstName = path.first().name

        val packageView = scopeForFirstPart?.getPackageView(firstName, NoLookupLocation.FROM_PACKAGE)


        var fqName = if (packageView?.isEmpty() == false) {
            packageView.fqName.child(
                FqName.fromSegments(
                    path.subList(1, possiblePackagePrefixSize).map { it.name.asString() })
            )

        } else {
            FqName.fromSegments(path.subList(0, possiblePackagePrefixSize).map { it.name.asString() })

        }
        var prefixSize = possiblePackagePrefixSize
//        if(packageView?.isEmpty() == false){
//            recordPackageViews(shouldBeVisibleFrom, path.subList(0, prefixSize), packageDescriptor, trace, position)
//            return Pair(packageView, prefixSize)
//        }
//
        while (!fqName.isRoot) {
            val packageDescriptor = getPackage(fqName)
            if (!packageDescriptor.isEmpty()) {
                recordPackageViews(shouldBeVisibleFrom, path.subList(0, prefixSize), packageDescriptor, trace, position)
                return Pair(packageDescriptor, prefixSize)
            }
            fqName = fqName.parent()
            prefixSize--
        }
        return Pair(getPackage(FqName.ROOT), 0)
    }

    /**
     * 快速解析到包（不通过导入）
     *
     * 使用贪心算法快速匹配最长的包前缀，避免逐个查找。
     * 例如：a.b.c.d，会尝试 a.b.c.d -> a.b.c -> a.b -> a
     *
     * 优化策略：
     * - 首先尝试完整路径
     * - 逐步缩短直到找到有效包
     * - 记录路径中的所有包视图
     *
     * @return (包视图描述符, 解析到的索引位置)
     */
    private fun ModuleDescriptor.quickResolveToPackage(
        shouldBeVisibleFrom: DeclarationDescriptor?,
        path: List<QualifierPart>,
        trace: BindingTrace,
        position: QualifierPosition
    ): Pair<PackageViewDescriptor, Int> {
        val possiblePackagePrefixSize =
            path.indexOfFirst { it.typeArguments != null }.let { if (it == -1) path.size else it + 1 }
        var fqName = FqName.fromSegments(path.subList(0, possiblePackagePrefixSize).map { it.name.asString() })

        var prefixSize = possiblePackagePrefixSize
        while (!fqName.isRoot) {
            val packageDescriptor = getPackage(fqName)
            if (!packageDescriptor.isEmpty()) {
                recordPackageViews(shouldBeVisibleFrom, path.subList(0, prefixSize), packageDescriptor, trace, position)
                return Pair(packageDescriptor, prefixSize)
            }
            fqName = fqName.parent()
            prefixSize--
        }
        return Pair(getPackage(FqName.ROOT), 0)
    }

    /**
     * 记录包视图的绑定信息
     *
     * 将路径中的每个包名与对应的包描述符关联，供 IDE 功能使用（如导航）。
     * 从右到左逆向记录，确保每个名称都能正确解析到其包描述符。
     *
     * @param shouldBeVisibleFrom 可见性检查的起点
     * @param path 限定符路径
     * @param packageView 最终的包视图（最长匹配）
     * @param trace 用于记录绑定信息
     * @param position 解析位置
     */
    private fun recordPackageViews(
        shouldBeVisibleFrom: DeclarationDescriptor?,

        path: List<QualifierPart>,
        packageView: PackageViewDescriptor,
        trace: BindingTrace,
        position: QualifierPosition
    ) {
        path.foldRight(packageView) { qualifierPart, currentView ->
            storeResult(
                trace,
                qualifierPart.expression,
                currentView,
                shouldBeVisibleFrom = shouldBeVisibleFrom,
                position = position
            )
            currentView.containingDeclaration
                ?: error(
                    "Containing Declaration must be not null for package with fqName: ${currentView.fqName}, " +
                            "path: ${path.joinToString()}, packageView fqName: ${packageView.fqName}"
                )
        }
    }

    /**
     * 存储解析结果（多个候选描述符的情况）
     *
     * 处理解析到多个候选符号的情况（如重载），根据可见性过滤并处理歧义。
     *
     * 处理策略：
     * - 过滤出可见的描述符
     * - 如果全部不可见，报告不可见错误
     * - 如果有多个可见的，记录为歧义引用
     * - 如果只有一个可见的，记录为正常引用
     *
     * @param trace 用于记录绑定信息
     * @param referenceExpression 引用表达式 PSI 节点
     * @param descriptors 候选描述符集合
     * @param shouldBeVisibleFrom 可见性检查的起点
     * @param position 解析位置
     * @param isQualifier 是否作为限定符（决定是否创建 QualifierReceiver）
     */
    private fun storeResult(
        trace: BindingTrace,
        referenceExpression: CjSimpleNameExpression?,
        descriptors: Collection<DeclarationDescriptor>,
        shouldBeVisibleFrom: DeclarationDescriptor?,
        position: QualifierPosition,
        isQualifier: Boolean = true
    ) {
        referenceExpression ?: return
        if (descriptors.size > 1) {
            val visibleDescriptors =
                descriptors.filter { isVisible(it, shouldBeVisibleFrom, position, languageVersionSettings) }

                    .distinctBy { descriptor ->
                        descriptor.fqNameSafe
                    }


            when {
                visibleDescriptors.isEmpty() -> {
                    val descriptor = descriptors.first() as DeclarationDescriptorWithVisibility
                    trace.report(
                        INVISIBLE_REFERENCE.on(
                            referenceExpression,
                            descriptor,
                            descriptor.visibility,
                            descriptor
                        )
                    )
                }

                visibleDescriptors.size > 1 -> {
                    trace.record(BindingContext.AMBIGUOUS_REFERENCE_TARGET, referenceExpression, visibleDescriptors)
                }

                else -> {
                    storeResult(trace, referenceExpression, visibleDescriptors.single(), null, position, isQualifier)
                }
            }
        } else {
            storeResult(
                trace,
                referenceExpression,
                descriptors.singleOrNull(),
                shouldBeVisibleFrom,
                position,
                isQualifier
            )
        }
    }

    /**
     * 存储解析结果（单个描述符的情况）
     *
     * 将解析到的描述符与 PSI 表达式关联，并执行必要的检查。
     * 这是所有名称解析的最终落点，负责：
     * - 记录引用目标绑定
     * - 检查可见性
     * - 创建限定符接收器（如适用）
     * - 处理特殊情况（包声明、导入等）
     *
     * @param trace 用于记录绑定信息
     * @param referenceExpression 引用表达式 PSI 节点
     * @param descriptor 解析到的描述符，null 表示未解析
     * @param shouldBeVisibleFrom 可见性检查的起点
     * @param position 解析位置（影响可见性规则）
     * @param isQualifier 是否作为限定符使用
     * @param packageView 如果是包成员解析，提供包视图用于错误检查
     * @param reportReexportError 是否报告重导出错误
     * @param scope 词法作用域，用于类型解析
     * @return 如果是限定符，返回 QualifierReceiver；否则返回 null
     */
    private fun storeResult(
        trace: BindingTrace,

        referenceExpression: CjSimpleNameExpression?,
        descriptor: DeclarationDescriptor?,
        shouldBeVisibleFrom: DeclarationDescriptor?,
        position: QualifierPosition,
        isQualifier: Boolean = true,
        packageView: DeclarationDescriptor? = null,
        reportReexportError: Boolean = true,
        scope: LexicalScope? = null,
    ): QualifierReceiver? {
        referenceExpression ?: return null
        if (descriptor == null) {
            trace.report(UNRESOLVED_REFERENCE.on(referenceExpression, referenceExpression))
            return null
        }
        if (descriptor !is DeclarationDescriptorWithVisibility) {
            return null
        }




        trace.record(BindingContext.REFERENCE_TARGET, referenceExpression, descriptor)

//        UnderscoreUsageChecker.checkSimpleNameUsage(descriptor, referenceExpression, trace)

//        if (descriptor is DeclarationDescriptorWithVisibility) {

        val fromToCheck =
            if (shouldBeVisibleFrom is PackageFragmentDescriptor && shouldBeVisibleFrom.source == SourceElement.NO_SOURCE && referenceExpression.containingFile !is DummyHolder) {
                PackageFragmentWithCustomSource(
                    shouldBeVisibleFrom,
                    CangJieSourceElement(referenceExpression.getContainingCjFile())
                )
            } else {
                shouldBeVisibleFrom
            }

        when (position) {
            QualifierPosition.PACKAGE_HEADER -> {
                if (descriptor is LazyPackageViewDescriptorImpl) {


// TODO 如何处理包的修饰符不一致问题
                }
            }

            QualifierPosition.IMPORT, QualifierPosition.TYPE -> {

//                //                不能使用 除private以外的修饰符修饰import语句
//                val importDirective = referenceExpression.getParentOfType<CjImportDirectiveItem>(true)
//
//                if (importDirective != null) {
//                    if (importDirective.modifierVisibility != DescriptorVisibilities.PRIVATE) {
//                        importDirective.importedFqName?.let {
//                            trace.report(
//                                IMPORTED_PACKAGE_MODIFICATION_NOT_ALLOWED.on(
//                                    importDirective,
//                                    it,
//                                    importDirective.modifierVisibility
//                                )
//                            )
//                        }
//                    }
//                }


//                    不能导入模块名
//                if (packageDescriptor.fqName.isModuleName) {
//                    trace.report(MODULE_PACKAGE_CANNOT_BE_IMPORTED.on(referenceExpression))
//                    descriptors.add(packageOrClassDescriptor)
//                }


                if (!isVisible(descriptor, fromToCheck, position, languageVersionSettings)) {
                    trace.report(
                        INVISIBLE_REFERENCE.on(
                            referenceExpression,
                            descriptor,
                            descriptor.visibility,
                            descriptor
                        )
                    )
                }


            }


            else -> {

            }
        }
//        }
        return if (isQualifier) storeQualifier(trace, referenceExpression, descriptor, scope) else null
    }

    /**
     * 存储限定符并创建接收器
     *
     * 根据描述符类型创建相应的限定符接收器，用于后续的成员解析。
     *
     * 限定符类型：
     * - PackageQualifier：包限定符（如 com.example）
     * - ClassQualifier：类限定符（如 MyClass）
     * - EnumClassQualifier：枚举类限定符
     * - TypeParameterQualifier：类型参数限定符
     * - TypeAliasQualifier：类型别名限定符
     *
     * @param trace 用于记录绑定信息
     * @param referenceExpression 引用表达式 PSI 节点
     * @param descriptor 描述符
     * @param scope 词法作用域，用于解析类型
     * @return 创建的限定符接收器
     */
    private fun storeQualifier(
        trace: BindingTrace,
        referenceExpression: CjSimpleNameExpression,
        descriptor: DeclarationDescriptor,
        scope: LexicalScope? = null,

        ): QualifierReceiver? {
        val qualifier =
            when (descriptor) {
                is PackageViewDescriptor -> PackageQualifier(referenceExpression, descriptor)
                is ClassDescriptor -> {
//                    if (descriptor.kind == ClassKind.ENUM) {
//                        EnumClassQualifier(referenceExpression, descriptor)
//                    } else {


                    ClassQualifier(referenceExpression, descriptor, scope?.let {

                        typeResolver.resolveTypeForClass(referenceExpression, it, trace, descriptor)
                    })

//                    }


                }

                is TypeParameterDescriptor -> TypeParameterQualifier(referenceExpression, descriptor)
                is TypeAliasDescriptor -> {
                    val classDescriptor = descriptor.classDescriptor ?: return null
                    TypeAliasQualifier(referenceExpression, descriptor, classDescriptor)
                }

                else -> return null
            }

        trace.record(BindingContext.QUALIFIER, qualifier.expression, qualifier)

        return qualifier
    }

    private fun resolveToPackageOrClass(
        path: List<QualifierPart>,
        moduleDescriptor: ModuleDescriptor,
        trace: BindingTrace,
        shouldBeVisibleFrom: DeclarationDescriptor?,
        scopeForFirstPart: LexicalScope?,
        position: QualifierPosition
    ): DeclarationDescriptor? {
        val (packageOrClassDescriptor, endIndex) =
            resolveToPackageOrClassPrefix(
                path,
                moduleDescriptor,
                trace,
                shouldBeVisibleFrom,
                scopeForFirstPart,
                position
            )

        if (endIndex != path.size) {
            return null
        }

        return packageOrClassDescriptor
    }


    /**
     * 处理导入引用（公开接口）
     *
     * 对外提供的导入处理入口，支持多模块解析。
     * 如果当前模块有解析锚点（resolution anchor），会同时在两个模块中解析。
     *
     * @param importDirective 导入指令
     * @param moduleDescriptor 模块描述符
     * @param trace 用于记录绑定信息
     * @param excludedImportNames 排除的导入名称
     * @param packageFragmentForVisibilityCheck 用于可见性检查的包片段
     * @return 导入作用域
     */
    fun processImportReference(
        importDirective: CjImportInfo,
        moduleDescriptor: ModuleDescriptor,
        trace: BindingTrace,
        excludedImportNames: Collection<FqName>,
        packageFragmentForVisibilityCheck: PackageFragmentDescriptor?
    ): ImportingScope? {
        fun processReferenceInContextOf(moduleDescriptor: ModuleDescriptor): ImportingScope? =
            doProcessImportReference(
                importDirective,
                moduleDescriptor,
                trace,
                excludedImportNames,
                packageFragmentForVisibilityCheck
            )

        val primaryImportingScope = processReferenceInContextOf(moduleDescriptor)


        val resolutionAnchor = moduleDescriptor.getResolutionAnchorIfAny() ?: return primaryImportingScope
        val anchorImportingScope = processReferenceInContextOf(resolutionAnchor) ?: return primaryImportingScope
        if (primaryImportingScope == null) return anchorImportingScope
        return CompositePrioritizedImportingScope(anchorImportingScope, primaryImportingScope)
    }
}


