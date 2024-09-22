package com.huawei.cangjie.types.util

import com.huawei.cangjie.descriptors.ClassConstructorDescriptor


fun ClassConstructorDescriptor.getConstructorNameByPrimaryClassDefinition(): String {
    return this.constructedClass.name.asString()
}
