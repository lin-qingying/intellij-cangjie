package com.huawei.cangjie.resolve.calls.util

import com.huawei.cangjie.builtins.isBuiltinExtensionFunctionalType
import com.huawei.cangjie.builtins.isBuiltinFunctionalType
import com.huawei.cangjie.descriptors.FunctionDescriptor
import com.huawei.cangjie.descriptors.SourceElement
import com.huawei.cangjie.descriptors.ValueParameterDescriptor
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.descriptors.impl.ValueParameterDescriptorImpl
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.TypeProjection


fun createValueParametersForInvokeInFunctionType(
    functionDescriptor: FunctionDescriptor, parameterTypes: List<TypeProjection>
): List<ValueParameterDescriptor> {
    return parameterTypes.mapIndexed { i, typeProjection ->
        ValueParameterDescriptorImpl(
            functionDescriptor, null, i, Annotations.EMPTY,
            Name.identifier("p${i + 1}"), false, typeProjection.type,
            /* declaresDefaultValue = */ false,
            /* isCrossinline = */
//            false,
//            /* isNoinline = */ false,
//            null,
            SourceElement.NO_SOURCE
        )
    }
}

fun getValueParametersCountFromFunctionType(type: CangJieType): Int {
    assert(type.isBuiltinFunctionalType) { "Not a function type: $type" }
    // Function type arguments = receiver? + parameters + return-type
    return type.arguments.size - (if (type.isBuiltinExtensionFunctionalType) 1 else 0) - 1
}
