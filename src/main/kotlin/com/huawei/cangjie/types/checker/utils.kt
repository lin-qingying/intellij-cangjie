package com.huawei.cangjie.types.checker

import com.huawei.cangjie.descriptors.TypeParameterDescriptor
import com.huawei.cangjie.types.TypeConstructor

interface NewTypeVariableConstructor : TypeConstructor {
    val originalTypeParameter: TypeParameterDescriptor?
}
