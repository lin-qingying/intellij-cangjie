/*
 * Copyright 2024 LinQingYing. and contributors.
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

package com.linqingying.cangjie.metadata

import com.linqingying.cangjie.metadata.node.CmType
import kotlin.contracts.ExperimentalContracts

/**
 * Represents a contract of a Kotlin function.
 *
 * Contracts are an internal feature of the standard Kotlin library, and their behavior and/or binary format
 * may change in a subsequent release.
 */
@ExperimentalContracts
class CmContract {
    /**
     * Effects of this contract.
     */
    val effects: MutableList<CmEffect> = ArrayList(1)
}

/**
 * Represents an effect (a part of the contract of a Kotlin function).
 *
 * Contracts are an internal feature of the standard Kotlin library, and their behavior and/or binary format
 * may change in a subsequent release.
 *
 * @property type type of the effect
 * @property invocationKind optional number of invocations of the lambda parameter of this function,
 *   specified further in the effect expression
 */
@ExperimentalContracts
class CmEffect(
    var type: CmEffectType,
    var invocationKind: CmEffectInvocationKind?,
) {
    /**
     * Arguments of the effect constructor, i.e., the constant value for the [CmEffectType.RETURNS_CONSTANT] effect,
     * or the parameter reference for the [CmEffectType.CALLS] effect.
     */
    val constructorArguments: MutableList<CmEffectExpression> = ArrayList(1)

    /**
     * Conclusion of the effect. If this value is set, the effect represents an implication with this value as the right-hand side.
     */
    var conclusion: CmEffectExpression? = null
}

/**
 * Represents an effect expression, the contents of an effect (a part of the contract of a Kotlin function).
 *
 * Contracts are an internal feature of the standard Kotlin library, and their behavior and/or binary format
 * may change in a subsequent release.
 *
 * Various effect expression attributes can be read and manipulated via extension properties,
 * such as [CmEffectExpression.isNegated].
 */
@ExperimentalContracts
class CmEffectExpression {
    internal var flags: Int = 0

    /**
     * Optional 1-based index of the value parameter of the function, for effects which assert something about
     * the function parameters. Index 0 means the extension receiver parameter.
     */
    var parameterIndex: Int? = null

    /**
     * Constant value used in the effect expression.
     */
    var constantValue: CmConstantValue? = null

    /**
     * Type used as the target of an `is`-expression in the effect expression.
     */
    var isInstanceType: CmType? = null

    /**
     * Arguments of an `&&`-expression. If this list is non-empty, the resulting effect expression is a conjunction of this expression
     * and elements of the list.
     */
    val andArguments: MutableList<CmEffectExpression> = ArrayList(0)

    /**
     * Arguments of an `||`-expression. If this list is non-empty, the resulting effect expression is a disjunction of this expression
     * and elements of the list.
     */
    val orArguments: MutableList<CmEffectExpression> = ArrayList(0)
}

/**
 * Represents a constant value used in an effect expression.
 *
 * Contracts are an internal feature of the standard Kotlin library, and their behavior and/or binary format
 * may change in a subsequent release.
 *
 * @property value the constant value. May be `true`, `false` or `null`
 */
@ExperimentalContracts
data class CmConstantValue(val value: Any?)


/**
 * Type of an effect (a part of the contract of a Kotlin function).
 *
 * Contracts are an internal feature of the standard Kotlin library, and their behavior and/or binary format
 * may change in a subsequent release.
 */
@ExperimentalContracts
enum class CmEffectType {
    /**
     * Represents `returns(value)` contract effect:
     * a situation when a function returns normally with the specified return value.
     * Return value is stored in the [CmEffect.constructorArguments].
     */
    RETURNS_CONSTANT,

    /**
     * Represents `callsInPlace` contract effect:
     * A situation when the referenced lambda is invoked in place (optionally) specified number of times.
     *
     * Referenced lambda is stored in the [CmEffect.constructorArguments].
     * Number of invocations, if specified, is stored in [CmEffect.invocationKind].
     */
    CALLS,

    /**
     * Represents `returnsNotNull` contract effect:
     * a situation when a function returns normally with any value that is not null.
     */
    RETURNS_NOT_NULL,
}

/**
 * Number of invocations of a lambda parameter specified by an effect (a part of the contract of a Kotlin function).
 *
 * Contracts are an internal feature of the standard Kotlin library, and their behavior and/or binary format
 * may change in a subsequent release.
 */
@ExperimentalContracts
enum class CmEffectInvocationKind {
    /**
     * A function parameter will be invoked one time or not invoked at all.
     */
    AT_MOST_ONCE,

    /**
     * A function parameter will be invoked exactly one time.
     */
    EXACTLY_ONCE,

    /**
     * A function parameter will be invoked one or more times.
     */
    AT_LEAST_ONCE,
}
