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

package org.cangnova.cangjie.resolve.calls.inference.ecs

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.resolve.calls.results.FlatSignature
import org.cangnova.cangjie.resolve.calls.results.TypeSpecificityComparator
import org.cangnova.cangjie.resolve.calls.results.TypeWithConversion
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.checker.CangJieTypeChecker
import org.cangnova.cangjie.types.model.CangJieTypeMarker
import org.cangnova.cangjie.types.model.TypeParameterMarker

/**
 * ECS 签名比较器
 *
 * 使用存在性约束系统比较两个函数签名的特异性关系。
 *
 * ## 设计思路
 *
 * 与旧的 ConstraintSystem.isSignatureNotLessSpecific 不同，
 * SignatureComparator 只判断存在性，不进行完整的类型推导。
 *
 * 判断 A 是否不比 B 更不具体：
 * - 对于每对参数 (Ai, Bi)，检查是否存在类型代换使 Ai <: Bi
 * - 如果所有参数对都满足，则 A 不比 B 更不具体
 *
 * @property builtIns 内置类型系统
 * @property typeChecker 类型检查器
 * @property specificityComparator 类型特异性比较器
 */
class SignatureComparator(
    private val builtIns: CangJieBuiltIns,
    private val typeChecker: CangJieTypeChecker = CangJieTypeChecker.DEFAULT,
    private val specificityComparator: TypeSpecificityComparator = TypeSpecificityComparator.NONE
) {

    /**
     * 判断 specific 签名是否不比 general 签名更不具体
     *
     * 这是 ECS 版本的特异性比较，只返回 YES/NO。
     *
     * @param specific 更具体的签名候选
     * @param general 更一般的签名候选
     * @return true 如果 specific 不比 general 更不具体
     */
    fun <T> isSignatureNotLessSpecific(
        specific: FlatSignature<T>,
        general: FlatSignature<T>
    ): Boolean {
        // 参数数量必须相同
        if (specific.valueParameterTypes.size != general.valueParameterTypes.size) {
            return false
        }

        // 创建 ECS 求解器
        val solver = ExistentialConstraintSolver(builtIns, typeChecker)

        // 注册 general 签名的类型参数为类型变量
        val typeVariableMap = registerTypeParameters(solver, general.typeParameters)

        // 检查每个参数位置
        for (index in specific.valueParameterTypes.indices) {
            val specificType = specific.valueParameterTypes[index]?.resultType ?: continue
            val generalType = general.valueParameterTypes[index]?.resultType ?: continue

            // 如果 specific 明确更不具体，直接返回 false
            if (specificityComparator.isDefinitelyLessSpecific(specificType, generalType)) {
                return false
            }

            // 检查是否存在满足条件的代换
            if (!checkParameterSpecificity(solver, specificType, generalType, typeVariableMap)) {
                return false
            }
        }

        // 求解约束系统
        return solver.solve().isSatisfiable
    }

    /**
     * 比较两个签名的特异性关系
     *
     * @param sig1 第一个签名
     * @param sig2 第二个签名
     * @return 特异性比较结果
     */
    fun <T> compareSpecificity(
        sig1: FlatSignature<T>,
        sig2: FlatSignature<T>
    ): SpecificityResult {
        val sig1NotLessSpecific = isSignatureNotLessSpecific(sig1, sig2)
        val sig2NotLessSpecific = isSignatureNotLessSpecific(sig2, sig1)

        return when {
            sig1NotLessSpecific && sig2NotLessSpecific -> SpecificityResult.EQUALLY_SPECIFIC
            sig1NotLessSpecific -> SpecificityResult.MORE_SPECIFIC
            sig2NotLessSpecific -> SpecificityResult.LESS_SPECIFIC
            else -> SpecificityResult.INCOMPARABLE
        }
    }

    /**
     * 注册类型参数为类型变量
     *
     * @return 类型参数到类型变量的映射
     */
    private fun registerTypeParameters(
        solver: ExistentialConstraintSolver,
        typeParameters: Collection<TypeParameterMarker>
    ): Map<TypeParameterMarker, ExistentialTypeVariable> {
        val map = mutableMapOf<TypeParameterMarker, ExistentialTypeVariable>()

        for (typeParameter in typeParameters) {
            if (typeParameter is TypeParameterDescriptor) {
                val typeVar = solver.registerTypeVariable(typeParameter)
                map[typeParameter] = typeVar
            }
        }

        return map
    }

    /**
     * 检查单个参数位置的特异性
     *
     * 判断是否存在类型代换使 specificType <: generalType
     */
    private fun checkParameterSpecificity(
        solver: ExistentialConstraintSolver,
        specificType: CangJieTypeMarker,
        generalType: CangJieTypeMarker,
        typeVariableMap: Map<TypeParameterMarker, ExistentialTypeVariable>
    ): Boolean {
        // 将 generalType 中的类型参数替换为类型变量
        val substitutedGeneralType = substituteTypeParameters(generalType, typeVariableMap)

        // 添加子类型约束: specificType <: substitutedGeneralType
        solver.addSubtypeConstraint(specificType, substitutedGeneralType)

        return true
    }

    /**
     * 将类型中的类型参数替换为对应的类型变量
     *
     * 使用 TypeSubstitutor 执行完整的类型替换，包括：
     * - 简单类型参数：T -> TypeVar
     * - 复杂类型参数：List<T> -> List<TypeVar>
     * - 嵌套类型参数：Map<K, List<V>> -> Map<TypeVarK, List<TypeVarV>>
     */
    private fun substituteTypeParameters(
        type: CangJieTypeMarker,
        typeVariableMap: Map<TypeParameterMarker, ExistentialTypeVariable>
    ): CangJieTypeMarker {
        if (typeVariableMap.isEmpty()) return type
        if (type !is CangJieType) return type

        // 构建 TypeConstructor -> UnwrappedType 的映射
        val substitutionMap = buildMap<TypeConstructor, UnwrappedType> {
            typeVariableMap.forEach { (typeParameter, typeVariable) ->
                if (typeParameter is TypeParameterDescriptor) {
                    put(typeParameter.typeConstructor, typeVariable.defaultType.unwrap())
                }
            }
        }

        // 如果没有需要替换的类型参数，直接返回原类型
        if (substitutionMap.isEmpty()) return type

        // 使用 TypeSubstitutor 执行完整的类型替换
        val substitutor = TypeSubstitutors.create(substitutionMap)
        return substitutor.substitute(type)
    }

    companion object {
        /**
         * 创建签名比较器
         */
        fun create(
            builtIns: CangJieBuiltIns,
            specificityComparator: TypeSpecificityComparator = TypeSpecificityComparator.NONE
        ): SignatureComparator {
            return SignatureComparator(builtIns, CangJieTypeChecker.DEFAULT, specificityComparator)
        }
    }
}
