package com.huawei.cangjie.resolve.calls.inference

import com.huawei.cangjie.resolve.calls.components.PostponedArgumentsAnalyzerContext
import com.huawei.cangjie.resolve.calls.inference.components.NewTypeSubstitutor
import com.huawei.cangjie.resolve.calls.inference.model.ConstraintStorage
import com.huawei.cangjie.resolve.calls.model.CallableReferenceCangJieCallArgument
import com.huawei.cangjie.resolve.calls.model.CangJieCallArgument
import com.huawei.cangjie.resolve.calls.model.LHSResult
import com.huawei.cangjie.resolve.calls.model.SubCangJieCallArgument
import com.huawei.cangjie.types.model.TypeSystemInferenceExtensionContext

fun ConstraintStorage.buildResultingSubstitutor(
    context: TypeSystemInferenceExtensionContext,
    transformTypeVariablesToErrorTypes: Boolean = true
): NewTypeSubstitutor {
    return buildAbstractResultingSubstitutor(context, transformTypeVariablesToErrorTypes) as NewTypeSubstitutor
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
