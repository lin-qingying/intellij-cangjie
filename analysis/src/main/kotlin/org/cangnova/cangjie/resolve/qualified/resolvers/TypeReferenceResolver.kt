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
import org.cangnova.cangjie.diagnostics.infos.errors.ENUM_ENTRY_AS_TYPE
import org.cangnova.cangjie.diagnostics.infos.errors.MODULE_CANNOT_BE_USED_AS_TYPE
import org.cangnova.cangjie.diagnostics.reportInvisibleReference
import org.cangnova.cangjie.diagnostics.reportUnresolvedReference
import org.cangnova.cangjie.incremental.CangJieLookupLocation
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.psi.CjUserType
import org.cangnova.cangjie.resolve.qualified.TypeQualifierResolutionResult
import org.cangnova.cangjie.resolve.qualified.asQualifierPartList
import org.cangnova.cangjie.psi.psiUtil.getTopmostParentQualifiedExpressionForSelector
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.fqNameSafe
import org.cangnova.cangjie.resolve.qualified.ExpressionQualifierPart
import org.cangnova.cangjie.resolve.qualified.context.ResolutionContext
import org.cangnova.cangjie.resolve.qualified.result.TypeResolutionResult
import org.cangnova.cangjie.resolve.qualified.validation.ModuleNameValidator
import org.cangnova.cangjie.resolve.qualified.validation.VisibilityChecker
import org.cangnova.cangjie.resolve.scopes.findClassifier
import org.cangnova.cangjie.resolve.scopes.findFirstClassifierWithDeprecationStatus

/**
 * 类型引用解析器
 *
 * 专门处理类型位置的限定名称解析，如 `com.example.MyClass<T>`。
 *
 * ## 功能
 *
 * 1. 解析简单类型引用（无限定名）
 * 2. 解析带限定名的类型引用
 * 3. 验证类型的可见性
 * 4. 检查是否误用枚举条目作为类型
 * 5. 检查是否在类型位置使用了模块名限定符
 *
 * ## 使用示例
 *
 * ```kotlin
 * val resolver = TypeReferenceResolver(
 *     lookupStrategies = listOf(...),
 *     visibilityChecker = VisibilityChecker(settings),
 *     moduleNameValidator = ModuleNameValidator()
 * )
 *
 * val result = resolver.resolve(userType, context)
 * when (result) {
 *     is TypeResolutionResult.Success -> {
 *         // 使用 result.descriptor
 *     }
 *     is TypeResolutionResult.NotFound -> {
 *         // 报告未解析引用错误
 *     }
 *     // ...
 * }
 * ```
 */
class TypeReferenceResolver(
    private val prefixResolver: QualifierPrefixResolver,
    private val visibilityChecker: VisibilityChecker,
    private val moduleNameValidator: ModuleNameValidator = ModuleNameValidator()
) {
    /**
     * 解析用户类型
     *
     * @param userType PSI 类型节点
     * @param context 解析上下文
     * @return 类型解析结果
     */
    fun resolve(
        userType: CjUserType,
        context: ResolutionContext
    ): TypeQualifierResolutionResult {
        val (qualifierPartList, hasError) = userType.asQualifierPartList()

        // 1. 无限定名：直接在当前作用域查找
        if (userType.qualifier == null) {
            val result = resolveSimpleType(userType, context)
            return when (result) {
                is TypeResolutionResult.Success -> TypeQualifierResolutionResult(qualifierPartList, result.descriptor)
                else -> TypeQualifierResolutionResult(qualifierPartList, null)
            }
        }

        // 2. 有限定名：逐级解析
        if (hasError || qualifierPartList.isEmpty()) {
            return TypeQualifierResolutionResult(qualifierPartList, null)
        }

        val result = resolveQualifiedType(qualifierPartList, context)
        return when (result) {
            is TypeResolutionResult.Success -> TypeQualifierResolutionResult(qualifierPartList, result.descriptor)
            else -> TypeQualifierResolutionResult(qualifierPartList, null)
        }
    }

    /**
     * 解析双冒号左侧的描述符
     *
     * 用于成员引用表达式（如 `String::length`）的左侧类型解析。
     *
     * @param expression 表达式
     * @param context 解析上下文
     * @return 类型解析结果
     */
    fun resolveForDoubleColonLHS(
        expression: CjExpression,
        context: ResolutionContext
    ): TypeQualifierResolutionResult {
        val qualifierPartList = expression.asQualifierPartList(doubleColonLHS = true)
        if (qualifierPartList.isEmpty()) {
            return TypeQualifierResolutionResult(qualifierPartList, null)
        }

        if (qualifierPartList.size == 1) {
            val (name, simpleNameExpression) = qualifierPartList.single()
            val scope = context.scopeForFirstPart
                ?: return TypeQualifierResolutionResult(qualifierPartList, null)

            val (classifier, _) = scope.findFirstClassifierWithDeprecationStatus(
                name,
                CangJieLookupLocation(simpleNameExpression)
            ) ?: return TypeQualifierResolutionResult(qualifierPartList, null)

            simpleNameExpression?.let { expr ->
                context.trace.record(BindingContext.REFERENCE_TARGET, expr, classifier)
            }

            return TypeQualifierResolutionResult(qualifierPartList, classifier)
        }

        // 多段限定名的情况
        val prefixParts = qualifierPartList.dropLast(1).map {
            ExpressionQualifierPart(it.name, it.expression, it.typeArguments)
        }
        val (qualifier, nextIndex) = resolveQualifierPrefix(prefixParts, context)

        if (qualifier == null || nextIndex != prefixParts.size) {
            return TypeQualifierResolutionResult(qualifierPartList, null)
        }

        val lastPart = qualifierPartList.last()
        val classifier = findClassifierInQualifier(
            qualifier,
            ExpressionQualifierPart(lastPart.name, lastPart.expression, lastPart.typeArguments),
            context
        )

        lastPart.expression?.let { expr ->
            if (classifier != null) {
                context.trace.record(BindingContext.REFERENCE_TARGET, expr, classifier)
            }
        }

        return TypeQualifierResolutionResult(qualifierPartList, classifier)
    }

    /**
     * 解析简单类型（无限定名）
     */
    private fun resolveSimpleType(
        userType: CjUserType,
        context: ResolutionContext
    ): TypeResolutionResult {
        val expression = userType.referenceExpression
            ?: return TypeResolutionResult.Error("Missing type name")

        val name = expression.referencedNameAsName
        val scope = context.scopeForFirstPart
            ?: return TypeResolutionResult.Error("No scope available")

        // 在当前作用域查找分类器
        val (classifier, isDeprecated) = scope.findFirstClassifierWithDeprecationStatus(
            name,
            CangJieLookupLocation(expression)
        ) ?: run {
            // 使用统一的错误报告方法
            context.trace.reportUnresolvedReference(expression)
            return TypeResolutionResult.NotFound(name)
        }

        // 检查是否是枚举条目（不能作为类型）
        if (!checkNotEnumEntry(classifier, expression, context)) {
            return TypeResolutionResult.Error("Enum entry cannot be used as type", name)
        }

        // 检查可见性
        if (!visibilityChecker.isVisible(classifier, context)) {
            // 可见性错误仍然返回描述符，但会报告错误
            reportVisibilityError(classifier, expression, context)
        }

        // 记录绑定
        storeTypeReference(expression, classifier, context)

        // 记录废弃状态
        if (isDeprecated) {
            context.trace.record(BindingContext.DEPRECATED_SHORT_NAME_ACCESS, expression)
        }

        return TypeResolutionResult.Success(classifier)
    }

    /**
     * 解析带限定名的类型
     */
    private fun resolveQualifiedType(
        qualifierPartList: List<ExpressionQualifierPart>,
        context: ResolutionContext
    ): TypeResolutionResult {
        // 解析限定符前缀（除最后一部分外）
        val prefixParts = qualifierPartList.dropLast(1)
        val (qualifier, nextIndex) = resolveQualifierPrefix(prefixParts, context)

        if (qualifier == null || nextIndex != prefixParts.size) {
            // 错误已经在 resolveQualifierPrefix 中报告
            val unresolvedPart = if (nextIndex < prefixParts.size) {
                prefixParts[nextIndex]
            } else {
                prefixParts.firstOrNull()
            }
            return TypeResolutionResult.NotFound(
                unresolvedPart?.name ?: qualifierPartList.first().name
            )
        }

        // 检查是否使用了模块名作为限定符
        val moduleError = moduleNameValidator.validateForType(qualifierPartList, qualifier, context)
        if (moduleError != null) {
            moduleNameValidator.reportError(moduleError, context)
            // 继续解析，但报告错误
        }

        // 在限定符作用域中查找类型
        val lastPart = qualifierPartList.last()
        val classifier = findClassifierInQualifier(qualifier, lastPart, context)

        if (classifier == null) {
            // 使用统一的错误报告方法
            lastPart.expression?.let { expr ->
                context.trace.reportUnresolvedReference(expr)
            }
            return TypeResolutionResult.NotFound(lastPart.name)
        }

        // 检查是否是枚举条目
        if (!checkNotEnumEntry(classifier, lastPart.expression, context)) {
            return TypeResolutionResult.Error("Enum entry cannot be used as type", lastPart.name)
        }

        // 检查可见性
        if (!visibilityChecker.isVisible(classifier, context)) {
            lastPart.expression?.let { expr ->
                reportVisibilityError(classifier, expr, context)
            }
        }

        // 记录绑定
        lastPart.expression?.let { expr ->
            storeTypeReference(expr, classifier, context)
        }

        return TypeResolutionResult.Success(classifier)
    }

    /**
     * 解析限定符前缀
     */
    private fun resolveQualifierPrefix(
        parts: List<ExpressionQualifierPart>,
        context: ResolutionContext
    ): Pair<DeclarationDescriptor?, Int> {
        if (parts.isEmpty()) {
            return Pair(null, 0)
        }

        val scope = context.scopeForFirstPart
        val module = context.moduleDescriptor

        // 尝试从当前作用域查找第一部分
        val firstPart = parts.first()
        var currentDescriptor: DeclarationDescriptor? = scope?.findClassifier(
            firstPart.name,
            NoLookupLocation.FOR_DEFAULT_IMPORTS
        )

        // 如果在作用域中找不到，尝试作为包查找
        if (currentDescriptor == null) {
            val packageView = module.getPackage(org.cangnova.cangjie.name.FqName(firstPart.name.asString()))
            if (!packageView.isEmpty()) {
                currentDescriptor = packageView
            }
        }

        if (currentDescriptor == null) {
            // 第一部分解析失败，使用统一的错误报告方法
            firstPart.expression?.let { expr ->
                context.trace.reportUnresolvedReference(expr)
            }
            return Pair(null, 0)
        }

        // 记录第一部分的绑定
        firstPart.expression?.let { expr ->
            context.trace.record(BindingContext.REFERENCE_TARGET, expr, currentDescriptor)
        }

        // 继续解析剩余部分
        for (index in 1 until parts.size) {
            val part = parts[index]
            val nextDescriptor = findNextDescriptor(currentDescriptor!!, part, context)

            if (nextDescriptor == null) {
                // 使用统一的错误报告方法
                part.expression?.let { expr ->
                    context.trace.reportUnresolvedReference(expr)
                }
                return Pair(currentDescriptor, index)
            }

            // 记录绑定
            part.expression?.let { expr ->
                context.trace.record(BindingContext.REFERENCE_TARGET, expr, nextDescriptor)
            }

            currentDescriptor = nextDescriptor
        }

        return Pair(currentDescriptor, parts.size)
    }

    /**
     * 在当前描述符的作用域中查找下一个描述符
     */
    private fun findNextDescriptor(
        current: DeclarationDescriptor,
        part: ExpressionQualifierPart,
        context: ResolutionContext
    ): DeclarationDescriptor? {
        return when (current) {
            is PackageViewDescriptor -> {
                // 先尝试子包
                val childPackage = context.moduleDescriptor.getPackage(current.fqName.child(part.name))
                if (!childPackage.isEmpty()) {
                    childPackage
                } else {
                    // 再尝试类
                    current.memberScope.getContributedClassifier(part.name, NoLookupLocation.FOR_DEFAULT_IMPORTS)
                }
            }
            is ClassDescriptor -> {
                current.unsubstitutedMemberScope.getContributedClassifier(part.name, NoLookupLocation.FOR_DEFAULT_IMPORTS)
            }
            else -> null
        }
    }

    /**
     * 在限定符作用域中查找分类器
     */
    private fun findClassifierInQualifier(
        qualifier: DeclarationDescriptor,
        part: ExpressionQualifierPart,
        context: ResolutionContext
    ): ClassifierDescriptor? {
        return when (qualifier) {
            is PackageViewDescriptor -> {
                qualifier.memberScope.getContributedClassifier(part.name, NoLookupLocation.FOR_DEFAULT_IMPORTS)
            }
            is ClassDescriptor -> {
                qualifier.unsubstitutedMemberScope.getContributedClassifier(part.name, NoLookupLocation.FOR_DEFAULT_IMPORTS)
            }
            else -> null
        }
    }

    /**
     * 检查是否是枚举条目（枚举条目不能作为类型）
     *
     * @return true 如果不是枚举条目（可以作为类型），false 如果是枚举条目
     */
    private fun checkNotEnumEntry(
        descriptor: ClassifierDescriptor?,
        expression: CjSimpleNameExpression?,
        context: ResolutionContext
    ): Boolean {
        if (expression == null || descriptor == null) return true

        if (DescriptorUtils.isEnumConstructor(descriptor)) {
            val qualifiedParent = expression.getTopmostParentQualifiedExpressionForSelector()
            if (qualifiedParent == null) {
                context.trace.report(ENUM_ENTRY_AS_TYPE.on(expression))
                return false
            }
        }
        return true
    }

    /**
     * 记录类型引用绑定
     */
    private fun storeTypeReference(
        expression: CjSimpleNameExpression,
        descriptor: ClassifierDescriptor,
        context: ResolutionContext
    ) {
        context.trace.record(BindingContext.REFERENCE_TARGET, expression, descriptor)
    }

    /**
     * 报告可见性错误
     */
    private fun reportVisibilityError(
        descriptor: ClassifierDescriptor,
        expression: CjSimpleNameExpression,
        context: ResolutionContext
    ) {
        val descriptorWithVisibility = descriptor as? DeclarationDescriptorWithVisibility
            ?: return
        // 使用统一的错误报告方法
        context.trace.reportInvisibleReference(expression, descriptorWithVisibility)
    }

    /**
     * 将 UserType 转换为限定符部分列表
     */
    private fun CjUserType.asQualifierPartList(): Pair<List<ExpressionQualifierPart>, Boolean> {
        var hasError = false
        val result = mutableListOf<ExpressionQualifierPart>()
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
}
