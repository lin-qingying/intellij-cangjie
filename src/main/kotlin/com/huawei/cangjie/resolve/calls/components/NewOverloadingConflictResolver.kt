package com.huawei.cangjie.resolve.calls.components

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.config.LanguageFeature
import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.huawei.cangjie.resolve.calls.components.candidate.ResolutionCandidate
import com.huawei.cangjie.resolve.calls.inference.components.ConstraintInjector
import com.huawei.cangjie.resolve.calls.model.CangJieCallArgument
import com.huawei.cangjie.resolve.calls.results.*
import com.huawei.cangjie.types.checker.CangJieTypeRefiner
import com.huawei.cangjie.utils.CancellationChecker
import java.util.HashMap
import com.huawei.cangjie.resolve.calls.model.ResolvedCallArgument
class NewOverloadingConflictResolver(
    builtIns: CangJieBuiltIns,
    module: ModuleDescriptor,
    specificityComparator: TypeSpecificityComparator,
    platformOverloadsSpecificityComparator: PlatformOverloadsSpecificityComparator,
    cancellationChecker: CancellationChecker,
    statelessCallbacks: CangJieResolutionStatelessCallbacks,
    constraintInjector: ConstraintInjector,
    cangjieTypeRefiner: CangJieTypeRefiner,
) : OverloadingConflictResolver<ResolutionCandidate>(
    builtIns,
    module,
    specificityComparator,
    platformOverloadsSpecificityComparator,
    cancellationChecker,
    {
        // todo investigate
        it.resolvedCall.candidateDescriptor
    },
    { statelessCallbacks.createConstraintSystemForOverloadResolution(constraintInjector, builtIns) },
    Companion::createFlatSignature,
    { it.variableCandidateIfInvoke },
    { statelessCallbacks.isDescriptorFromSource(it) },
    { it.resolvedCall.hasSamConversion },
    cangjieTypeRefiner,
){

    companion object {
        private fun createFlatSignature(candidate: ResolutionCandidate): FlatSignature<ResolutionCandidate> {
            val resolvedCall = candidate.resolvedCall
            val isEliminationAmbiguitiesWithExternalTypeParametersEnabled =
                candidate.callComponents.languageVersionSettings.supportsFeature(LanguageFeature.EliminateAmbiguitiesWithExternalTypeParameters)
            val isEliminationAmbiguitiesOnInheritedSamInterfacesEnabled =
                candidate.callComponents.languageVersionSettings.supportsFeature(LanguageFeature.EliminateAmbiguitiesOnInheritedSamInterfaces)
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
                        val originalType = candidate.resolvedCall.argumentsWithConversion[valueArgument]?.originalParameterType
                        val resultType = candidate.resolvedCall.argumentsWithConversion[valueArgument]?.convertedTypeByOriginParameter
                            ?: valueArgument.getExpectedType(originalValueParameter, candidate.callComponents.languageVersionSettings)
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
