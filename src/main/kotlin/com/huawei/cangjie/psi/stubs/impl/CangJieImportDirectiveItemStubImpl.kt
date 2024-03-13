package com.huawei.cangjie.psi.stubs.impl

import com.huawei.cangjie.psi.CjImportDirectiveItem
import com.huawei.cangjie.psi.stubs.CangJieImportDirectiveItemStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.psi.stubs.StubElement

class CangJieImportDirectiveItemStubImpl(
    parent: StubElement<*>,

    ) :
    CangJieStubBaseImpl<CjImportDirectiveItem>(parent, CjStubElementTypes.IMPORT_DIRECTIVE_ITEM),
    CangJieImportDirectiveItemStub