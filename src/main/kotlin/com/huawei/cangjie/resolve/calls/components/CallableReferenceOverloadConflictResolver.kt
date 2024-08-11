package com.huawei.cangjie.resolve.calls.components

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.huawei.cangjie.resolve.calls.components.candidate.CallableReferenceResolutionCandidate
import com.huawei.cangjie.resolve.calls.inference.components.ConstraintInjector
import com.huawei.cangjie.resolve.calls.results.*
import com.huawei.cangjie.types.checker.CangJieTypeRefiner
import com.huawei.cangjie.utils.CancellationChecker


class CallableReferenceOverloadConflictResolver(
    builtIns: CangJieBuiltIns,
    module: ModuleDescriptor,
    specificityComparator: TypeSpecificityComparator,
    platformOverloadsSpecificityComparator: PlatformOverloadsSpecificityComparator,
    cancellationChecker: CancellationChecker,
    statelessCallbacks: CangJieResolutionStatelessCallbacks,
    constraintInjector: ConstraintInjector,
    cangjieTypeRefiner: CangJieTypeRefiner,
) : OverloadingConflictResolver<CallableReferenceResolutionCandidate>(
    builtIns,
    module,
    specificityComparator,
    platformOverloadsSpecificityComparator,
    cancellationChecker,
    { it.candidate },
    { statelessCallbacks.createConstraintSystemForOverloadResolution(constraintInjector, builtIns) },
    Companion::createFlatSignature,
    { null },
    { statelessCallbacks.isDescriptorFromSource(it) },
    null,
    cangjieTypeRefiner,
)
{

    companion object {
        private fun createFlatSignature(candidate: CallableReferenceResolutionCandidate) =
            FlatSignature.createFromReflectionType(
                candidate, candidate.candidate, candidate.numDefaults, hasBoundExtensionReceiver = candidate.extensionReceiver != null,
                candidate.reflectionCandidateType
            )
    }
}
