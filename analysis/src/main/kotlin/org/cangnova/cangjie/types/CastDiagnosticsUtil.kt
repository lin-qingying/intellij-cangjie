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

import com.google.common.collect.Maps
import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.descriptors.ClassDescriptor
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.types.checker.CangJieTypeChecker
import org.cangnova.cangjie.types.checker.TypeCheckingProcedure
import org.cangnova.cangjie.types.checker.TypeIntersector

object CastDiagnosticsUtil {
    /**
     * 检查是否为向上转型。
     *
     * @param candidateType 候选类型
     * @param targetType 目标类型
     * @return 如果候选类型是目标类型的子类型，则返回 true，否则返回 false
     */
    private fun isUpcast(candidateType: CangJieType, targetType: CangJieType): Boolean {
        if (!CangJieTypeChecker.DEFAULT.isSubtypeOf(candidateType, targetType)) return false

        if (candidateType.isFunctionType && targetType.isFunctionType) {
            return candidateType.isExtensionFunctionType == targetType.isExtensionFunctionType
        }

        return true
    }

    /**
     * 检查类型细化是否无用。
     *
     * @param possibleTypes 可能的类型集合
     * @param targetType 目标类型
     * @param shouldCheckForExactType 是否检查精确类型
     * @return 如果类型细化无用，则返回 true，否则返回 false
     */
    fun isRefinementUseless(
        possibleTypes: Collection<CangJieType>,
        targetType: CangJieType,
        shouldCheckForExactType: Boolean
    ): Boolean {
        val intersectedType = TypeIntersector.intersectTypes(possibleTypes.map { it.upperIfFlexible() }) ?: return false

        return if (shouldCheckForExactType)
            isExactTypeCast(intersectedType, targetType)
        else
            isUpcast(intersectedType, targetType)
    }

    /**
     * 检查是否为精确类型转换。
     *
     * @param candidateType 候选类型
     * @param targetType 目标类型
     * @return 如果候选类型和目标类型完全相同，则返回 true，否则返回 false
     */
    private fun isExactTypeCast(candidateType: CangJieType, targetType: CangJieType): Boolean {
        return candidateType == targetType && candidateType.isExtensionFunctionType == targetType.isExtensionFunctionType
    }

    /**
     * 检查类型转换是否可能。
     *
     * @param lhsType 左操作数类型
     * @param rhsType 右操作数类型
     * @param platformToCangJieClassMapper 平台到 CangJie 类的映射器
     * @return 如果类型转换可能，则返回 true，否则返回 false
     */
    @JvmStatic
    fun isCastPossible(
        lhsType: CangJieType,
        rhsType: CangJieType,
        platformToCangJieClassMapper: PlatformToCangJieClassMapper
    ): Boolean {
        val typeConstructor = lhsType.constructor
        if (typeConstructor is IntersectionTypeConstructor) {
            return typeConstructor.supertypes.any { isCastPossible(it, rhsType, platformToCangJieClassMapper) }
        }
        val rhsNullable = TypeUtils.isNullableType(rhsType)
        val lhsNullable = TypeUtils.isNullableType(lhsType)
        if (CangJieBuiltIns.isNothing(lhsType)) return true
        if (CangJieBuiltIns.isNullableNothing(lhsType) && !rhsNullable) return false
        if (CangJieBuiltIns.isNothing(rhsType)) return false
        if (CangJieBuiltIns.isNullableNothing(rhsType)) return lhsNullable
        if (lhsNullable && rhsNullable) return true
        if (lhsType.isError) return true
        if (isRelated(lhsType, rhsType, platformToCangJieClassMapper)) return true
        if (TypeUtils.isTypeParameter(lhsType) || TypeUtils.isTypeParameter(rhsType)) return true

        return false
    }

    /**
     * 检查两个类型是否相关。
     *
     * @param a 第一个类型
     * @param b 第二个类型
     * @param platformToCangJieClassMapper 平台到 CangJie 类的映射器
     * @return 如果两个类型相关，则返回 true，否则返回 false
     */
    private fun isRelated(
        a: CangJieType,
        b: CangJieType,
        platformToCangJieClassMapper: PlatformToCangJieClassMapper
    ): Boolean {
        val aClasses = mapToPlatformIndependentClasses(a, platformToCangJieClassMapper)
        val bClasses = mapToPlatformIndependentClasses(b, platformToCangJieClassMapper)

        return aClasses.any {
            DescriptorUtils.isSubtypeOfClass(
                b,
                it
            )
        } || bClasses.any { DescriptorUtils.isSubtypeOfClass(a, it) }
    }

    /**
     * 将类型映射到平台无关的类。
     *
     * @param type 类型
     * @param platformToCangJieClassMapper 平台到 CangJie 类的映射器
     * @return 映射后的类列表
     */
    private fun mapToPlatformIndependentClasses(
        type: CangJieType,
        platformToCangJieClassMapper: PlatformToCangJieClassMapper
    ): List<ClassDescriptor> {
        val descriptor = type.constructor.declarationDescriptor as? ClassDescriptor ?: return listOf()

        return platformToCangJieClassMapper.mapPlatformClass(descriptor) + descriptor
    }

    /**
     * 检查从超类型到子类型的转换是否被擦除。
     *
     * @param supertype 超类型
     * @param subtype 子类型
     * @param typeChecker 类型检查器
     * @return 如果转换被擦除，则返回 true，否则返回 false
     */
    @JvmStatic
    fun isCastErased(supertype: CangJieType, subtype: CangJieType, typeChecker: CangJieTypeChecker): Boolean {
        val isNonReifiedTypeParameter = TypeUtils.isNonReifiedTypeParameter(subtype)
        val isUpcast = typeChecker.isSubtypeOf(supertype, subtype)

        // 这里我们希望限制类似 `x is T` 的情况，其中 x = ?T，而 T 可能有可空的上界
        if (isNonReifiedTypeParameter && !isUpcast) {
            val nullableToDefinitelyNotNull =
                !TypeUtils.isNullableType(subtype) && supertype.makeNonOption() == subtype
            if (!nullableToDefinitelyNotNull) {
                return true
            }
        }

        // 在 `is` 语句中这是错误，在 `as` 语句中这是警告
        if (supertype.isMarkedOption || subtype.isMarkedOption) {
            return isCastErased(TypeUtils.makeNonOption(supertype), TypeUtils.makeNonOption(subtype), typeChecker)
        }

        // 如果是向上转型，永远不会被擦除
        if (isUpcast) return false

        // 向下转型到非具体化类型参数总是被擦除
        if (isNonReifiedTypeParameter) return true

        val staticallyKnownSubtype =
            findStaticallyKnownSubtype(supertype, subtype.constructor).resultingType ?: return true

        // 如果替换失败，这意味着结果是不可能的类型，例如 Out<in Foo>
        // 在这种情况下，我们无法保证任何事情，因此认为转换被擦除

        // 如果计算出的类型是目标类型的子类型，则可以使用目标类型代替
        // 如果不是，则使用它是错误的
        return !typeChecker.isSubtypeOf(staticallyKnownSubtype, subtype)
    }

    /**
     * 查找静态已知的子类型。
     *
     * @param supertype 超类型
     * @param subtypeConstructor 子类型的构造器
     * @return 类型重建结果
     */
    @JvmStatic
    fun findStaticallyKnownSubtype(
        supertype: CangJieType,
        subtypeConstructor: TypeConstructor
    ): TypeReconstructionResult {
        assert(!supertype.isMarkedOption) { "此方法仅适用于非可空类型" }

        // 假设我们将表达式类型为 Collection<Foo> 的表达式转换为 List<Bar>
        // 首先，让我们创建 List<T>，其中 T 是类型变量
        val descriptor = subtypeConstructor.declarationDescriptor ?: error("无法为 ${subtypeConstructor} 创建默认类型")
        val subtypeWithVariables = descriptor.defaultType

        // 现在，让我们找到 List<T> 的一个超类型，它是一个 Collection 的某个类型，
        // 在这种情况下，它将是 Collection<T>
        val supertypeWithVariables = TypeCheckingProcedure.findCorrespondingSupertype(subtypeWithVariables, supertype)

        val variables = subtypeWithVariables.constructor.parameters
        val variableConstructors = variables.map(TypeParameterDescriptor::getTypeConstructor).toSet()

        val substitution: MutableMap<TypeConstructor, TypeProjection> = if (supertypeWithVariables != null) {
            // 现在，让我们尝试统一 Collection<T> 和 Collection<Foo>，解决方案是从 T 到 Foo 的映射
            val solution = TypeUnifier.unify(
                TypeProjectionImpl(supertype),
                TypeProjectionImpl(supertypeWithVariables),
                variableConstructors::contains
            )
            Maps.newHashMap(solution.substitution)
        } else {
            // 如果没有对应的超类型，没有确定的变量
            // 这可能是正常的，例如在 'Any as List<*>' 的情况下
            Maps.newHashMapWithExpectedSize<TypeConstructor, TypeProjection>(variables.size)
        }

        // 如果某些参数没有通过统一确定，这意味着这些参数丢失了，
        // 让我们用星号代替，这样我们只能转换为类似 List<*> 的类型，例如 (a: Any) as List<*>
        var allArgumentsInferred = true
        for (variable in variables) {
            val value = substitution[variable.typeConstructor]
            if (value == null) {
                substitution[variable.typeConstructor] = TypeUtils.makeProjection(variable)
                allArgumentsInferred = false
            }
        }

        // 此时我们已经为 List 的所有类型参数确定了值
        // 让我们通过替换它们来创建一个类型：List<T> -> List<Foo>
        val substituted = TypeSubstitutor.create(substitution).substitute(subtypeWithVariables, Variance.INVARIANT)

        return TypeReconstructionResult(substituted, allArgumentsInferred)
    }
}
