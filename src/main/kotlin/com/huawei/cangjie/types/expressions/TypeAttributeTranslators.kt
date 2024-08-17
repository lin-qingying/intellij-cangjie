package com.huawei.cangjie.types.expressions

import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.types.TypeAttributeTranslator
import com.huawei.cangjie.types.TypeAttributes
import com.huawei.cangjie.types.TypeConstructor


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
