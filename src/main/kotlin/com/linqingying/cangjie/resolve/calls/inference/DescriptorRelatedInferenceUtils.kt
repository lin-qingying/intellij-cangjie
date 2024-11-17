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

package com.linqingying.cangjie.resolve.calls.inference

import com.linqingying.cangjie.descriptors.CallableDescriptor
import com.linqingying.cangjie.resolve.calls.components.PostponedArgumentsAnalyzerContext
import com.linqingying.cangjie.resolve.calls.inference.components.NewTypeSubstitutor
import com.linqingying.cangjie.resolve.calls.inference.model.ConstraintStorage
import com.linqingying.cangjie.resolve.calls.model.CallableReferenceCangJieCallArgument
import com.linqingying.cangjie.resolve.calls.model.CangJieCallArgument
import com.linqingying.cangjie.resolve.calls.model.LHSResult
import com.linqingying.cangjie.resolve.calls.model.SubCangJieCallArgument
import com.linqingying.cangjie.types.*
import com.linqingying.cangjie.types.model.TypeSystemInferenceExtensionContext

fun TypeSubstitutor.substitute(type: UnwrappedType): UnwrappedType = safeSubstitute(type, Variance.INVARIANT).unwrap()

fun CallableDescriptor.substituteAndApproximateTypes(
    substitutor: NewTypeSubstitutor,
    typeApproximator: TypeApproximator?,
    positionDependentApproximation: Boolean = false
): CallableDescriptor {
    if (substitutor.isEmpty) return this

    val wrappedSubstitution = object : TypeSubstitution() {
        override fun get(key: CangJieType): TypeProjection? = null

        override fun prepareTopLevelType(topLevelType: CangJieType, position: Variance) =
            substitutor.safeSubstitute(topLevelType.unwrap()).let { substitutedType ->
                typeApproximator?.approximateTo(
                    substitutedType,
                    TypeApproximatorConfiguration.FinalApproximationAfterResolutionAndInference,
                      !positionDependentApproximation
                ) ?: substitutedType
            }
    }

    return substitute(TypeSubstitutor.create(wrappedSubstitution)) ?: this
}

fun ConstraintStorage.buildResultingSubstitutor(
    context: TypeSystemInferenceExtensionContext,
    transformTypeVariablesToErrorTypes: Boolean = true
): NewTypeSubstitutor {
    return buildAbstractResultingSubstitutor(context, transformTypeVariablesToErrorTypes) as NewTypeSubstitutor
}

fun CallableDescriptor.substitute(substitutor: NewTypeSubstitutor): CallableDescriptor {
    if (substitutor.isEmpty) return this

    val wrappedSubstitution = object : TypeSubstitution() {
        override fun get(key: CangJieType): TypeProjection? = null
        override fun prepareTopLevelType(topLevelType: CangJieType, position: Variance) = substitutor.safeSubstitute(topLevelType.unwrap())
    }
    return substitute(TypeSubstitutor.create(wrappedSubstitution))
}
fun PostponedArgumentsAnalyzerContext.addSubsystemFromArgument(argument: CangJieCallArgument?): Boolean {
    return when (argument) {
        is SubCangJieCallArgument -> {
            addOtherSystem(argument.callResult.constraintSystem.getBuilder().currentStorage())
            true
        }

        is CallableReferenceCangJieCallArgument -> {
            addSubsystemFromArgument((argument.lhsResult as? LHSResult.Expression)?.lshCallArgument)
        }

        else -> false
    }
}
