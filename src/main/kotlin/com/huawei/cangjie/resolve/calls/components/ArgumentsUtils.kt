package com.huawei.cangjie.resolve.calls.components

import com.huawei.cangjie.descriptors.ParameterDescriptor
import com.huawei.cangjie.descriptors.ValueParameterDescriptor
import com.huawei.cangjie.resolve.calls.model.CangJieCallArgument


internal fun CangJieCallArgument.getExpectedType(
    parameter: ParameterDescriptor
//                                                 , languageVersionSettings: LanguageVersionSettings
) =
//    if (
//        this.isSpread
////        ||
////        this.isArrayAssignedAsNamedArgumentInAnnotation(parameter, languageVersionSettings) ||
////        this.isArrayAssignedAsNamedArgumentInFunction(parameter, languageVersionSettings)
//    ) {
        parameter.type.unwrap()
//    } else {
//        (parameter as? ValueParameterDescriptor)?.varargElementType?.unwrap() ?: parameter.type.unwrap()
//    }
