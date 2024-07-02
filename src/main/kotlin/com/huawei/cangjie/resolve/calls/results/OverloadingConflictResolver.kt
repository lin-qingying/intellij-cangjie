package com.huawei.cangjie.resolve.calls.results

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.huawei.cangjie.types.checker.CangJieTypeRefiner
import com.huawei.cangjie.utils.CancellationChecker


open class OverloadingConflictResolver<C : Any>(
    private val builtIns: CangJieBuiltIns,
    private val module: ModuleDescriptor,
//    private val specificityComparator: TypeSpecificityComparator,
//    private val platformOverloadsSpecificityComparator: PlatformOverloadsSpecificityComparator,
    private val cancellationChecker: CancellationChecker,
    private val getResultingDescriptor: (C) -> CallableDescriptor,
//    private val createEmptyConstraintSystem: () -> SimpleConstraintSystem,
//    private val createFlatSignature: (C) -> FlatSignature<C>,
    private val getVariableCandidates: (C) -> C?, // for variable WithInvoke
    private val isFromSources: (CallableDescriptor) -> Boolean,
    private val hasSAMConversion: ((C) -> Boolean)?,
    private val cangjieTypeRefiner: CangJieTypeRefiner,
)
