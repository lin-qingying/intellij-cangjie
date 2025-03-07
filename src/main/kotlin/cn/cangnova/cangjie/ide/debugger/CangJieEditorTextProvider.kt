//package cn.cangnova.cangjie.ide.debugger
//
//import com.intellij.debugger.engine.evaluation.CodeFragmentKind
//import com.intellij.debugger.engine.evaluation.TextWithImports
//import com.intellij.debugger.engine.evaluation.TextWithImportsImpl
//import com.intellij.debugger.impl.EditorTextProvider
//import com.intellij.openapi.application.ReadAction
//import com.intellij.openapi.project.DumbService
//import com.intellij.openapi.project.IndexNotReadyException
//import com.intellij.openapi.util.Pair
//import com.intellij.openapi.util.TextRange
//import com.intellij.psi.PsiElement
//import com.intellij.psi.PsiNameIdentifierOwner
//import com.intellij.psi.util.PsiTreeUtil
//import com.intellij.psi.util.parents
//import cn.cangnova.cangjie.lang.CangJieFileType
//import cn.cangnova.cangjie.lang.CangJieLanguage
//import cn.cangnova.cangjie.lexer.CjTokens
//import cn.cangnova.cangjie.psi.CjImportDirective
//import cn.cangnova.cangjie.psi.CjPackageDirective
//import cn.cangnova.cangjie.psi.*
//import cn.cangnova.cangjie.psi.psiUtil.getParentOfType
//import cn.cangnova.cangjie.references.mainReference
//import cn.cangnova.cangjie.resolve.BindingContext
//import cn.cangnova.cangjie.resolve.DescriptorUtils
//import cn.cangnova.cangjie.resolve.caches.analyze
//import cn.cangnova.cangjie.resolve.caches.resolveToCall
//import cn.cangnova.cangjie.utils.registryFlag
//
//import org.jetbrains.annotations.TestOnly
//
//
//interface CangJieEditorTextProvider : EditorTextProvider {
//    fun findEvaluationTarget(originalElement: PsiElement, allowMethodCalls: Boolean): PsiElement?
//    fun isAcceptedAsCodeFragmentContext(element: PsiElement): Boolean
//
//    companion object {
//        val instance: CangJieEditorTextProvider
//             get() = EditorTextProvider.EP.forLanguage(CangJieLanguage  ) as CangJieEditorTextProvider
//    }
//}
//
//
//internal object LegacyCangJieEditorTextProvider : CangJieEditorTextProvider {
//    override fun getEditorText(elementAtCaret: PsiElement): TextWithImports? {
//        val expression = findExpressionInner(elementAtCaret, true) ?: return null
//
//        val expressionText = getElementInfo(expression) { it.text }
//        return TextWithImportsImpl(CodeFragmentKind.EXPRESSION, expressionText, "", CangJieFileType.INSTANCE)
//    }
//
//    override fun findExpression(elementAtCaret: PsiElement, allowMethodCalls: Boolean): Pair<PsiElement, TextRange>? {
//        val expression = findExpressionInner(elementAtCaret, allowMethodCalls) ?: return null
//
//        val expressionRange = getElementInfo(expression) { it.textRange }
//        return Pair(expression, expressionRange)
//    }
//
//    override fun findEvaluationTarget(originalElement: PsiElement, allowMethodCalls: Boolean): PsiElement? {
//        return findExpressionInner(originalElement, allowMethodCalls)
//    }
//
//    private fun <T> getElementInfo(expr: CjExpression, f: (PsiElement) -> T): T {
//        var expressionText = f(expr)
//
//        val nameIdentifier = expr.getNameIdentifier()
//        if (nameIdentifier != null) {
//            expressionText = f(nameIdentifier)
//        }
//
//        return expressionText
//    }
//
//    private fun findExpressionInner(element: PsiElement, allowMethodCalls: Boolean): CjExpression? {
//        if (!isAcceptedAsCodeFragmentContext(element)) return null
//
//        val cjElement = PsiTreeUtil.getParentOfType(element, CjElement::class.java) ?: return null
//
//        val nameIdentifier = cjElement.getNameIdentifier()
//        if (nameIdentifier == element) {
//            return cjElement as? CjExpression
//        }
//
//        fun CjExpression.qualifiedParentOrSelf(isSelector: Boolean = true): CjExpression {
//            val parent = parent
//            return if (parent is CjQualifiedExpression && (!isSelector || parent.selectorExpression == this)) parent else this
//        }
//
//        val newExpression = when (val parent = cjElement.parent) {
//            is CjThisExpression -> parent
//            is CjSuperExpression -> parent.qualifiedParentOrSelf(isSelector = false)
//            is CjArrayAccessExpression -> if (parent.arrayExpression == cjElement) cjElement else parent.qualifiedParentOrSelf()
//            is CjReferenceExpression -> parent.qualifiedParentOrSelf()
//            is CjQualifiedExpression -> if (parent.receiverExpression != cjElement) parent else null
//            is CjOperationExpression -> if (parent.operationReference == cjElement) parent else null
//            else -> null
//        }
//
//        if (!allowMethodCalls && newExpression != null) {
//            fun PsiElement.isCall() = this is CjCallExpression || this is CjOperationExpression || this is CjArrayAccessExpression
//
//            if (newExpression.isCall() || newExpression is CjQualifiedExpression && newExpression.selectorExpression!!.isCall()) {
//                return null
//            }
//        }
//
//        return when {
//            newExpression is CjExpression -> newExpression
//            cjElement is CjSimpleNameExpression -> {
//                val context = cjElement.analyze()
//                val qualifier = context[BindingContext.QUALIFIER, cjElement]
//                if (qualifier != null ) {
//                    null
//                } else {
//                    cjElement
//                }
//            }
//            else -> null
//        }
//
//    }
//
//    private fun CjElement.getNameIdentifier() =
//        if (this is CjProperty || this is CjParameter)
//            (this as PsiNameIdentifierOwner).nameIdentifier
//        else
//            null
//
//
//    private val NOT_ACCEPTED_AS_CONTEXT_TYPES = arrayOf(
//        CjUserType::class.java,
//        CjImportDirective::class.java,
//        CjPackageDirective::class.java,
//        CjValueArgumentName::class.java
//    )
//
//    override fun isAcceptedAsCodeFragmentContext(element: PsiElement): Boolean =
//        !NOT_ACCEPTED_AS_CONTEXT_TYPES.contains(element::class.java as Class<*>) &&
//                PsiTreeUtil.getParentOfType(element, *NOT_ACCEPTED_AS_CONTEXT_TYPES) == null
//}
//
//internal class EnclosingCangJieEditorTextProvider : CangJieEditorTextProvider {
//    internal var useAnalysisApi: Boolean by registryFlag("debugger.kotlin.analysis.api.editor.text.provider")
//        @TestOnly set
//
//    private val implementation: CangJieEditorTextProvider
//        get() {
//            if (!useAnalysisApi) {
//                CangJieDebuggerLegacyFacade.getInstance()?.editorTextProvider?.let { return it }
//            }
//
//            return AnalysisApiBasedCangJieEditorTextProvider
//        }
//
//    override fun getEditorText(elementAtCaret: PsiElement): TextWithImports? =
//        implementation.getEditorText(elementAtCaret)
//
//    override fun findExpression(elementAtCaret: PsiElement, allowMethodCalls: Boolean): Pair<PsiElement, TextRange>? =
//        implementation.findExpression(elementAtCaret, allowMethodCalls)
//
//    override fun findEvaluationTarget(originalElement: PsiElement, allowMethodCalls: Boolean): PsiElement? =
//        implementation.findEvaluationTarget(originalElement, allowMethodCalls)
//
//    override fun isAcceptedAsCodeFragmentContext(element: PsiElement): Boolean =
//        implementation.isAcceptedAsCodeFragmentContext(element)
//}
//
//private object AnalysisApiBasedCangJieEditorTextProvider : CangJieEditorTextProvider {
//    override fun getEditorText(elementAtCaret: PsiElement): TextWithImports? {
//        val expression = findEvaluationTarget(elementAtCaret, true) ?: return null
//        return TextWithImportsImpl(CodeFragmentKind.EXPRESSION, expression.text, "", CangJieFileType.INSTANCE)
//    }
//
//    override fun findExpression(elementAtCaret: PsiElement, allowMethodCalls: Boolean): Pair<PsiElement, TextRange>? {
//        val expression = findEvaluationTarget(elementAtCaret, allowMethodCalls) ?: return null
//        return Pair(expression, expression.textRange)
//    }
//
//    override tailrec fun findEvaluationTarget(originalElement: PsiElement, allowMethodCalls: Boolean): PsiElement? {
//        val candidate = calculateCandidate(originalElement, allowMethodCalls) ?: return null
//
//        val target = when (candidate) {
//            is CjParameter, is CjVariableDeclaration -> originalElement
//            else -> candidate
//        }
//
//        val isAllowed = when (target) {
//            is CjBinaryExpressionWithTypeRHS, is CjIsExpression -> true
////            is CjOperationExpression -> isReferenceAllowed(target.operationReference, allowMethodCalls)
////            is CjReferenceExpression -> isReferenceAllowed(target, allowMethodCalls)
////            is CjQualifiedExpression -> {
////                val selector = target.selectorExpression
////                selector is CjReferenceExpression && isReferenceAllowed(selector, allowMethodCalls)
////            }
//            else -> true
//        }
//
//        return if (isAllowed) target else findEvaluationTarget(target.parent, allowMethodCalls)
//    }
//
//    private tailrec fun calculateCandidate(originalElement: PsiElement?, allowMethodCalls: Boolean): PsiElement? {
//        val candidate = originalElement?.getParentOfType<CjExpression>(strict = false) ?: return null
//
//        if (!isAcceptedAsCandidate(candidate)) {
//            return null
//        }
//
//        return when (candidate) {
//            is CjParameter -> if (originalElement == candidate.nameIdentifier) candidate.nameIdentifier else null
//            is CjVariableDeclaration -> if (originalElement == candidate.nameIdentifier) candidate.nameIdentifier else null
//
//            is CjDeclaration, is CjFile -> null
//            is CjIfExpression -> if (candidate.`else` != null) candidate else null
//            is CjStatementExpression/*, is CjLabeledExpression */-> null
//            is CjStringTemplateExpression -> if (isStringTemplateAllowed(candidate, allowMethodCalls)) candidate else null
//            is CjConstantExpression -> calculateCandidate(candidate.parent, allowMethodCalls)
//            else -> when (val parent = candidate.parent) {
//                is CjThisExpression -> parent
//                is CjSuperExpression -> calculateCandidate(parent.parent, allowMethodCalls)
//                is CjCallExpression -> calculateCandidate(parent, allowMethodCalls)
//                is CjArrayAccessExpression -> when (parent.arrayExpression) {
//                    candidate -> candidate
//                    else -> calculateCandidate(parent, allowMethodCalls)
//                }
//                is CjQualifiedExpression -> when (parent.receiverExpression) {
//                    candidate -> candidate
//                    else -> calculateCandidate(parent, allowMethodCalls)
//                }
//                is CjOperationExpression -> if (parent.operationReference == candidate) parent else candidate
//                is CjCallableReferenceExpression -> if (parent.callableReference == candidate) parent else candidate
//                else -> candidate
//            }
//        }
//    }
////
////    private fun isReferenceAllowed(reference: CjReferenceExpression, allowMethodCalls: Boolean): Boolean = runDumbAnalyze(reference, fallback = false) f@ {
////        when {
////
////            reference is CjOperationReferenceExpression && reference.operationSignTokenType == CjTokens.ELVIS -> return@f true
////            reference is CjCollectionLiteralExpression -> return@f false
////            reference is CjCallExpression -> {
////        return@f true
////            }
////
////        }
////    }
//
//
//
//    private fun isStringTemplateAllowed(candidate: CjStringTemplateExpression, allowMethodCalls: Boolean) =
//        !candidate.text.startsWith("\"\"\"") || allowMethodCalls
//
//
//    private val FORBIDDEN_PARENT_TYPES = setOf(
//        CjUserType::class.java,
//        CjImportDirective::class.java,
//        CjPackageDirective::class.java,
//        CjValueArgumentName::class.java,
//        CjTypeAlias::class.java,
//        CjAnnotationEntry::class.java,
//        CjDeclarationModifierList::class.java
//    )
//
//    private fun isAcceptedAsCandidate(element: PsiElement): Boolean {
//        return isAccepted(element, FORBIDDEN_PARENT_TYPES)
//    }
//
//    override fun isAcceptedAsCodeFragmentContext(element: PsiElement): Boolean {
//        return isAccepted(element, FORBIDDEN_PARENT_TYPES)
//    }
//
//    private fun isAccepted(element: PsiElement, forbiddenTypes: Set<Class<*>>): Boolean {
//        for (parent in element.parents(withSelf = true)) {
//            if (parent::class.java in forbiddenTypes) {
//                return false
//            }
//        }
//
//        return true
//    }
//}