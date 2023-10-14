package com.huawei.cangjie.lang.core.psi.ext

import com.huawei.cangjie.ide.refactoring.CjNamesValidator

import com.huawei.cangjie.lang.core.psi.CjLifetime
import com.huawei.cangjie.lang.core.psi.CjPsiFactory
import com.huawei.cangjie.lang.core.stubs.CjLifetimeStub
import com.huawei.cangjie.lang.core.resolve.ref.CjLifetimeReferenceImpl
import com.huawei.cangjie.lang.core.resolve.ref.CjReference
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.search.SearchScope
import com.intellij.psi.stubs.IStubElementType

//声明周期
val CjLifetime.isPredefined: Boolean get() = referenceName in CjNamesValidator.RESERVED_LIFETIME_NAMES

abstract class CjLifetimeImplMixin : CjStubbedNamedElementImpl<CjLifetimeStub>, CjLifetime {

    constructor(node: ASTNode) : super(node)

    constructor(stub: CjLifetimeStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    override val referenceNameElement: PsiElement get() = nameIdentifier

    override val referenceName: String get() = greenStub?.name ?: referenceNameElement.text

    override fun getReference(): CjReference = CjLifetimeReferenceImpl(this)

    override fun getNameIdentifier(): PsiElement = quoteIdentifier

    override fun setName(name: String): PsiElement? {
        nameIdentifier.replace(CjPsiFactory(project).createQuoteIdentifier(name))
        return this
    }

    override fun getUseScope(): SearchScope =  super.getUseScope()
}

sealed class LifetimeName {

    data class Parameter(val name: String) : LifetimeName()


    object Implicit : LifetimeName()


    object Underscore : LifetimeName()


    object Static : LifetimeName()
}

val LifetimeName.isElided: Boolean
    get() = when (this) {
        LifetimeName.Implicit, LifetimeName.Underscore -> true
        is LifetimeName.Parameter, LifetimeName.Static -> false
    }

val CjLifetime?.typedName: LifetimeName
    get() {
        return when (val text = this?.referenceName) {
            null -> LifetimeName.Implicit
            "'_" -> LifetimeName.Underscore
            "'static" -> LifetimeName.Static
            else -> LifetimeName.Parameter(text)
        }
    }

val CjLifetime?.isElided: Boolean get() = typedName.isElided
