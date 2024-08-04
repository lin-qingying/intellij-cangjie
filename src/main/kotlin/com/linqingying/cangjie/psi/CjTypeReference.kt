package com.linqingying.cangjie.psi

import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.psi.stubs.CangJiePlaceHolderStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.linqingying.cangjie.psi.stubs.elements.CjTokenSets
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement


/**
 *键入Reference Element。
 *底层令牌为[com.linqingying.cangjie.CjNodeTypes.TYPE_REFERENCE]
 */
class CjTypeReference : CjModifierListOwnerStub<CangJiePlaceHolderStub<CjTypeReference>>,
    CjElement {

    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePlaceHolderStub<CjTypeReference>) : super(stub, CjStubElementTypes.TYPE_REFERENCE)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitTypeReference(this, data)
    }
    override fun getAnnotationEntries(): List<CjAnnotationEntry> {
        return modifierList?.annotationEntries.orEmpty()
    }
    val isPlaceholder: Boolean
        get() = ((typeElement as? CjUserType)?.referenceExpression as? CjNameReferenceExpression)?.isPlaceholder == true

    val typeElement: CjTypeElement?
        get() = CjStubbedPsiUtil.getStubOrPsiChild(this, CjTokenSets.TYPE_ELEMENT_TYPES, CjTypeElement.ARRAY_FACTORY)

    override fun toString(): String = node.elementType.toString();

    fun hasParentheses(): Boolean {
        return findChildByType<PsiElement>(CjTokens.LPAR) != null && findChildByType<PsiElement>(CjTokens.RPAR) != null
    }

    fun nameForReceiverLabel() = (typeElement as? CjUserType)?.referencedName


    fun getTypeText(): String {
        return stub?.let { getTypeText(typeElement) } ?: text
    }

    private fun getQualifiedName(userType: CjUserType): String? {
        val qualifier = userType.qualifier ?: return userType.referencedName
        return getQualifiedName(qualifier) + "." + userType.referencedName
    }

    private fun getTypeText(typeElement: CjTypeElement?): String? {
        return when (typeElement) {
            is CjUserType -> buildString {
                append(getQualifiedName(typeElement))
                val args = typeElement.typeArguments
                if (args.isNotEmpty()) {
                    append(args.joinToString(", ", "<", ">") {
                        val projection = when (it.projectionKind) {
                            CjProjectionKind.IN -> "in "

                            CjProjectionKind.STAR -> "*"
                            CjProjectionKind.NONE -> ""
                        }
                        projection + (getTypeText(it.typeReference?.typeElement) ?: "")
                    })
                }
            }


            null -> null
            else -> error("Unsupported type $typeElement")
        }
    }
}
