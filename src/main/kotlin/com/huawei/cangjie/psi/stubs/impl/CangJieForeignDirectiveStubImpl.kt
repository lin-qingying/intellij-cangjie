package com.huawei.cangjie.psi.stubs.impl

import com.huawei.cangjie.psi.CjForeignDirective
import com.huawei.cangjie.psi.stubs.CangJieForeignDirectiveStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement

class CangJieForeignDirectiveStubImpl(
    parent: StubElement<out PsiElement>?,

    ) : CangJieStubBaseImpl<CjForeignDirective>(parent, CjStubElementTypes.FOREIGN), CangJieForeignDirectiveStub