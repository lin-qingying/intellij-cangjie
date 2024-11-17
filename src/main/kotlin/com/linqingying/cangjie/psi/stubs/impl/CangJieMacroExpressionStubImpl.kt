package com.linqingying.cangjie.psi.stubs.impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import com.linqingying.cangjie.psi.CjAnnotationEntry
import com.linqingying.cangjie.psi.CjMacroExpression
import com.linqingying.cangjie.psi.stubs.CangJieAnnotationEntryStub
import com.linqingying.cangjie.psi.stubs.CangJieMacroExpressionStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes

class CangJieMacroExpressionStubImpl (
    parent: StubElement<out PsiElement>?,
    private val shortName: StringRef?,
    private val hasValueArguments: Boolean,
//    val valueArguments: Map<Name, ConstantValue<*>>?
):CangJieStubBaseImpl<CjMacroExpression>(parent, CjStubElementTypes.MACRO_EXPRESSION), CangJieMacroExpressionStub {

    override fun getShortName() = shortName?.string

    override fun hasValueArguments() = hasValueArguments
}
