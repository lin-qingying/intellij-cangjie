package com.huawei.cangjie.resolve.calls.model

import com.huawei.cangjie.descriptors.FunctionDescriptor
import com.huawei.cangjie.descriptors.VariableDescriptor

interface VariableAsFunctionResolvedCall {
    val functionCall: ResolvedCall<FunctionDescriptor>
    val variableCall: ResolvedCall<VariableDescriptor>
}
