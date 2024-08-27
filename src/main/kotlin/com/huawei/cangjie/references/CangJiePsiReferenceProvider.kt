package com.huawei.cangjie.references

import com.huawei.cangjie.psi.CjElement
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.util.containers.MultiMap


interface CangJiePsiReferenceProvider {
    fun getReferencesByElement(element: PsiElement): Array<PsiReference>
}

interface CangJieReferenceProviderContributor {
    fun registerReferenceProviders(registrar: CangJiePsiReferenceRegistrar)

    companion object {
        fun getInstance(project: Project): CangJieReferenceProviderContributor =
            project.getService(CangJieReferenceProviderContributor::class.java)
    }
}


class CangJiePsiReferenceRegistrar {
    val providers: MultiMap<Class<out PsiElement>, CangJiePsiReferenceProvider> = MultiMap(LinkedHashMap())

    inline fun <reified E : CjElement> registerProvider(crossinline factory: (E) -> PsiReference?) {
        registerMultiProvider<E> { element ->
            factory(element)?.let { reference -> arrayOf(reference) } ?: PsiReference.EMPTY_ARRAY
        }
    }

    inline fun <reified E : CjElement> registerMultiProvider(crossinline factory: (E) -> Array<PsiReference>) {
        val provider: CangJiePsiReferenceProvider = object : CangJiePsiReferenceProvider {
            override fun getReferencesByElement(element: PsiElement): Array<PsiReference> {
                return factory(element as E)
            }
        }

        registerMultiProvider(E::class.java, provider)
    }

    fun registerMultiProvider(klass: Class<out PsiElement>, provider: CangJiePsiReferenceProvider) {
        providers.putValue(klass, provider)
    }
}
//
//class CangJieFirReferenceContributor : CangJieReferenceProviderContributor {
//    override fun registerReferenceProviders(registrar: CangJiePsiReferenceRegistrar) {
//        with(registrar) {
//            registerProvider(factory = ::CjFirForLoopInReference)
//            registerProvider(factory = ::CjFirInvokeFunctionReference)
//            registerProvider(factory = ::CjFirPropertyDelegationMethodsReference)
//            registerProvider(factory = ::CjFirDestructuringDeclarationReference)
//            registerProvider(factory = ::CjFirArrayAccessReference)
//            registerProvider(factory = ::CjFirConstructorDelegationReference)
//            registerProvider(factory = ::CjFirCollectionLiteralReference)
//            registerProvider(factory = ::CjFirCDOCReference)
//
//            registerMultiProvider<CjSimpleNameExpression> { nameReferenceExpression ->
//                when (nameReferenceExpression.readWriteAccess(useResolveForReadWrite = true)) {
//                    ReferenceAccess.READ -> arrayOf(CjFirSimpleNameReference(nameReferenceExpression, isRead = true))
//                    ReferenceAccess.WRITE -> arrayOf(CjFirSimpleNameReference(nameReferenceExpression, isRead = false))
//                    ReferenceAccess.READ_WRITE -> arrayOf(
//                        CjFirSimpleNameReference(nameReferenceExpression, isRead = true),
//                        CjFirSimpleNameReference(nameReferenceExpression, isRead = false),
//                    )
//                }
//            }
//
//
//        }
//    }
//}
