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

package org.cangnova.cangjie.resolve.calls.inference

import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintKind
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintPosition
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemError
import org.cangnova.cangjie.types.model.CangJieTypeMarker
import org.cangnova.cangjie.types.model.TypeConstructorMarker
import org.cangnova.cangjie.types.model.TypeSubstitutorMarker
import org.cangnova.cangjie.types.model.TypeVariableMarker


/**
 * 约束系统操作接口
 *
 * 定义对类型约束系统的基本操作。
 * 约束系统用于类型推断，通过收集和求解类型约束来推断泛型类型参数。
 *
 * 核心概念：
 * - **类型变量**: 需要推断的泛型类型参数
 * - **约束**: 类型之间的关系（子类型、相等等）
 * - **矛盾**: 不可满足的约束组合
 * - **后置变量**: 延迟解析的类型变量（用于 lambda 参数推断）
 *
 * 使用场景：
 * - 函数调用的类型参数推断
 * - Lambda 表达式的参数类型推断
 * - 泛型方法的返回类型推断
 *
 * @see ConstraintSystemBuilder
 * @see ConstraintStorage
 */
interface ConstraintSystemOperation {
    /**
     * 是否存在矛盾
     *
     * 如果约束系统中存在不可满足的约束，返回 true。
     * 例如：同时要求 T <: Int 和 T <: String
     */
    val hasContradiction: Boolean

    /**
     * 注册类型变量
     *
     * 将一个新的类型变量添加到约束系统中。
     *
     * @param variable 要注册的类型变量
     */
    fun registerVariable(variable: TypeVariableMarker)

    /**
     * 标记后置类型变量
     *
     * 将类型变量标记为后置变量，延迟其求解。
     * 用于支持构建器推断等高级特性。
     *
     * @param variable 要标记的类型变量
     */
    fun markPostponedVariable(variable: TypeVariableMarker)

    /**
     * 标记可以使用无限制构建器推断解析
     *
     * 表示当前约束系统可以通过无限制构建器推断来解析。
     */
    fun markCouldBeResolvedWithUnrestrictedBuilderInference()

    /**
     * 取消标记后置类型变量
     *
     * @param variable 要取消标记的类型变量
     */
    fun unmarkPostponedVariable(variable: TypeVariableMarker)

    /**
     * 移除所有后置类型变量
     *
     * 清除所有延迟解析的类型变量。
     */
    fun removePostponedVariables()

    /**
     * 替换已固定的类型变量
     *
     * 使用给定的替换器替换约束系统中已经解析的类型变量。
     *
     * @param substitutor 类型替换器
     */
    fun substituteFixedVariables(substitutor: TypeSubstitutorMarker)

    /**
     * 获取后置参数的函数期望类型（带路径）
     *
     * 获取为后置参数构建的函数期望类型，通过路径定位到嵌套的类型变量。
     *
     * @param topLevelVariable 顶层类型构造器
     * @param pathToExpectedType 到期望类型的路径
     * @return 构建的函数类型，如果不存在则返回 null
     */
    fun getBuiltFunctionalExpectedTypeForPostponedArgument(
        topLevelVariable: TypeConstructorMarker,
        pathToExpectedType: List<Pair<TypeConstructorMarker, Int>>
    ): CangJieTypeMarker?

    /**
     * 获取后置参数的函数期望类型
     *
     * 获取为后置参数构建的函数期望类型。
     *
     * @param expectedTypeVariable 期望类型变量的构造器
     * @return 构建的函数类型，如果不存在则返回 null
     */
    fun getBuiltFunctionalExpectedTypeForPostponedArgument(expectedTypeVariable: TypeConstructorMarker): CangJieTypeMarker?

    /**
     * 存储后置参数的函数期望类型（带路径）
     *
     * @param topLevelVariable 顶层类型构造器
     * @param pathToExpectedType 到期望类型的路径
     * @param builtFunctionalType 构建的函数类型
     */
    fun putBuiltFunctionalExpectedTypeForPostponedArgument(
        topLevelVariable: TypeConstructorMarker,
        pathToExpectedType: List<Pair<TypeConstructorMarker, Int>>,
        builtFunctionalType: CangJieTypeMarker
    )

    /**
     * 存储后置参数的函数期望类型
     *
     * @param expectedTypeVariable 期望类型变量的构造器
     * @param builtFunctionalType 构建的函数类型
     */
    fun putBuiltFunctionalExpectedTypeForPostponedArgument(
        expectedTypeVariable: TypeConstructorMarker,
        builtFunctionalType: CangJieTypeMarker
    )

    /**
     * 添加子类型约束
     *
     * 添加一个约束：lowerType 是 upperType 的子类型（lowerType <: upperType）。
     *
     * 示例：
     * - 推断 `List<T>` 中的 `T`，如果 `T` 赋值给 `Number`，则添加 `T <: Number`
     *
     * @param lowerType 子类型
     * @param upperType 超类型
     * @param position 约束位置信息，用于错误报告
     */
    fun addSubtypeConstraint(lowerType: CangJieTypeMarker, upperType: CangJieTypeMarker, position: ConstraintPosition)

    /**
     * 添加相等约束
     *
     * 添加一个约束：a 和 b 必须是相同的类型。
     *
     * @param a 第一个类型
     * @param b 第二个类型
     * @param position 约束位置信息，用于错误报告
     */
    fun addEqualityConstraint(a: CangJieTypeMarker, b: CangJieTypeMarker, position: ConstraintPosition)

    /**
     * 判断是否为具体类型
     *
     * 具体类型是不包含未解析的类型变量的类型。
     *
     * @param type 要检查的类型
     * @return 如果是具体类型则返回 true
     */
    fun isProperType(type: CangJieTypeMarker): Boolean

    /**
     * 判断是否为类型变量
     *
     * @param type 要检查的类型
     * @return 如果是类型变量则返回 true
     */
    fun isTypeVariable(type: CangJieTypeMarker): Boolean

    /**
     * 判断是否为后置类型变量
     *
     * @param typeVariable 要检查的类型变量
     * @return 如果是后置类型变量则返回 true
     */
    fun isPostponedTypeVariable(typeVariable: TypeVariableMarker): Boolean

    /**
     * 获取具体超类型构造器列表
     *
     * 返回给定类型的所有具体超类型的构造器。
     *
     * @param type 要获取超类型的类型
     * @return 超类型构造器列表
     */
    fun getProperSuperTypeConstructors(type: CangJieTypeMarker): List<TypeConstructorMarker>

    /**
     * 添加其他约束系统
     *
     * 合并另一个约束系统的约束到当前系统。
     * 用于组合多个推断结果。
     *
     * @param otherSystem 要合并的约束存储
     */
    fun addOtherSystem(otherSystem: ConstraintStorage)

    /**
     * 约束系统中的错误列表
     *
     * 包含所有在约束求解过程中产生的错误。
     */
    val errors: List<ConstraintSystemError>
}

/**
 * 在事务中运行约束系统操作
 *
 * 这个函数提供事务支持，允许尝试性地执行约束系统操作。如果操作成功（返回 true），
 * 则提交事务；如果失败（返回 false），则回滚所有更改。
 *
 * ## 工作原理
 *
 * 1. 创建事务状态快照 ([prepareTransaction])
 * 2. 执行提供的操作 ([runOperations])
 * 3. 根据操作结果决定提交或回滚：
 *    - 返回 true：提交事务，保留所有更改
 *    - 返回 false：回滚事务，恢复到事务前状态
 *
 * ## 使用场景
 *
 * - 尝试添加约束，如果导致矛盾则放弃
 * - 测试某个类型假设是否有效
 * - 探索多个可能的类型推断路径
 *
 * ## 示例
 *
 * ```kotlin
 * // 尝试添加约束，如果成功则继续，否则尝试其他路径
 * val success = constraintSystem.runTransaction {
 *     addSubtypeConstraint(typeA, typeB, position)
 *     !hasContradiction  // 如果无矛盾则返回 true，提交事务
 * }
 * ```
 *
 * @param runOperations 要在事务中执行的操作，返回 true 表示成功，false 表示失败
 * @return 如果操作成功并提交事务则返回 true，否则返回 false
 *
 * @see ConstraintSystemBuilder.prepareTransaction
 * @see ConstraintSystemTransaction
 */
inline fun ConstraintSystemBuilder.runTransaction(crossinline runOperations: ConstraintSystemOperation.() -> Boolean): Boolean {
    val transactionState = prepareTransaction()

    // 执行操作，如果返回 true 则提交事务
    if (runOperations()) {
        transactionState.closeTransaction()
        return true
    }

    // 操作失败，回滚事务
    transactionState.rollbackTransaction()
    return false
}

/**
 * 约束系统事务抽象基类
 *
 * 表示一个约束系统的事务状态。事务提供了快照和回滚机制，
 * 允许尝试性地修改约束系统，并在需要时恢复到事务前的状态。
 *
 * ## 事务生命周期
 *
 * 1. **创建**: 通过 [ConstraintSystemBuilder.prepareTransaction] 创建事务
 * 2. **操作**: 在事务中执行约束系统操作
 * 3. **提交或回滚**:
 *    - 调用 [closeTransaction] 提交更改
 *    - 调用 [rollbackTransaction] 撤销更改
 *
 * ## 使用场景
 *
 * - 尝试多个类型推断路径，选择最优解
 * - 测试约束兼容性而不影响主约束系统
 * - 实现渐进式类型推断
 *
 * @see ConstraintSystemBuilder.prepareTransaction
 * @see runTransaction
 */
abstract class ConstraintSystemTransaction {
    /**
     * 关闭并提交事务
     *
     * 将事务中的所有更改永久应用到约束系统。
     * 调用此方法后，事务完成，不能再回滚。
     */
    abstract fun closeTransaction()

    /**
     * 回滚事务
     *
     * 撤销事务中的所有更改，将约束系统恢复到事务开始前的状态。
     * 调用此方法后，事务中的所有操作都将被丢弃。
     */
    abstract fun rollbackTransaction()
}

/**
 * 检查子类型约束是否兼容
 *
 * 测试添加子类型约束 `lowerType <: upperType` 是否会导致约束系统产生矛盾。
 * 这是一个只读操作，不会实际修改约束系统。
 *
 * ## 工作原理
 *
 * 在事务中尝试添加约束，检查是否产生矛盾，然后回滚事务。
 * 这样可以安全地测试约束兼容性而不影响主约束系统。
 *
 * ## 使用场景
 *
 * - 在重载解析时选择最匹配的候选函数
 * - 判断类型转换是否有效
 * - 验证类型参数是否满足边界约束
 *
 * ## 示例
 *
 * ```kotlin
 * // 检查 String 是否可以赋值给 Any
 * val compatible = constraintSystem.isSubtypeConstraintCompatible(
 *     stringType,    // lowerType (子类型)
 *     anyType,       // upperType (超类型)
 *     position
 * )
 * // compatible 为 true，因为 String <: Any
 * ```
 *
 * @param lowerType 子类型
 * @param upperType 超类型
 * @param position 约束位置信息，用于错误报告
 * @return 如果添加约束后无矛盾则返回 true，否则返回 false
 *
 * @see isConstraintCompatible
 * @see addSubtypeConstraintIfCompatible
 */
fun ConstraintSystemBuilder.isSubtypeConstraintCompatible(
    lowerType: CangJieTypeMarker,
    upperType: CangJieTypeMarker,
    position: ConstraintPosition
): Boolean = isConstraintCompatible(lowerType, upperType, position, ConstraintKind.LOWER)

/**
 * 检查约束是否兼容（内部实现）
 *
 * 通用的约束兼容性检查实现，支持三种约束类型：
 * - [ConstraintKind.LOWER]: 子类型约束 (lowerType <: upperType)
 * - [ConstraintKind.UPPER]: 超类型约束 (upperType <: lowerType)
 * - [ConstraintKind.EQUALITY]: 相等约束 (lowerType = upperType)
 *
 * ## 实现细节
 *
 * 1. 开启事务
 * 2. 根据约束类型添加相应约束
 * 3. 检查是否产生矛盾
 * 4. 回滚事务（始终返回 false 以触发回滚）
 * 5. 返回兼容性检查结果
 *
 * @param lowerType 第一个类型
 * @param upperType 第二个类型
 * @param position 约束位置信息
 * @param kind 约束类型（LOWER/UPPER/EQUALITY）
 * @return 如果添加约束后无矛盾则返回 true，否则返回 false
 */
private fun ConstraintSystemBuilder.isConstraintCompatible(
    lowerType: CangJieTypeMarker,
    upperType: CangJieTypeMarker,
    position: ConstraintPosition,
    kind: ConstraintKind
): Boolean {
    var isCompatible = false
    runTransaction {
        if (!hasContradiction) {
            when (kind) {
                ConstraintKind.LOWER -> addSubtypeConstraint(lowerType, upperType, position)
                ConstraintKind.UPPER -> addSubtypeConstraint(upperType, lowerType, position)
                ConstraintKind.EQUALITY -> addEqualityConstraint(lowerType, upperType, position)
            }
        }
        isCompatible = !hasContradiction
        false  // 始终回滚事务，这只是兼容性测试
    }
    return isCompatible
}

/**
 * 约束系统构建器接口
 *
 * 扩展 [ConstraintSystemOperation]，添加了事务管理和状态查询功能。
 * 这是类型推断系统的核心接口，用于构建和求解类型约束。
 *
 * ## 核心功能
 *
 * 1. **约束管理**: 添加、查询和修改类型约束（继承自 [ConstraintSystemOperation]）
 * 2. **事务支持**: 提供快照和回滚机制 ([prepareTransaction])
 * 3. **类型替换**: 构建当前的类型替换器 ([buildCurrentSubstitutor])
 * 4. **状态访问**: 访问当前约束存储 ([currentStorage])
 *
 * ## 使用场景
 *
 * - 函数调用的类型参数推断
 * - Lambda 表达式的参数类型推断
 * - 类型检查和验证
 * - 重载解析
 *
 * ## 典型工作流程
 *
 * ```kotlin
 * // 1. 创建约束系统构建器
 * val builder: ConstraintSystemBuilder = ...
 *
 * // 2. 注册类型变量
 * builder.registerVariable(typeVariable)
 *
 * // 3. 添加约束
 * builder.addSubtypeConstraint(argType, paramType, position)
 *
 * // 4. 检查是否有矛盾
 * if (!builder.hasContradiction) {
 *     // 5. 构建类型替换器
 *     val substitutor = builder.buildCurrentSubstitutor()
 *     // 6. 应用替换
 *     val resolvedType = substitutor.substitute(originalType)
 * }
 * ```
 *
 * ## 事务示例
 *
 * ```kotlin
 * // 尝试添加约束，如果失败则回滚
 * val success = builder.runTransaction {
 *     addSubtypeConstraint(typeA, typeB, position)
 *     !hasContradiction
 * }
 * ```
 *
 * @see ConstraintSystemOperation
 * @see ConstraintSystemTransaction
 * @see ConstraintStorage
 */
interface ConstraintSystemBuilder : ConstraintSystemOperation {
    /**
     * 准备事务
     *
     * 创建一个新的约束系统事务，保存当前状态的快照。
     * 事务可以稍后提交或回滚。
     *
     * @return 约束系统事务对象
     * @see ConstraintSystemTransaction
     */
    fun prepareTransaction(): ConstraintSystemTransaction

    /**
     * 构建当前类型替换器
     *
     * 基于约束系统的当前状态，构建一个类型替换器，用于将类型变量替换为推断的类型。
     *
     * ## 使用场景
     *
     * - 获取类型推断的结果
     * - 在表达式上应用推断的类型
     * - 生成完整的类型信息用于代码生成
     *
     * ## 示例
     *
     * ```kotlin
     * // 推断泛型函数的类型参数
     * fun <T> identity(x: T): T = x
     *
     * // 调用 identity(42)
     * constraintSystem.addEqualityConstraint(T, Int64Type, position)
     * val substitutor = constraintSystem.buildCurrentSubstitutor()
     * val resolvedReturnType = substitutor.substitute(returnType)
     * // resolvedReturnType 现在是 Int64
     * ```
     *
     * @return 类型替换器，将类型变量映射到推断的类型
     */
    fun buildCurrentSubstitutor(): TypeSubstitutorMarker

    /**
     * 获取当前约束存储
     *
     * 返回包含所有当前约束的存储对象。这是约束系统的底层表示。
     *
     * ## 使用场景
     *
     * - 检查约束系统的详细状态
     * - 导出约束用于调试或诊断
     * - 合并多个约束系统
     *
     * @return 当前的约束存储
     * @see ConstraintStorage
     */
    fun currentStorage(): ConstraintStorage
}

/**
 * 如果兼容则添加子类型约束
 *
 * 尝试添加子类型约束 `lowerType <: upperType`，如果添加后约束系统无矛盾则保留约束，
 * 否则回滚。这是一个写操作，成功时会修改约束系统。
 *
 * ## 与 [isSubtypeConstraintCompatible] 的区别
 *
 * - [isSubtypeConstraintCompatible]: 只读测试，不修改约束系统
 * - [addSubtypeConstraintIfCompatible]: 写操作，成功时添加约束，失败时回滚
 *
 * ## 工作原理
 *
 * 1. 在事务中尝试添加约束
 * 2. 检查是否产生矛盾
 * 3. 如果无矛盾，提交事务（保留约束）
 * 4. 如果有矛盾，回滚事务（撤销约束）
 *
 * ## 使用场景
 *
 * - 渐进式类型推断，逐步添加约束
 * - 尝试推断，失败时不影响已有约束
 * - 可选约束的添加
 *
 * ## 示例
 *
 * ```kotlin
 * // 尝试推断类型参数 T
 * val success = constraintSystem.addSubtypeConstraintIfCompatible(
 *     argumentType,
 *     parameterType,
 *     position
 * )
 * if (success) {
 *     // 约束添加成功，继续推断
 * } else {
 *     // 约束不兼容，尝试其他推断路径
 * }
 * ```
 *
 * @param lowerType 子类型
 * @param upperType 超类型
 * @param position 约束位置信息，用于错误报告
 * @return 如果约束兼容并成功添加则返回 true，否则返回 false
 *
 * @see isSubtypeConstraintCompatible
 * @see addConstraintIfCompatible
 */
fun ConstraintSystemBuilder.addSubtypeConstraintIfCompatible(
    lowerType: CangJieTypeMarker,
    upperType: CangJieTypeMarker,
    position: ConstraintPosition
): Boolean = addConstraintIfCompatible(lowerType, upperType, position, ConstraintKind.LOWER)

/**
 * 如果兼容则添加约束（内部实现）
 *
 * 通用的条件约束添加实现，支持三种约束类型：
 * - [ConstraintKind.LOWER]: 子类型约束 (lowerType <: upperType)
 * - [ConstraintKind.UPPER]: 超类型约束 (upperType <: lowerType)
 * - [ConstraintKind.EQUALITY]: 相等约束 (lowerType = upperType)
 *
 * ## 实现细节
 *
 * 使用 [runTransaction] 在事务中执行操作：
 * 1. 添加相应类型的约束
 * 2. 检查是否产生矛盾
 * 3. 返回 true（提交事务）或 false（回滚事务）
 *
 * @param lowerType 第一个类型
 * @param upperType 第二个类型
 * @param position 约束位置信息
 * @param kind 约束类型（LOWER/UPPER/EQUALITY）
 * @return 如果约束兼容并成功添加则返回 true，否则返回 false
 */
private fun ConstraintSystemBuilder.addConstraintIfCompatible(
    lowerType: CangJieTypeMarker,
    upperType: CangJieTypeMarker,
    position: ConstraintPosition,
    kind: ConstraintKind
): Boolean = runTransaction {
    if (!hasContradiction) {
        when (kind) {
            ConstraintKind.LOWER -> addSubtypeConstraint(lowerType, upperType, position)
            ConstraintKind.UPPER -> addSubtypeConstraint(upperType, lowerType, position)
            ConstraintKind.EQUALITY -> addEqualityConstraint(lowerType, upperType, position)
        }
    }
    !hasContradiction  // 如果无矛盾则提交事务（返回 true），否则回滚（返回 false）
}

/**
 * 如果兼容则添加相等约束
 *
 * 尝试添加相等约束 `lowerType = upperType`，如果添加后约束系统无矛盾则保留约束，
 * 否则回滚。
 *
 * ## 使用场景
 *
 * - Lambda 表达式的精确类型推断
 * - 泛型类型参数的双向推断
 * - 类型别名的解析
 *
 * ## 示例
 *
 * ```kotlin
 * // 推断泛型函数的类型参数
 * // fun <T> box(value: T): Box<T>
 * // let x: Box<Int64> = box(42)
 *
 * // 从返回类型推断: Box<T> = Box<Int64>
 * val success = constraintSystem.addEqualityConstraintIfCompatible(
 *     returnType,     // Box<T>
 *     expectedType,   // Box<Int64>
 *     position
 * )
 * // 推断出 T = Int64
 * ```
 *
 * @param lowerType 第一个类型
 * @param upperType 第二个类型
 * @param position 约束位置信息，用于错误报告
 * @return 如果约束兼容并成功添加则返回 true，否则返回 false
 *
 * @see addSubtypeConstraintIfCompatible
 * @see addConstraintIfCompatible
 */
fun ConstraintSystemBuilder.addEqualityConstraintIfCompatible(
    lowerType: CangJieTypeMarker,
    upperType: CangJieTypeMarker,
    position: ConstraintPosition
): Boolean = addConstraintIfCompatible(lowerType, upperType, position, ConstraintKind.EQUALITY)
