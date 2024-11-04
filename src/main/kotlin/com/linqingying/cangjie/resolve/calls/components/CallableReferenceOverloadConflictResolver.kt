package com.linqingying.cangjie.resolve.calls.components

import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.descriptors.ModuleDescriptor
import com.linqingying.cangjie.resolve.calls.components.candidate.CallableReferenceResolutionCandidate
import com.linqingying.cangjie.resolve.calls.inference.components.ConstraintInjector
import com.linqingying.cangjie.resolve.calls.results.*
import com.linqingying.cangjie.types.checker.CangJieTypeRefiner
import com.linqingying.cangjie.utils.CancellationChecker


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
