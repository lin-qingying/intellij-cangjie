package com.huawei.cangjie.references.rename


import com.huawei.cangjie.analyzer.withRootPrefixIfNeeded
import com.huawei.cangjie.ide.ShortenReferences
import com.huawei.cangjie.ide.codeinsight.shorten.addDelayedImportRequest
import com.huawei.cangjie.ide.codeinsight.shorten.addToShorteningWaitSet
import com.huawei.cangjie.highlighter.unwrapped

import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.name.*
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.psiUtil.*
import com.huawei.cangjie.references.*
import com.huawei.cangjie.utils.isDispatchThread
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException

/**
 * 重命名重构
 */
abstract class CjReferenceMutateServiceBase : CjReferenceMutateService {

    override fun handleElementRename(cjReference: CjReference, newElementName: String): PsiElement? {
        return when (cjReference) {
//            is CjArrayAccessReference -> cjReference.renameTo(newElementName)
//            is CDocReference -> cjReference.renameTo(newElementName)
//            is CjInvokeFunctionReference -> cjReference.renameTo(newElementName)
            is CjSimpleNameReference -> cjReference.renameTo(newElementName)
//            is CjDefaultAnnotationArgumentReference -> cjReference.renameTo(newElementName)
            else -> throw IncorrectOperationException()
        }
    }
    private fun CjSimpleNameReference.renameTo(newElementName: String): CjExpression {
        if (!canRename()) throw IncorrectOperationException()

        if (newElementName.unquoteCangJieIdentifier() == "") {
            return when (val qualifiedElement = expression.getQualifiedElement()) {
                is CjQualifiedExpression -> {
                    expression.replace(qualifiedElement.receiverExpression)
                    qualifiedElement.replaced(qualifiedElement.selectorExpression!!)
                }

                is CjUserType -> expression.replaced(
                    CjPsiFactory(expression).createSimpleName(
                        SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT.asString()
                    )
                )

                else -> expression
            }
        }

        // Do not rename if the reference corresponds to synthesized component function
        val expressionText = expression.text
        if (expressionText != null && Name.isValidIdentifier(expressionText)) {
            if ( resolve() is CjParameter) {
                return expression
            }
        }

        val project = expression.project
        val psiFactory = CjPsiFactory(project)
        val nameElement = expression.getReferencedNameElement()
//        val elementType = nameElement.node.elementType
//        val opExpression = if (elementType is CjToken && OperatorConventions.getNameForOperationSymbol(elementType) != null) {
//            expression.parent as? CjOperationExpression
//        } else null

        val quotedNewName = newElementName.quoteIfNeeded()
//        if (opExpression != null) {
//            val (newExpression: CjExpression, newNameElement: CjSimpleNameExpression) = convertOperatorToFunctionCall(opExpression)
//            //newNameElement is expression here, should be replaced with expression for psi consistency
//            newNameElement.replace(psiFactory.createSimpleName(quotedNewName))
//            return newExpression
//        }

        val renamedByExtension = SimpleNameReferenceExtension.EP_NAME
            .extensions
            .firstNotNullOfOrNull { it.handleElementRename(this, psiFactory, newElementName) }

        val element = renamedByExtension ?: psiFactory.createNameIdentifier(quotedNewName)
        if (element.node.elementType == CjTokens.IDENTIFIER) {
            nameElement.astReplace(element)
        } else {
            nameElement.replace(element)
        }
        return expression
    }
    /**
     * Converts a call to an operator to a regular explicit function call.
      *
      * @return A pair of resulting function call expression and a [CjSimpleNameExpression] pointing to the function name in that expression.
      */
//    private fun convertOperatorToFunctionCall(opExpression: CjOperationExpression): Pair<CjExpression, CjSimpleNameExpression> =
//        OperatorToFunctionConverter.convert(opExpression)


    override fun bindToElement(
        simpleNameReference: CjSimpleNameReference,
        element: PsiElement,
        shorteningMode: CjSimpleNameReference.ShorteningMode
    ): PsiElement {
        return element.cangjieFqName?.let { fqName -> bindToFqName(simpleNameReference, fqName, shorteningMode, element) }
            ?: simpleNameReference.expression
    }


}

class CangJieReferenceMutateService : CjReferenceMutateServiceBase() {
    override fun bindToElement(cjReference: CjReference, element: PsiElement): PsiElement  = when (cjReference) {
        is CjSimpleNameReference -> bindToElement(cjReference, element, CjSimpleNameReference.ShorteningMode.DELAYED_SHORTENING)
//        is CDocReference -> bindToElement(cjReference, element, CjSimpleNameReference.ShorteningMode.FORCED_SHORTENING)

        else -> throw IncorrectOperationException()
    }
    /**
     * Replace [[CjNameReferenceExpression]] (and its enclosing qualifier) with qualified element given by FqName
     * Result is either the same as original element, or [[CjQualifiedExpression]], or [[CjUserType]]
     * Note that FqName may not be empty
     */
    private fun CjNameReferenceExpression.changeQualifiedName(
        fqName: FqName,
        targetElement: PsiElement? = null
    ): CjNameReferenceExpression {
        assert(!fqName.isRoot) { "Can't set empty FqName for element $this" }

        val shortName = fqName.shortName().asString()
        val psiFactory = CjPsiFactory(project)
        val parent = parent

        if (parent is CjUserType && !fqName.isOneSegmentFQN()) {
            val qualifier = parent.qualifier
            val qualifierReference = qualifier?.referenceExpression as? CjNameReferenceExpression
            if (qualifierReference != null && qualifier.typeArguments.isNotEmpty()) {
                qualifierReference.changeQualifiedName(fqName.parent(), targetElement)
                return this
            }
        }

        val targetUnwrapped = targetElement?.unwrapped

        if (targetUnwrapped != null && fqName.isOneSegmentFQN()) {
            addDelayedImportRequest(targetUnwrapped, getContainingCjFile())
        }

        var parentDelimiter = "."
        val fqNameBase = when {
            parent is CjCallElement -> {
                val callCopy = parent.copied()
                callCopy.calleeExpression!!.replace(psiFactory.createSimpleName(shortName)).parent!!.text
            }
            parent is CjCallableReferenceExpression && parent.callableReference == this -> {
                parentDelimiter = ""
                val callableRefCopy = parent.copied()
                callableRefCopy.receiverExpression?.delete()
                val newCallableRef = callableRefCopy
                    .callableReference
                    .replace(psiFactory.createSimpleName(shortName))
                    .parent as CjCallableReferenceExpression
                if (targetUnwrapped != null  ) {
                    addDelayedImportRequest(targetUnwrapped, parent.getContainingCjFile())
                    return parent.replaced(newCallableRef).callableReference as CjNameReferenceExpression
                }
                newCallableRef.text
            }
            else -> shortName
        }

        val text = if (!fqName.isOneSegmentFQN()) "${fqName.parent().asString()}$parentDelimiter$fqNameBase" else fqNameBase

        val elementToReplace = getQualifiedElementOrCallableRef()

        val newElement = when (elementToReplace) {
            is CjUserType -> {
                val typeText = "$text${elementToReplace.typeArgumentList?.text ?: ""}"
                elementToReplace.replace(psiFactory.createType(typeText).typeElement!!)
            }
            else -> CjPsiUtil.safeDeparenthesize(elementToReplace.replaced(psiFactory.createExpression(text)))
        } as CjElement

        val selector = (newElement as? CjCallableReferenceExpression)?.callableReference
            ?: newElement.getQualifiedElementSelector()
            ?: error("No selector for $newElement")
        return selector as CjNameReferenceExpression
    }
    override fun bindToFqName(
        simpleNameReference: CjSimpleNameReference,
        fqName: FqName,
        shorteningMode: CjSimpleNameReference.ShorteningMode,
        targetElement: PsiElement?
    ): PsiElement {
        val expression = simpleNameReference.expression
        if (fqName.isRoot) return expression

        // not supported for infix calls and operators
        if (expression !is CjNameReferenceExpression) return expression
        if (expression.parent is CjThisExpression || expression.parent is CjSuperExpression) return expression // TODO: it's a bad design of PSI tree, we should change it

        val newExpression = expression.changeQualifiedName(
            fqName.quoteIfNeeded().let {
                if (shorteningMode == CjSimpleNameReference.ShorteningMode.NO_SHORTENING)
                    it
                else
                    it.withRootPrefixIfNeeded(expression)
            },
            targetElement
        )
        val newQualifiedElement = newExpression.getQualifiedElementOrCallableRef()

        if (shorteningMode == CjSimpleNameReference.ShorteningMode.NO_SHORTENING) return newExpression

        val needToShorten = PsiTreeUtil.getParentOfType(expression, CjImportDirective::class.java, CjPackageDirective::class.java) == null
        if (!needToShorten) {
            return newExpression
        }

        return if (shorteningMode == CjSimpleNameReference.ShorteningMode.FORCED_SHORTENING || !isDispatchThread()) {
            ShortenReferences.DEFAULT.process(newQualifiedElement)
        } else {
            newQualifiedElement.addToShorteningWaitSet()
            newExpression
        }
    }

}
