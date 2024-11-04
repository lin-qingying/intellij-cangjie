package com.linqingying.cangjie.psi.stubs.impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.linqingying.cangjie.psi.CjFunctionType
import com.linqingying.cangjie.psi.stubs.CangJieFunctionTypeStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes


/**
 * @param abbreviatedType The type alias application from which this type was originally expanded. It can be used to render or navigate to
 *  the original type alias instead of the expanded type.
 */
class CangJieFunctionTypeStubImpl(
    parent: StubElement<out PsiElement>?,
    val abbreviatedType: CangJieClassTypeBean? = null,
) : CangJieStubBaseImpl<CjFunctionType>(parent, CjStubElementTypes.FUNCTION_TYPE), CangJieFunctionTypeStub
