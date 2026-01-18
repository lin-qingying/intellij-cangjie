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

package org.cangnova.cangjie.types

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.descriptors.ClassDescriptor
import org.cangnova.cangjie.descriptors.ClassifierDescriptor
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.renderer.DescriptorRenderer
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.types.error.ErrorScopeKind
import org.cangnova.cangjie.types.error.ErrorTypeKind
import org.cangnova.cangjie.utils.DFS
import java.util.*
import java.util.stream.Collectors

/**
 * 公共超类型计算工具
 *
 * 该对象负责计算多个类型的最近公共超类型（Least Common Supertype）。
 * 这在类型推断、类型合并等场景中非常重要，例如：
 * - 条件表达式中不同分支的类型统一
 * - 集合元素的公共类型推断
 * - 类型层次结构的分析
 */
object CommonSupertypes {
    /**
     * 查找公共超类型的核心方法
     *
     * @param types 需要查找公共超类型的类型集合
     * @param recursionDepth 当前递归深度，用于防止无限递归
     * @param maxDepth 允许的最大递归深度
     * @return 所有输入类型的公共超类型
     */
    private fun findCommonSupertype(types: Collection<CangJieType>, recursionDepth: Int, maxDepth: Int): CangJieType {
        // 确保递归深度不超过最大深度，防止栈溢出
        assert(recursionDepth <= maxDepth) { "Recursion depth exceeded: $recursionDepth > $maxDepth for types $types" }

        var hasFlexible = false
        // 上界类型列表（对于灵活类型）
        val upper: MutableList<SimpleType> = ArrayList(types.size)
        // 下界类型列表（对于灵活类型）
        val lower: MutableList<SimpleType> = ArrayList(types.size)

        // 遍历所有类型，分别处理灵活类型和简单类型
        for (type in types) {
            val unwrappedType: UnwrappedType = type.unwrap()
            if (unwrappedType is FlexibleType) {
                // 如果是动态类型，直接返回
                if (unwrappedType.isDynamic()) {
                    return unwrappedType
                }
                hasFlexible = true
                // 灵活类型有上界和下界
                upper.add(unwrappedType.upperBound)
                lower.add(unwrappedType.lowerBound)
            } else {
                // 简单类型直接添加
                val simpleType = unwrappedType as SimpleType
                upper.add(simpleType)
                lower.add(simpleType)
            }
        }

        // 如果没有灵活类型，直接计算简单类型的公共超类型
        if (!hasFlexible) return commonSuperTypeForInflexible(upper, recursionDepth, maxDepth)

        // 如果有灵活类型，分别计算上界和下界的公共超类型，然后构造灵活类型
        return CangJieTypeFactory.flexibleType(
            commonSuperTypeForInflexible(lower, recursionDepth, maxDepth),
            commonSuperTypeForInflexible(upper, recursionDepth, maxDepth)
        )
    }

    /**
     * 拓扑排序超类并记录所有实例
     *
     * 使用深度优先搜索（DFS）遍历类型的继承层次结构，
     * 并记录每个类型构造器的所有实例化情况。
     *
     * @param type 起始类型
     * @param constructorToAllInstances 映射：类型构造器 -> 该构造器的所有实例化
     * @param visited 已访问的类型构造器集合，用于避免重复访问
     * @return 拓扑排序后的类型构造器列表
     */
    private fun topologicallySortSuperclassesAndRecordAllInstances(
        type: SimpleType,
        constructorToAllInstances: MutableMap<TypeConstructor, MutableSet<SimpleType>>,
        visited: MutableSet<TypeConstructor>
    ): List<TypeConstructor> {
        return DFS.dfs(
            listOf(type),
            // 邻居函数：获取当前类型的所有超类型
            { current ->
                val substitutor: ComposableTypeSubstitutor = ComposableTypeSubstitutor.create(current)
                val supertypes: Collection<CangJieType> = current.constructor.supertypes
                val result: MutableList<SimpleType> = ArrayList(supertypes.size)
                for (supertype in supertypes) {
                    // 跳过已访问的类型构造器
                    if (visited.contains(supertype.constructor)) {
                        continue
                    }
                    // 应用类型替换并转换为下界（如果是灵活类型）
                    result.add(
                        substitutor.safeSubstitute(supertype.unwrap()).lowerIfFlexible()
                    )
                }
                result
            },
            // 访问函数：标记当前节点为已访问
            { current -> visited.add(current.constructor) },
            // 节点处理器：记录每个类型构造器的实例
            object : DFS.NodeHandlerWithListResult<SimpleType, TypeConstructor>() {
                override fun beforeChildren(current: SimpleType): Boolean {
                    // 在访问子节点前，记录当前类型实例
                    val instances =
                        constructorToAllInstances.computeIfAbsent(
                            current.constructor
                        ) { _: TypeConstructor -> LinkedHashSet<SimpleType>() }
                    instances.add(current)

                    return true
                }

                override fun afterChildren(current: SimpleType) {
                    // 在访问完子节点后，将类型构造器添加到结果列表的开头
                    // 这样可以实现拓扑排序（子类在父类之前）
                    result.addFirst(current.constructor)
                }
            }
        )
    }

    /**
     * 计算公共原始超类型
     *
     * 原始超类型是指不带类型参数的超类。
     * 例如：List<String> 和 List<Int> 的原始超类型是 List
     *
     * @param types 输入类型集合
     * @return 映射：类型构造器 -> 该构造器作为超类型出现的所有实例化
     */
    private fun computeCommonRawSupertypes(types: Collection<SimpleType>): Map<TypeConstructor, Set<SimpleType>> {
        assert(!types.isEmpty())

        // 记录每个类型构造器的所有实例化
        val constructorToAllInstances: MutableMap<TypeConstructor, MutableSet<SimpleType>> = HashMap()
        // 公共超类的类型构造器集合
        var commonSuperclasses: MutableSet<TypeConstructor>? = null

        var order: List<TypeConstructor>? = null
        // 对每个输入类型进行处理
        for (type in types) {
            val visited: MutableSet<TypeConstructor> = HashSet()
            // 获取拓扑排序后的超类列表
            order = topologicallySortSuperclassesAndRecordAllInstances(
                type,
                constructorToAllInstances,
                visited
            )

            // 计算所有类型的公共超类（交集）
            if (commonSuperclasses?.retainAll(visited) == null) {
                commonSuperclasses = visited
            }
        }
        checkNotNull(order)

        // 记录非源类型（被其他类型继承的类型）
        val notSource: MutableSet<TypeConstructor> = HashSet()
        val result: MutableMap<TypeConstructor, Set<SimpleType>> = LinkedHashMap()

        // 按拓扑顺序处理超类
        for (superConstructor in order) {
            // 只处理公共超类
            if (!commonSuperclasses!!.contains(superConstructor)) {
                continue
            }

            // 如果该类型构造器不是非源类型，将其添加到结果中
            if (!notSource.contains(superConstructor)) {
                result[superConstructor] = constructorToAllInstances[superConstructor]!!
                // 标记该构造器及其所有超类为非源类型
                markAll(superConstructor, notSource)
            }
        }

        return result
    }

    /**
     * 标记类型构造器及其所有超类
     *
     * @param typeConstructor 要标记的类型构造器
     * @param markerSet 标记集合
     */
    private fun markAll(typeConstructor: TypeConstructor, markerSet: MutableSet<TypeConstructor>) {
        markerSet.add(typeConstructor)
        // 递归标记所有超类型
        for (type in typeConstructor.supertypes) {
            markAll(type.constructor, markerSet)
        }
    }

    /**
     * 获取类型声明描述符的类对象
     *
     * @param type 输入类型
     * @return 描述符的 Java 类对象，如果不存在则返回 null
     */
    private fun classOfDeclarationDescriptor(type: CangJieType): Class<*>? {
        val descriptor = type.constructor.declarationDescriptor
        if (descriptor != null) {
            return descriptor::class.java
        }

        return null
    }

    /**
     * 计算非灵活类型的公共超类型
     *
     * 这是公共超类型计算的核心算法：
     * 1. 移除 Nothing 类型
     * 2. 处理错误类型
     * 3. 计算公共原始超类型
     * 4. 重建类型参数
     *
     * @param types 简单类型集合
     * @param recursionDepth 当前递归深度
     * @param maxDepth 最大递归深度
     * @return 公共超类型
     */
    private fun commonSuperTypeForInflexible(
        types: Collection<SimpleType>,
        recursionDepth: Int,
        maxDepth: Int
    ): SimpleType {
        assert(!types.isEmpty())
        val typeSet: MutableCollection<SimpleType> = mutableSetOf()

        // 移除 Nothing 类型和处理错误类型
        val iterator = typeSet.iterator()
        while (iterator.hasNext()) {
            val type: CangJieType = checkNotNull(iterator.next())
            assert(!type.isFlexible()) { "Flexible type $type passed to commonSuperTypeForInflexible" }

            // Nothing 是所有类型的子类型，不影响公共超类型计算
            if (CangJieBuiltIns.isNothing(type)) {
                iterator.remove()
            }
            // 如果存在错误类型，返回错误类型
            if (type.isError) {
                return ErrorUtils.createErrorType(ErrorTypeKind.SUPER_TYPE_FOR_ERROR_TYPE, type.toString())
            }
        }

        // 如果所有类型都被删除（都是 Nothing），返回 Nothing 类型
        if (typeSet.isEmpty()) {
            val builtIns = types.iterator().next().constructor.builtIns
            return builtIns.nothingType
        }

        // 如果只剩一个类型，直接返回
        if (typeSet.size == 1) {
            return typeSet.iterator().next()
        }

        // 计算公共原始超类型
        // 映射：类型构造器 -> 该构造器作为超类型出现的所有实例化
        var commonSupertypes: Map<TypeConstructor, Set<SimpleType>> =
            computeCommonRawSupertypes(typeSet)

        // 迭代合并，直到只剩一个公共超类型
        while (commonSupertypes.size > 1) {
            val merge: MutableSet<SimpleType> = LinkedHashSet()
            for (supertypes in commonSupertypes.values) {
                merge.addAll(supertypes)
            }
            commonSupertypes = computeCommonRawSupertypes(merge)
        }

        // 如果没有公共超类型，抛出异常（理论上不应该发生）
        if (commonSupertypes.isEmpty()) {
            val info = StringBuilder()
            for (type in types) {
                val superTypes: String = TypeUtils.getAllSupertypes(type).stream()
                    .map { t -> "-- " + renderTypeFully(t) }
                    .collect(Collectors.joining("\n"))

                info
                    .append("Info about ").append(renderTypeFully(type)).append(": ").append('\n')
                    .append("- Supertypes: ").append('\n')
                    .append(superTypes).append('\n')
                    .append("- DeclarationDescriptor class: ")
                    .append(classOfDeclarationDescriptor(type)).append('\n')
                    .append('\n')
            }
            throw IllegalStateException("[Report version 3] There is no common supertype for: $types \n$info")
        }

        // 获取唯一的公共超类型构造器及其实例
        val entry = commonSupertypes.entries.iterator().next()

        // 重建类型参数
        val result: SimpleType =
            computeSupertypeProjections(entry.key, entry.value, recursionDepth, maxDepth)
        return result
    }

    /**
     * 完整渲染类型信息（用于调试）
     *
     * @param type 要渲染的类型
     * @return 类型的完整字符串表示
     */
    private fun renderTypeFully(type: CangJieType): String {
        return DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(type) + ", typeConstructor debug: " +
                renderTypeConstructorVerboseDebugInformation(type.constructor)
    }

    /**
     * 渲染类型构造器的详细调试信息
     *
     * @param typeConstructor 类型构造器
     * @return 详细的调试信息字符串
     */
    private fun renderTypeConstructorVerboseDebugInformation(typeConstructor: TypeConstructor): String {
        if (typeConstructor !is AbstractTypeConstructor) {
            return (typeConstructor.toString() + "[" + typeConstructor::class.simpleName) + "]"
        }

        val declarationDescriptor = typeConstructor.declarationDescriptor
        val moduleDescriptorString = Objects.toString(DescriptorUtils.getContainingModule(declarationDescriptor))
        return "descriptor=" + declarationDescriptor + "@" + Integer.toHexString(Objects.hashCode(declarationDescriptor)) +
                ", moduleDescriptor=" + moduleDescriptorString +
                ", " + typeConstructor.renderAdditionalDebugInformation()
    }

    /**
     * 计算超类型投影（重建类型参数）
     *
     * 对于泛型类型，需要计算类型参数的公共超类型投影。
     * 例如：List<String> 和 List<Int> 的公共超类型是 List<Any>
     *
     * @param constructor 要实例化的超类型的类型构造器
     * @param types 该构造器作为超类型出现的所有实例化
     * @param recursionDepth 当前递归深度
     * @param maxDepth 最大递归深度
     * @return 实例化后的超类型
     */
    private fun computeSupertypeProjections(
        constructor: TypeConstructor,
        types: Set<SimpleType>,
        recursionDepth: Int,
        maxDepth: Int
    ): SimpleType {
        // 假设所有给定类型都是同一个类型构造器的应用
        assert(types.isNotEmpty())

        // 如果只有一个类型，直接返回
        if (types.size == 1) {
            return types.iterator().next()
        }

        // 获取类型参数列表
        val parameters: List<TypeParameterDescriptor> = constructor.parameters
        val newProjections: MutableList<TypeArgument> = ArrayList(parameters.size)

        // 为每个类型参数计算投影
        for (parameterDescriptor in parameters) {
            // 收集所有类型中该参数位置的类型投影
            val typeProjections: MutableSet<TypeArgument> = LinkedHashSet()
            for (type in types) {
                typeProjections.add(type.arguments[parameterDescriptor.index])
            }
            // 计算该参数的公共超类型投影
            newProjections.add(
                computeSupertypeProjection(
                    parameterDescriptor,
                    typeProjections,
                    recursionDepth,
                    maxDepth
                )
            )
        }

        // 计算可空性：只要有一个类型是可选的，结果就是可选的
        var nullable = false
        for (type in types) {
            nullable = nullable or type.isOption
        }

        // 创建成员作用域
        val newScope: MemberScope = when (val classifier: ClassifierDescriptor? = constructor.declarationDescriptor) {
            is ClassDescriptor -> {
                // 对于类描述符，使用新的类型投影创建成员作用域
                classifier.getMemberScope(newProjections)
            }

            is TypeParameterDescriptor -> {
                // 对于类型参数，使用默认类型的成员作用域
                classifier.defaultType.memberScope
            }

            else -> {
                // 其他情况创建错误作用域
                ErrorUtils.createErrorScope(ErrorScopeKind.NON_CLASSIFIER_SUPER_TYPE_SCOPE, true)
            }
        }

        // 创建新的简单类型并设置可空性
        return CangJieTypeFactory.simpleTypeWithNonTrivialMemberScope(
            TypeAttributes.Empty,
            constructor,
            newProjections,
            newScope
        ).makeOptionalAsSpecified(nullable)
    }

    /**
     * 计算类型的深度
     *
     * 类型深度定义为：1 + 类型参数的最大深度
     *
     * @param type 输入类型
     * @return 类型深度
     */
    private fun depth(type: CangJieType): Int {
        return 1 + maxDepth(type.arguments.map { projection ->
            projection.type
        })
    }

    /**
     * 计算超类型投影（针对单个类型参数）
     *
     * 在仓颉语言中，所有类型参数都是不变的（invariant），
     * 因此需要找到所有投影类型的公共超类型。
     *
     * @param parameterDescriptor 类型参数描述符
     * @param typeProjections 该参数位置的所有类型投影
     * @param recursionDepth 当前递归深度
     * @param maxDepth 最大递归深度
     * @return 计算得到的类型投影
     */
    private fun computeSupertypeProjection(
        parameterDescriptor: TypeParameterDescriptor,
        typeProjections: Set<TypeArgument>,
        recursionDepth: Int, maxDepth: Int
    ): TypeArgument {
        // 如果所有投影都相同，直接返回该投影
        val singleBestProjection = typeProjections.singleBestRepresentative()
        if (singleBestProjection != null) {
            return singleBestProjection
        }

        // 仓颉语言所有类型参数都是不变的(invariant)
        // 因此需要计算所有投影类型的公共超类型
        val outs: MutableSet<CangJieType> = LinkedHashSet<CangJieType>()

        for (projection in typeProjections) {
            outs.add(projection.type)
        }

        // 递归计算公共超类型
        val superType: CangJieType = findCommonSupertype(outs, recursionDepth + 1, maxDepth)

        return TypeArgumentImpl(superType)
    }

    /**
     * 计算公共超类型（公开接口）
     *
     * 这是外部调用的主要方法。
     *
     * @param types 需要计算公共超类型的类型集合
     * @return 公共超类型
     */
    fun commonSupertype(types: Collection<CangJieType>): CangJieType {
        // 如果只有一个类型，直接返回
        if (types.size == 1) return types.iterator().next()

        // 递归深度不应该显著超过最深类型的深度
        // 但可以稍微深一点：例如当初始类型简单，但其超类型复杂时
        return findCommonSupertype(types, 0, maxDepth(types) + 3)
    }

    /**
     * 计算类型集合的最大深度
     *
     * @param types 类型集合
     * @return 最大深度
     */
    private fun maxDepth(types: Collection<CangJieType>): Int {
        var max = 0
        for (type in types) {
            val depth: Int = depth(type)
            if (max < depth) {
                max = depth
            }
        }
        return max
    }

    /**
     * 计算不可表示类型的公共超类型
     *
     * 不可表示类型是指不能直接在源代码中写出的类型，
     * 例如交集类型（intersection type）。
     *
     * @param types 类型集合
     * @return 公共超类型，如果集合为空则返回 null
     */
    fun commonSupertypeForNonDenotableTypes(types: Collection<CangJieType>): CangJieType? {
        if (types.isEmpty()) return null

        // 如果只有一个交集类型，递归处理其组成类型
        if (types.size == 1) {
            val type: CangJieType = types.iterator().next()
            if (type.constructor is IntersectionTypeConstructor) {
                return commonSupertypeForNonDenotableTypes(type.constructor.supertypes)
            }
        }

        return commonSupertype(types)
    }

}