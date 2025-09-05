package org.cangnova.cangjie.psi.stubs.impl

import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjScript
import org.cangnova.cangjie.psi.stubs.CangJieScriptStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef

class CangJieScriptStubImpl(
    parent: StubElement<out PsiElement>?,
    private val _fqName: StringRef?,
) : CangJieStubBaseImpl<CjScript>(parent, CjStubElementTypes.CJ_SCRIPT), CangJieScriptStub {
    override fun getName(): String = getFqName().shortName().asString()

    override fun getFqName(): FqName = FqName(StringRef.toString(_fqName)!!)
}
