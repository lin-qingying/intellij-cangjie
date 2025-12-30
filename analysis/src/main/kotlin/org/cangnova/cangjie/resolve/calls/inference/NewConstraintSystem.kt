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

import org.cangnova.cangjie.resolve.calls.components.PostponedArgumentsAnalyzerContext
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemCompletionContext
import org.cangnova.cangjie.resolve.calls.inference.model.Constraint
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemError
import org.cangnova.cangjie.resolve.checkers.EmptyIntersectionTypeInfo
import org.cangnova.cangjie.types.model.CangJieTypeMarker
import org.cangnova.cangjie.types.model.TypeVariableMarker



interface NewConstraintSystem {
    val hasContradiction: Boolean
    val errors: List<ConstraintSystemError>


    fun asConstraintSystemCompleterContext(): ConstraintSystemCompletionContext

    // after this method we shouldn't mutate system via ConstraintSystemBuilder
    fun asReadOnlyStorage(): ConstraintStorage

    fun getBuilder(): ConstraintSystemBuilder
    fun asPostponedArgumentsAnalyzerContext(): PostponedArgumentsAnalyzerContext
    fun getEmptyIntersectionTypeKind(types: Collection<CangJieTypeMarker>): EmptyIntersectionTypeInfo?

}


/**
 * In some cases we're not only adding constraints linearly to the system, but sometimes we need to consider several variants of constraints
 *
 * For example, from smartcast we've got a value of a type A<Int, String> & A<E, F> that we'd like to pass as an argument to the parameter
 * of type A<Xv, Yv> (where Xv and Yv are the type variables of the current call)
 *
 * So, we've got a subtyping constraint
 * A<Int, String> & A<E, F> <: A<Xv, Yv>
 *
 * And we might go with the first intersection component, having the following variables constraint set: {Xv=Int,Yv=String}
 * Or, if we'd consider the second component it would be {Xv=E, Yv=F}
 *
 * And all existing and future constraints might work differently depending on which option we've chosen.
 * Thus, ideally we need to create two versions of the constraint system and try to resolveName each of them.
 * But that lead to exponential complexity, so we only use some set of heuristics for that
 *
 * Lately, we call such situation a "fork point" and each of the options a "fork point branch"
 * Each branch is defined by the set of constraints that need to be added to the system if we choose the particular branch.
 */
typealias ForkPointData = List<ForkPointBranchDescription>
typealias ForkPointBranchDescription = Set<Pair<TypeVariableMarker, Constraint>>
