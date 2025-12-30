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

import com.intellij.codeInsight.completion.CompletionUtilCore
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.config.LanguageVersionSettingsImpl
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.incremental.CangJieLookupLocation
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.getTopmostParentQualifiedExpressionForSelector
import org.cangnova.cangjie.resolve.TypeResolver
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.DelegatingBindingTrace
import org.cangnova.cangjie.resolve.calls.CallExpressionElement
import org.cangnova.cangjie.resolve.calls.unrollToLeftMostQualifiedExpression
import org.cangnova.cangjie.resolve.qualified.ExpressionQualifierPart
import org.cangnova.cangjie.resolve.qualified.QualifiedExpressionResolveResult
import org.cangnova.cangjie.resolve.qualified.QualifierPart
import org.cangnova.cangjie.resolve.qualified.QualifierPosition
import org.cangnova.cangjie.resolve.qualified.context.IsValueChecker
import org.cangnova.cangjie.resolve.qualified.context.ResolutionContext
import org.cangnova.cangjie.resolve.scopes.*
import org.cangnova.cangjie.resolve.scopes.receivers.*
import org.cangnova.cangjie.types.expressions.ExpressionTypingContext

/**
 * 表达式限定符解析器
 *
 * 专门处理表达式位置的限定名称解析，识别哪部分是类型限定符，哪部分是成员调用链。
 *
 * ## 解析策略
 *
 * 在表达式位置，值（变量/函数）优先于类型（类/包）：
 * - `foo.bar()` 中的 `foo` 首先被检查是否是变量
 * - 如果 `foo` 是变量，则 `bar` 是成员调用
 * - 如果 `foo` 是类/包，则继续解析限定符
 *
 * ## 使用示例
 *
 * ```kotlin
 * val resolver = ExpressionQualifierResolver(typeResolver, prefixResolver)
 *
 * // 解析 com.example.MyClass.companion.foo()
 * val elements = resolver.resolveAndUnroll(expression, context) { name ->
 *     isValueInScope(name)
 * }
 * // elements = [foo()]（companion 解析为限定符）
 * ```
 */
class ExpressionQualifierResolver(
    private val typeResolver: TypeResolver,
    private val prefixResolver: QualifierPrefixResolver,
    val languageVersionSettings: LanguageVersionSettings,

) {
    /**
     * 解析限定表达式中的类或包
     *
     * 从左到右解析限定表达式，尽可能多地识别出类或包的前缀部分。
     *
     * @param expression 要解析的限定表达式
     * @param scope 解析的词法作用域
     * @param bindingContext 绑定上下文，用于查询已解析的信息
     * @param moduleDescriptor 模块描述符
     * @return 解析结果，包含识别出的类/包描述符和剩余的成员名
     */
    fun resolveClassOrPackage(
        expression: CjQualifiedExpression,
        scope: LexicalScope,
        bindingContext: BindingContext,
        moduleDescriptor: ModuleDescriptor
    ): QualifiedExpressionResolveResult {
        val qualifiedExpressions = unrollToLeftMostQualifiedExpression(expression)
        val path = mapToQualifierParts(qualifiedExpressions, 0)
        val trace = DelegatingBindingTrace(bindingContext, "Temp trace for resolving qualified expression")

        val context = ResolutionContext.fromScope(
            trace = trace,
            scope = scope,
            position = QualifierPosition.EXPRESSION,
            languageVersionSettings = languageVersionSettings
        )

        val result = prefixResolver.resolve(path, context)

        if (result.descriptor == null) return QualifiedExpressionResolveResult.UNRESOLVED

        return when (result.nextIndex) {
            path.size -> QualifiedExpressionResolveResult(result.descriptor, null)
            path.size - 1 -> QualifiedExpressionResolveResult(result.descriptor, path[result.nextIndex].name)
            else -> QualifiedExpressionResolveResult.UNRESOLVED
        }
    }

    /**
     * 解析表达式中的限定符并展开调用链
     *
     * 在表达式位置解析限定名称，识别哪部分是类型限定符，哪部分是成员调用链。
     *
     * @param expression 限定表达式
     * @param context 表达式类型推导上下文
     * @param isValue 判断简单名称表达式是否表示值的谓词
     * @return 调用表达式元素列表，表示成员访问和调用链
     */
    fun resolveAndUnroll(
        expression: CjQualifiedExpression,
        context: ExpressionTypingContext,
        isValue: IsValueChecker
    ): List<CallExpressionElement> {
        val qualifiedExpressions = unrollToLeftMostQualifiedExpression(expression)
        val maxPossibleQualifierPrefix = mapToQualifierParts(qualifiedExpressions, 1)

        val resolutionContext = ResolutionContext.fromScope(
            trace = context.trace,
            scope = context.scope,
            position = QualifierPosition.EXPRESSION,
            languageVersionSettings = languageVersionSettings
        )

        // 首先尝试直接解析
        var result = prefixResolver.resolve(maxPossibleQualifierPrefix, resolutionContext, isValue)

        // 如果直接解析失败，尝试从导入作用域查找
        if (result.nextIndex == 0) {
            val importContext = resolutionContext.copy(
                scopeForFirstPart = context.scope
            )
            val importResult = prefixResolver.resolve(maxPossibleQualifierPrefix, importContext, isValue)
            if (importResult.nextIndex != 0) {
                result = importResult
            }
        }

        val nextExpressionIndexAfterQualifier =
            if (result.nextIndex == 0) 0 else result.nextIndex - 1

        return qualifiedExpressions
            .subList(nextExpressionIndexAfterQualifier, qualifiedExpressions.size)
            .map(::CallExpressionElement)
    }

    /**
     * 为诊断目的解析简单名称表达式为限定符接收器
     *
     * 在表达式位置尝试将简单名称解析为限定符（包、类、枚举类等），
     * 用于提供代码补全、导航等 IDE 功能。
     *
     * @param expression 简单名称表达式
     * @param receiver 接收器（如果有）
     * @param context 表达式类型推导上下文
     * @return 限定符接收器，如果无法解析为限定符则返回 null
     */
    fun resolveNameExpressionAsQualifier(
        expression: CjSimpleNameExpression,
        receiver: Receiver?,
        context: ExpressionTypingContext
    ): QualifierReceiver? {
        val name = expression.referencedNameAsName

        // 跳过补全占位符
        if (!expression.isPhysical && !name.isSpecial &&
            name.asString().endsWith(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED)) {
            return null
        }

        val location = CangJieLookupLocation(expression)
        val qualifierDescriptor = resolveQualifierDescriptor(name, receiver, context, location)

        if (qualifierDescriptor != null) {
            typeResolver.resolveTypeForClass(expression, context.scope, context.trace, qualifierDescriptor)
            return createQualifierReceiver(expression, qualifierDescriptor, context)
        }

        return null
    }

    /**
     * 解析限定符描述符
     */
    private fun resolveQualifierDescriptor(
        name: Name,
        receiver: Receiver?,
        context: ExpressionTypingContext,
        location: CangJieLookupLocation
    ): DeclarationDescriptor? {
        return when (receiver) {
            is PackageQualifier -> {
                val childPackageFQN = receiver.descriptor.fqName.child(name)
                receiver.descriptor.module.getPackage(childPackageFQN).takeUnless { it.isEmpty() }
                    ?: receiver.descriptor.memberScope.getContributedClassifier(name, location)
            }

            is EnumClassQualifier -> receiver.staticScope.getContributedClassifier(name, location)
            is ClassQualifier -> receiver.staticScope.getContributedClassifier(name, location)

            null -> context.scope.findClassifier(name, location)
                ?: context.scope.getPackageView(name, location)

            is ReceiverValue -> receiver.type.memberScope.memberScopeAsImportingScope().findClassifier(name, location)
            else -> null
        }
    }

    /**
     * 创建限定符接收器
     */
    private fun createQualifierReceiver(
        expression: CjSimpleNameExpression,
        descriptor: DeclarationDescriptor,
        context: ExpressionTypingContext
    ): QualifierReceiver? {
        val qualifier = when (descriptor) {
            is PackageViewDescriptor -> PackageQualifier(expression, descriptor)

            is ClassDescriptor -> ClassQualifier(
                expression,
                descriptor,
                typeResolver.resolveTypeForClass(expression, context.scope, context.trace, descriptor)
            )

            is TypeParameterDescriptor -> TypeParameterQualifier(expression, descriptor)

            is TypeAliasDescriptor -> {
                val classDescriptor = descriptor.classDescriptor ?: return null
                TypeAliasQualifier(expression, descriptor, classDescriptor)
            }

            else -> return null
        }

        context.trace.record(BindingContext.QUALIFIER, qualifier.expression, qualifier)
        context.trace.record(BindingContext.REFERENCE_TARGET, expression, descriptor)

        return qualifier
    }

    /**
     * 将限定表达式列表映射为限定符部分列表
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
}
