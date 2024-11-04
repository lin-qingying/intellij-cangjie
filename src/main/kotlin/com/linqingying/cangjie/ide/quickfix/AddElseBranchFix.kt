package com.linqingying.cangjie.ide.quickfix

import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.descriptors.ClassDescriptor
import com.linqingying.cangjie.descriptors.ClassKind
import com.linqingying.cangjie.diagnostics.Diagnostic
import com.linqingying.cangjie.diagnostics.MatchMissingCase
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.psi.psiUtil.endOffset
import com.linqingying.cangjie.psi.psiUtil.getNonStrictParentOfType
import com.linqingying.cangjie.resolve.caches.analyze
import com.linqingying.cangjie.resolve.caches.safeAnalyzeNonSourceRootCode
import com.linqingying.cangjie.types.expressions.match.MatchChecker
import com.intellij.codeInsight.CodeInsightUtilCore
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

sealed class AddElseBranchFix<T : CjExpression>(element: T) : CangJiePsiOnlyQuickFixAction<T>(element) {
    override fun getFamilyName() = CangJieBundle.message("fix.add.else.branch.when")
    override fun getText() = familyName

    abstract override fun isAvailable(project: Project, editor: Editor?, file: CjFile): Boolean

    abstract override fun invoke(project: Project, editor: Editor?, file: CjFile)
}


class AddMatchElseBranchFix(element: CjMatchExpression) : AddElseBranchFix<CjMatchExpression>(element),
    LowPriorityAction {
    override fun isAvailable(project: Project, editor: Editor?, file: CjFile): Boolean = element?.closeBrace != null

    override fun invoke(project: Project, editor: Editor?, file: CjFile) {
        val element = element ?: return
        val whenCloseBrace = element.closeBrace ?: return
        val entry = CjPsiFactory(project).createMatchEntry("case _ => throw Exception(\"Unreachable code\")")
        CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(
            element.addBefore(
                entry,
                whenCloseBrace
            )
        )?.endOffset?.let { offset ->
            editor?.caretModel?.moveToOffset(offset - 1)
        }
    }

    companion object :
        QuickFixesPsiBasedFactory<PsiElement>(PsiElement::class, PsiElementSuitabilityCheckers.ALWAYS_SUITABLE) {
        override fun doCreateQuickFix(psiElement: PsiElement): List<IntentionAction> {
            return listOfNotNull(psiElement.getNonStrictParentOfType<CjMatchExpression>()?.let(::AddMatchElseBranchFix))
        }
    }
}

class AddMatchRemainingBranchesFix(
    expression: CjMatchExpression,
    val withImport: Boolean = false
) : CangJieQuickFixAction<CjMatchExpression>(expression) {

    override fun getFamilyName() = text

    override fun getText(): String {
        if (withImport) {
            return CangJieBundle.message("fix.add.remaining.branches.with.star.import")
        } else {
            return CangJieBundle.message("fix.add.remaining.branches")
        }
    }

    override fun isAvailable(project: Project, editor: Editor?, file: CjFile): Boolean {
        return isAvailable(element)
    }

    override fun invoke(project: Project, editor: Editor?, file: CjFile) {
        addRemainingBranches(element, withImport)
    }

    companion object : CangJieIntentionActionsFactory() {
        private fun CjMatchExpression.hasEnumSubject(): Boolean {
            val subject = subjectExpression ?: return false
            val descriptor = subject.analyze().getType(subject)?.constructor?.declarationDescriptor ?: return false
            return (descriptor as? ClassDescriptor)?.kind == ClassKind.ENUM
        }

        override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction> {
            val whenExpression = diagnostic.psiElement.getNonStrictParentOfType<CjMatchExpression>() ?: return emptyList()
            val actions = mutableListOf(AddMatchRemainingBranchesFix(whenExpression))
            if (whenExpression.hasEnumSubject()) {
                actions += AddMatchRemainingBranchesFix(whenExpression, withImport = true)
            }
            return actions
        }

        fun isAvailable(element: CjMatchExpression?): Boolean {
            if (element == null) return false
            return element.closeBrace != null &&
                    with(MatchChecker.getMissingCases(element, element.safeAnalyzeNonSourceRootCode())) { isNotEmpty() && !hasUnknown }
        }

        fun addRemainingBranches(element: CjMatchExpression?, withImport: Boolean = false) {
//            if (element == null) return
//            val missingCases = MatchChecker.getMissingCases(element, element.analyze())

//            generateMatchBranches(element, missingCases)
//
//            ShortenReferences.DEFAULT.process(element)
//
//            if (withImport) {
//                importAllEntries(element)
//            }
        }

//        private fun importAllEntries(element: CjMatchExpression) {
//            element.entries
//                .map { it.conditions.toList() }
//                .flatten()
//                .firstNotNullOfOrNull {
//                    (it as? CjMatchConditionWithExpression)?.expression as? CjDotQualifiedExpression
//                }?.importReceiverMembers()
//        }
    }
}
val List<MatchMissingCase>.hasUnknown: Boolean
    get() = firstOrNull() == MatchMissingCase.Unknown
