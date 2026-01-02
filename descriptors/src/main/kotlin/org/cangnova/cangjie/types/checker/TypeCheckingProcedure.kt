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
package org.cangnova.cangjie.types.checker

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.resolve.builtIns
import org.cangnova.cangjie.types.*

/**
 * 类型检查过程
 * 
 * 实现仓颉语言的类型检查算法，包括：
 * - 类型相等性检查（忽略泛型）
 * - 子类型关系检查
 * - 类型投影处理
 * - 类型捕获处理
 * 
 * 核心功能：
 * - equalsIgnoringGenerics：检查类型是否相等（忽略泛型）
 * - isSubtypeOf：检查子类型关系
 * - capture：处理类型捕获
 * - getEffectiveProjectionKind：获取有效的投影类型
 * 
 * 示例：
 * ```kotlin
 * val procedure = TypeCheckingProcedure(callbacks)
 * 
 * // 检查类型相等性
 * val isEqual = procedure.equalsIgnoringGenerics(type1, type2)
 * 
 * // 检查子类型关系
 * val isSubtype = procedure.isSubtypeOf(subtype, supertype)
 * ```
 */
class TypeCheckingProcedure(private val constraints: TypeCheckingProcedureCallbacks) {

    /**
     * 检查两个类型是否相等（忽略泛型）
     * 
     * 比较两个类型是否相等，但忽略泛型参数的具体类型。
     * 对于灵活类型，使用异构等价性检查。
     * 
     * 示例：
     * ```kotlin
     * val type1: CangJieType = List<Int>
     * val type2: CangJieType = List<String>
     * val isEqual = procedure.equalsIgnoringGenerics(type1, type2) // true（忽略泛型参数）
     * 
     * val type3: CangJieType = Int
     * val type4: CangJieType = String
     * val isEqual = procedure.equalsIgnoringGenerics(type3, type4) // false
     * ```
     * 
     * @param type1 第一个类型
     * @param type2 第二个类型
     * @return true 如果类型相等（忽略泛型），false 否则
     */
    fun equalsIgnoringGenerics(type1: CangJieType, type2: CangJieType): Boolean {
        if (type1 === type2) return true
        if (type1.isFlexible()) {
            if (type2.isFlexible()) {
                return !type1.isError && !type2.isError &&
                        isSubtypeOf(type1, type2) && isSubtypeOf(type2, type1)
            }
            return heterogeneousEquivalence(type2, type1)
        } else if (type2.isFlexible()) {
            return heterogeneousEquivalence(type1, type2)
        }

        if (type1.isOption != type2.isOption) {
            return false
        }

        if (type1.isOption) {
            // Then type2 is nullable, too (see the previous condition
            return constraints.assertEqualTypes(
                type1.unwrapOption(),
                type2.unwrapOption(),
                this
            )
        }

        val constructor1 = type1.constructor
        val constructor2 = type2.constructor

        if (!constraints.assertEqualTypeConstructors(constructor1, constructor2)) {
            return false
        }

        val type1Arguments  = type1.arguments
        val type2Arguments  = type2.arguments
        if (type1Arguments.size != type2Arguments.size) {
            return false
        }

        for (i in type1Arguments.indices) {
            val typeProjection1 = type1Arguments.get(i)
            val typeProjection2 = type2Arguments.get(i)

            val typeParameter1 = constructor1.parameters.get(i)
            val typeParameter2 = constructor2.parameters.get(i)

            if (capture(typeProjection1, typeProjection2, typeParameter1)) {
                continue
            }
            if (getEffectiveProjectionKind(
                    typeParameter1,
                    typeProjection1
                ) != getEffectiveProjectionKind(typeParameter2, typeProjection2)
            ) {
                return false
            }

            if (!constraints.assertEqualTypes(typeProjection1.type, typeProjection2.type, this)) {
                return false
            }
        }
        return true
    }

    /**
     * 检查子类型关系
     * 
     * 检查subtype是否为supertype的子类型。
     * 这是类型检查的核心方法，处理各种类型关系。
     * 
     * 示例：
     * ```kotlin
     * val intType: CangJieType = Int类型
     * val numberType: CangJieType = Number类型
     * val isSubtype = procedure.isSubtypeOf(intType, numberType) // true
     * 
     * val stringType: CangJieType = String类型
     * val isSubtype = procedure.isSubtypeOf(stringType, numberType) // false
     * ```
     * 
     * @param subtype 子类型
     * @param supertype 超类型
     * @return true 如果subtype是supertype的子类型，false 否则
     */
    fun isSubtypeOf(subtype: CangJieType, supertype: CangJieType): Boolean {
        if (sameTypeConstructors(subtype, supertype)) {
            // Option类型的子类型关系
            if (subtype.isOption && supertype.isOption) {
                return isSubtypeOf(subtype.unwrapOption(), supertype.unwrapOption())
            }
            return true
        }
        
        val subtypeRepresentative: CangJieType = subtype.getSubtypeRepresentative()
        val supertypeRepresentative: CangJieType = supertype.getSupertypeRepresentative()
        if (subtypeRepresentative !== subtype || supertypeRepresentative !== supertype) {
            // recursive invocation for possible chain of representatives
            return isSubtypeOf(subtypeRepresentative, supertypeRepresentative)
        }
        return isSubtypeOfForRepresentatives(subtype, supertype)
    }

    protected fun heterogeneousEquivalence(inflexibleType: CangJieType, flexibleType: CangJieType): Boolean {
        // This is to account for the case when we have Collection<X> vs (Mutable)Collection<X>! or K(java.util.Collection<? extends X>)
        assert(!inflexibleType.isFlexible()) { "Only inflexible types are allowed here: " + inflexibleType }
        return isSubtypeOf(flexibleType.asFlexibleType().lowerBound, inflexibleType)
                && isSubtypeOf(inflexibleType, flexibleType.asFlexibleType().upperBound)
    }

    // 简化类型检查逻辑，适合IDE插件

    fun equalTypes(type1: CangJieType, type2: CangJieType): Boolean {
        if (type1 === type2) return true
        if (type1.isFlexible()) {
            if (type2.isFlexible()) {
                return !type1.isError && !type2.isError &&
                        isSubtypeOf(type1, type2) && isSubtypeOf(type2, type1)
            }
            return heterogeneousEquivalence(type2, type1)
        } else if (type2.isFlexible()) {
            return heterogeneousEquivalence(type1, type2)
        }

        // 检查Option类型
        if (type1.isOption != type2.isOption) {
            return false
        }

        if (type1.isOption) {
            // 都是Option类型，比较内部类型
            return constraints.assertEqualTypes(
                type1.unwrapOption(),
                type2.unwrapOption(),
                this
            )
        }

        val constructor1 = type1.constructor
        val constructor2 = type2.constructor

        if (!constraints.assertEqualTypeConstructors(constructor1, constructor2)) {
            return false
        }

        val type1Arguments  = type1.arguments
        val type2Arguments  = type2.arguments
        if (type1Arguments.size != type2Arguments.size) {
            return false
        }

        for (i in type1Arguments.indices) {
            val typeProjection1 = type1Arguments.get(i)
            val typeProjection2 = type2Arguments.get(i)

            val typeParameter1 = constructor1.parameters.get(i)
            val typeParameter2 = constructor2.parameters.get(i)

            if (capture(typeProjection1, typeProjection2, typeParameter1)) {
                continue
            }
            if (getEffectiveProjectionKind(
                    typeParameter1,
                    typeProjection1
                ) != getEffectiveProjectionKind(typeParameter2, typeProjection2)
            ) {
                return false
            }

            if (!constraints.assertEqualTypes(typeProjection1.type, typeProjection2.type, this)) {
                return false
            }
        }
        return true
    }

    private fun capture(
        subtypeArgumentProjection: TypeProjection,
        supertypeArgumentProjection: TypeProjection,
        parameter: TypeParameterDescriptor
    ): Boolean {
        // Capturing makes sense only for invariant classes
        if (parameter.variance !== Variance.INVARIANT) return false

        // Now, both subtype and supertype relations transform to equality constraints on type arguments:
        // Array<out Int> is a subtype or equal to Array<T> then T captures a type that extends Int: 'Captured(out Int)'
        // Array<in Int> is a subtype or equal to Array<T> then T captures a type that extends Int: 'Captured(in Int)'
        if (subtypeArgumentProjection.projectionKind !== Variance.INVARIANT && supertypeArgumentProjection.projectionKind === Variance.INVARIANT) {
            return constraints.capture(supertypeArgumentProjection.type, subtypeArgumentProjection)
        }
        return false
    }

    /**
     * 检查两个代表性类型之间的子类型关系
     *
     * 这是子类型检查的核心实现，处理各种特殊情况。
     *
     * ## 编译器对应逻辑
     * 参考 cangjie_compiler/src/Sema/TypeManager.cpp IsSubtype() 函数
     *
     * ### Option 类型的子类型规则
     * 根据编译器实现 (TypeManager.cpp:855-858):
     * ```cpp
     * if (root.IsCoreOptionType() && allowOptionBox &&
     *     CountOptionNestedLevel(leaf) < CountOptionNestedLevel(root)) {
     *     // T <: Option<T> (Option 自动装箱)
     *     return IsSubtype(&leaf, root.typeArgs[0], implicitBoxed);
     * }
     * ```
     *
     * **正确的规则**:
     * 1. `T <: Option<T>` - 任何类型都是其 Option 类型的子类型
     * 2. `Option<T> <: Option<Option<T>>` - Option 可以多层嵌套
     * 3. `Option<T>` 与 `T` 之间没有其他子类型关系
     *
     * ### Nothing 类型的特殊规则
     * 根据编译器实现 (TypeManager.cpp:1013-1020):
     * ```cpp
     * (leaf->IsNothing() && !root->IsPlaceholder())  // Nothing 是所有类型的子类型
     * if (root->IsNothing()) { return false; }       // Nothing 不是其他类型的父类型
     * ```
     *
     * @param subtype 子类型
     * @param supertype 超类型
     * @return true 如果 subtype 是 supertype 的子类型
     */
    private fun isSubtypeOfForRepresentatives(subtype: CangJieType, supertype: CangJieType): Boolean {
        // 快速路径 1: Nothing 是所有类型的子类型 (编译器: leaf->IsNothing() && !root->IsPlaceholder())
        if (CangJieBuiltIns.isNothing(subtype)) {
            return true
        }

        // 快速路径 2: Nothing 永远不是其他类型的父类型 (编译器: if (root->IsNothing()) return false)
        if (CangJieBuiltIns.isNothing(supertype)) {
            return false
        }

        // Option 类型的子类型关系 (两个都是 Option)
        if (subtype.isOption && supertype.isOption) {
            // Option<T> <: Option<U> 当且仅当 T <: U
            return isSubtypeOf(subtype.unwrapOption(), supertype.unwrapOption())
        }

        // Option 自动装箱: T <: Option<T> (编译器 allowOptionBox 机制)
        // 对应编译器: root.IsCoreOptionType() && CountOptionNestedLevel(leaf) < CountOptionNestedLevel(root)
        if (supertype.isOption && !subtype.isOption) {
            // T <: Option<T>
            return isSubtypeOf(subtype, supertype.unwrapOption())
        }

        // 注意: Option<T> 不是 T 的子类型！这与编译器一致
        // 编译器中没有 "leaf.IsOption() -> unwrap" 的逻辑

        // 错误类型总是兼容
        if (subtype.isError || supertype.isError) {
            return true
        }

        // TODO: 实现完整的子类型检查逻辑
        // - 类继承关系检查
        // - 接口实现检查
        // - 泛型类型参数检查
        // - 元组/函数类型检查
        return false
    }

    private fun checkSubtypeForTheSameConstructor(subtype: CangJieType, supertype: CangJieType): Boolean {
        val constructor = subtype.constructor

        // this assert was moved to checker/utils.cj
        //assert constraints.assertEqualTypeConstructors(constructor, supertype.getConstructor()) : constructor + " is not " + supertype.getConstructor();
        val subArguments = subtype.arguments
        val superArguments = supertype.arguments
        if (subArguments.size != superArguments.size) return false

        val parameters  = constructor.parameters
        for (i in parameters.indices) {
            val parameter = parameters.get(i)

            val superArgument = superArguments.get(i)
            val subArgument = subArguments.get(i)



            if (capture(subArgument, superArgument, parameter)) continue

            val argumentIsErrorType = subArgument.type.isError || superArgument.type.isError
            if (!argumentIsErrorType && parameter.variance === Variance.INVARIANT && subArgument.projectionKind === Variance.INVARIANT && superArgument.projectionKind === Variance.INVARIANT) {
                if (!constraints.assertEqualTypes(subArgument.type, superArgument.type, this)) return false
                continue
            }

            val superOut: CangJieType = getOutType(parameter, superArgument)
            val subOut: CangJieType = getOutType(parameter, subArgument)
            if (!constraints.assertSubtype(subOut, superOut, this)) return false
        }
        return true
    }

    companion object {
        // This method returns the supertype of the first parameter that has the same constructor
        // as the second parameter, applying the substitution of type arguments to it
        // This method returns the supertype of the first parameter that has the same constructor
        // as the second parameter, applying the substitution of type arguments to it
        @JvmOverloads
        fun findCorrespondingSupertype(
            subtype: CangJieType,
            supertype: CangJieType,
            typeCheckingProcedureCallbacks: TypeCheckingProcedureCallbacks = TypeCheckerProcedureCallbacksImpl()
        ): CangJieType? {
            return findCorrespondingSupertype(subtype, supertype, typeCheckingProcedureCallbacks)
        }

        // If class C<out T> then C<T> and C<out T> mean the same
        // out * out = out
        // out * in  = *
        // out * inv = out
        //
        // in * out  = *
        // in * in   = in
        // in * inv  = in
        //
        // inv * out = out
        // inv * in  = out
        // inv * inv = inv
        fun getEffectiveProjectionKind(
            typeParameterVariance: Variance,
            typeArgumentVariance: Variance
        ): EnrichedProjectionKind {
            return EnrichedProjectionKind.Companion.getEffectiveProjectionKind(
                typeParameterVariance,
                typeArgumentVariance
            )
        }

        fun getEffectiveProjectionKind(
            typeParameter: TypeParameterDescriptor,
            typeArgument: TypeProjection
        ): EnrichedProjectionKind? {
            return getEffectiveProjectionKind(typeParameter.variance, typeArgument.projectionKind)
        }


        private fun getOutType(parameter: TypeParameterDescriptor, argument: TypeProjection): CangJieType {
            return parameter.builtIns.stdlibTypes.anyType
        }
    }
}
