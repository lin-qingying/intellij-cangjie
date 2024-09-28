package com.huawei.cangjie.references

import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.CjImportDirective
import com.huawei.cangjie.psi.CjNameReferenceExpression
import com.huawei.cangjie.psi.CjPackageDirective
import com.huawei.cangjie.psi.CjUserType
import com.huawei.cangjie.psi.psiUtil.parents
import com.intellij.psi.PsiReference


class CangJieReferenceContributor : CangJieReferenceProviderContributor {
    override fun registerReferenceProviders(registrar: CangJiePsiReferenceRegistrar) {
        with(registrar) {
            registerProvider(factory = ::CjSimpleNameReference)
            registerProvider(factory = ::CjInvokeFunctionReference)
            registerProvider(factory = ::CjConstructorDelegationReference)
//            registerProvider(factory = ::CjPatternEnumReference)
//
            registerMultiProvider<CjNameReferenceExpression> { nameReferenceExpression ->

                if (nameReferenceExpression.getReferencedNameElementType() != CjTokens.IDENTIFIER) {
                    return@registerMultiProvider PsiReference.EMPTY_ARRAY
                }
                if (nameReferenceExpression.parents.any { it is CjImportDirective || it is CjPackageDirective || it is CjUserType }) {
                    return@registerMultiProvider PsiReference.EMPTY_ARRAY
                }
                when (nameReferenceExpression.readWriteAccess(useResolveForReadWrite = false)) {
                    ReferenceAccess.READ ->
                        arrayOf(CangJieSyntheticPropertyAccessorReference(nameReferenceExpression, getter = true))

                    ReferenceAccess.WRITE -> arrayOf(
                        CangJieSyntheticPropertyAccessorReference(
                            nameReferenceExpression,
                            getter = false
                        )
                    )

                    ReferenceAccess.READ_WRITE -> arrayOf(
                        CangJieSyntheticPropertyAccessorReference(nameReferenceExpression, getter = true),
                        CangJieSyntheticPropertyAccessorReference(nameReferenceExpression, getter = false)
                    )
                }
            }
        }
    }
}
