package com.huawei.cangjie.references

import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.psi.CjReferenceExpression
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.impl.source.resolve.ResolveCache


interface CjReference : PsiPolyVariantReference {
    val resolver: ResolveCache.PolyVariantResolver<CjReference>
    override fun getElement(): CjElement


    val resolvesByNames: Collection<Name>

}



abstract class CjSimpleReference<T : CjReferenceExpression>(expression: T) : AbstractCjReference<T>(expression)
//interface CjReference : PsiPolyVariantReference {
//
//    override fun getElement(): CjElement
//
//    override fun resolve(): CjElement?
//
//    fun multiResolve(): List<CjElement>
//}
//
//
//
//
