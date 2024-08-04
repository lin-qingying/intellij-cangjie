package com.linqingying.cangjie.psi.stubs.impl

import com.linqingying.cangjie.psi.CjImportDirectiveItem
import com.linqingying.cangjie.psi.stubs.CangJieImportDirectiveItemStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.psi.stubs.StubElement

class CangJieImportDirectiveItemStubImpl(
    parent: StubElement<*>,

    ) :
    CangJieStubBaseImpl<CjImportDirectiveItem>(parent, CjStubElementTypes.IMPORT_DIRECTIVE_ITEM),
    CangJieImportDirectiveItemStub
