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
import org.cangnova.cangjie.types.checker.SimpleClassicTypeSystemContext.isClassTypeConstructor
import org.cangnova.cangjie.types.checker.SimpleClassicTypeSystemContext.isInterface

/**
 * 类和接口类型子类型检查器
 *
 * 处理类和接口类型的子类型关系：
 * - 类继承关系：class Dog : Animal → Dog <: Animal
 * - 接口实现关系：class Dog : Runnable → Dog <: Runnable
 * - 多接口实现：class Dog : Animal & Runnable & Comparable
 *
 * 参考: TypeManager.cpp IsSubtype 中的类类型处理
 */
internal object ClassTypeChecker {

    /**
     * 检查类/接口类型的子类型关系
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

        // 2. 同一类型构造器
        if (CangJieSubtypeChecker.areEqualTypeConstructors(subConstructor, superConstructor)) {
            // 对于非泛型类型，直接返回 true
            if (subType.arguments.isEmpty() && superType.arguments.isEmpty()) {
                return true
            }
            // 对于泛型类型，检查类型参数（不变）
            return checkTypeArgumentsEqual(subType.arguments, superType.arguments)
        }

        // 3. 检查继承关系
        return checkInheritanceChain(subType, superType, implicitBoxed)
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
     * 检查类型参数是否相等（不变）
     */
    private fun checkTypeArgumentsEqual(
        subArgs: List<TypeArgument>,
        superArgs: List<TypeArgument>
    ): Boolean {
        if (subArgs.size != superArgs.size) {
            return false
        }

        for (i in subArgs.indices) {
            if (!CangJieTypeEquality.areEqual(subArgs[i].type, superArgs[i].type)) {
                return false
            }
        }

        return true
    }

    /**
     * 检查继承链是否满足子类型关系
     *
     * 遍历子类型的所有父类型（包括类和接口），
     * 检查是否有类型与目标父类型匹配
     */
    private fun checkInheritanceChain(
        subType: SimpleType,
        superType: SimpleType,
        implicitBoxed: Boolean
    ): Boolean {
        val superConstructor = superType.constructor
        val visited = mutableSetOf<TypeConstructor>()

        return checkInheritanceRecursive(subType.constructor, superConstructor, superType.arguments, visited)
    }

    /**
     * 递归检查继承链
     */
    private fun checkInheritanceRecursive(
        currentConstructor: TypeConstructor,
        targetConstructor: TypeConstructor,
        targetArgs: List<TypeArgument>,
        visited: MutableSet<TypeConstructor>
    ): Boolean {
        // 防止循环引用
        if (currentConstructor in visited) {
            return false
        }
        visited.add(currentConstructor)

        // 获取当前类型的所有父类型
        val supertypes = currentConstructor.supertypes

        for (parentType in supertypes) {
            val parentSimple = parentType.unwrap() as? SimpleType ?: continue
            val parentConstructor = parentSimple.constructor

            // 检查是否匹配目标类型
            if (CangJieSubtypeChecker.areEqualTypeConstructors(parentConstructor, targetConstructor)) {
                // 找到匹配的类型构造器，检查类型参数
                if (targetArgs.isEmpty() && parentSimple.arguments.isEmpty()) {
                    return true
                }
                if (checkTypeArgumentsEqual(parentSimple.arguments, targetArgs)) {
                    return true
                }
            }

            // 递归检查父类型的继承链
            if (checkInheritanceRecursive(parentConstructor, targetConstructor, targetArgs, visited)) {
                return true
            }
        }

        return false
    }

    /**
     * 检查类型是否是类类型
     *
     * @param type 待检查的类型
     * @return 如果是类类型返回 true
     */
    fun isClassType(type: SimpleType): Boolean {
        return type.constructor.isClassTypeConstructor()
    }

    /**
     * 检查类型是否是接口类型
     *
     * @param type 待检查的类型
     * @return 如果是接口类型返回 true
     */
    fun isInterfaceType(type: SimpleType): Boolean {
        return type.constructor.isInterface()
    }

    /**
     * 检查类型是否是类或接口类型
     *
     * @param type 待检查的类型
     * @return 如果是类或接口类型返回 true
     */
    fun isClassLikeType(type: SimpleType): Boolean {
        val constructor = type.constructor
        return constructor.isClassTypeConstructor() || constructor.isInterface()
    }

    /**
     * 获取类型的直接父类型列表
     *
     * @param type 待查询的类型
     * @return 直接父类型列表
     */
    fun getDirectSupertypes(type: SimpleType): List<CangJieType> {
        return type.constructor.supertypes.toList()
    }

    /**
     * 获取类型的所有父类型（包括间接父类型）
     *
     * @param type 待查询的类型
     * @return 所有父类型列表
     */
    fun getAllSupertypes(type: SimpleType): Set<CangJieType> {
        val result = mutableSetOf<CangJieType>()
        val visited = mutableSetOf<TypeConstructor>()

        collectAllSupertypes(type.constructor, result, visited)

        return result
    }

    /**
     * 递归收集所有父类型
     */
    private fun collectAllSupertypes(
        constructor: TypeConstructor,
        result: MutableSet<CangJieType>,
        visited: MutableSet<TypeConstructor>
    ) {
        if (constructor in visited) return
        visited.add(constructor)

        for (supertype in constructor.supertypes) {
            result.add(supertype)
            val superSimple = supertype.unwrap() as? SimpleType ?: continue
            collectAllSupertypes(superSimple.constructor, result, visited)
        }
    }

    /**
     * 查找两个类型的最近公共父类型
     *
     * @param a 第一个类型
     * @param b 第二个类型
     * @return 最近公共父类型，如果没有则返回 Any
     */
    fun findCommonSupertype(a: SimpleType, b: SimpleType): CangJieType? {
        // 获取 a 的所有父类型
        val aSupertypes = getAllSupertypes(a)
        val aConstructors = aSupertypes.mapNotNull {
            (it.unwrap() as? SimpleType)?.constructor
        }.toSet()

        // 在 b 的继承链中查找第一个与 a 的父类型匹配的类型
        val bSupertypes = getDirectSupertypes(b)
        val queue = ArrayDeque(bSupertypes)
        val visited = mutableSetOf<TypeConstructor>()

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val currentSimple = current.unwrap() as? SimpleType ?: continue
            val currentConstructor = currentSimple.constructor

            if (currentConstructor in visited) continue
            visited.add(currentConstructor)

            if (currentConstructor in aConstructors) {
                return current
            }

            queue.addAll(getDirectSupertypes(currentSimple))
        }

        return null
    }

    /**
     * 检查类型是否实现了指定接口
     *
     * @param type 待检查的类型
     * @param interfaceType 接口类型
     * @return 如果类型实现了该接口返回 true
     */
    fun implementsInterface(type: SimpleType, interfaceType: SimpleType): Boolean {
        if (!isInterfaceType(interfaceType)) {
            return false
        }

        return isSubtype(type, interfaceType, implicitBoxed = false)
    }

    /**
     * 获取类型实现的所有接口
     *
     * @param type 待查询的类型
     * @return 实现的所有接口类型列表
     */
    fun getAllImplementedInterfaces(type: SimpleType): List<SimpleType> {
        return getAllSupertypes(type)
            .mapNotNull { it.unwrap() as? SimpleType }
            .filter { isInterfaceType(it) }
    }
}
