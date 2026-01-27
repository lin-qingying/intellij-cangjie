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

package org.cangnova.cangjie.resolve.calls.inference.components

import org.cangnova.cangjie.container.DefaultImplementation
import org.cangnova.cangjie.resolve.calls.inference.model.Constraint
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemError
import org.cangnova.cangjie.resolve.calls.inference.model.InitialConstraint
import org.cangnova.cangjie.types.model.CangJieTypeMarker
import org.cangnova.cangjie.types.model.ConstraintSystemMarker
import org.cangnova.cangjie.types.model.TypeVariableMarker


/**
 * 类型推断日志记录器
 *
 * 该抽象类用于在类型推断过程中记录各种事件，主要用于调试和测试目的。
 * 通过记录约束的添加、类型变量的创建、错误的产生等事件，
 * 可以帮助开发者理解和调试复杂的类型推断过程。
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ 设计目的                                                                      │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * 1. **调试支持**：在类型推断出现问题时，可以通过日志追踪约束的传播过程
 * 2. **测试验证**：在测试中验证类型推断的中间步骤是否正确
 * 3. **可视化**：将复杂的类型推断过程以可读的形式展示出来
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ 使用方式                                                                      │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * 在生产环境中，使用 [Dummy] 实现（默认）：
 * - 所有方法调用会抛出错误，因为不应该被调用
 * - 通过扩展函数 [withOrigin] 和 [withOrigins] 的空安全检查来避免实际调用
 *
 * 在测试环境中，可以提供自定义实现：
 * - 实现所有抽象方法来记录推断过程
 * - 用于验证约束传播的正确性
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ 记录的事件类型                                                                │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * - [logInitial]：记录初始约束的添加
 * - [log]：记录派生约束的创建
 * - [logError]：记录约束系统错误
 * - [logNewVariable]：记录新类型变量的创建
 * - [logReadiness]：记录类型变量固定的准备状态
 * - [withOrigin]：追踪约束的来源（单一来源）
 * - [withOrigins]：追踪约束的来源（双重来源，用于约束合并）
 *
 * @see ConstraintIncorporator 约束合并器，使用此日志记录器追踪合并过程
 * @see ConstraintInjector 约束注入器，使用此日志记录器追踪注入过程
 */
@DefaultImplementation(InferenceLogger.Dummy::class)
abstract class InferenceLogger {

    /**
     * 记录初始约束
     *
     * 当一个初始约束被添加到约束系统时调用。
     * 初始约束来自于函数调用的参数类型、返回值类型、显式类型注解等。
     *
     * @param constraint 被添加的初始约束
     * @param system 约束系统标记，用于标识约束所属的系统
     */
    abstract fun logInitial(constraint: InitialConstraint, system: ConstraintSystemMarker)

    /**
     * 记录派生约束的创建
     *
     * 当一个新的 [Constraint] 被创建时调用。
     * 注意：此函数追踪的是约束的实例化，而非约束被添加到约束存储的时刻。
     * 这是因为编译器可能会"跳过"添加某些中间约束，但日志记录器仍应打印它们，
     * 以使推断过程更加清晰明了。
     *
     * @param variable 约束所关联的类型变量
     * @param constraint 被创建的约束
     * @param system 约束系统标记
     *
     * @see MutableVariableWithConstraints.addConstraint 约束添加逻辑
     */
    abstract fun log(variable: TypeVariableMarker, constraint: Constraint, system: ConstraintSystemMarker)

    /**
     * 记录约束系统错误
     *
     * 当类型推断过程中发生错误时调用，例如：
     * - 类型不匹配
     * - 约束冲突
     * - 无法推导的类型变量
     *
     * @param error 发生的错误
     * @param system 约束系统标记
     */
    abstract fun logError(error: ConstraintSystemError, system: ConstraintSystemMarker)

    /**
     * 记录新类型变量的创建
     *
     * 当一个新的类型变量被创建并添加到约束系统时调用。
     * 例如，泛型函数调用时会为每个类型参数创建对应的类型变量。
     *
     * @param variable 新创建的类型变量
     * @param system 约束系统标记
     */
    abstract fun logNewVariable(variable: TypeVariableMarker, system: ConstraintSystemMarker)

    /**
     * 类型变量固定日志记录
     *
     * 记录类型变量固定过程中的状态信息，包括：
     * - 每个类型变量的准备状态
     * - 被选中固定的类型变量
     * - 最终固定到的类型
     *
     * @property map 类型变量到其固定信息的映射
     * @property chosen 被选中进行固定的类型变量（如果有）
     * @property fixedTo 类型变量最终被固定到的类型（固定完成后设置）
     */
    class FixationLogRecord(
        val map: Map<TypeVariableMarker, FixationLogVariableInfo<*>>,
        val chosen: TypeVariableMarker?,
    ) {
        var fixedTo: CangJieTypeMarker? = null
    }

    /**
     * 类型变量固定信息
     *
     * 记录单个类型变量在固定过程中的状态信息。
     *
     * @param Readiness 准备状态的类型，用于表示类型变量是否准备好被固定
     * @property readiness 类型变量的准备状态
     * @property constraints 固定前该类型变量的所有约束
     * @property constraintsBeforeFixationCount 固定前的约束数量
     */
    class FixationLogVariableInfo<Readiness : Any>(
        val readiness: Readiness,
        val constraints: List<Constraint>,
    ) {
        val constraintsBeforeFixationCount = constraints.size
    }

    /**
     * 记录类型变量的固定准备状态
     *
     * 在类型变量固定过程中调用，记录每个类型变量的准备状态，
     * 以及最终选择固定的类型变量。
     *
     * @param fixationLog 固定日志记录，包含所有类型变量的状态
     * @param system 约束系统标记
     */
    abstract fun logReadiness(
        fixationLog: FixationLogRecord,
        system: ConstraintSystemMarker,
    )

    /**
     * 在指定初始约束的上下文中执行代码块
     *
     * 用于追踪约束的来源，将后续产生的约束与初始约束关联起来。
     * 此方法主要在测试中使用，不需要高性能。
     *
     * @param T 代码块的返回类型
     * @param constraint 作为来源的初始约束
     * @param block 要执行的代码块
     * @return 代码块的执行结果
     */
    abstract fun <T> withOrigin(constraint: InitialConstraint, block: () -> T): T

    /**
     * 在指定双重约束来源的上下文中执行代码块
     *
     * 用于追踪约束合并过程中的来源，将后续产生的约束与两个源约束关联起来。
     * 这在约束传播和合并过程中特别有用，可以追踪派生约束是如何产生的。
     *
     * 此方法主要在测试中使用，不需要高性能。
     *
     * 示例场景：
     * ```
     * // 如果有约束 α <: A 和 B <: α
     * // 合并后产生 B <: A
     * // withOrigins 可以追踪这个派生过程
     * ```
     *
     * @param T 代码块的返回类型
     * @param variable1 第一个约束关联的类型变量
     * @param constraint1 第一个源约束
     * @param variable2 第二个约束关联的类型变量
     * @param constraint2 第二个源约束
     * @param block 要执行的代码块
     * @return 代码块的执行结果
     */
    abstract fun <T> withOrigins(
        variable1: TypeVariableMarker,
        constraint1: Constraint,
        variable2: TypeVariableMarker,
        constraint2: Constraint,
        block: () -> T,
    ): T


    /**
     * 空实现（哑对象）
     *
     * 这是 [InferenceLogger] 的默认实现，所有方法都会抛出错误。
     * 在生产环境中，通过扩展函数的空安全检查来避免实际调用这些方法。
     *
     * 设计目的：
     * 1. 作为默认实现，避免在不需要日志时创建不必要的对象
     * 2. 如果意外被调用，立即报错以便发现问题
     * 3. 配合 [ConstraintIncorporator] 中的 `takeIf { it !is Dummy }` 检查使用
     *
     * @see withOrigin 扩展函数会检查 logger 是否为 null
     * @see withOrigins 扩展函数会检查 logger 是否为 null
     */
    object Dummy : InferenceLogger() {
        override fun logInitial(
            constraint: InitialConstraint,
            system: ConstraintSystemMarker
        ) = error("Should never be called")

        override fun log(
            variable: TypeVariableMarker,
            constraint: Constraint,
            system: ConstraintSystemMarker
        ) = error("Should never be called")

        override fun logError(
            error: ConstraintSystemError,
            system: ConstraintSystemMarker
        ) = error("Should never be called")

        override fun logNewVariable(
            variable: TypeVariableMarker,
            system: ConstraintSystemMarker
        ) = error("Should never be called")

        override fun logReadiness(
            fixationLog: FixationLogRecord,
            system: ConstraintSystemMarker
        ) = error("Should never be called")

        override fun <T> withOrigin(constraint: InitialConstraint, block: () -> T): T = error("Should never be called")

        override fun <T> withOrigins(
            variable1: TypeVariableMarker,
            constraint1: Constraint,
            variable2: TypeVariableMarker,
            constraint2: Constraint,
            block: () -> T,
        ): T = error("Should never be called")
    }
}

/**
 * 空安全的 withOrigin 扩展函数
 *
 * 如果 logger 为 null，直接执行代码块并返回结果；
 * 否则，调用 logger 的 withOrigin 方法来追踪约束来源。
 *
 * 这是一个内联函数，在生产环境中（logger 为 null 时）不会产生额外开销。
 *
 * @param T 代码块的返回类型
 * @param constraint 作为来源的初始约束
 * @param block 要执行的代码块
 * @return 代码块的执行结果
 */
inline fun <T> InferenceLogger?.withOrigin(constraint: InitialConstraint, crossinline block: () -> T): T = when {
    this == null -> block()
    else -> withOrigin(constraint) { block() }
}

/**
 * 空安全的 withOrigins 扩展函数
 *
 * 如果 logger 为 null，直接执行代码块并返回结果；
 * 否则，调用 logger 的 withOrigins 方法来追踪约束合并的来源。
 *
 * 这是一个内联函数，在生产环境中（logger 为 null 时）不会产生额外开销。
 *
 * 使用场景示例（在 ConstraintIncorporator 中）：
 * ```kotlin
 * inferenceLogger.withOrigins(typeVariable, existingConstraint, typeVariable, newConstraint) {
 *     addNewIncorporatedConstraint(...)
 * }
 * ```
 *
 * @param T 代码块的返回类型
 * @param variable1 第一个约束关联的类型变量
 * @param constraint1 第一个源约束
 * @param variable2 第二个约束关联的类型变量
 * @param constraint2 第二个源约束
 * @param block 要执行的代码块
 * @return 代码块的执行结果
 */
inline fun <T> InferenceLogger?.withOrigins(
    variable1: TypeVariableMarker,
    constraint1: Constraint,
    variable2: TypeVariableMarker,
    constraint2: Constraint,
    crossinline block: () -> T,
): T = when {
    this == null -> block()
    else -> withOrigins(variable1, constraint1, variable2, constraint2) { block() }
}
