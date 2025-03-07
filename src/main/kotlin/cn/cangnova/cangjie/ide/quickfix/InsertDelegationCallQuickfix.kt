/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package cn.cangnova.cangjie.ide.quickfix

import cn.cangnova.cangjie.CangJieBundle
import cn.cangnova.cangjie.descriptors.ClassDescriptor
import cn.cangnova.cangjie.diagnostics.Diagnostic
import cn.cangnova.cangjie.psi.CjClass
import cn.cangnova.cangjie.psi.CjFile
import cn.cangnova.cangjie.psi.CjSecondaryConstructor
import cn.cangnova.cangjie.psi.CjTypeStatement
import cn.cangnova.cangjie.psi.psiUtil.getNonStrictParentOfType
import cn.cangnova.cangjie.psi.psiUtil.moveCaret
import cn.cangnova.cangjie.resolve.caches.descriptor
import cn.cangnova.cangjie.resolve.caches.resolveToCall
import cn.cangnova.cangjie.resolve.caches.unsafeResolveToDescriptor
import cn.cangnova.cangjie.resolve.calls.model.isReallySuccess
import cn.cangnova.cangjie.resolve.lazy.BodyResolveMode
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
