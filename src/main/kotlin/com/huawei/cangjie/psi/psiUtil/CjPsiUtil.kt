package com.huawei.cangjie.psi.psiUtil

import com.huawei.cangjie.CjNodeTypes
import com.huawei.cangjie.lexer.CangJieLexer
import com.huawei.cangjie.lexer.CjModifierKeywordToken
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.name.SpecialNames
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.stubs.CangJieTypeStatementStub
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.tree.TokenSet
import com.intellij.util.codeInsight.CommentUtilCore
import java.util.*

fun CjExpression.getQualifiedExpressionForReceiver(): CjQualifiedExpression? {
    val parent = parent
    return if (parent is CjQualifiedExpression && parent.receiverExpression == this) parent else null
}
private val BAD_NEIGHBOUR_FOR_SIMPLE_TEMPLATE_ENTRY_PATTERN = Regex("([a-zA-Z0-9_]|[^\\p{ASCII}]).*")
/**
 * Returns enclosing qualifying element for given [[CjSimpleNameExpression]]
 * ([[CjQualifiedExpression]] or [[CjUserType]] or original expression)
 */
fun CjSimpleNameExpression.getQualifiedElement(): CjElement {
    val baseExpression = (parent as? CjCallExpression) ?: this
    val parent = baseExpression.parent
    return when (parent) {
        is CjQualifiedExpression -> if (parent.selectorExpression == baseExpression) parent else baseExpression
        is CjUserType -> if (parent.referenceExpression == baseExpression) parent else baseExpression
        else -> baseExpression
    }
}
fun CjModifierListOwner.isPrivate(): Boolean = hasModifier(CjTokens.PRIVATE_KEYWORD)

fun canPlaceAfterSimpleNameEntry(element: PsiElement?): Boolean {
    val entryText = element?.text ?: return true
    return !BAD_NEIGHBOUR_FOR_SIMPLE_TEMPLATE_ENTRY_PATTERN.matches(entryText)
}

fun CjSimpleNameExpression.getReceiverExpression(): CjExpression? {
    val parent = parent
    when {
        parent is CjQualifiedExpression -> {
            val receiverExpression = parent.receiverExpression
            // Name expression can't be receiver for itself
            if (receiverExpression != this) {
                return receiverExpression
            }
        }
        parent is CjCallExpression -> {
            //This is in case `a().b()`
            val grandParent = parent.parent
            if (grandParent is CjQualifiedExpression) {
                val parentsReceiver = grandParent.receiverExpression
                if (parentsReceiver != parent) {
                    return parentsReceiver
                }
            }
        }
//        parent is CjBinaryExpression && parent.operationReference == this -> {
//            return if (parent.operationToken in OperatorConventions.IN_OPERATIONS) parent.right else parent.left
//        }
        parent is CjUnaryExpression && parent.operationReference == this -> {
            return parent.baseExpression
        }
        parent is CjUserType -> {
            val qualifier = parent.qualifier
            if (qualifier != null) {
                return qualifier.referenceExpression!!
            }
        }
    }

    return null
}
fun PsiElement.isFunctionalExpression(): Boolean = this is CjNamedFunction && nameIdentifier == null
fun CjElement.containingClass(): CjClass? = getStrictParentOfType()

fun CjSimpleNameExpression.getTopmostParentQualifiedExpressionForSelector(): CjQualifiedExpression? {
    return generateSequence<CjExpression>(this) {
        val parentQualified = it.parent as? CjQualifiedExpression
        if (parentQualified?.selectorExpression == it) parentQualified else null
    }.last() as? CjQualifiedExpression
}
fun CjModifierListOwner.visibilityModifier() = modifierList?.modifierFromTokenSet(CjTokens.VISIBILITY_MODIFIERS)
private fun CjModifierList.modifierFromTokenSet(set: TokenSet): PsiElement? {
    return set.types
        .asSequence()
        .map { getModifier(it as CjModifierKeywordToken) }
        .firstOrNull { it != null }

}
fun CjModifierListOwner.visibilityModifierType(): CjModifierKeywordToken? =
    visibilityModifier()?.node?.elementType as CjModifierKeywordToken?

fun CjExpression.referenceExpression(): CjReferenceExpression? =
    (if (this is CjCallExpression) calleeExpression else this) as? CjReferenceExpression

fun CjNamedFunction.isContractPresentPsiCheck(isAllowedOnMembers: Boolean): Boolean {
    val contractAllowedHere =
        (isAllowedOnMembers || isTopLevel) &&
                hasBlockBody() &&
                !hasModifier(CjTokens.OPERATOR_KEYWORD)
    if (!contractAllowedHere) return false

    val firstExpression = (this as? CjFunction)?.bodyBlockExpression?.statements?.firstOrNull() ?: return false

    return firstExpression.isContractDescriptionCallPsiCheck()
}

fun CjExpression.isContractDescriptionCallPsiCheck(): Boolean =
    (this is CjCallExpression && calleeExpression?.text == "contract") || (this is CjQualifiedExpression && isContractDescriptionCallPsiCheck())

fun <D> visitChildren(element: CjElement, visitor: CjVisitor<Void, D>, data: D) {
    var child = element.firstChild
    while (child != null) {
        if (child is CjElement) {
            child.accept(visitor, data)
        }
        child = child.nextSibling
    }

}

private fun StubElement<*>.collectAnnotationEntriesFromStubElement(): List<CjAnnotationEntry> {
    return childrenStubs.flatMap { child ->
        when (child.stubType) {
            CjNodeTypes.ANNOTATION_ENTRY -> listOf(child.psi as CjAnnotationEntry)

            else -> emptyList()
        }
    }
}

private fun CjAnnotationsContainer.collectAnnotationEntriesFromPsi(): List<CjAnnotationEntry> {
    return children.flatMap { child ->
        when (child) {
            is CjAnnotationEntry -> listOf(child)

            else -> emptyList()
        }
    }
}

fun CjAnnotationsContainer.collectAnnotationEntriesFromStubOrPsi(): List<CjAnnotationEntry> {
    return when (this) {
        is StubBasedPsiElementBase<*> -> stub?.collectAnnotationEntriesFromStubElement()
            ?: collectAnnotationEntriesFromPsi()

        else -> collectAnnotationEntriesFromPsi()
    }
}

fun StubBasedPsiElementBase<out CangJieTypeStatementStub<out CjTypeStatement>>.getSuperNames(): List<String> {
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

    require(this is CjTypeStatement) { "it should be ${CjTypeStatement::class} but it is a ${this::class.java.name}" }

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

inline fun <reified T : PsiElement> T.copied(): T {
    return copy() as T
}

fun getTrailingCommaByClosingElement(closingElement: PsiElement?): PsiElement? {
    val elementBeforeClosingElement =
        closingElement?.getPrevSiblingIgnoringWhitespaceAndComments() ?: return null

    return elementBeforeClosingElement.run { if (node.elementType == CjTokens.COMMA) this else null }
}

val CjQualifiedExpression.callExpression: CjCallExpression?
    get() = selectorExpression as? CjCallExpression

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

val CjDeclaration.containingClassOrStruct: CjTypeStatement?
    get() = parent.let {
        when (it) {
            is CjClassBody -> it.parent as? CjTypeStatement
            is CjTypeStatement -> it
            is CjParameterList -> (it.parent as? CjPrimaryConstructor)?.getContainingTypeStatement()
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


fun List<CangJieImportField>.addIf(element: CangJieImportField) {
    if (!this.contains(element)) {
        this + element // 如果不存在，则返回新列表
    }
}
fun CjDeclaration.modalityModifier() = modifierFromTokenSet(CjTokens.MODALITY_MODIFIERS)
private fun CjModifierListOwner.modifierFromTokenSet(set: TokenSet) = modifierList?.modifierFromTokenSet(set)
