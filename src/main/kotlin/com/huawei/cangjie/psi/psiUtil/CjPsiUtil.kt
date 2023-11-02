package com.huawei.cangjie.psi.psiUtil

import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.lexer.CangJieLexer
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.name.SpecialNames
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.stubs.CangJieClassOrStructStub
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.util.codeInsight.CommentUtilCore
import java.util.*


fun <D> visitChildren(element: CjElement, visitor: CjVisitor<Void, D>, data: D) {
    var child = element.firstChild
    while (child != null) {
        if (child is CjElement) {
            child.accept(visitor, data)
        }
        child = child.nextSibling
    }

}


fun StubBasedPsiElementBase<out CangJieClassOrStructStub<out CjClassOrStruct>>.getSuperNames(): List<String> {
    fun addSuperName(result: MutableList<String>, referencedName: String) {
        result.add(referencedName)

        val file = containingFile
        if (file is CjFile) {
            val directive = file.findImportByAlias(referencedName)
            if (directive != null) {
                var reference = directive.importedReference
                while (reference is CjDotQualifiedExpression) {
                    reference = reference.selectorExpression
                }
                if (reference is CjSimpleNameExpression) {
                    result.add(reference.getReferencedName())
                }
            }
        }
    }

    require(this is CjClassOrStruct) { "it should be ${CjClassOrStruct::class} but it is a ${this::class.java.name}" }

    val stub = stub
    if (stub != null) {
        return stub.getSuperNames()
    }

    val specifiers = this.superTypeListEntries
    if (specifiers.isEmpty()) return Collections.emptyList()

    val result = ArrayList<String>()
    for (specifier in specifiers) {
        val superType = specifier.typeAsUserType
        if (superType != null) {
            val referencedName = superType.referencedName
            if (referencedName != null) {
                addSuperName(result, referencedName)
            }
        }
    }

    return result
}

fun isComment(element: PsiElement): Boolean {
    return CommentUtilCore.isComment(element)
}


fun CjNamedDeclaration.safeFqNameForLazyResolve(): FqName? {
    //应该只为包级声明创建特殊名称，这样就可以安全地依赖于父级的真实fq名称
    val parentFqName = CjNamedDeclarationUtil.getParentFqName(this)
    return parentFqName?.child(safeNameForLazyResolve())
}

fun CjNamedDeclaration.safeNameForLazyResolve(): Name {
    return nameAsName.safeNameForLazyResolve()
}
//fun CjFunctionImpl.isContractPresentPsiCheck(isAllowedOnMembers: Boolean): Boolean {
//    val contractAllowedHere =
//        (isAllowedOnMembers || isTopLevel) &&
//                hasBlockBody() &&
//                !hasModifier(CjTokens.OPERATOR_KEYWORD)
//    if (!contractAllowedHere) return false
//
//    val firstExpression = (this as? CjFunction)?.bodyBlockExpression?.statements?.firstOrNull() ?: return false
//
//    return firstExpression.isContractDescriptionCallPsiCheck()
//}
//fun CjExpression.isContractDescriptionCallPsiCheck(): Boolean =
//    (this is CjCallExpression && calleeExpression?.text == "contract") || (this is CjQualifiedExpression && isContractDescriptionCallPsiCheck())

fun Name?.safeNameForLazyResolve(): Name = this?.takeUnless(Name::isSpecial) ?: SpecialNames.NO_NAME_PROVIDED


fun CjExpression.getAssignmentByLHS(): CjBinaryExpression? {
    val parent = parent as? CjBinaryExpression ?: return null
    return if (CjPsiUtil.isAssignment(parent) && parent.left == this) parent else null
}

fun CjExpression.getQualifiedExpressionForSelectorOrThis(): CjExpression {
    return getQualifiedExpressionForSelector() ?: this
}

fun getTrailingCommaByClosingElement(closingElement: PsiElement?): PsiElement? {
    val elementBeforeClosingElement =
        closingElement?.getPrevSiblingIgnoringWhitespaceAndComments() ?: return null

    return elementBeforeClosingElement.run { if (node.elementType == CjTokens.COMMA) this else null }
}

var CjElement.parentSubstitute: PsiElement? by UserDataProperty(Key.create<PsiElement>("PARENT_SUBSTITUTE"))
fun String.quoteIfNeeded(): String = if (this.isIdentifier()) this else "`$this`"
fun String?.isIdentifier(): Boolean {
    if (this == null || isEmpty()) return false

    val lexer = CangJieLexer()
    lexer.start(this, 0, length)
    if (lexer.tokenType !== CjTokens.IDENTIFIER) return false
    lexer.advance()
    return lexer.tokenType == null
}

fun PsiElement.astReplace(newElement: PsiElement) = parent.node.replaceChild(node, newElement.node)
fun CjElement.getQualifiedElementSelector(): CjElement? {
    return when (this) {
        is CjSimpleNameExpression -> this
        is CjCallExpression -> calleeExpression
        is CjQualifiedExpression -> {
            val selector = selectorExpression
            (selector as? CjCallExpression)?.calleeExpression ?: selector
        }

        is CjUserType -> referenceExpression
        else -> null
    }
}

fun CjElement.getQualifiedExpressionForSelector(): CjQualifiedExpression? {
    val parent = parent
    return if (parent is CjQualifiedExpression && parent.selectorExpression == this) parent else null
}

val CjDeclaration.containingClassOrStruct: CjClassOrStruct?
    get() = parent.let {
        when (it) {
            is CjClassBody -> it.parent as? CjClassOrStruct
            is CjClassOrStruct -> it
            is CjParameterList -> (it.parent as? CjPrimaryConstructor)?.getContainingClassOrStruct()
            else -> null
        }
    }

fun getTrailingCommaByElementsList(elementList: PsiElement?): PsiElement? {
    val lastChild =
        elementList?.lastChild?.let { if (it !is PsiComment) it else it.getPrevSiblingIgnoringWhitespaceAndComments() }
    return lastChild?.takeIf { it.node.elementType == CjTokens.COMMA }
}

fun CjStringTemplateExpression.getContentRange(): TextRange {
    val start = node.firstChildNode.textLength
    val lastChild = node.lastChildNode
    val length = textLength
    return TextRange(
        start,
        if (lastChild.elementType == CjTokens.CLOSING_QUOTE) length - lastChild.textLength else length
    )
}

fun CjStringTemplateExpression.isSingleQuoted(): Boolean = node.firstChildNode.textLength == 1
fun CjStringTemplateExpression.isPlain() = entries.all { it is CjLiteralStringTemplateEntry }
