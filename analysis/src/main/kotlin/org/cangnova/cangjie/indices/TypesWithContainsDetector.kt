/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.indices

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.resolve.scopes.LexicalScope
import org.cangnova.cangjie.resolve.scopes.collectFunctions
import org.cangnova.cangjie.search.isValidOperator
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.TypeSubstitutor
import org.cangnova.cangjie.types.checker.CangJieTypeChecker
import org.cangnova.cangjie.types.checker.SimpleClassicTypeSystemContext.isOptionType
import org.cangnova.cangjie.types.isOptionType
import org.cangnova.cangjie.utils.addIfNotNull
import org.cangnova.cangjie.utils.isExtension

/**
 * 检测支持 `contains` 操作符的类型
 *
 * 用于在代码补全等场景中查找支持 `contains` 操作的类型。
 * 此类使用新约束系统进行类型检查，而不是已废弃的 FuzzyType。
 *
 * @param scope 词法作用域
 * @param indicesHelper 索引助手，用于查找操作符
 * @param argumentType 参数类型，用于检查操作符参数是否匹配
 */
class TypesWithContainsDetector(
    scope: LexicalScope,
    indicesHelper: CangJieIndicesHelper?,
    private val argumentType: CangJieType
) : TypesWithOperatorDetector(OperatorNameConventions.CONTAINS, scope, indicesHelper) {

    /**
     * 检查操作符是否适合指定的参数类型
     *
     * 使用新约束系统检查操作符的参数类型是否为 argumentType 的超类型。
     * 如果是，则创建并返回类型替换器；否则返回 null。
     *
     * @param operator 待检查的函数描述符
     * @param freeTypeParams 自由类型参数集合
     * @return 如果类型匹配则返回类型替换器，否则返回 null
     */
    override fun checkIsSuitableByType(
        operator: FunctionDescriptor,
        freeTypeParams: Collection<TypeParameterDescriptor>
    ): TypeSubstitutor? {
        val parameter = operator.valueParameters.single()
        val parameterType = parameter.type

        // 使用新约束系统检查参数类型是否为 argumentType 的超类型
        // 即检查 argumentType 是否可以赋值给 parameterType
        val isSubtype = CangJieTypeChecker.DEFAULT.isSubtypeOf(argumentType, parameterType)

        if (!isSubtype) {
            return null
        }

        // 如果类型匹配，返回空替换器（表示不需要类型替换）
        // 如果未来需要支持泛型类型推断，这里需要实现更复杂的替换逻辑
        return TypeSubstitutor.EMPTY
    }
}

/**
 * 操作符类型检测器抽象基类
 *
 * 用于查找支持特定操作符的类型。此类使用新约束系统进行类型检查。
 *
 * @param name 操作符名称（如 CONTAINS、ITERATOR 等）
 * @param scope 词法作用域
 * @param indicesHelper 索引助手，用于查找操作符
 */
abstract class TypesWithOperatorDetector(
    private val name: Name,
    private val scope: LexicalScope,
    private val indicesHelper: CangJieIndicesHelper?
) {
    /**
     * 检查操作符是否适合指定的类型
     *
     * 子类需要实现此方法，使用新约束系统进行类型检查。
     *
     * @param operator 待检查的函数描述符
     * @param freeTypeParams 自由类型参数集合
     * @return 如果类型匹配则返回类型替换器，否则返回 null
     */
    protected abstract fun checkIsSuitableByType(
        operator: FunctionDescriptor,
        freeTypeParams: Collection<TypeParameterDescriptor>
    ): TypeSubstitutor?

    /**
     * 缓存类型和对应的操作符及替换器
     *
     * 使用 CangJieType 作为键，而不是已废弃的 FuzzyType
     */
    private val cache = HashMap<CangJieType, Pair<FunctionDescriptor, TypeSubstitutor>?>()

    /**
     * 扩展操作符集合
     *
     * 延迟初始化，收集作用域内和索引中的所有扩展操作符
     */
    val extensionOperators: Collection<FunctionDescriptor> by lazy {
        val result = ArrayList<FunctionDescriptor>()

        val extensionsFromScope = scope
            .collectFunctions(name, NoLookupLocation.FROM_IDE)
            .filter { it.isExtension }
        result.addSuitableOperators(extensionsFromScope)

        indicesHelper?.getTopLevelExtensionOperatorsByName(name.asString())?.let { result.addSuitableOperators(it) }

        result.distinctBy { it.original }
    }

    /**
     * 包含成员操作符的类集合
     *
     * 延迟初始化，收集索引中所有包含成员操作符的类
     */
    val classesWithMemberOperators: Collection<ClassDescriptor> by lazy {
        if (indicesHelper == null) return@lazy emptyList<ClassDescriptor>()
        val operators = ArrayList<FunctionDescriptor>().addSuitableOperators(indicesHelper.getMemberOperatorsByName(name.asString()))
        operators.map { it.containingDeclaration as ClassDescriptor }.distinct()
    }

    /**
     * 添加合适的操作符到集合
     *
     * 遍历函数集合，检查每个函数是否适合作为操作符，如果适合则添加到结果集合。
     *
     * @param functions 待检查的函数集合
     * @return 更新后的函数集合
     */
    private fun MutableCollection<FunctionDescriptor>.addSuitableOperators(functions: Collection<FunctionDescriptor>): MutableCollection<FunctionDescriptor> {
        for (function in functions) {
            if (!function.isValidOperator()) continue

            var freeParameters = function.typeParameters
            val containingClass = function.containingDeclaration as? ClassDescriptor
            if (containingClass != null) {
                freeParameters += containingClass.typeConstructor.parameters
            }

            val substitutor = checkIsSuitableByType(function, freeParameters) ?: continue
            addIfNotNull(function.substitute(substitutor))
        }
        return this
    }

    /**
     * 查找支持操作符的类型
     *
     * 使用缓存机制提高性能，对于已检查过的类型直接返回缓存结果。
     *
     * @param type 待检查的类型
     * @return 如果找到合适的操作符，返回操作符描述符和类型替换器的配对；否则返回 null
     */
    fun findOperator(type: CangJieType): Pair<FunctionDescriptor, TypeSubstitutor>? = if (cache.containsKey(type)) {
        cache[type]
    } else {
        val result = findOperatorNoCache(type)
        cache[type] = result
        result
    }

    /**
     * 查找支持操作符的类型（无缓存版本）
     *
     * 首先在类型的成员函数中查找操作符，然后在扩展函数中查找。
     * 注意：Option 类型不支持成员操作符。
     *
     * @param type 待检查的类型
     * @return 如果找到合适的操作符，返回操作符描述符和类型替换器的配对；否则返回 null
     */
    private fun findOperatorNoCache(type: CangJieType): Pair<FunctionDescriptor, TypeSubstitutor>? {
        // 如果不是 Option 类型，则在成员函数中查找操作符
        if (!type.isOptionType()) {
            for (memberFunction in type.memberScope.getContributedFunctions(name, NoLookupLocation.FROM_IDE)) {
                if (memberFunction.isValidOperator()) {
                    // 获取成员函数所在类的类型参数作为自由类型参数
                    val freeParams = (memberFunction.containingDeclaration as? ClassDescriptor)
                        ?.typeConstructor
                        ?.parameters
                        ?: emptyList()

                    val substitutor = checkIsSuitableByType(memberFunction, freeParams) ?: continue
                    val substituted = memberFunction.substitute(substitutor) ?: continue
                    return substituted to substitutor
                }
            }
        }

        // 在扩展函数中查找操作符
        for (operator in extensionOperators) {
            // 在仓颉语言中，extend 成员使用 dispatchReceiver
            val receiverType = operator.dispatchReceiverParameter?.type ?: continue

            // 使用新约束系统检查类型是否匹配
            // 检查 type 是否为 receiverType 的子类型
            val isSubtype = CangJieTypeChecker.DEFAULT.isSubtypeOf(type, receiverType)

            if (!isSubtype) {
                continue
            }

            // 如果类型匹配，返回操作符和空替换器
            val substitutor = TypeSubstitutor.EMPTY
            val substituted = operator.substitute(substitutor) ?: continue
            return substituted to substitutor
        }

        return null
    }
}
