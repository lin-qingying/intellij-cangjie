package com.huawei.cangjie.resolve.calls.inference

import com.huawei.cangjie.resolve.calls.inference.model.Constraint
import com.huawei.cangjie.resolve.calls.inference.model.ConstraintSystemError
import com.huawei.cangjie.types.model.TypeVariableMarker

interface NewConstraintSystem {
//    val hasContradiction: Boolean
    val errors: List<ConstraintSystemError>
//
    fun getBuilder(): ConstraintSystemBuilder
//
//    // after this method we shouldn't mutate system via ConstraintSystemBuilder
//    fun asReadOnlyStorage(): ConstraintStorage
//
//    fun asConstraintSystemCompleterContext(): ConstraintSystemCompletionContext
//    fun asPostponedArgumentsAnalyzerContext(): PostponedArgumentsAnalyzerContext
//    fun resolveForkPointsConstraints()
//
//    fun getEmptyIntersectionTypeKind(types: Collection<CangJieTypeMarker>): EmptyIntersectionTypeInfo?
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
 * Thus, ideally we need to create two versions of the constraint system and try to resolve each of them.
 * But that lead to exponential complexity, so we only use some set of heuristics for that
 *
 * Lately, we call such situation a "fork point" and each of the options a "fork point branch"
 * Each branch is defined by the set of constraints that need to be added to the system if we choose the particular branch.
 */
typealias ForkPointData = List<ForkPointBranchDescription>
typealias ForkPointBranchDescription = Set<Pair<TypeVariableMarker, Constraint>>
