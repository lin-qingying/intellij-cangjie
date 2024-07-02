package com.huawei.cangjie.resolve.calls.model

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.builtins.ReflectionTypes
import com.huawei.cangjie.descriptors.FunctionDescriptor
import com.huawei.cangjie.incremental.components.LookupTracker
import com.huawei.cangjie.resolve.calls.components.CangJieResolutionStatelessCallbacks
import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import com.huawei.cangjie.types.TypeSubstitutor
import com.huawei.cangjie.types.checker.CangJieTypeRefiner
import com.huawei.cangjie.types.checker.NewCangJieTypeChecker


class CangJieCallComponents(
    val statelessCallbacks: CangJieResolutionStatelessCallbacks,
//    val argumentsToParametersMapper: ArgumentsToParametersMapper,
//    val typeArgumentsToParametersMapper: TypeArgumentsToParametersMapper,
//    val constraintInjector: ConstraintInjector,
    val reflectionTypes: ReflectionTypes,
    val builtIns: CangJieBuiltIns,
//    val languageVersionSettings: LanguageVersionSettings,
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
