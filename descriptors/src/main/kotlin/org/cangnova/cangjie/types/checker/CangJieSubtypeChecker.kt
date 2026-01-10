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
import org.cangnova.cangjie.types.error.ErrorType

/**
 * 仓颉类型子类型检查器
 *
 * 基于仓颉编译器 TypeManager.cpp 的 IsSubtype 实现
 * 参考: external/cangjie_compiler/src/Sema/TypeManager.cpp:996-1069
 *
 * 核心规则：
 * - Nothing <: T <: Any (对于所有类型 T)
 * - 用户自定义泛型类型全部不变（invariant）
 * - 函数类型：参数逆变，返回值协变
 * - 元组类型：元素协变
 */
object CangJieSubtypeChecker {

    /**
     * 子类型检查缓存，用于避免循环引用导致的无限递归
     * 使用 ThreadLocal 确保线程安全
     */
    private val subtypeCache = ThreadLocal.withInitial { mutableSetOf<Pair<TypeConstructor, TypeConstructor>>() }

    /**
     * 检查 subType 是否是 superType 的子类型
     *
     * @param subType 待检查的子类型
     * @param superType 待检查的父类型
     * @param implicitBoxed 是否允许隐式装箱（默认true，基本类型到Any的转换）
     * @param allowOptionBox 是否允许Option自动装箱（默认false）
     * @param typeConstructorEquality 自定义的类型构造器相等性判断（可选）
     * @return 如果 subType 是 superType 的子类型则返回 true
     */
    fun isSubtypeOf(
        subType: CangJieType,
        superType: CangJieType,
        implicitBoxed: Boolean = true,
        allowOptionBox: Boolean = false,
        typeConstructorEquality: CangJieTypeChecker.TypeConstructorEquality? = null
    ): Boolean {
        // 解包类型
        val unwrappedSub = subType.unwrap()
        val unwrappedSuper = superType.unwrap()

        return isSubtypeOfUnwrapped(unwrappedSub, unwrappedSuper, implicitBoxed, allowOptionBox, typeConstructorEquality)
    }

    /**
     * 检查未包装类型的子类型关系
     */
    private fun isSubtypeOfUnwrapped(
        subType: UnwrappedType,
        superType: UnwrappedType,
        implicitBoxed: Boolean,
        allowOptionBox: Boolean,
        typeConstructorEquality: CangJieTypeChecker.TypeConstructorEquality? = null
    ): Boolean {
        // 1. 错误类型检查 - 错误类型不参与子类型检查
        if (subType is ErrorType || superType is ErrorType) {
            return false
        }

        // 2. 同一对象快速路径
        if (subType === superType) {
            return true
        }

        // 3. FlexibleType 处理
        if (subType is FlexibleType) {
            // 灵活类型：下界必须是父类型的子类型
            return isSubtypeOfUnwrapped(subType.lowerBound, superType, implicitBoxed, allowOptionBox, typeConstructorEquality)
        }
        if (superType is FlexibleType) {
            // 灵活类型：子类型必须是上界的子类型
            return isSubtypeOfUnwrapped(subType, superType.upperBound, implicitBoxed, allowOptionBox, typeConstructorEquality)
        }

        // 4. 此时都应该是 SimpleType
        if (subType !is SimpleType || superType !is SimpleType) {
            return false
        }

        return isSubtypeOfSimple(subType, superType, implicitBoxed, allowOptionBox, typeConstructorEquality)
    }

    /**
     * 检查简单类型的子类型关系
     *
     * 参考: TypeManager.cpp:996-1069 IsSubtype
     */
    private fun isSubtypeOfSimple(
        subType: SimpleType,
        superType: SimpleType,
        implicitBoxed: Boolean,
        allowOptionBox: Boolean,
        typeConstructorEquality: CangJieTypeChecker.TypeConstructorEquality? = null
    ): Boolean {
        val subConstructor = subType.constructor
        val superConstructor = superType.constructor

        // 1. Nothing 是所有类型的子类型
        if (SpecialTypeChecker.isNothing(subType)) {
            return true
        }

        // 2. 任何非 Nothing 类型都不是 Nothing 的子类型
        if (SpecialTypeChecker.isNothing(superType)) {
            return false
        }

        // 3. 所有类型都是 Any 的子类型（考虑 implicitBoxed）
        if (SpecialTypeChecker.isAny(superType)) {
            // 如果允许隐式装箱，或者子类型是类/接口类型，则返回 true
            return implicitBoxed || isClassLikeType(subType)
        }

        // 4. Option 类型检查
        if (subType.isOption != superType.isOption) {
            // Option 兼容性检查
            if (superType.isOption && allowOptionBox) {
                // T <: ?T (当允许 Option 装箱时)
                val unwrappedSuper = superType.makeOptionAsSpecified(false)
                return isSubtypeOfSimple(subType, unwrappedSuper, implicitBoxed, allowOptionBox, typeConstructorEquality)
            }
            return false
        }

        // 5. 同一类型构造器快速路径
        if (areEqualTypeConstructors(subConstructor, superConstructor, typeConstructorEquality)) {
            // 检查类型参数
            return checkTypeArguments(subType, superType, implicitBoxed, typeConstructorEquality)
        }

        // 6. 使用缓存检查循环引用
        val cacheKey = Pair(subConstructor, superConstructor)
        val cache = subtypeCache.get()
        if (cache.contains(cacheKey)) {
            return false
        }

        try {
            cache.add(cacheKey)

            // 7. 分类型检查
            return when {
                // 函数类型检查
                isFunctionType(subType) && isFunctionType(superType) -> {
                    FunctionTypeChecker.isSubtype(
                        subType as FunctionType,
                        superType as FunctionType
                    )
                }

                // 元组类型检查
                isTupleType(subType) && isTupleType(superType) -> {
                    TupleTypeChecker.isSubtype(
                        subType as TupleType,
                        superType as TupleType
                    )
                }

                // 泛型类型检查（通过继承关系）
                isGenericType(subType) || isGenericType(superType) -> {
                    GenericTypeChecker.isSubtype(subType, superType, implicitBoxed)
                }

                // 类/接口类型检查（继承关系）
                isClassLikeType(subType) -> {
                    ClassTypeChecker.isSubtype(subType, superType, implicitBoxed)
                }

                // 基本类型检查
                isPrimitiveType(subType) && isPrimitiveType(superType) -> {
                    PrimitiveTypeChecker.isSubtype(subType, superType)
                }

                else -> false
            }
        } finally {
            cache.remove(cacheKey)
        }
    }

    /**
     * 检查类型参数是否满足子类型关系
     *
     * 仓颉语言中用户自定义泛型全部不变（invariant）
     * 即 G<A> <: G<B> 当且仅当 A = B
     */
    private fun checkTypeArguments(
        subType: SimpleType,
        superType: SimpleType,
        implicitBoxed: Boolean,
        typeConstructorEquality: CangJieTypeChecker.TypeConstructorEquality? = null
    ): Boolean {
        val subArgs = subType.arguments
        val superArgs = superType.arguments

        // 参数数量必须相同
        if (subArgs.size != superArgs.size) {
            return false
        }

        // 所有类型参数必须相等（不变）
        for (i in subArgs.indices) {
            val subArg = subArgs[i].type
            val superArg = superArgs[i].type

            if (!CangJieTypeEquality.areEqual(subArg, superArg, typeConstructorEquality)) {
                return false
            }
        }

        return true
    }

    /**
     * 检查两个类型构造器是否相等
     */
    internal fun areEqualTypeConstructors(
        a: TypeConstructor,
        b: TypeConstructor,
        typeConstructorEquality: CangJieTypeChecker.TypeConstructorEquality? = null
    ): Boolean {
        if (a === b) return true
        if (a == b) return true

        // 如果提供了自定义相等性判断，使用它
        if (typeConstructorEquality != null && typeConstructorEquality.equals(a, b)) {
            return true
        }

        // 比较声明描述符
        val aDecl = a.declarationDescriptor
        val bDecl = b.declarationDescriptor
        if (aDecl != null && bDecl != null) {
            return aDecl == bDecl
        }

        return false
    }

    /**
     * 判断类型是否为函数类型
     */
    private fun isFunctionType(type: SimpleType): Boolean {
        return type is FunctionType
    }

    /**
     * 判断类型是否为元组类型
     */
    private fun isTupleType(type: SimpleType): Boolean {
        return type is TupleType
    }

    /**
     * 判断类型是否为泛型类型（有类型参数）
     */
    private fun isGenericType(type: SimpleType): Boolean {
        return type.arguments.isNotEmpty() && type !is FunctionType && type !is TupleType
    }

    /**
     * 判断类型是否为类/接口类型
     */
    private fun isClassLikeType(type: SimpleType): Boolean {
        val constructor = type.constructor
        return constructor.isClassTypeConstructor() || constructor.isInterface()
    }

    /**
     * 判断类型是否为基本类型
     */
    private fun isPrimitiveType(type: SimpleType): Boolean {
        return type is BasicType && type.arguments.isEmpty()
    }

    /**
     * 清除缓存（测试用）
     */
    internal fun clearCache() {
        subtypeCache.get().clear()
    }

    /**
     * 查找对应的超类型
     *
     * 在子类型的类型层次结构中查找与给定超类型构造器匹配的类型。
     * 这是一个简化版本，基于递归查找，不需要类型替换。
     *
     * 示例：
     * - subtype = ArrayList<String>, supertype = List<?>
     * - 返回：List<String>
     *
     * @param subtype 子类型
     * @param supertype 要查找的超类型
     * @param typeConstructorEquality 自定义的类型构造器相等性判断（可选）
     * @return 对应的超类型，如果不存在则返回 null
     */
    fun findCorrespondingSupertype(
        subtype: CangJieType,
        supertype: CangJieType,
        typeConstructorEquality: CangJieTypeChecker.TypeConstructorEquality? = null
    ): CangJieType? {
        // 如果类型构造器相同，直接返回 subtype
        if (areEqualTypeConstructors(subtype.constructor, supertype.constructor, typeConstructorEquality)) {
            return subtype
        }

        // 在超类型中递归查找
        for (immediateSupertype in subtype.constructor.supertypes) {
            val result = findCorrespondingSupertype(immediateSupertype, supertype, typeConstructorEquality)
            if (result != null) {
                return result
            }
        }

        return null
    }
}
