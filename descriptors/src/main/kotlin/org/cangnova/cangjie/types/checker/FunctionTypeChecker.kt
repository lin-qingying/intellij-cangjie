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

import org.cangnova.cangjie.types.FunctionType

/**
 * 函数类型子类型检查器
 *
 * 实现函数类型的子类型规则：
 * - 参数类型：逆变（contravariant）
 * - 返回类型：协变（covariant）
 *
 * 即 (A1, A2, ...) -> R1 <: (B1, B2, ...) -> R2 当且仅当：
 * - B1 <: A1, B2 <: A2, ... （参数逆变）
 * - R1 <: R2 （返回值协变）
 *
 * 参考: TypeManager.cpp:870-900 IsFuncSubtype
 *
 * 示例：
 * - (Animal) -> Dog <: (Dog) -> Animal
 *   因为 Dog <: Animal (参数逆变) 且 Dog <: Animal (返回值协变)
 *
 * - (Dog) -> Animal 不是 (Animal) -> Dog 的子类型
 *   因为 Animal 不是 Dog 的子类型 (参数逆变不满足)
 */
internal object FunctionTypeChecker {

    /**
     * 检查函数类型的子类型关系
     *
     * @param subType 子类型（函数类型）
     * @param superType 父类型（函数类型）
     * @return 如果 subType 是 superType 的子类型返回 true
     */
    fun isSubtype(subType: FunctionType, superType: FunctionType): Boolean {
        // 1. Option 状态检查
        if (!checkOptionCompatibility(subType, superType)) {
            return false
        }

        // 2. 参数数量必须相同
        if (subType.parameterTypes.size != superType.parameterTypes.size) {
            return false
        }

        // 3. 参数类型检查（逆变：父类型参数 <: 子类型参数）
        for (i in subType.parameterTypes.indices) {
            val subParam = subType.parameterTypes[i]
            val superParam = superType.parameterTypes[i]

            // 注意：这里是逆变，所以检查方向相反
            // superType 的参数必须是 subType 参数的子类型
            if (!CangJieSubtypeChecker.isSubtypeOf(
                    superParam,
                    subParam,
                    implicitBoxed = false  // 函数参数不允许隐式装箱
                )
            ) {
                return false
            }
        }

        // 4. 返回类型检查（协变：子类型返回 <: 父类型返回）
        return CangJieSubtypeChecker.isSubtypeOf(
            subType.returnType,
            superType.returnType,
            implicitBoxed = false  // 函数返回值不允许隐式装箱
        )
    }

    /**
     * 检查函数类型的 Option 兼容性
     */
    private fun checkOptionCompatibility(subType: FunctionType, superType: FunctionType): Boolean {
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
     * 检查两个函数类型是否完全相等
     *
     * @param a 第一个函数类型
     * @param b 第二个函数类型
     * @return 两个函数类型相等时返回 true
     */
    fun areEqual(a: FunctionType, b: FunctionType): Boolean {
        // Option 状态必须相同
        if (a.isOption != b.isOption) {
            return false
        }

        // 参数数量必须相同
        if (a.parameterTypes.size != b.parameterTypes.size) {
            return false
        }

        // 所有参数类型必须相等
        for (i in a.parameterTypes.indices) {
            if (!CangJieTypeEquality.areEqual(a.parameterTypes[i], b.parameterTypes[i])) {
                return false
            }
        }

        // 返回类型必须相等
        return CangJieTypeEquality.areEqual(a.returnType, b.returnType)
    }

    /**
     * 检查函数类型是否互为子类型（类型等价）
     *
     * 两个函数类型互为子类型表示它们在类型系统中可以互换使用
     *
     * @param a 第一个函数类型
     * @param b 第二个函数类型
     * @return 两个函数类型互为子类型时返回 true
     */
    fun areMutualSubtypes(a: FunctionType, b: FunctionType): Boolean {
        return isSubtype(a, b) && isSubtype(b, a)
    }

    /**
     * 获取两个函数类型的公共父类型
     *
     * 如果存在公共父类型，返回该类型；否则返回 null
     *
     * @param a 第一个函数类型
     * @param b 第二个函数类型
     * @return 公共父类型，如果不存在则返回 null
     */
    fun findCommonSupertype(a: FunctionType, b: FunctionType): FunctionType? {
        // 参数数量必须相同
        if (a.parameterTypes.size != b.parameterTypes.size) {
            return null
        }

        // 公共父类型的参数是两个类型参数的公共子类型（逆变）
        // 公共父类型的返回值是两个类型返回值的公共父类型（协变）
        // 这需要更复杂的类型推导，暂时返回 null
        return null
    }

    /**
     * 检查函数类型是否可以调用
     *
     * 验证实际参数类型是否匹配函数的形式参数类型
     *
     * @param functionType 函数类型
     * @param argumentTypes 实际参数类型列表
     * @return 如果参数类型匹配返回 true
     */
    fun canInvokeWith(
        functionType: FunctionType,
        argumentTypes: List<org.cangnova.cangjie.types.CangJieType>
    ): Boolean {
        // 参数数量检查
        if (functionType.parameterTypes.size != argumentTypes.size) {
            return false
        }

        // 每个参数类型必须是对应形参类型的子类型
        for (i in argumentTypes.indices) {
            if (!CangJieSubtypeChecker.isSubtypeOf(
                    argumentTypes[i],
                    functionType.parameterTypes[i],
                    implicitBoxed = true  // 调用时允许隐式装箱
                )
            ) {
                return false
            }
        }

        return true
    }
}
