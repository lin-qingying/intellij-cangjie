package com.huawei.cangjie.resolve.calls.inference.components

import com.huawei.cangjie.resolve.calls.inference.model.FixVariableConstraintPosition
import com.huawei.cangjie.resolve.calls.model.PostponedAtomWithRevisableExpectedType
import com.huawei.cangjie.types.model.CangJieTypeMarker
import com.huawei.cangjie.types.model.TypeVariableMarker


/*
 * Functions from this context can not be moved to TypeSystemInferenceExtensionContext, because
 *   it's classic implementation, ClassicTypeSystemContext lays in :core:descriptors,
 *   but we need access classes from :compiler:resolution for this function implementation
 */
interface ConstraintSystemUtilContext{
//    fun TypeVariableMarker.shouldBeFlexible(): Boolean
//    fun TypeVariableMarker.hasOnlyInputTypesAttribute(): Boolean
//    fun CangJieTypeMarker.unCapture(): CangJieTypeMarker
//    fun TypeVariableMarker.isReified(): Boolean
//    fun CangJieTypeMarker.refineType(): CangJieTypeMarker

    // PostponedArgumentInputTypesResolver
//    fun createArgumentConstraintPosition(argument: PostponedAtomWithRevisableExpectedType): ArgumentConstraintPosition<*>
//    fun <T> createFixVariableConstraintPosition(variable: TypeVariableMarker, atom: T): FixVariableConstraintPosition<T>
    fun extractLambdaParameterTypesFromDeclaration(declaration: PostponedAtomWithRevisableExpectedType): List<CangJieTypeMarker?>?
    fun PostponedAtomWithRevisableExpectedType.isFunctionExpression(): Boolean
    fun PostponedAtomWithRevisableExpectedType.isFunctionExpressionWithReceiver(): Boolean
    fun PostponedAtomWithRevisableExpectedType.isLambda(): Boolean
//    fun createTypeVariableForLambdaReturnType(): TypeVariableMarker
//    fun createTypeVariableForLambdaParameterType(argument: PostponedAtomWithRevisableExpectedType, index: Int): TypeVariableMarker
//    fun createTypeVariableForCallableReferenceReturnType(): TypeVariableMarker
//    fun createTypeVariableForCallableReferenceParameterType(
//        argument: PostponedAtomWithRevisableExpectedType,
//        index: Int
//    ): TypeVariableMarker

//    val isForcedConsiderExtensionReceiverFromConstrainsInLambda get() = false

//    val isForcedAllowForkingInferenceSystem get() = false
}
