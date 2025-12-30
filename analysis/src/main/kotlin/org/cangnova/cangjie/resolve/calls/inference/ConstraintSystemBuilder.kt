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

// if runOperations return true, then this operation will be applied, and function return true
inline fun ConstraintSystemBuilder.runTransaction(crossinline runOperations: ConstraintSystemOperation.() -> Boolean): Boolean {
    val transactionState = prepareTransaction()

    // typeVariablesTransaction is clear
    if (runOperations()) {
        transactionState.closeTransaction()
        return true
    }

    transactionState.rollbackTransaction()
    return false
}

abstract class ConstraintSystemTransaction {
    abstract fun closeTransaction()

    abstract fun rollbackTransaction()
}

fun ConstraintSystemBuilder.isSubtypeConstraintCompatible(
    lowerType: CangJieTypeMarker,
    upperType: CangJieTypeMarker,
    position: ConstraintPosition
): Boolean = isConstraintCompatible(lowerType, upperType, position, ConstraintKind.LOWER)

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
        false
    }
    return isCompatible
}

interface ConstraintSystemBuilder : ConstraintSystemOperation {
    fun prepareTransaction(): ConstraintSystemTransaction

    fun buildCurrentSubstitutor(): TypeSubstitutorMarker

    fun currentStorage(): ConstraintStorage
}

fun ConstraintSystemBuilder.addSubtypeConstraintIfCompatible(
    lowerType: CangJieTypeMarker,
    upperType: CangJieTypeMarker,
    position: ConstraintPosition
): Boolean = addConstraintIfCompatible(lowerType, upperType, position, ConstraintKind.LOWER)

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
    !hasContradiction
}

fun ConstraintSystemBuilder.addEqualityConstraintIfCompatible(
    lowerType: CangJieTypeMarker,
    upperType: CangJieTypeMarker,
    position: ConstraintPosition
): Boolean = addConstraintIfCompatible(lowerType, upperType, position, ConstraintKind.EQUALITY)
