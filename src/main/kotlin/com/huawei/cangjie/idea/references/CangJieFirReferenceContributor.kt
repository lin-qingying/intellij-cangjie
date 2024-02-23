package com.huawei.cangjie.idea.references

class CangJieFirReferenceContributor:CangJieReferenceProviderContributor {
    override fun registerReferenceProviders(registrar: CangJiePsiReferenceRegistrar) {
        with(registrar) {
//            registerProvider(factory = ::CjFirForLoopInReference)
//            registerProvider(factory = ::CjFirInvokeFunctionReference)
//            registerProvider(factory = ::CjFirPropertyDelegationMethodsReference)
//            registerProvider(factory = ::CjFirDestructuringDeclarationReference)
//            registerProvider(factory = ::CjFirArrayAccessReference)
//            registerProvider(factory = ::CjFirConstructorDelegationReference)
//            registerProvider(factory = ::CjFirCollectionLiteralReference)
//            registerProvider(factory = ::CjFirKDocReference)

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

//            registerProvider provider@{ element: CjValueArgument ->
//                if (element.isNamed()) return@provider null
//                val annotationEntry = element.getParentOfTypeAndBranch<CjAnnotationEntry> { valueArgumentList } ?: return@provider null
//                if (annotationEntry.valueArguments.size != 1) return@provider null
//
//                CjDefaultAnnotationArgumentReference(element)
//            }
        }
    }
}