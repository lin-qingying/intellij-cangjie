package com.huawei.cangjie.psi.stubs.impl

import com.huawei.cangjie.descriptors.DescriptorVisibility
import com.huawei.cangjie.psi.CjPackageDirective
import com.huawei.cangjie.psi.stubs.CangJiePackageDirectiveStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.psi.stubs.StubElement

class CangJiePackageDirectiveStubImpl
    (parent: StubElement<*>, private val visibility: DescriptorVisibility)

    : CangJieStubBaseImpl<CjPackageDirective>(parent, CjStubElementTypes.PACKAGE_DIRECTIVE),
    CangJiePackageDirectiveStub {
    override fun getModifierVisibility(): DescriptorVisibility {
        return visibility
    }
}
