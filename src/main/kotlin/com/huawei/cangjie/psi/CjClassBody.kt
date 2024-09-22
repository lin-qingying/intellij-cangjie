package com.huawei.cangjie.psi

import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.stubs.CangJiePlaceHolderStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes.CLASS_BODY
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil


class CjClassBody : CjElementImplStub<CangJiePlaceHolderStub<CjClassBody>>, CjDeclarationContainer {
    private val lBraceTokenSet = TokenSet.create(CjTokens.LBRACE)
    private val rBraceTokenSet = TokenSet.create(CjTokens.RBRACE)

    constructor(node: ASTNode) : super(node)


    constructor(stub: CangJiePlaceHolderStub<CjClassBody>) : super(stub, CLASS_BODY)

    override fun getParent() = parentByStub
    internal val secondaryConstructors: List<CjSecondaryConstructor>
        get() = getStubOrPsiChildrenAsList(CjStubElementTypes.SECONDARY_CONSTRUCTOR)
    internal val primaryConstructors: List<CjPrimaryConstructor>
        get() = getStubOrPsiChildrenAsList(CjStubElementTypes.PRIMARY_CONSTRUCTOR)

    /**
     * @return annotations that do not belong to any declaration due to incomplete code or syntax errors
     */
//    val danglingAnnotations: List<CjAnnotationEntry>
//        get() = danglingModifierLists.flatMap { it.annotationEntries }

    override fun toString(): String {
        return node.elementType.toString()
    }

    val properties: List<CjProperty>
        get() = getStubOrPsiChildrenAsList(CjStubElementTypes.PROPERTY)
    val variables: List<CjVariable>
        get() = getStubOrPsiChildrenAsList(CjStubElementTypes.VARIABLE)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?) = visitor.visitClassBody(this, data)
    override val declarations: List<CjDeclaration>
        get() = stub?.getChildrenByType(CjFile.FILE_DECLARATION_TYPES, CjDeclaration.ARRAY_FACTORY)?.toList()
            ?: PsiTreeUtil.getChildrenOfTypeAsList(this, CjDeclaration::class.java)

    val rBrace: PsiElement?
        get() = node.getChildren(rBraceTokenSet).singleOrNull()?.psi

    val lBrace: PsiElement?
        get() = node.getChildren(lBraceTokenSet).singleOrNull()?.psi

}
