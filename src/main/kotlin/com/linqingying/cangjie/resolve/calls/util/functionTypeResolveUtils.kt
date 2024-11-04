package com.linqingying.cangjie.resolve.calls.util

import com.linqingying.cangjie.builtins.isBuiltinExtensionFunctionalType
import com.linqingying.cangjie.builtins.isBuiltinFunctionalType
import com.linqingying.cangjie.descriptors.FunctionDescriptor
import com.linqingying.cangjie.descriptors.SourceElement
import com.linqingying.cangjie.descriptors.ValueParameterDescriptor
import com.linqingying.cangjie.descriptors.annotations.Annotations
import com.linqingying.cangjie.descriptors.impl.ValueParameterDescriptorImpl
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.TypeProjection


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
