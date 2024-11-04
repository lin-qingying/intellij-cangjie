package com.linqingying.cangjie.resolve

import com.linqingying.cangjie.descriptors.DeclarationDescriptorWithVisibility
import com.linqingying.cangjie.types.CangJieType

interface DeclarationSignatureAnonymousTypeTransformer {
    fun transformAnonymousType(descriptor: DeclarationDescriptorWithVisibility, type: CangJieType): CangJieType?

    object Default : DeclarationSignatureAnonymousTypeTransformer {
        override fun transformAnonymousType(
            descriptor: DeclarationDescriptorWithVisibility,
            type: CangJieType
        ): CangJieType  {
            return type
        }

    }
}
