package com.huawei.cangjie.descriptors

import com.huawei.cangjie.CjNodeTypes
import com.huawei.cangjie.diagnostics.PositioningStrategy
import com.huawei.cangjie.diagnostics.hasSyntaxErrors
import com.huawei.cangjie.diagnostics.markElement
import com.huawei.cangjie.diagnostics.markRange
import com.huawei.cangjie.lexer.CjModifierKeywordToken
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.psiUtil.*
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet

object PositioningStrategies {


    open class DeclarationHeader<T : CjDeclaration> : PositioningStrategy<T>() {
        override fun isValid(element: T): Boolean {
            if (element is CjNamedDeclaration &&

                element !is CjSecondaryConstructor &&
                element !is CjFunction
            ) {
                if (element.nameIdentifier == null) {
                    return false
                }
            }
            return super.isValid(element)
        }
    }

    @JvmField
    val USELESS_ELVIS: PositioningStrategy<CjBinaryExpression> = object : PositioningStrategy<CjBinaryExpression>() {
        override fun mark(element: CjBinaryExpression): List<TextRange> {
            return listOf(TextRange(element.operationReference.startOffset, element.endOffset))
        }
    }


    @JvmField
    val RETURN_WITH_LABEL: PositioningStrategy<CjReturnExpression> =
        object : PositioningStrategy<CjReturnExpression>() {
            override fun mark(element: CjReturnExpression): List<TextRange> {
                val labeledExpression = element.labeledExpression
                if (labeledExpression != null) {
                    return markRange(element, labeledExpression)
                }

                return markElement(element.returnKeyword)
            }
        }

    @JvmStatic
    fun projectionPosition(): PositioningStrategy<CjModifierListOwner> {
        return object : PositioningStrategy<CjModifierListOwner>() {
            override fun mark(element: CjModifierListOwner): List<TextRange> {


                throw IllegalStateException("None of the modifiers is found: in, out")
            }
        }
    }

    @JvmField
    val VARIANCE_MODIFIER: PositioningStrategy<CjModifierListOwner> = projectionPosition()

    @JvmField
    val DECLARATION_SIGNATURE: PositioningStrategy<CjDeclaration> = object : DeclarationHeader<CjDeclaration>() {

    }

    @JvmField
    val CUT_CHAR_QUOTES: PositioningStrategy<CjElement> = object : PositioningStrategy<CjElement>() {
        override fun mark(element: CjElement): List<TextRange> {
            if (element is CjConstantExpression) {
                if (element.node.elementType == CjNodeTypes.RUNE_CONSTANT) {
                    val elementTextRange = element.getTextRange()
                    return listOf(TextRange.create(elementTextRange.startOffset + 1, elementTextRange.endOffset - 1))
                }
            }
            return markElement(element)
        }
    }

    @JvmField
    val SAFE_ACCESS: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            return markElement(element.node.findChildByType(CjTokens.SAFE_ACCESS)?.psi ?: element)
        }
    }
    val SELECTOR_BY_QUALIFIED: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            if (element is CjBinaryExpression && element.operationToken in CjTokens.ALL_ASSIGNMENTS) {
                element.left?.let { return mark(it) }
            }
            if (element is CjQualifiedExpression) {
                when (val selectorExpression = element.selectorExpression) {
                    is CjElement -> return mark(selectorExpression)
                }
            }
            if (element is CjImportDirective) {
                element.alias?.nameIdentifier?.let { return mark(it) }
                element.importedReference?.let { return mark(it) }
            }
            if (element is CjTypeReference) {
                element.typeElement?.getReferencedTypeExpression()?.let { return mark(it) }
            }
            return super.mark(element)
        }
    }

    private fun CjTypeElement.getReferencedTypeExpression(): CjElement? {
        return when (this) {
            is CjUserType -> referenceExpression
            is CjOptionType -> getInnerType()?.getReferencedTypeExpression()
            else -> null
        }
    }

    @JvmField
    val CALL_ELEMENT_WITH_DOT: PositioningStrategy<CjQualifiedExpression> =
        object : PositioningStrategy<CjQualifiedExpression>() {
            override fun mark(element: CjQualifiedExpression): List<TextRange> {
                val callElementRanges = SELECTOR_BY_QUALIFIED.mark(element)
                val callElementRange = when (callElementRanges.size) {
                    1 -> callElementRanges.first()
                    else -> return callElementRanges
                }

                val dotRanges = SAFE_ACCESS.mark(element)
                val dotRange = when (dotRanges.size) {
                    1 -> dotRanges.first()
                    else -> return dotRanges
                }

                return listOf(TextRange(dotRange.startOffset, callElementRange.endOffset))
            }
        }

    @JvmField
    val DECLARATION_SIGNATURE_OR_DEFAULT: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            return if (element is CjDeclaration)
                DECLARATION_SIGNATURE.mark(element)
            else
                DEFAULT.mark(element)
        }

        override fun isValid(element: PsiElement): Boolean {
            return if (element is CjDeclaration)
                DECLARATION_SIGNATURE.isValid(element)
            else
                DEFAULT.isValid(element)
        }
    }

    @JvmField
    val FOR_REDECLARATION: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            val nameIdentifier = when (element) {
                is CjNamedDeclaration -> element.nameIdentifier
                is CjFile -> element.packageDirective!!.nameIdentifier
                else -> null
            }


            return markElement(nameIdentifier ?: element)
        }
    }

    @JvmField
    val CALL_EXPRESSION: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            if (element is CjCallExpression) {
                return markRange(element, element.typeArgumentList ?: element.calleeExpression ?: element)
            }
            return markElement(element)
        }
    }

    @JvmField
    val VISIBILITY_MODIFIER: PositioningStrategy<CjModifierListOwner> =
        ModifierSetBasedPositioningStrategy(CjTokens.VISIBILITY_MODIFIERS)

    @JvmField
    val PARAMETER_DEFAULT_VALUE: PositioningStrategy<CjParameter> = object : PositioningStrategy<CjParameter>() {
        override fun mark(element: CjParameter): List<TextRange> {
            return markNode(element.defaultValue!!.node)
        }
    }

    @JvmField
    val LET_OR_VAR_NODE: PositioningStrategy<CjDeclaration> = object : PositioningStrategy<CjDeclaration>() {
        override fun mark(element: CjDeclaration): List<TextRange> {
            return when (element) {
                is CjParameter -> markElement(element.letOrVarKeyword ?: element)
//                is CjProperty -> markElement(element.valOrVarKeyword)
                is CjVariable -> markElement(element.letOrVarKeyword ?: element)
                is CjDestructuringDeclaration -> markElement(element.letOrVarKeyword ?: element)
                else -> error("Declaration is neither a parameter nor a property: " + element.getElementTextWithContext())
            }
        }
    }

    @JvmField
    val OVERRIDE_MODIFIER: PositioningStrategy<CjModifierListOwner> =
        ModifierSetBasedPositioningStrategy(CjTokens.OVERRIDE_KEYWORD)

    @JvmField
    val DECLARATION_RETURN_TYPE: PositioningStrategy<CjDeclaration> = object : PositioningStrategy<CjDeclaration>() {
        override fun mark(element: CjDeclaration): List<TextRange> {
            return markElement(getElementToMark(element))
        }

        override fun isValid(element: CjDeclaration): Boolean {
            return !hasSyntaxErrors(getElementToMark(element))
        }

        private fun getElementToMark(declaration: CjDeclaration): PsiElement {
            val (returnTypeRef, nameIdentifierOrPlaceholder) = when (declaration) {
                is CjCallableDeclaration -> Pair(declaration.typeReference, declaration.nameIdentifier)
                is CjPropertyAccessor -> Pair(declaration.returnTypeReference, declaration.namePlaceholder)
                else -> Pair(null, null)
            }

            if (returnTypeRef != null) return returnTypeRef
            if (nameIdentifierOrPlaceholder != null) return nameIdentifierOrPlaceholder
            return declaration
        }
    }

    @JvmField
    val OPTIONAL_TYPE: PositioningStrategy<CjOptionType> = object : PositioningStrategy<CjOptionType>() {
        override fun mark(element: CjOptionType): List<TextRange> {
            return markNode(element.getQuestionMarkNode())
        }
    }

    @JvmField
    val TYPE_PARAMETERS_OR_DECLARATION_SIGNATURE: PositioningStrategy<CjDeclaration> =
        object : PositioningStrategy<CjDeclaration>() {
            override fun mark(element: CjDeclaration): List<TextRange> {
                if (element is CjTypeParameterListOwner) {
                    val cjTypeParameterList = element.typeParameterList
                    if (cjTypeParameterList != null) {
                        return markElement(cjTypeParameterList)
                    }
                }
                return DECLARATION_SIGNATURE.mark(element)
            }
        }


    @JvmField
    val DECLARATION_WITH_BODY: PositioningStrategy<CjDeclarationWithBody> = object : PositioningStrategy<CjDeclarationWithBody>() {
        override fun mark(element: CjDeclarationWithBody): List<TextRange> {
            val lastBracketRange = element.bodyBlockExpression?.lastBracketRange
            return if (lastBracketRange != null)
                markRange(lastBracketRange)
            else
                markElement(element)
        }

        override fun isValid(element: CjDeclarationWithBody): Boolean {
            return super.isValid(element) && element.bodyBlockExpression?.lastBracketRange != null
        }
    }

    @JvmField
    val DECLARATION_NAME: PositioningStrategy<CjNamedDeclaration> = object : DeclarationHeader<CjNamedDeclaration>() {
        override fun mark(element: CjNamedDeclaration): List<TextRange> {
            val nameIdentifier = element.nameIdentifier
            if (nameIdentifier != null) {
                if (element is CjTypeStatement) {
                    val startElement =
                        element.modifierList?.getModifier(CjTokens.ENUM_KEYWORD)
                            ?: element.node.findChildByType(
                                TokenSet.create(
                                    CjTokens.CLASS_KEYWORD,
                                    CjTokens.STRUCT_KEYWORD
                                )
                            )?.psi
                            ?: element

                    return markRange(startElement, nameIdentifier)
                }
                return markElement(nameIdentifier)
            }
            if (element is CjNamedFunction) {
                return DECLARATION_SIGNATURE.mark(element)
            }
            return DEFAULT.mark(element)
        }
    }

    @JvmField
    val VALUE_ARGUMENTS: PositioningStrategy<CjElement> = object : PositioningStrategy<CjElement>() {
        override fun mark(element: CjElement): List<TextRange> {
            if (element is CjBinaryExpression && element.operationToken in CjTokens.ALL_ASSIGNMENTS) {
                element.left.let { left ->
                    left.unwrapParenthesesLabelsAndAnnotations()?.let { return markElement(it) }
                }
            }
            val qualifiedAccess = when (element) {
                is CjQualifiedExpression -> element.selectorExpression ?: element
                is CjTypeStatement -> element.getSuperTypeList() ?: element
                else -> element
            }
            val argumentList = qualifiedAccess as? CjValueArgumentList
                ?: qualifiedAccess.getChildOfType()
            return when {
                argumentList != null -> {
                    val rightParenthesis = argumentList.rightParenthesis ?: return markElement(qualifiedAccess)
                    val lastArgument = argumentList.children.findLast { it is CjValueArgument }
                    if (lastArgument != null) {
                        markRange(lastArgument, rightParenthesis)
                    } else {
                        val leftParenthesis = argumentList.leftParenthesis
                        markRange(leftParenthesis ?: qualifiedAccess, rightParenthesis)
                    }
                }

                qualifiedAccess is CjCallExpression -> markElement(
                    qualifiedAccess.getChildOfType<CjNameReferenceExpression>() ?: qualifiedAccess
                )

                else -> markElement(qualifiedAccess)
            }
        }
    }


    @JvmField
    val SECONDARY_CONSTRUCTOR_DELEGATION_CALL: PositioningStrategy<PsiElement> =
        object : PositioningStrategy<PsiElement>() {
            override fun mark(element: PsiElement): List<TextRange> {
                return when (element) {
                    is CjSecondaryConstructor -> {
                        val valueParameterList = element.valueParameterList ?: return markElement(element)
                        markRange(element.getConstructorKeyword(), valueParameterList.lastChild)
                    }

                    is CjConstructorDelegationCall -> {
                        if (element.isImplicit) {
                            // TODO: [VD] FIR collects for some reason implicit CjConstructorDelegationCall
                            // check(!element.isImplicit) { "Implicit CjConstructorDelegationCall should not be collected directly" }
                            val constructor = element.getStrictParentOfType<CjSecondaryConstructor>()!!
                            val valueParameterList = constructor.valueParameterList ?: return markElement(constructor)
                            return markRange(constructor.getConstructorKeyword(), valueParameterList.lastChild)
                        }
                        markElement(element.calleeExpression ?: element)
                    }

                    else -> markElement(element)
                }
            }
        }

    @JvmField
    val VARIANCE_IN_PROJECTION: PositioningStrategy<CjTypeProjection> =
        object : PositioningStrategy<CjTypeProjection>() {
            override fun mark(element: CjTypeProjection): List<TextRange> {
                return markElement(element.projectionToken!!)
            }
        }

    @JvmField
    val CALL_ELEMENT: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            return markElement((element as? CjCallElement)?.calleeExpression ?: element)
        }
    }

    @JvmField
    val FOR_UNRESOLVED_REFERENCE: PositioningStrategy<CjReferenceExpression> =
        object : PositioningStrategy<CjReferenceExpression>() {
            override fun mark(element: CjReferenceExpression): List<TextRange> {
                if (element is CjArrayAccessExpression) {
                    val ranges = element.bracketRanges
                    if (ranges.isNotEmpty()) {
                        return ranges
                    }
                }
                return listOf(element.textRange)
            }
        }

    @JvmField
    val DEFAULT: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            when (element) {

                else -> {
                    return super.mark(element)
                }
            }
        }
    }


    private open class ModifierSetBasedPositioningStrategy(private val modifierSet: TokenSet) :
        PositioningStrategy<CjModifierListOwner>() {
        constructor(vararg tokens: IElementType) : this(TokenSet.create(*tokens))

        protected fun markModifier(element: CjModifierListOwner?): List<TextRange>? =
            modifierSet.types.mapNotNull {
                element?.modifierList?.getModifier(it as CjModifierKeywordToken)?.textRange
            }.takeIf { it.isNotEmpty() }

        override fun mark(element: CjModifierListOwner): List<TextRange> {
            val result = markModifier(element)
            if (result != null) return result

            // Try to resolve situation when there's no visibility modifiers written before element
            if (element is PsiNameIdentifierOwner) {
                val nameIdentifier = element.nameIdentifier
                if (nameIdentifier != null) {
                    return markElement(nameIdentifier)
                }
            }

            val elementToMark = when (element) {

                is CjPropertyAccessor -> element.namePlaceholder
                is CjAnonymousInitializer, is CjPrimaryConstructor -> element
                else -> throw IllegalArgumentException(
                    "Can't find text range for element '${element::class.java.canonicalName}' with the text '${element.text}'"
                )
            }
            return markElement(elementToMark)
        }
    }

}

fun markNode(node: ASTNode): List<TextRange> {
    return markElement(node.psi)
}
