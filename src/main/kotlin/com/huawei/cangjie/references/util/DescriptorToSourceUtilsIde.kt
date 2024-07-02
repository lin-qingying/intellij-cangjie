package com.huawei.cangjie.references.util

object DescriptorToSourceUtilsIde {

//    private fun getDeclarationsStream(
//        project: Project, targetDescriptor: DeclarationDescriptor, builtInsSearchScope: GlobalSearchScope? = null
//    ): Sequence<PsiElement> {
//        val effectiveReferencedDescriptors = DescriptorToSourceUtils.getEffectiveReferencedDescriptors(targetDescriptor).asSequence()
//        return effectiveReferencedDescriptors.flatMap { effectiveReferenced ->
//            // References in library sources should be resolved to corresponding decompiled declarations,
//            // therefore we put both source declaration and decompiled declaration to stream, and afterwards we filter it in getAllDeclarations
////            sequenceOfLazyValues(
////                { DescriptorToSourceUtils.getSourceFromDescriptor(effectiveReferenced) },
////                { KtFe10ReferenceResolutionHelper.getInstance().findDecompiledDeclaration(project, effectiveReferenced, builtInsSearchScope) }
////            )
//        }.filterNotNull()
//    }
    // Returns all PSI elements for descriptor. It can find declarations in builtins or decompiled code.
//    fun getAllDeclarations(
//        project: Project,
//        targetDescriptor: DeclarationDescriptor,
//        builtInsSearchScope: GlobalSearchScope? = null
//    ): Collection<PsiElement> {
//        val result = getDeclarationsStream(project, targetDescriptor, builtInsSearchScope).toHashSet()
//        // filter out elements which are navigate to some other element of the result
//        // this is needed to avoid duplicated results for references to declaration in same library source file
//        return result.filter { element -> result.none { element != it && it.navigationElement == element } }
//    }
}
