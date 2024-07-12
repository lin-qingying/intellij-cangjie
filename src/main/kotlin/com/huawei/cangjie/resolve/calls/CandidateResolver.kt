package com.huawei.cangjie.resolve.calls

import com.huawei.cangjie.builtins.ReflectionTypes
import com.huawei.cangjie.resolve.UpperBoundChecker
import com.huawei.cangjie.resolve.calls.checkers.AdditionalTypeChecker
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import com.huawei.cangjie.resolve.calls.smartcasts.SmartCastManager

class CandidateResolver(
    private val argumentTypeResolver: ArgumentTypeResolver,
    private val genericCandidateResolver: GenericCandidateResolver,
    private val reflectionTypes: ReflectionTypes,
    private val additionalTypeCheckers: Iterable<AdditionalTypeChecker>,
    private val smartCastManager: SmartCastManager,
    private val dataFlowValueFactory: DataFlowValueFactory,
    private val upperBoundChecker: UpperBoundChecker
){

}
