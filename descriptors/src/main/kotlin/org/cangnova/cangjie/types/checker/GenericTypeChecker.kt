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

package org.cangnova.cangjie.types.checker

import org.cangnova.cangjie.types.*

/**
 * 泛型类型子类型检查器
 *
 * 仓颉语言中用户自定义泛型类型全部不变（invariant）
 * 即 G<A> <: G<B> 当且仅当 A = B
 *
 * 参考: 仓颉文档 generic_subtype.md
 *
 * 核心规则：
 * - 泛型类型参数严格不变
 * - List<Dog> 不是 List<Animal> 的子类型（即使 Dog <: Animal）
 * - 继承关系仍然适用：如果 class ArrayList<T> : List<T>，
 *   则 ArrayList<Int> <: List<Int>
 *
 * 示例：
 * - List<Int> 是 List<Int> 的子类型（同一类型）
 * - List<Int> 不是 List<Number> 的子类型（不变）
 * - ArrayList<Int> 是 List<Int> 的子类型（继承关系）
 */
internal object GenericTypeChecker {

    /**
     * 检查泛型类型的子类型关系
     *
     * @param subType 子类型
     * @param superType 父类型
     * @param implicitBoxed 是否允许隐式装箱
     * @return 如果 subType 是 superType 的子类型返回 true
     */
    fun isSubtype(subType: SimpleType, superType: SimpleType, implicitBoxed: Boolean): Boolean {
        // 1. Option 状态检查
        if (!checkOptionCompatibility(subType, superType)) {
            return false
        }

        val subConstructor = subType.constructor
        val superConstructor = superType.constructor

        // 2. 同一类型构造器：检查类型参数是否相等（不变）
        if (CangJieSubtypeChecker.areEqualTypeConstructors(subConstructor, superConstructor)) {
            return checkTypeArgumentsEqual(subType.arguments, superType.arguments)
        }

        // 3. 不同类型构造器：检查是否通过继承关系满足
        return checkInheritanceSubtype(subType, superType, implicitBoxed)
    }

    /**
     * 检查 Option 兼容性
     */
    private fun checkOptionCompatibility(subType: SimpleType, superType: SimpleType): Boolean {
        if (subType.isOption == superType.isOption) {
            return true
        }

        // 非 Option 可以赋值给 Option
        if (!subType.isOption && superType.isOption) {
            return true
        }

        return false
    }

    /**
     * 检查类型参数是否完全相等（不变检查）
     *
     * 仓颉语言中泛型参数是不变的，所以需要完全相等
     */
    private fun checkTypeArgumentsEqual(
        subArgs: List<TypeArgument>,
        superArgs: List<TypeArgument>
    ): Boolean {
        // 参数数量必须相同
        if (subArgs.size != superArgs.size) {
            return false
        }

        // 所有类型参数必须相等（不变）
        for (i in subArgs.indices) {
            val subArg = subArgs[i].type
            val superArg = superArgs[i].type

            if (!CangJieTypeEquality.areEqual(subArg, superArg)) {
                return false
            }
        }

        return true
    }

    /**
     * 检查是否通过继承关系满足子类型关系
     *
     * 示例：如果 ArrayList<T> 继承 List<T>，
     * 则 ArrayList<Int> 是 List<Int> 的子类型
     */
    internal fun checkInheritanceSubtype(
        subType: SimpleType,
        superType: SimpleType,
        implicitBoxed: Boolean
    ): Boolean {
        val superConstructor = superType.constructor

        // 获取子类型的所有父类型
        val supertypes = subType.constructor.supertypes

        // 在父类型中查找与目标类型构造器匹配的类型
        for (parentType in supertypes) {
            val parentSimple = parentType.unwrap() as? SimpleType ?: continue

            if (CangJieSubtypeChecker.areEqualTypeConstructors(parentSimple.constructor, superConstructor)) {
                // 找到匹配的父类型构造器，检查类型参数
                if (checkTypeArgumentsEqual(parentSimple.arguments, superType.arguments)) {
                    return true
                }
            }

            // 递归检查父类型的继承链
            if (checkInheritanceSubtype(parentSimple, superType, implicitBoxed)) {
                return true
            }
        }

        return false
    }

    /**
     * 检查两个泛型类型是否相等
     *
     * @param a 第一个类型
     * @param b 第二个类型
     * @return 两个类型相等时返回 true
     */
    fun areEqual(a: SimpleType, b: SimpleType): Boolean {
        // Option 状态必须相同
        if (a.isOption != b.isOption) {
            return false
        }

        // 类型构造器必须相同
        if (!CangJieSubtypeChecker.areEqualTypeConstructors(a.constructor, b.constructor)) {
            return false
        }

        // 类型参数必须相等
        return checkTypeArgumentsEqual(a.arguments, b.arguments)
    }

    /**
     * 检查类型是否是泛型类型（有类型参数）
     *
     * @param type 待检查的类型
     * @return 如果是泛型类型返回 true
     */
    fun isGenericType(type: SimpleType): Boolean {
        return type.arguments.isNotEmpty() && type !is FunctionType && type !is TupleType
    }

    /**
     * 获取泛型类型的类型参数数量
     *
     * @param type 泛型类型
     * @return 类型参数数量
     */
    fun getTypeParameterCount(type: SimpleType): Int {
        return type.arguments.size
    }

    /**
     * 获取泛型类型的指定位置类型参数
     *
     * @param type 泛型类型
     * @param index 参数索引（从0开始）
     * @return 类型参数，如果索引无效返回 null
     */
    fun getTypeArgument(type: SimpleType, index: Int): CangJieType? {
        return type.arguments.getOrNull(index)?.type
    }

    /**
     * 检查泛型类型是否有类型参数的上界约束
     *
     * @param type 泛型类型
     * @return 如果有上界约束返回 true
     */
    fun hasUpperBoundConstraints(type: SimpleType): Boolean {
        val constructor = type.constructor
        val parameters = constructor.parameters

        return parameters.any { param ->
            param.upperBounds.isNotEmpty() &&
                    !param.upperBounds.all { SpecialTypeChecker.isAny(it) }
        }
    }

    /**
     * 获取类型参数的上界列表
     *
     * @param type 泛型类型
     * @param paramIndex 参数索引
     * @return 上界类型列表
     */
    fun getUpperBounds(type: SimpleType, paramIndex: Int): List<CangJieType> {
        val constructor = type.constructor
        val parameters = constructor.parameters

        return parameters.getOrNull(paramIndex)?.upperBounds ?: emptyList()
    }

    /**
     * 检查类型参数是否满足其上界约束
     *
     * @param typeArg 实际类型参数
     * @param bounds 上界约束列表
     * @return 如果满足所有约束返回 true
     */
    fun satisfiesBounds(typeArg: CangJieType, bounds: List<CangJieType>): Boolean {
        return bounds.all { bound ->
            CangJieSubtypeChecker.isSubtypeOf(typeArg, bound)
        }
    }

    /**
     * 创建泛型类型的替换映射
     *
     * 将类型参数映射到实际类型参数
     *
     * @param type 泛型类型
     * @return 类型参数名到实际类型的映射
     */
    fun createSubstitutionMap(type: SimpleType): Map<String, CangJieType> {
        val constructor = type.constructor
        val parameters = constructor.parameters
        val arguments = type.arguments

        if (parameters.size != arguments.size) {
            return emptyMap()
        }

        return parameters.indices.associate { i ->
            val paramName = parameters[i].name.toString()
            val argType = arguments[i].type
            paramName to argType
        }
    }
}
