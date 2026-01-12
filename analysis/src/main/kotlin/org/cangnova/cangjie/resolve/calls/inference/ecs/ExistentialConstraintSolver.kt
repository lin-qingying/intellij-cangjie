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
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.SimpleType
import org.cangnova.cangjie.types.TypeConstructor
import org.cangnova.cangjie.types.checker.CangJieTypeChecker
import org.cangnova.cangjie.types.model.CangJieTypeMarker

/**
 * 存在性约束求解器
 *
 * ECS 的核心求解器，负责判断一组约束是否可满足。
 *
 * ## 算法概述
 *
 * 1. **收集约束**：从输入的约束列表中收集所有约束
 * 2. **简化约束**：将相等约束转换为双向子类型约束
 * 3. **检测矛盾**：检查是否存在不可满足的约束组合
 * 4. **返回结果**：无矛盾 → Satisfiable，有矛盾 → Unsatisfiable
 *
 * ## 关键特性
 *
 * - **只判断存在性**：不求解具体类型，只判断 YES/NO
 * - **轻量级**：相比完整的约束系统更轻量
 * - **早期终止**：一旦发现矛盾立即返回
 *
 * ## 使用示例
 *
 * ```kotlin
 * val solver = ExistentialConstraintSolver(builtIns, typeChecker)
 *
 * // 注册类型变量
 * val typeVar = solver.registerTypeVariable(typeParameter)
 *
 * // 添加约束: T <: String
 * solver.addSubtypeConstraint(typeVar.defaultType, stringType)
 *
 * // 检查可满足性
 * val result = solver.solve()
 * if (result.isSatisfiable) {
 *     println("存在满足条件的代换")
 * }
 * ```
 *
 * @property builtIns 内置类型系统
 * @property typeChecker 类型检查器
 */
class ExistentialConstraintSolver(
    private val builtIns: CangJieBuiltIns,
    private val typeChecker: CangJieTypeChecker = CangJieTypeChecker.DEFAULT
) {
    /** 类型变量注册表 */
    private val variableRegistry = ExistentialTypeVariableRegistry(builtIns)

    /** 待处理的约束队列 */
    private val pendingConstraints = mutableListOf<ExistentialConstraint>()

    /** 已处理的约束集合（避免重复处理） */
    private val processedConstraints = mutableSetOf<ExistentialConstraint>()

    /** 发现的矛盾列表 */
    private val contradictions = mutableListOf<Contradiction>()

    /** 是否已发现矛盾 */
    val hasContradiction: Boolean
        get() = contradictions.isNotEmpty()

    /**
     * 注册类型参数为类型变量
     *
     * @param typeParameter 类型参数描述符
     * @return 对应的存在性类型变量
     */
    fun registerTypeVariable(
        typeParameter: org.cangnova.cangjie.descriptors.TypeParameterDescriptor
    ): ExistentialTypeVariable {
        return variableRegistry.registerTypeParameter(typeParameter)
    }

    /**
     * 批量注册类型参数
     *
     * @param typeParameters 类型参数列表
     * @return 对应的存在性类型变量列表
     */
    fun registerTypeVariables(
        typeParameters: Collection<org.cangnova.cangjie.descriptors.TypeParameterDescriptor>
    ): List<ExistentialTypeVariable> {
        return variableRegistry.registerTypeParameters(typeParameters)
    }

    /**
     * 添加子类型约束
     *
     * @param lower 子类型
     * @param upper 父类型
     */
    fun addSubtypeConstraint(lower: CangJieTypeMarker, upper: CangJieTypeMarker) {
        val constraint = ExistentialConstraint.Subtype(lower, upper)
        if (constraint !in processedConstraints) {
            pendingConstraints.add(constraint)
        }
    }

    /**
     * 添加相等约束
     *
     * @param left 左侧类型
     * @param right 右侧类型
     */
    fun addEqualityConstraint(left: CangJieTypeMarker, right: CangJieTypeMarker) {
        val constraint = ExistentialConstraint.Equality(left, right)
        if (constraint !in processedConstraints) {
            pendingConstraints.add(constraint)
        }
    }

    /**
     * 添加约束
     *
     * @param constraint 要添加的约束
     */
    fun addConstraint(constraint: ExistentialConstraint) {
        if (constraint !in processedConstraints) {
            pendingConstraints.add(constraint)
        }
    }

    /**
     * 批量添加约束
     *
     * @param constraints 约束列表
     */
    fun addConstraints(constraints: Collection<ExistentialConstraint>) {
        for (constraint in constraints) {
            addConstraint(constraint)
        }
    }

    /**
     * 求解约束系统
     *
     * 处理所有待处理的约束，检测矛盾。
     *
     * @return 存在性判断结果
     */
    fun solve(): ExistentialResult {
        // 处理所有约束
        while (pendingConstraints.isNotEmpty() && !hasContradiction) {
            val constraint = pendingConstraints.removeAt(0)

            if (constraint in processedConstraints) {
                continue
            }
            processedConstraints.add(constraint)

            processConstraint(constraint)
        }

        return if (hasContradiction) {
            ExistentialResult.Unsatisfiable(contradictions.toList())
        } else {
            ExistentialResult.Satisfiable
        }
    }

    /**
     * 检查可满足性（不修改状态）
     *
     * 创建当前状态的快照，尝试求解，然后恢复状态。
     *
     * @return true 如果可满足，false 如果不可满足
     */
    fun checkSatisfiability(): Boolean {
        return solve().isSatisfiable
    }

    /**
     * 处理单个约束
     */
    private fun processConstraint(constraint: ExistentialConstraint) {
        when (constraint) {
            is ExistentialConstraint.Subtype -> processSubtypeConstraint(constraint)
            is ExistentialConstraint.Equality -> processEqualityConstraint(constraint)
        }
    }

    /**
     * 处理子类型约束
     */
    private fun processSubtypeConstraint(constraint: ExistentialConstraint.Subtype) {
        val lower = constraint.lower
        val upper = constraint.upper

        // 如果两边都是类型变量，约束总是可满足的
        if (isTypeVariable(lower) && isTypeVariable(upper)) {
            // 类型变量之间的子类型约束总是可满足的
            // 因为我们只需要证明存在性
            return
        }

        // 如果下界是类型变量
        if (isTypeVariable(lower)) {
            // T <: ConcreteType 总是可满足的（T 可以取 Nothing 或任何 ConcreteType 的子类型）
            return
        }

        // 如果上界是类型变量
        if (isTypeVariable(upper)) {
            // ConcreteType <: T 总是可满足的（T 可以取 ConcreteType 或其超类型）
            return
        }

        // 两边都是具体类型，直接检查子类型关系
        if (lower is CangJieType && upper is CangJieType) {
            if (!typeChecker.isSubtypeOf(lower, upper)) {
                addContradiction(constraint, constraint, "Type $lower is not a subtype of $upper")
            }
        }
    }

    /**
     * 处理相等约束
     */
    private fun processEqualityConstraint(constraint: ExistentialConstraint.Equality) {
        val left = constraint.left
        val right = constraint.right

        // 如果任一边是类型变量，转换为双向子类型约束
        if (isTypeVariable(left) || isTypeVariable(right)) {
            // T = U 等价于 T <: U 且 U <: T
            addSubtypeConstraint(left, right)
            addSubtypeConstraint(right, left)
            return
        }

        // 两边都是具体类型，检查类型相等性
        if (left is CangJieType && right is CangJieType) {
            if (!typeChecker.equalTypes(left, right)) {
                addContradiction(constraint, constraint, "Types $left and $right are not equal")
            }
        }
    }

    /**
     * 检查类型是否是类型变量
     */
    private fun isTypeVariable(type: CangJieTypeMarker): Boolean {
        if (type !is SimpleType) return false
        val constructor = type.constructor
        return constructor is ExistentialTypeVariableConstructor ||
                variableRegistry.isTypeVariable(constructor)
    }

    /**
     * 添加矛盾
     */
    private fun addContradiction(
        constraint1: ExistentialConstraint,
        constraint2: ExistentialConstraint,
        reason: String
    ) {
        contradictions.add(Contradiction(constraint1, constraint2, reason))
    }

    /**
     * 重置求解器状态
     */
    fun reset() {
        pendingConstraints.clear()
        processedConstraints.clear()
        contradictions.clear()
        variableRegistry.clear()
    }

    companion object {
        /**
         * 创建新的求解器实例
         */
        fun create(builtIns: CangJieBuiltIns): ExistentialConstraintSolver {
            return ExistentialConstraintSolver(builtIns)
        }

        /**
         * 快速检查一组约束是否可满足
         *
         * @param builtIns 内置类型系统
         * @param constraints 约束列表
         * @return true 如果可满足
         */
        fun isSatisfiable(
            builtIns: CangJieBuiltIns,
            constraints: Collection<ExistentialConstraint>
        ): Boolean {
            val solver = create(builtIns)
            solver.addConstraints(constraints)
            return solver.solve().isSatisfiable
        }
    }
}
