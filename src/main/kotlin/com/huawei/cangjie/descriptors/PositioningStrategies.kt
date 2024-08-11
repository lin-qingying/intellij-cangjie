package com.huawei.cangjie.descriptors

import com.huawei.cangjie.diagnostics.PositioningStrategy
import com.huawei.cangjie.diagnostics.markElement
import com.huawei.cangjie.diagnostics.markRange
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.psiUtil.getChildOfType
import com.huawei.cangjie.psi.psiUtil.getStrictParentOfType
import com.huawei.cangjie.psi.psiUtil.unwrapParenthesesLabelsAndAnnotations
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

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

    @JvmStatic
    fun projectionPosition(): PositioningStrategy<CjModifierListOwner> {
        return object : PositioningStrategy<CjModifierListOwner>() {
            override fun mark(element: CjModifierListOwner): List<TextRange> {
                if (element is CjTypeProjection && element.projectionKind == CjProjectionKind.STAR) {
                    return markElement(element)
                }



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

//            if (nameIdentifier == null && element is CjObjectDeclaration) return DEFAULT.mark(element)

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
    val VALUE_ARGUMENTS: PositioningStrategy<CjElement> = object : PositioningStrategy<CjElement>() {
        override fun mark(element: CjElement): List<TextRange> {
            if (element is CjBinaryExpression && element.operationToken in CjTokens.ALL_ASSIGNMENTS) {
                element.left.let { left ->
                    left.unwrapParenthesesLabelsAndAnnotations()?.let { return markElement(it) }
                }
            }
            val qualifiedAccess = when (element) {
                is CjQualifiedExpression -> element.selectorExpression ?: element
                is CjClassOrStruct -> element.getSuperTypeList() ?: element
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
}
