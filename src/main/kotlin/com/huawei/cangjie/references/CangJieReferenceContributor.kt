package com.huawei.cangjie.references

import com.huawei.cangjie.ide.references.CangJiePsiReferenceRegistrar
import com.huawei.cangjie.ide.references.CangJieReferenceProviderContributor


class CangJieReferenceContributor : CangJieReferenceProviderContributor {
    override fun registerReferenceProviders(registrar: CangJiePsiReferenceRegistrar) {
        with(registrar) {
            registerProvider(factory =  ::CjSimpleNameReference)
//
//
//
//            registerMultiProvider<CjNameReferenceExpression> { nameReferenceExpression ->
//
//                if (nameReferenceExpression.getReferencedNameElementType() != CjTokens.IDENTIFIER) {
//                    return@registerMultiProvider PsiReference.EMPTY_ARRAY
//                }
//                if (nameReferenceExpression.parents.any { it is CjImportDirective || it is CjPackageDirective || it is CjUserType }) {
//                    return@registerMultiProvider PsiReference.EMPTY_ARRAY
//                }
//                when (nameReferenceExpression.readWriteAccess(useResolveForReadWrite = false)) {
//                    ReferenceAccess.READ ->
//                        arrayOf(CangJieSyntheticPropertyAccessorReference(nameReferenceExpression, getter = true))
//
//                    ReferenceAccess.   WRITE -> arrayOf(CangJieSyntheticPropertyAccessorReference(nameReferenceExpression, getter = false))
//
//                    ReferenceAccess.  READ_WRITE -> arrayOf(
//                        CangJieSyntheticPropertyAccessorReference(nameReferenceExpression, getter = true),
//                        CangJieSyntheticPropertyAccessorReference(nameReferenceExpression, getter = false)
//                    )
//                }
//            }
        }
    }
}
