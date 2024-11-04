package com.linqingying.cangjie.psi

import com.linqingying.cangjie.descriptors.DescriptorVisibilities
import com.linqingying.cangjie.descriptors.DescriptorVisibility
import com.linqingying.cangjie.lexer.CjModifierKeywordToken
import com.linqingying.cangjie.psi.psiUtil.addModifier
import com.linqingying.cangjie.psi.psiUtil.removeModifier
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.linqingying.cangjie.resolve.ModifiersChecker.Companion.resolveVisibilityFromModifiers
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.linqingying.cangjie.lexer.CjKeywordToken

open class CjModifierListOwnerStub<T : StubElement<*> > : CjElementImplStub<T>,
    CjModifierListOwner {
    constructor(node: ASTNode) : super(node)

    constructor(stub: T, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    override val modifierList: CjModifierList?
        get() = getStubOrPsiChild(CjStubElementTypes.MODIFIER_LIST)

    override fun hasModifier(modifier: CjKeywordToken): Boolean {
        val modifierList = modifierList
        return modifierList != null && modifierList.hasModifier(modifier)
    }

    override val annotations: List<CjAnnotation>
        get() {
            val modifierList = modifierList ?: return emptyList()
            return modifierList.annotations
        }

    override val annotationEntries: List<CjAnnotationEntry>
        get() {
            val modifierList = modifierList ?: return emptyList()
            return modifierList.annotationEntries
        }
    override val modifierVisibility: DescriptorVisibility
        get() = resolveVisibilityFromModifiers(
            this,
            DescriptorVisibilities.INTERNAL
        )

    override fun addModifier(modifier: CjKeywordToken) {
        addModifier(this, modifier)
    }

    override fun removeModifier(modifier: CjKeywordToken) {
        removeModifier(this, modifier)
    }
}
