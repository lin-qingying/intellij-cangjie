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

package cn.cangnova.cangjie.resolve.calls.inference.components

import cn.cangnova.cangjie.resolve.calls.inference.model.ArgumentConstraintPosition
import cn.cangnova.cangjie.resolve.calls.inference.model.FixVariableConstraintPosition
import cn.cangnova.cangjie.resolve.calls.model.PostponedAtomWithRevisableExpectedType
import cn.cangnova.cangjie.types.model.CangJieTypeMarker
import cn.cangnova.cangjie.types.model.TypeVariableMarker


/*
 * Functions from this context can not be moved to TypeSystemInferenceExtensionContext, because
 *   it's classic implementation, ClassicTypeSystemContext lays in :core:descriptors,
 *   but we need access classes from :compiler:resolution for this function implementation
 */
interface ConstraintSystemUtilContext{
    fun createTypeVariableForLambdaReturnType(): TypeVariableMarker
    fun createTypeVariableForCallableReferenceReturnType(): TypeVariableMarker

    fun TypeVariableMarker.shouldBeFlexible(): Boolean
    fun createArgumentConstraintPosition(argument: PostponedAtomWithRevisableExpectedType): ArgumentConstraintPosition<*>
    fun createTypeVariableForLambdaParameterType(argument: PostponedAtomWithRevisableExpectedType, index: Int): TypeVariableMarker
    fun createTypeVariableForCallableReferenceParameterType(
        argument: PostponedAtomWithRevisableExpectedType,
        index: Int
    ): TypeVariableMarker
     fun extractLambdaParameterTypesFromDeclaration(declaration: PostponedAtomWithRevisableExpectedType): List<CangJieTypeMarker?>?
    fun PostponedAtomWithRevisableExpectedType.isFunctionExpression(): Boolean
    fun PostponedAtomWithRevisableExpectedType.isFunctionExpressionWithReceiver(): Boolean
    fun PostponedAtomWithRevisableExpectedType.isLambda(): Boolean
    fun <T> createFixVariableConstraintPosition(variable: TypeVariableMarker, atom: T): FixVariableConstraintPosition<T>
    val isForcedAllowForkingInferenceSystem get() = false
    fun CangJieTypeMarker.unCapture(): CangJieTypeMarker
    fun TypeVariableMarker.hasOnlyInputTypesAttribute(): Boolean
    val isForcedConsiderExtensionReceiverFromConstrainsInLambda get() = false

}
