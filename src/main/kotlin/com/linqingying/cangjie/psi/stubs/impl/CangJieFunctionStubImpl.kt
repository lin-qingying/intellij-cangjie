package com.linqingying.cangjie.psi.stubs.impl

import com.linqingying.cangjie.contracts.description.CjContractDescriptionElement
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.psi.CjFunctionImpl
import com.linqingying.cangjie.psi.stubs.CangJieFunctionStub
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import com.linqingying.cangjie.psi.stubs.CangJieFunctionForExtendStub
import java.io.IOException


  class CangJieFunctionStubImpl(
    parent: StubElement<out PsiElement>?,
    element: IStubElementType<*, *>,
    private val nameRef: StringRef?,
    private val isTopLevel: Boolean,
    private val fqName: FqName?,
    private val isExtension: Boolean,
    private val hasBlockBody: Boolean,
    private val hasBody: Boolean,
    private val hasTypeParameterListBeforeFunctionName: Boolean,
    val contract: List<CjContractDescriptionElement<CangJieTypeBean, Nothing?>>?,
    val origin: CangJieStubOrigin?
) : CangJieStubBaseImpl<CjFunctionImpl>(parent, element), CangJieFunctionStub {
    init {
        if (isTopLevel && fqName == null) {
            throw IllegalArgumentException("fqName shouldn't be null for top level functions")
        }
    }

    override fun getFqName() = fqName

    override fun getName() = StringRef.toString(nameRef)
    override fun isTopLevel() = isTopLevel
    override fun isExtension() = isExtension
    override fun hasBlockBody() = hasBlockBody
    override fun hasBody() = hasBody
    override fun hasTypeParameterListBeforeFunctionName() = hasTypeParameterListBeforeFunctionName

    companion object {

    }

}

class CangJieFunctionForExtendStubImpl(
    parent: StubElement<out PsiElement>?,
    element: IStubElementType<*, *>,
    private val nameRef: StringRef?,
    private val isTopLevel: Boolean,
    private val fqName: FqName?,
    private val isExtension: Boolean,
    private val hasBlockBody: Boolean,
    private val hasBody: Boolean,
    private val hasTypeParameterListBeforeFunctionName: Boolean,
    val contract: List<CjContractDescriptionElement<CangJieTypeBean, Nothing?>>?,
    val origin: CangJieStubOrigin?
) : CangJieStubBaseImpl<CjFunctionImpl>(parent, element), CangJieFunctionForExtendStub {
    init {
        if (isTopLevel && fqName == null) {
            throw IllegalArgumentException("fqName shouldn't be null for top level functions")
        }
    }

    override fun getFqName() = fqName

    override fun getName() = StringRef.toString(nameRef)
    override fun isTopLevel() = isTopLevel
    override fun isExtension() = isExtension
    override fun hasBlockBody() = hasBlockBody
    override fun hasBody() = hasBody
    override fun hasTypeParameterListBeforeFunctionName() = hasTypeParameterListBeforeFunctionName

    companion object {

    }

}
