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
import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.descriptors.FunctionDescriptor
import org.cangnova.cangjie.resolve.calls.results.FlatSignature
import org.cangnova.cangjie.resolve.calls.results.TypeSpecificityComparator
import org.cangnova.cangjie.resolve.calls.results.createFromCallableDescriptor
import org.cangnova.cangjie.types.ErrorUtils
import org.cangnova.cangjie.types.checker.CangJieTypeChecker

/**
 * 存在性约束系统 (Existential Constraint System)
 *
 * ECS 是专门用于重载决议的轻量级约束系统，其核心目标是回答存在性问题：
 *
 * > **是否存在一个类型代换 σ，使得给定的约束成立？**
 *
 * ## 与完整约束系统的区别
 *
 * ECS **只返回 YES/NO**，不进行：
 * - 类型推断（不给出具体类型）
 * - 最优解选择
 * - 调用解析
 * - Lambda 推断
 * - Builder inference
 *
 * ## 核心应用场景
 *
 * ### 1. 重载冲突检测 (Overloadability)
 *
 * ```cangjie
 * func <T> f(x: T): Unit
 * func f(x: Any): Unit
 * ```
 *
 * 问题：是否存在 T，使得 T <: Any？
 * 答案：存在（例如 T = String）
 * 结论：两个声明可以共存
 *
 * ### 2. 特异性比较 (Specificity)
 *
 * ```cangjie
 * func <T> f(x: T): Unit      // A
 * func f(x: String): Unit     // B
 * ```
 *
 * 问题：A 是否不比 B 更不具体？
 * 即：是否存在 T，使得 T <: String？
 * 答案：存在（T = String）
 * 结论：A 至少和 B 一样具体
 *
 * ## 使用示例
 *
 * ```kotlin
 * val ecs = ExistentialConstraintSystem.create(builtIns)
 *
 * // 检查两个函数是否可以重载
 * val canOverload = ecs.checkOverloadability(funcA, funcB)
 *
 * // 比较特异性
 * val result = ecs.compareSpecificity(signatureA, signatureB)
 * if (result == SpecificityResult.MORE_SPECIFIC) {
 *     println("A 比 B 更具体")
 * }
 * ```
 *
 * @property builtIns 内置类型系统
 * @property typeChecker 类型检查器
 * @property specificityComparator 类型特异性比较器
 */
class ExistentialConstraintSystem private constructor(
    private val builtIns: CangJieBuiltIns,
    private val typeChecker: CangJieTypeChecker,
    private val specificityComparator: TypeSpecificityComparator
) {
    /** 签名比较器 */
    private val signatureComparator = SignatureComparator(builtIns, typeChecker, specificityComparator)

    // ==================== 重载检测 API ====================

    /**
     * 检查两个可调用描述符是否可以重载
     *
     * 两个函数可以重载，当且仅当它们的签名可区分。
     * 如果签名不可区分（互相不比对方更具体），则存在歧义，不能重载。
     *
     * @param a 第一个可调用描述符
     * @param b 第二个可调用描述符
     * @return 重载检查结果
     */
    fun checkOverloadability(a: CallableDescriptor, b: CallableDescriptor): OverloadabilityResult {
        // 快速路径：如果一个有类型参数而另一个没有，可以重载
        if (a.typeParameters.isEmpty() != b.typeParameters.isEmpty()) {
            return OverloadabilityResult.OVERLOADABLE
        }

        // 快速路径：如果包含错误类型，认为可以重载（避免级联错误）
        if (a is FunctionDescriptor && ErrorUtils.containsErrorTypeInParameters(a) ||
            b is FunctionDescriptor && ErrorUtils.containsErrorTypeInParameters(b)
        ) {
            return OverloadabilityResult.OVERLOADABLE
        }

        // 创建扁平签名
        val aSignature = FlatSignature.createFromCallableDescriptor(a)
        val bSignature = FlatSignature.createFromCallableDescriptor(b)

        return checkOverloadability(aSignature, bSignature)
    }

    /**
     * 检查两个签名是否可以重载
     *
     * @param sig1 第一个签名
     * @param sig2 第二个签名
     * @return 重载检查结果
     */
    fun <T> checkOverloadability(sig1: FlatSignature<T>, sig2: FlatSignature<T>): OverloadabilityResult {
        // A 和 B 不可重载 ⟺ A 不比 B 更不具体 且 B 不比 A 更不具体
        val aNotLessSpecificThanB = signatureComparator.isSignatureNotLessSpecific(sig1, sig2)
        val bNotLessSpecificThanA = signatureComparator.isSignatureNotLessSpecific(sig2, sig1)

        return if (aNotLessSpecificThanB && bNotLessSpecificThanA) {
            OverloadabilityResult.CONFLICTING
        } else {
            OverloadabilityResult.OVERLOADABLE
        }
    }

    /**
     * 检查两个签名是否可区分
     *
     * 可区分 = 可以重载
     *
     * @param sig1 第一个签名
     * @param sig2 第二个签名
     * @return true 如果签名可区分
     */
    fun <T> areSignaturesDistinguishable(sig1: FlatSignature<T>, sig2: FlatSignature<T>): Boolean {
        return checkOverloadability(sig1, sig2) == OverloadabilityResult.OVERLOADABLE
    }

    // ==================== 特异性比较 API ====================

    /**
     * 判断 specific 签名是否不比 general 签名更不具体
     *
     * @param specific 更具体的签名候选
     * @param general 更一般的签名候选
     * @return true 如果 specific 不比 general 更不具体
     */
    fun <T> isNotLessSpecific(specific: FlatSignature<T>, general: FlatSignature<T>): Boolean {
        return signatureComparator.isSignatureNotLessSpecific(specific, general)
    }

    /**
     * 比较两个签名的特异性关系
     *
     * @param sig1 第一个签名
     * @param sig2 第二个签名
     * @return 特异性比较结果
     */
    fun <T> compareSpecificity(sig1: FlatSignature<T>, sig2: FlatSignature<T>): SpecificityResult {
        return signatureComparator.compareSpecificity(sig1, sig2)
    }

    /**
     * 比较两个可调用描述符的特异性关系
     *
     * @param a 第一个可调用描述符
     * @param b 第二个可调用描述符
     * @return 特异性比较结果
     */
    fun compareSpecificity(a: CallableDescriptor, b: CallableDescriptor): SpecificityResult {
        val aSignature = FlatSignature.createFromCallableDescriptor(a)
        val bSignature = FlatSignature.createFromCallableDescriptor(b)
        return compareSpecificity(aSignature, bSignature)
    }

    // ==================== 底层约束 API ====================

    /**
     * 创建一个新的约束求解器
     *
     * 用于自定义约束检查场景。
     *
     * @return 新的存在性约束求解器
     */
    fun createSolver(): ExistentialConstraintSolver {
        return ExistentialConstraintSolver(builtIns, typeChecker)
    }

    /**
     * 快速检查一组约束是否可满足
     *
     * @param constraints 约束列表
     * @return true 如果可满足
     */
    fun isSatisfiable(constraints: Collection<ExistentialConstraint>): Boolean {
        return ExistentialConstraintSolver.isSatisfiable(builtIns, constraints)
    }

    companion object {
        /**
         * 创建 ECS 实例
         *
         * @param builtIns 内置类型系统
         * @param specificityComparator 类型特异性比较器（可选）
         * @return ECS 实例
         */
        fun create(
            builtIns: CangJieBuiltIns,
            specificityComparator: TypeSpecificityComparator = TypeSpecificityComparator.NONE
        ): ExistentialConstraintSystem {
            return ExistentialConstraintSystem(
                builtIns,
                CangJieTypeChecker.DEFAULT,
                specificityComparator
            )
        }

        /**
         * 创建带自定义类型检查器的 ECS 实例
         *
         * @param builtIns 内置类型系统
         * @param typeChecker 类型检查器
         * @param specificityComparator 类型特异性比较器
         * @return ECS 实例
         */
        fun create(
            builtIns: CangJieBuiltIns,
            typeChecker: CangJieTypeChecker,
            specificityComparator: TypeSpecificityComparator = TypeSpecificityComparator.NONE
        ): ExistentialConstraintSystem {
            return ExistentialConstraintSystem(builtIns, typeChecker, specificityComparator)
        }
    }
}
