package com.huawei.cangjie.resolve.calls.components

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.huawei.cangjie.resolve.calls.components.candidate.ResolutionCandidate
import com.huawei.cangjie.resolve.calls.results.OverloadingConflictResolver
import com.huawei.cangjie.types.checker.CangJieTypeRefiner
import com.huawei.cangjie.utils.CancellationChecker

class NewOverloadingConflictResolver(
    builtIns: CangJieBuiltIns,
    module: ModuleDescriptor,
//    specificityComparator: TypeSpecificityComparator,
//    platformOverloadsSpecificityComparator: PlatformOverloadsSpecificityComparator,
    cancellationChecker: CancellationChecker,
    statelessCallbacks: CangJieResolutionStatelessCallbacks,
//    constraintInjector: ConstraintInjector,
    cangjieTypeRefiner: CangJieTypeRefiner,
) : OverloadingConflictResolver<ResolutionCandidate>(
    builtIns,
    module,
//    specificityComparator,
//    platformOverloadsSpecificityComparator,
    cancellationChecker,
    {
        // todo investigate
        it.resolvedCall.candidateDescriptor
    },
//    { statelessCallbacks.createConstraintSystemForOverloadResolution(constraintInjector, builtIns) },
//    Companion::createFlatSignature,
    { it.variableCandidateIfInvoke },
    { statelessCallbacks.isDescriptorFromSource(it) },
    { it.resolvedCall.hasSamConversion },
    cangjieTypeRefiner,
)
