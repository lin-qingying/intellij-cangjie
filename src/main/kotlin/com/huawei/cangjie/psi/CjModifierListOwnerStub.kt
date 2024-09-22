package com.huawei.cangjie.psi

import com.huawei.cangjie.descriptors.DescriptorVisibilities
import com.huawei.cangjie.descriptors.DescriptorVisibility
import com.huawei.cangjie.lexer.CjModifierKeywordToken
import com.huawei.cangjie.psi.psiUtil.addModifier
import com.huawei.cangjie.psi.psiUtil.removeModifier
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.huawei.cangjie.resolve.ModifiersChecker.Companion.resolveVisibilityFromModifiers
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement

open class CjModifierListOwnerStub<T : StubElement<*> > : CjElementImplStub<T>,
    CjModifierListOwner {
    constructor(node: ASTNode) : super(node)

    constructor(stub: T, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    override val modifierList: CjModifierList?
        get() = getStubOrPsiChild(CjStubElementTypes.MODIFIER_LIST)

    override fun hasModifier(modifier: CjModifierKeywordToken): Boolean {
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

    override fun addModifier(modifier: CjModifierKeywordToken) {
        addModifier(this, modifier)
    }

    override fun removeModifier(modifier: CjModifierKeywordToken) {
        removeModifier(this, modifier)
    }
}
