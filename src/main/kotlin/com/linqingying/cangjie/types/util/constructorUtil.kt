package com.linqingying.cangjie.types.util

import com.linqingying.cangjie.descriptors.ClassConstructorDescriptor


fun ClassConstructorDescriptor.getConstructorNameByPrimaryClassDefinition(): String {
    return this.constructedClass.name.asString()
}
