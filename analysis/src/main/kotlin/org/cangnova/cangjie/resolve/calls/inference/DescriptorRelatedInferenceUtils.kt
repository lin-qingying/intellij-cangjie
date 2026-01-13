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

package org.cangnova.cangjie.resolve.calls.inference

import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.resolve.calls.components.PostponedArgumentsAnalyzerContext
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.resolve.calls.model.CallableReferenceCangJieCallArgument
import org.cangnova.cangjie.resolve.calls.model.CangJieCallArgument
import org.cangnova.cangjie.resolve.calls.model.LHSResult
import org.cangnova.cangjie.resolve.calls.model.SubCangJieCallArgument
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.model.TypeSystemInferenceExtensionContext


fun CallableDescriptor.substituteAndApproximateTypes(
    substitutor: ComposableTypeSubstitutor,
    typeApproximator: TypeApproximator?,
    positionDependentApproximation: Boolean = false
): CallableDescriptor {
    if (substitutor.isEmpty) return this

    // 创建一个组合替换器，先替换再近似
    val substitutorWithApproximation = if (typeApproximator != null) {
        // 使用 andThen 在替换后应用类型近似
        val function = SubstitutorFunction { constructor ->
            substitutor.substituteByConstructor(constructor)?.let { substitutedType ->
                typeApproximator.approximateTo(
                    substitutedType,
                    TypeApproximatorConfiguration.FinalApproximationAfterResolutionAndInference,
                    !positionDependentApproximation
                )
            }
        }
        ComposableTypeSubstitutor.create(function)
    } else {
        substitutor
    }

    return substitute(substitutorWithApproximation) ?: this
}

fun ConstraintStorage.buildResultingSubstitutor(
    context: TypeSystemInferenceExtensionContext,
    transformTypeVariablesToErrorTypes: Boolean = true
): ComposableTypeSubstitutor {
    return buildAbstractResultingSubstitutor(context, transformTypeVariablesToErrorTypes) as ComposableTypeSubstitutor
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
