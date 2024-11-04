package com.linqingying.cangjie.resolve.calls.model

import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.builtins.ReflectionTypes
import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.descriptors.FunctionDescriptor
import com.linqingying.cangjie.incremental.components.LookupTracker
import com.linqingying.cangjie.resolve.calls.components.ArgumentsToParametersMapper
import com.linqingying.cangjie.resolve.calls.components.CangJieResolutionStatelessCallbacks
import com.linqingying.cangjie.resolve.calls.components.TypeArgumentsToParametersMapper
import com.linqingying.cangjie.resolve.calls.inference.components.ConstraintInjector

import com.linqingying.cangjie.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import com.linqingying.cangjie.types.TypeSubstitutor
import com.linqingying.cangjie.types.checker.CangJieTypeRefiner
import com.linqingying.cangjie.types.checker.NewCangJieTypeChecker


class CangJieCallComponents(
    val statelessCallbacks: CangJieResolutionStatelessCallbacks,
    val argumentsToParametersMapper: ArgumentsToParametersMapper,
    val typeArgumentsToParametersMapper: TypeArgumentsToParametersMapper,
    val constraintInjector: ConstraintInjector,
    val reflectionTypes: ReflectionTypes,
    val builtIns: CangJieBuiltIns,
    val languageVersionSettings: LanguageVersionSettings,
//    val samConversionOracle: SamConversionOracle,
//    val samConversionResolver: SamConversionResolver,
    val cangjieTypeChecker: NewCangJieTypeChecker,
    val lookupTracker: LookupTracker,
    val cangjieTypeRefiner: CangJieTypeRefiner,
//    val callableReferenceArgumentResolver: CallableReferenceArgumentResolver
)

class GivenCandidate(
    val descriptor: FunctionDescriptor,
    val dispatchReceiver: ReceiverValueWithSmartCastInfo?,
    val knownTypeParametersResultingSubstitutor: TypeSubstitutor?
)
