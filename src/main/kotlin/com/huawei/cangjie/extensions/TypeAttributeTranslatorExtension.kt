package com.huawei.cangjie.extensions

import com.huawei.cangjie.types.DefaultTypeAttributeTranslator
import com.huawei.cangjie.types.TypeAttributeTranslator
import com.huawei.cangjie.types.expressions.TypeAttributeTranslators
import com.intellij.openapi.project.Project


interface TypeAttributeTranslatorExtension : TypeAttributeTranslator {
    companion object : ProjectExtensionDescriptor<TypeAttributeTranslatorExtension>(
        "com.huawei.cangjie.extensions.typeAttributeTranslatorExtension",
        TypeAttributeTranslatorExtension::class.java
    ) {
        val Default = TypeAttributeTranslators(listOf(DefaultTypeAttributeTranslator))

        fun createTranslators(project: Project): TypeAttributeTranslators {
            return TypeAttributeTranslators(getInstances(project) + DefaultTypeAttributeTranslator)
        }
    }
}
