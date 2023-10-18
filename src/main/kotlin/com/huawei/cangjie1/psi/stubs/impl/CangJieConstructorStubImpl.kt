package com.huawei.cangjie1.psi.stubs.impl

import com.huawei.cangjie1.psi.CjConstructor
import com.huawei.cangjie1.psi.CjConstructorElementType
import com.huawei.cangjie1.psi.stubs.CangJieConstructorStub
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef



class CangJieConstructorStubImpl<T : CjConstructor<T>>(
    parent: StubElement<out PsiElement>?,
    elementType: CjConstructorElementType<T>,
    private val containingClassName: StringRef?,
    private val hasBody: Boolean,
    private val isDelegatedCallToThis: Boolean,
) : CangJieStubBaseImpl<T>(parent, elementType), CangJieConstructorStub<T> {
    override fun getFqName() = null
    override fun getName() = StringRef.toString(containingClassName)
    override fun isTopLevel() = false
    override fun isExtension() = false
    override fun hasBody() = hasBody
    override fun isDelegatedCallToThis() = isDelegatedCallToThis
}
