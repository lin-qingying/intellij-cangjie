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

package org.cangnova.cangjie.resolve.calls.components

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.descriptors.ModuleDescriptor
import org.cangnova.cangjie.resolve.calls.components.candidate.ResolutionCandidate
import org.cangnova.cangjie.resolve.calls.model.CangJieCallArgument
import org.cangnova.cangjie.resolve.calls.model.ResolvedCallArgument
import org.cangnova.cangjie.resolve.calls.results.*
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner
import org.cangnova.cangjie.utils.CancellationChecker
import java.util.HashMap

class OverloadingConflictResolver(
    builtIns: CangJieBuiltIns,
    module: ModuleDescriptor,
    specificityComparator: TypeSpecificityComparator,
    platformOverloadsSpecificityComparator: PlatformOverloadsSpecificityComparator,
    cancellationChecker: CancellationChecker,
    statelessCallbacks: CangJieResolutionStatelessCallbacks,
    cangjieTypeRefiner: CangJieTypeRefiner,
) : AbstractOverloadingConflictResolver<ResolutionCandidate>(
    builtIns,
    module,
    specificityComparator,
    platformOverloadsSpecificityComparator,
    cancellationChecker,
    {
        // todo investigate
        it.resolvedCall.candidateDescriptor
    },
    Companion::createFlatSignature,
    { it.variableCandidateIfInvoke },
    { statelessCallbacks.isDescriptorFromSource(it) },
    { it.resolvedCall.hasSamConversion },
    cangjieTypeRefiner,
) {

    companion object {
        private fun createFlatSignature(candidate: ResolutionCandidate): FlatSignature<ResolutionCandidate> {
            val resolvedCall = candidate.resolvedCall
            // 默认启用：消除外部类型参数的歧义
            val isEliminationAmbiguitiesWithExternalTypeParametersEnabled = true
            // 默认启用：消除继承的SAM接口上的歧义
            val isEliminationAmbiguitiesOnInheritedSamInterfacesEnabled = true
            val descriptor = if (isEliminationAmbiguitiesWithExternalTypeParametersEnabled) {
                resolvedCall.candidateDescriptor
            } else {
                resolvedCall.candidateDescriptor.original
            }
            val valueParameters = descriptor.valueParameters

            var numDefaults = 0
            val valueArgumentToParameterType = HashMap<CangJieCallArgument, TypeWithConversion>()
            for ((valueParameter, resolvedValueArgument) in resolvedCall.argumentMappingByOriginal) {
                if (resolvedValueArgument is ResolvedCallArgument.DefaultArgument) {
                    numDefaults++
                } else {
                    val originalValueParameter = valueParameters[valueParameter.index]
                    for (valueArgument in resolvedValueArgument.arguments) {
                        val originalType =
                            candidate.resolvedCall.argumentsWithConversion[valueArgument]?.originalParameterType
                        val resultType =
                            candidate.resolvedCall.argumentsWithConversion[valueArgument]?.convertedTypeByOriginParameter
                                ?: valueArgument.getExpectedType(
                                    originalValueParameter,
                                    candidate.callComponents.languageVersionSettings
                                )
                        valueArgumentToParameterType[valueArgument] = TypeWithConversion(
                            resultType,
                            if (isEliminationAmbiguitiesOnInheritedSamInterfacesEnabled) originalType else null
                        )
                    }
                }
            }

            return FlatSignature.create(
                candidate,
                descriptor,
                numDefaults,
                parameterTypes = resolvedCall.atom.argumentsInParenthesis.map { valueArgumentToParameterType[it] } +
                        listOfNotNull(resolvedCall.atom.externalArgument?.let { valueArgumentToParameterType[it] })
            )

        }
    }
}
