package com.linqingying.cangjie.psi.stubs.impl

import com.linqingying.cangjie.psi.CjForeignDirective
import com.linqingying.cangjie.psi.stubs.CangJieForeignDirectiveStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement

class CangJieForeignDirectiveStubImpl(
    parent: StubElement<out PsiElement>?,

    ) : CangJieStubBaseImpl<CjForeignDirective>(parent, CjStubElementTypes.FOREIGN), CangJieForeignDirectiveStub
