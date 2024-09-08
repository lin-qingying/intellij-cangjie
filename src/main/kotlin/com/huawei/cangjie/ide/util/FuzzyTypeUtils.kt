package com.huawei.cangjie.ide.util

import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.TypeParameterDescriptor
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.FuzzyType


fun CallableDescriptor.fuzzyExtensionReceiverType() = extensionReceiverParameter?.type?.toFuzzyType(typeParameters)
fun CangJieType.toFuzzyType(freeParameters: Collection<TypeParameterDescriptor>) = FuzzyType(this, freeParameters)
