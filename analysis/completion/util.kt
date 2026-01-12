/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.completion

import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.endOffset
import org.cangnova.cangjie.psi.psiUtil.getQualifiedExpressionForSelector
import org.cangnova.cangjie.resolve.caches.analyze
import org.cangnova.cangjie.resolve.caches.getResolutionFacade
import org.cangnova.cangjie.resolve.calls.util.getResolvedCall
import org.cangnova.cangjie.resolve.lazy.BodyResolveMode
import org.cangnova.cangjie.types.error.ErrorType
import org.cangnova.cangjie.utils.CopyablePsiUserDataProperty
import org.cangnova.cangjie.utils.match
import org.cangnova.cangjie.psi.psiUtil.parents
import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.codeinsight.intentions.CangJieInsertExplicitTypeArgumentsIntention
import org.cangnova.cangjie.resolve.binding.BindingContext

fun addParamTypesIfNeeded(position: PsiElement): PsiElement {
    if (!callExprToUpdateExists(position)) return position
    return addParamTypes(position)
}
private fun addParamTypes(position: PsiElement): PsiElement {

    data class CallAndDiff(
        // callExpression is a child node of dotExprWithoutCaret in general case (in simple case they are equal)
        val callExpression: CjCallExpression,  // like call()
        val dotExprWithoutCaret: CjExpression, // like smth.call()
        val dotExprWithCaret: CjQualifiedExpression // initial expression like smth.call().IntellijIdeaRulezzz (now separate synthetic tree)
    )

    fun getCallWithParamTypesToAdd(positionInCopy: PsiElement): CallAndDiff? {
        /*
        ............CjDotQualifiedExpression [call().IntellijIdeaRulezzz]
        ...............CjCallExpression [call()]
        ............................................................
        ...............CjNameReferenceExpression [IntellijIdeaRulezzz]
        ..................LeafPsiElement [IntellijIdeaRulezzz] (*) <= positionInCopy

        Replacing CjQualifiedExpression with its nested CjCallExpression we're getting "non-broken" tree.
        */

        val dotExprWithCaret =
            positionInCopy.parents.match(CjNameReferenceExpression::class, last = CjQualifiedExpression::class) ?: return null
        val dotExprWithCaretCopy = dotExprWithCaret.copy() as CjQualifiedExpression

        val beforeDotExpr = dotExprWithCaret.receiverExpression // smth.call()
        val dotExpressionWithoutCaret = dotExprWithCaret.replace(beforeDotExpr) as CjExpression // dotExprWithCaret = beforeDotExpr + '.[?]' + caret
        val targetCall = dotExpressionWithoutCaret.findLastCallExpression() ?: return null // call()

        return CallAndDiff(targetCall, dotExpressionWithoutCaret, dotExprWithCaretCopy)
    }

    fun applyTypeArguments(callAndDiff: CallAndDiff, bindingContext: BindingContext): Pair<CjTypeArgumentList, PsiElement>? {
        val (callExpression, dotExprWithoutCaret, dotExprWithCaret) = callAndDiff

        // CjCallExpression [call()]
        CangJieInsertExplicitTypeArgumentsIntention.applyTo(callExpression, false) // affects dotExprWithoutCaret as a parent
        // CjCallExpression [call<TypeA, TypeB>()]

        val dotExprWithoutCaretCopy = dotExprWithoutCaret.copy() as CjExpression

        // Now we're restoring original smth.call().IntellijIdeaRulezzz on its place and
        // replace call() with call<TypeA, TypeB>().

        // smth.call() -> smth.call().IntellijIdeaRulezzz
        val originalDotExpr = dotExprWithoutCaret.replace(dotExprWithCaret) as CjQualifiedExpression
        val originalNestedDotExpr = originalDotExpr.receiverExpression // smth.call()
        originalNestedDotExpr.replace(dotExprWithoutCaretCopy) // smth.call() -> smth.call<TypeA, TYpeB>

        // IntellijIdeaRulezzz as before
        val newPosition = (originalDotExpr.selectorExpression as? CjNameReferenceExpression)?.referencedNameElement ?: return null
        val typeArguments = CangJieInsertExplicitTypeArgumentsIntention.createTypeArguments(callExpression, bindingContext) ?: return null

        return typeArguments to newPosition

    }

    val fileCopy = position.containingFile.copy() as CjFile
    val positionInCopy = PsiTreeUtil.findSameElementInCopy(position, fileCopy)
    val callAndDiff = getCallWithParamTypesToAdd(positionInCopy) ?: return position
    val (callExpression, dotExprWithoutCaret, _) = callAndDiff

    val bindingContext = fileCopy.getResolutionFacade().analyze(dotExprWithoutCaret, BodyResolveMode.PARTIAL_FOR_COMPLETION)

//    if (!InsertExplicitTypeArgumentsIntention.isApplicableTo(callExpression, bindingContext))
//        return position

    // We need to fix expression offset so that later 'typeArguments' could be inserted into the editor.
    // See usages of `argList` -> JustTypingLookupElementDecorator#handleInsert.

    val exprOffset = callExpression.endOffset // applyTypeArguments modifies PSI, offset is to be calculated before
    val (typeArguments, newPosition) = applyTypeArguments(callAndDiff, bindingContext) ?: return position

    return newPosition.also { it.argList = TypeArgsWithOffset(typeArguments, exprOffset) }
}
private fun callExprToUpdateExists(position: PsiElement): Boolean {
    /*
     Case: call().IntellijIdeaRulezzz or call()?.IntellijIdeaRulezzz or smth.call()?.IntellijIdeaRulezzz
     'position' points to the caret - IntellijIdeaRulezzz and on PSI level it looks as follows:
     ............CjDotQualifiedExpression [call().IntellijIdeaRulezzz]
     ..............CjCallExpression [call()]
     .............................................................
     ..............CjNameReferenceExpression [IntellijIdeaRulezzz]
     ..................LeafPsiElement [IntellijIdeaRulezzz] (*)
     */
    val afterDotExprWithCaret = position.parent as? CjNameReferenceExpression ?: return false
    val callBeforeDot = afterDotExprWithCaret.getPreviousInQualifiedChain() as? CjCallExpression ?: return false
    return callBeforeDot.requiresTypeParams()
}
private fun CjExpression.getPreviousInQualifiedChain(): CjExpression? {
    val receiverExpression = getQualifiedExpressionForSelector()?.receiverExpression
    return (receiverExpression as? CjQualifiedExpression)?.selectorExpression ?: receiverExpression
}
private fun CjCallExpression.requiresTypeParams(): Boolean {
    if (typeArguments.isNotEmpty()) return false

    val bindingContext = analyze(BodyResolveMode.PARTIAL)
    val resolvedCall = getResolvedCall(bindingContext) ?: return false
    if (resolvedCall.typeArguments.isEmpty()) return false

    return resolvedCall.typeArguments.values.any { type -> type is ErrorType }
}
var UserDataHolder.argList: TypeArgsWithOffset? by UserDataProperty(Key("CangJieInsertTypeArgument.ARG_LIST"))
data class TypeArgsWithOffset(val args: CjTypeArgumentList, val offset: Int)
private fun CjExpression.findLastCallExpression() =
    ((this as? CjQualifiedExpression)?.selectorExpression ?: this) as? CjCallExpression

fun PrefixMatcher.asStringNameFilter() = { name: String -> prefixMatches(name) }

var CjCodeFragment.extraCompletionFilter: ((LookupElement) -> Boolean)? by CopyablePsiUserDataProperty(Key.create("EXTRA_COMPLETION_FILTER"))
