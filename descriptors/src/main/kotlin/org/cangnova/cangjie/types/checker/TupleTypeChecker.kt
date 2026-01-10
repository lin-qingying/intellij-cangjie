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

import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.TupleType

/**
 * 元组类型子类型检查器
 *
 * 实现元组类型的子类型规则：
 * - 元素类型：协变（covariant）
 *
 * 即 (T1, T2, ...) <: (U1, U2, ...) 当且仅当：
 * - T1 <: U1, T2 <: U2, ... （所有元素协变）
 *
 * 参考: TypeManager.cpp:902-913 IsTupleSubtype
 *
 * 示例：
 * - (Dog, Cat) <: (Animal, Animal)
 *   因为 Dog <: Animal 且 Cat <: Animal
 *
 * - (Animal, Animal) 不是 (Dog, Cat) 的子类型
 *   因为 Animal 不是 Dog 的子类型
 */
internal object TupleTypeChecker {

    /**
     * 检查元组类型的子类型关系
     *
     * @param subType 子类型（元组类型）
     * @param superType 父类型（元组类型）
     * @return 如果 subType 是 superType 的子类型返回 true
     */
    fun isSubtype(subType: TupleType, superType: TupleType): Boolean {
        // 1. Option 状态检查
        if (!checkOptionCompatibility(subType, superType)) {
            return false
        }

        // 2. 元素数量必须相同
        if (subType.elementTypes.size != superType.elementTypes.size) {
            return false
        }

        // 3. 每个元素类型协变检查
        for (i in subType.elementTypes.indices) {
            val subElement = subType.elementTypes[i]
            val superElement = superType.elementTypes[i]

            // 元素类型协变：子类型元素 <: 父类型元素
            if (!CangJieSubtypeChecker.isSubtypeOf(
                    subElement,
                    superElement,
                    implicitBoxed = false  // 元组元素不允许隐式装箱
                )
            ) {
                return false
            }
        }

        return true
    }

    /**
     * 检查元组类型的 Option 兼容性
     */
    private fun checkOptionCompatibility(subType: TupleType, superType: TupleType): Boolean {
        // 相同的 Option 状态
        if (subType.isOption == superType.isOption) {
            return true
        }

        // 非 Option 可以赋值给 Option
        if (!subType.isOption && superType.isOption) {
            return true
        }

        // Option 不能赋值给非 Option
        return false
    }

    /**
     * 检查两个元组类型是否完全相等
     *
     * @param a 第一个元组类型
     * @param b 第二个元组类型
     * @return 两个元组类型相等时返回 true
     */
    fun areEqual(a: TupleType, b: TupleType): Boolean {
        // Option 状态必须相同
        if (a.isOption != b.isOption) {
            return false
        }

        // 元素数量必须相同
        if (a.elementTypes.size != b.elementTypes.size) {
            return false
        }

        // 所有元素类型必须相等
        for (i in a.elementTypes.indices) {
            if (!CangJieTypeEquality.areEqual(a.elementTypes[i], b.elementTypes[i])) {
                return false
            }
        }

        return true
    }

    /**
     * 检查元组类型是否互为子类型（类型等价）
     *
     * @param a 第一个元组类型
     * @param b 第二个元组类型
     * @return 两个元组类型互为子类型时返回 true
     */
    fun areMutualSubtypes(a: TupleType, b: TupleType): Boolean {
        return isSubtype(a, b) && isSubtype(b, a)
    }

    /**
     * 获取元组类型的元素类型
     *
     * @param tupleType 元组类型
     * @param index 元素索引（从0开始）
     * @return 指定位置的元素类型，如果索引无效返回 null
     */
    fun getElementType(tupleType: TupleType, index: Int): CangJieType? {
        return tupleType.elementTypes.getOrNull(index)
    }

    /**
     * 获取元组类型的元素数量
     *
     * @param tupleType 元组类型
     * @return 元素数量
     */
    fun getArity(tupleType: TupleType): Int {
        return tupleType.elementTypes.size
    }

    /**
     * 检查元组是否为空元组
     *
     * 空元组 () 相当于 Unit 类型
     *
     * @param tupleType 元组类型
     * @return 如果是空元组返回 true
     */
    fun isEmpty(tupleType: TupleType): Boolean {
        return tupleType.elementTypes.isEmpty()
    }

    /**
     * 检查元组是否为单元素元组
     *
     * @param tupleType 元组类型
     * @return 如果是单元素元组返回 true
     */
    fun isSingleton(tupleType: TupleType): Boolean {
        return tupleType.elementTypes.size == 1
    }

    /**
     * 检查实际参数类型是否可以匹配元组类型
     *
     * 用于解构赋值等场景
     *
     * @param tupleType 元组类型
     * @param targetTypes 目标类型列表
     * @return 如果可以匹配返回 true
     */
    fun canDestructureTo(tupleType: TupleType, targetTypes: List<CangJieType>): Boolean {
        // 元素数量检查
        if (tupleType.elementTypes.size != targetTypes.size) {
            return false
        }

        // 每个元素必须是对应目标类型的子类型
        for (i in targetTypes.indices) {
            if (!CangJieSubtypeChecker.isSubtypeOf(
                    tupleType.elementTypes[i],
                    targetTypes[i],
                    implicitBoxed = true
                )
            ) {
                return false
            }
        }

        return true
    }

    /**
     * 查找两个元组类型的公共父类型
     *
     * @param a 第一个元组类型
     * @param b 第二个元组类型
     * @return 公共父类型，如果不存在则返回 null
     */
    fun findCommonSupertype(a: TupleType, b: TupleType): TupleType? {
        // 元素数量必须相同
        if (a.elementTypes.size != b.elementTypes.size) {
            return null
        }

        // 需要更复杂的类型推导来计算每个元素的公共父类型
        // 暂时返回 null
        return null
    }
}
