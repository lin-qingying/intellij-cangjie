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

package cn.cangnova.cangjie.resolve.calls.inference

import cn.cangnova.cangjie.resolve.calls.inference.constraintPosition.ConstraintPositionKind

interface ConstraintSystemStatus{
    /**
     * Returns `true` if constraint system has a solution (has no contradiction and has enough information to infer each registered type variable).
     */
    fun isSuccessful(): Boolean

    /**
     * Return `true` if constraint system has no contradiction (it can be not successful because of the lack of information for a type variable).
     */
    fun hasContradiction(): Boolean

    /**
     * Returns `true` if type constraints for some type variable are contradicting.
     *
     * For example, for `fun <R> foo(r: R, t: java.util.List<R>) {}` in invocation `foo(1, arrayList("s"))`
     * type variable `R` has two conflicting constraints:
     * - "R is a supertype of Int"
     * - "List<R> is a supertype of List<String>" which leads to "R is equal to String"
     */
    fun hasConflictingConstraints(): Boolean

    /**
     * Returns `true` if contradiction of type constraints comes from declared bounds for type parameters.
     *
     * For example, for `fun <R: Any> foo(r: R) {}` in invocation `foo(null)`
     * upper bounds `Any` for type parameter `R` is violated.
     *
     * It's the special case of 'hasConflictingConstraints' case.
     */
    fun hasViolatedUpperBound(): Boolean

    /**
     * Returns `true` if there is no information for some registered type variable.
     *
     * For example, for `fun <E> newList()` in invocation `val nl = newList()`
     * there is no information to infer type variable `E`.
     */
    fun hasUnknownParameters(): Boolean

    /**
     * Returns `true` if some constraint cannot be processed because of type constructor mismatch.
     *
     * For example, for `fun <R> foo(t: List<R>) {}` in invocation `foo(hashSetOf("s"))`
     * there is type constructor mismatch: "HashSet<String> cannot be a subtype of List<R>".
     */
    fun hasParameterConstraintError(): Boolean

    /**
     * Returns `true` if there is type constructor mismatch only in constraintPosition or
     * constraint system is successful without constraints from this position.
     */
    fun hasOnlyErrorsDerivedFrom(kind: ConstraintPositionKind): Boolean

    /**
     * Returns `true` if there is an error in constraining types.
     * Is used not to generate type inference error if there was one in argument types.
     */
    fun hasErrorInConstrainingTypes(): Boolean

    /**
     * Returns `true` if a user type contains the type projection that cannot be captured.
     *
     * For example, for `fun <T> foo(t: Array<Array<T>>) {}`
     * in invocation `foo(array)` where `array` has type `Array<Array<out Int>>`.
     */
    fun hasCannotCaptureTypesError(): Boolean

    /**
     * Returns `true` if there's an error in constraint system incorporation.
     */
    fun hasTypeInferenceIncorporationError(): Boolean

    fun hasTypeParameterWithUnsatisfiedOnlyInputTypesError(): Boolean

    val constraintErrors: List<ConstraintError>
}
