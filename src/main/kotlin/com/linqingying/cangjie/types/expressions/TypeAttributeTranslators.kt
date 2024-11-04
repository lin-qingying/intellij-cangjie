package com.linqingying.cangjie.types.expressions

import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.descriptors.annotations.Annotations
import com.linqingying.cangjie.types.TypeAttributeTranslator
import com.linqingying.cangjie.types.TypeAttributes
import com.linqingying.cangjie.types.TypeConstructor


class TypeAttributeTranslators(val translators: List<TypeAttributeTranslator>) {
    fun toAttributes(
        annotations: Annotations,
        typeConstructor: TypeConstructor,
        containingDeclaration: DeclarationDescriptor? = null
    ): TypeAttributes {
        val translated = translators.map { translator ->
            translator.toAttributes(annotations, typeConstructor, containingDeclaration)
        }.flatten()
        return TypeAttributes.create(translated)
    }

    fun toAnnotations(attributes: TypeAttributes): Annotations {
        val translated = translators.map { translator ->
            translator.toAnnotations(attributes)
        }.flatten()
        return Annotations.create(translated)
    }
}
