package com.linqingying.cangjie.psi.stubs

import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.psi.stubs.StubElement

val StubBasedPsiElementBase<*>.containingFileStub: PsiFileStub<*>?
    get() {
        val stub = this.greenStub ?: return null
        return stub.containingFileStub
    }

val StubElement<*>.containingCangJieFileStub: CangJieFileStub?
    get() = containingFileStub as? CangJieFileStub
val StubBasedPsiElementBase<*>.containingCangJieFileStub: PsiFileStub<*>?
    get() = containingFileStub as? CangJieFileStub
