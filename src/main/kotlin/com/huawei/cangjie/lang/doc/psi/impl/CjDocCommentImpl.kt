package com.huawei.cangjie.lang.doc.psi.impl

import com.huawei.cangjie.lang.doc.psi.CjDocComment
import com.huawei.cangjie.lang.core.psi.ext.ancestorStrict
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry
import com.intellij.psi.impl.source.tree.LazyParseablePsiElement
import com.intellij.psi.tree.IElementType

class CjDocCommentImpl(type: IElementType, text: CharSequence?) : LazyParseablePsiElement(type, text), CjDocComment {
    override fun getTokenType(): IElementType = elementType


     override fun getReferences(): Array<PsiReference> {
        return ReferenceProvidersRegistry.getReferencesFromProviders(this)
    }

     override fun accept(visitor: PsiElementVisitor) {
        visitor.visitComment(this)
    }

    override fun toString(): String {
        return "PsiComment($elementType)"
    }

//    override fun getOwner(): CjDocAndAttributeOwner? = ancestorStrict()
        override fun getOwner(): PsiElement? = ancestorStrict()
//
//    override val codeFences: List<CjDocCodeFence>
//        get() = childrenOfType()
//
//    override val linkDefinitions: List<CjDocLinkDefinition>
//        get() = childrenOfType()
//
//    override val linkReferenceMap: Map<String, CjDocLinkDefinition>
//        get() = CachedValuesManager.getCachedValue(this) {
//            val result = linkDefinitions.associateBy { it.linkLabel.markdownValue }
//            CachedValueProvider.Result(result, containingFile)
//        }
}
