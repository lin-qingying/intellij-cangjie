package com.linqingying.cangjie.psi.stubs.impl

import com.linqingying.cangjie.descriptors.DescriptorVisibility
import com.linqingying.cangjie.psi.CjPackageDirective
import com.linqingying.cangjie.psi.stubs.CangJiePackageDirectiveStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.psi.stubs.StubElement

class CangJiePackageDirectiveStubImpl
    (parent: StubElement<*>, private val visibility: DescriptorVisibility)

    : CangJieStubBaseImpl<CjPackageDirective>(parent, CjStubElementTypes.PACKAGE_DIRECTIVE),
    CangJiePackageDirectiveStub {
    override fun getModifierVisibility(): DescriptorVisibility {
        return visibility
    }
}
