package com.linqingying.cangjie.extensions

import com.linqingying.cangjie.types.DefaultTypeAttributeTranslator
import com.linqingying.cangjie.types.TypeAttributeTranslator
import com.linqingying.cangjie.types.expressions.TypeAttributeTranslators
import com.intellij.openapi.project.Project


interface TypeAttributeTranslatorExtension : TypeAttributeTranslator {
    companion object : ProjectExtensionDescriptor<TypeAttributeTranslatorExtension>(
        "com.linqingying.cangjie.extensions.typeAttributeTranslatorExtension",
        TypeAttributeTranslatorExtension::class.java
    ) {
        val Default = TypeAttributeTranslators(listOf(DefaultTypeAttributeTranslator))

        fun createTranslators(project: Project): TypeAttributeTranslators {
            return TypeAttributeTranslators(getInstances(project) + DefaultTypeAttributeTranslator)
        }
    }
}
