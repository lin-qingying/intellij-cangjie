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

package org.cangnova.cangjie.types


import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.descriptors.ClassifierDescriptor
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.resolve.scopes.TypeIntersectionScope
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner
import org.cangnova.cangjie.types.model.IntersectionTypeConstructorMarker

/**
 * 交集类型构造器
 *
 * 表示多个类型的交集（Intersection Type）。交集类型用于表示同时满足多个类型约束的类型。
 *
 * 用途：
 * - **类型推导**：在类型推断过程中合并多个类型约束
 * - **泛型上界**：表示类型参数必须同时满足多个接口或类约束
 * - **智能类型转换**：when 表达式或 if 语句中的类型细化
 * - **重载解析**：计算多个重载方法的公共类型
 *
 * 示例概念（伪代码）：
 * ```
 * // T 必须同时实现 Comparable 和 Serializable
 * func <T: Comparable & Serializable> process(value: T)
 *
 * // 智能类型转换后的交集类型
 * if (x is A && x is B) {
 *     // 此处 x 的类型为 A & B 的交集
 * }
 * ```
 *
 * @param typesToIntersect 要进行交集的类型集合
 */
class IntersectionTypeConstructor(typesToIntersect: Collection<CangJieType>) : TypeConstructor,
    IntersectionTypeConstructorMarker {

    /**
     * 备选类型
     *
     * 在某些情况下，交集类型可以有一个备选类型表示，用于优化或提供更友好的类型显示
     */
    private var alternative: CangJieType? = null

    /**
     * 私有构造函数，用于创建带备选类型的交集类型构造器
     */
    private constructor(
        typesToIntersect: Collection<CangJieType>,
        alternative: CangJieType?,
    ) : this(typesToIntersect) {
        this.alternative = alternative
    }

    init {
        assert(!typesToIntersect.isEmpty()) { "Attempt to create an empty intersection" }
    }

    /**
     * 交集中的类型集合
     *
     * 使用 LinkedHashSet 保持类型的顺序并去重
     */
    private val intersectedTypes = LinkedHashSet(typesToIntersect)

    /**
     * 预计算的哈希码
     *
     * 由于交集类型构造器是不可变的，可以缓存哈希码以提高性能
     */
    private val hashCode = intersectedTypes.hashCode()

    /**
     * 类型参数列表
     *
     * 交集类型本身不引入新的类型参数，返回空列表
     */
    override val parameters: List<TypeParameterDescriptor>
        get() = emptyList()

    /**
     * 父类型集合
     *
     * 对于交集类型，其父类型就是构成交集的所有类型
     */
    override val supertypes: Collection<CangJieType>
        get() = intersectedTypes

    /**
     * 为交集类型创建成员作用域
     *
     * 交集类型的成员作用域包含所有交集类型的成员。
     * 注意：类型不应在作用域的调试名称中渲染，这可能在复杂交集类型的情况下导致性能问题。
     *
     * @return 交集类型的成员作用域
     */
    fun createScopeForCangJieType(): MemberScope =
        TypeIntersectionScope.create("member scope for intersection type", intersectedTypes)

    /**
     * 交集类型不是 final 的
     *
     * 因为交集类型可能包含非 final 的类型
     */
    override val isFinal : Boolean = false

    /**
     * 交集类型不可表示
     *
     * 交集类型是编译器内部使用的类型，不能在源代码中直接写出
     */
    override val isDenotable : Boolean = false

    /**
     * 声明描述符
     *
     * 交集类型没有对应的声明，返回 null
     */
    override val declarationDescriptor: ClassifierDescriptor?
        get() = null

    /**
     * 内置类型集合
     *
     * 从交集中的任意一个类型获取内置类型集合
     * （所有类型应该共享同一个内置类型集合）
     */
    override val builtIns: CangJieBuiltIns
        get() = intersectedTypes.iterator().next().constructor.builtIns

    /**
     * 返回交集类型的调试字符串表示
     */
    override fun toString(): String = makeDebugNameForIntersectionType()

    /**
     * 生成交集类型的调试名称
     *
     * 将交集中的类型按字典序排序后用 " & " 连接，并用大括号包裹。
     * 例如：{Comparable & Serializable}
     *
     * @param getProperTypeRelatedToStringify 自定义类型字符串化函数（默认使用 toString）
     * @return 交集类型的调试字符串
     */
    fun makeDebugNameForIntersectionType(getProperTypeRelatedToStringify: (CangJieType) -> Any = { it.toString() }): String {
        return intersectedTypes.sortedBy { getProperTypeRelatedToStringify(it).toString() }
            .joinToString(
                separator = " & ",
                prefix = "{",
                postfix = "}"
            ) { getProperTypeRelatedToStringify(it).toString() }
    }

    /**
     * 相等性比较
     *
     * 两个交集类型构造器相等当且仅当它们包含相同的类型集合
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IntersectionTypeConstructor) return false

        return intersectedTypes == other.intersectedTypes
    }

    /**
     * 创建交集类型
     *
     * 创建一个带有非平凡成员作用域的简单类型，表示这个交集
     *
     * @return 表示此交集的简单类型
     */
    fun createType(): SimpleType =
        CangJieTypeFactory.simpleTypeWithNonTrivialMemberScope(
            TypeAttributes.Empty, this, listOf(), false, this.createScopeForCangJieType()
        ) { cangjieTypeRefiner ->
            this.refine(cangjieTypeRefiner).createType()
        }

    /**
     * 返回预计算的哈希码
     */
    override fun hashCode(): Int = hashCode

    /**
     * 精化交集类型构造器
     *
     * 对交集中的每个类型进行精化，如果有变化则创建新的交集类型构造器
     *
     * @param cangjieTypeRefiner 类型精化器
     * @return 精化后的交集类型构造器，如果没有变化则返回 this
     */
    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner) =
        transformComponents { it.refine(cangjieTypeRefiner) } ?: this

    /**
     * 设置备选类型
     *
     * 创建一个新的交集类型构造器，带有指定的备选类型
     *
     * @param alternative 备选类型
     * @return 带有备选类型的新交集类型构造器
     */
    fun setAlternative(alternative: CangJieType?): IntersectionTypeConstructor {
        return IntersectionTypeConstructor(intersectedTypes, alternative)
    }

    /**
     * 获取备选类型
     *
     * @return 备选类型，如果没有则返回 null
     */
    fun getAlternativeType(): CangJieType? = alternative
}

/**
 * 转换交集类型构造器的组件
 *
 * 对交集中满足谓词条件的类型应用转换函数，如果有类型被转换则创建新的交集类型构造器。
 *
 * 使用场景：
 * - 类型精化：将交集中的所有类型精化
 * - 可选性传播：将交集中的类型都标记为可选或非可选
 * - 类型替换：替换交集中的特定类型
 *
 * @param predicate 判断类型是否需要转换的谓词（默认为所有类型）
 * @param transform 类型转换函数
 * @return 如果有变化则返回新的交集类型构造器，否则返回 null
 */
inline fun IntersectionTypeConstructor.transformComponents(
    predicate: (CangJieType) -> Boolean = { true },
    transform: (CangJieType) -> CangJieType
): IntersectionTypeConstructor? {
    var changed = false
    // 转换父类型（交集中的类型）
    val newSupertypes = supertypes.map {
        if (predicate(it)) {
            changed = true
            transform(it)
        } else {
            it
        }
    }

    // 如果没有变化，返回 null 表示不需要创建新的构造器
    if (!changed) return null

    // 如果有备选类型，也对其进行转换
    val updatedAlternative = getAlternativeType()?.let { alternative ->
        if (predicate(alternative)) transform(alternative) else alternative
    }

    return IntersectionTypeConstructor(newSupertypes).setAlternative(updatedAlternative)
}
