package com.huawei.cangjie1.psi

import com.huawei.cangjie1.lexer.CjTokens
import com.huawei.cangjie1.psi.stubs.CangJiePlaceHolderStub
import com.huawei.cangjie1.psi.stubs.elements.CjStubElementTypes
import com.huawei.cangjie1.psi.stubs.elements.CjTokenSets
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement


/**
 *键入Reference Element。
 *底层令牌为[com.huawei.cangjie1.CjNodeTypes.TYPE_REFERENCE]
 */
class CjTypeReference : CjModifierListOwnerStub<CangJiePlaceHolderStub<CjTypeReference>>,
    CjElement {

    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePlaceHolderStub<CjTypeReference>) : super(stub, CjStubElementTypes.TYPE_REFERENCE)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R {
        return visitor.visitTypeReference(this, data)
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

    /**
     * Returns presentable text for the underlying type based on stubs when provided.
     * No decompilation happens if [CjTypeReference] represents compiled code.
     */
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
