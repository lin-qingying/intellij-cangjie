package com.huawei.cangjie.resolve

import com.huawei.cangjie.descriptors.DeclarationDescriptorWithVisibility
import com.huawei.cangjie.types.CangJieType

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
