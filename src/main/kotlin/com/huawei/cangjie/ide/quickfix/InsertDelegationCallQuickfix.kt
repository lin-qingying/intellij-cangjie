package com.huawei.cangjie.ide.quickfix

import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.diagnostics.Diagnostic
import com.huawei.cangjie.psi.CjClass
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.psi.CjSecondaryConstructor
import com.huawei.cangjie.psi.CjTypeStatement
import com.huawei.cangjie.psi.psiUtil.getNonStrictParentOfType
import com.huawei.cangjie.psi.psiUtil.moveCaret
import com.huawei.cangjie.resolve.caches.descriptor
import com.huawei.cangjie.resolve.caches.resolveToCall
import com.huawei.cangjie.resolve.caches.unsafeResolveToDescriptor
import com.huawei.cangjie.resolve.calls.model.isReallySuccess
import com.huawei.cangjie.resolve.lazy.BodyResolveMode
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project

class InsertDelegationCallQuickfix(val isThis: Boolean, element: CjSecondaryConstructor) :
    CangJieQuickFixAction<CjSecondaryConstructor>(element) {
    override fun getText() = CangJieBundle.message("fix.insert.delegation.call", keywordToUse)
    override fun getFamilyName() = CangJieBundle.message("insert.explicit.delegation.call")

    private val keywordToUse = if (isThis) "this" else "super"

    override fun invoke(project: Project, editor: Editor?, file: CjFile) {
        val element = element ?: return
        val newDelegationCall = element.replaceImplicitDelegationCallWithExplicit(isThis)

        val resolvedCall = newDelegationCall.resolveToCall(BodyResolveMode.FULL)
        val descriptor = element.unsafeResolveToDescriptor()

        // if empty call is ok and it's resolved to another constructor, do not move caret
        if (resolvedCall?.isReallySuccess() == true && resolvedCall.candidateDescriptor.original != descriptor) return

        val leftParOffset = newDelegationCall.valueArgumentList!!.leftParenthesis!!.textOffset

        editor?.moveCaret(leftParOffset + 1)
    }

    override fun isAvailable(project: Project, editor: Editor?, file: CjFile): Boolean {
        val element = element ?: return false
        return element.hasImplicitDelegationCall()
    }

    object InsertThisDelegationCallFactory : CangJieSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic) =
            diagnostic.createIntentionForFirstParentOfType<CjSecondaryConstructor> { secondaryConstructor ->
                return if (secondaryConstructor.getContainingTypeStatement().getConstructorsCount() <= 1 ||
                    !secondaryConstructor.hasImplicitDelegationCall()
                ) null
                else
                    InsertDelegationCallQuickfix(isThis = true, element = secondaryConstructor)
            }

        private fun CjTypeStatement.getConstructorsCount() = (descriptor as ClassDescriptor).constructors.size
    }

    object InsertSuperDelegationCallFactory : CangJieSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val secondaryConstructor =
                diagnostic.psiElement.getNonStrictParentOfType<CjSecondaryConstructor>() ?: return null
            if (!secondaryConstructor.hasImplicitDelegationCall()) return null
            val cclass = secondaryConstructor.getContainingTypeStatement() as? CjClass ?: return null
            if (cclass.hasPrimaryConstructor()) return null

            return InsertDelegationCallQuickfix(isThis = false, element = secondaryConstructor)
        }
    }
}
