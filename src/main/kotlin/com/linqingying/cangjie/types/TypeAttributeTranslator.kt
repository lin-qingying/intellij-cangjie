package com.linqingying.cangjie.types

import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.descriptors.annotations.Annotations



interface TypeAttributeTranslator {
    fun toAttributes(
        annotations: Annotations,
        typeConstructor: TypeConstructor? = null,
        containingDeclaration: DeclarationDescriptor? = null
    ): TypeAttributes

    fun toAnnotations(attributes: TypeAttributes): Annotations
}

object DefaultTypeAttributeTranslator : TypeAttributeTranslator {
    override fun toAttributes(
        annotations: Annotations,
        typeConstructor: TypeConstructor?,
        containingDeclaration: DeclarationDescriptor?
    ): TypeAttributes {
        return if (annotations.isEmpty())
            TypeAttributes.Empty else
            TypeAttributes.create(listOf(AnnotationsTypeAttribute(annotations)))
    }

    override fun toAnnotations(attributes: TypeAttributes): Annotations {
        return attributes.annotations
    }
}
