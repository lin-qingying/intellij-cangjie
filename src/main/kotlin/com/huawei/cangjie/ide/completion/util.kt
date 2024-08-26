package com.huawei.cangjie.ide.completion

import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.psiUtil.endOffset
import com.huawei.cangjie.psi.psiUtil.getQualifiedExpressionForSelector
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.caches.analyze
import com.huawei.cangjie.resolve.caches.getResolutionFacade
import com.huawei.cangjie.resolve.calls.util.getResolvedCall
import com.huawei.cangjie.resolve.lazy.BodyResolveMode
import com.huawei.cangjie.types.error.ErrorType
import com.huawei.cangjie.utils.CopyablePsiUserDataProperty
import com.huawei.cangjie.utils.match
import com.huawei.cangjie.psi.psiUtil.parents
import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

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
//        val (callExpression, dotExprWithoutCaret, dotExprWithCaret) = callAndDiff
//
//        // CjCallExpression [call()]
//        InsertExplicitTypeArgumentsIntention.applyTo(callExpression, false) // affects dotExprWithoutCaret as a parent
//        // CjCallExpression [call<TypeA, TypeB>()]
//
//        val dotExprWithoutCaretCopy = dotExprWithoutCaret.copy() as CjExpression
//
//        // Now we're restoring original smth.call().IntellijIdeaRulezzz on its place and
//        // replace call() with call<TypeA, TypeB>().
//
//        // smth.call() -> smth.call().IntellijIdeaRulezzz
//        val originalDotExpr = dotExprWithoutCaret.replace(dotExprWithCaret) as CjQualifiedExpression
//        val originalNestedDotExpr = originalDotExpr.receiverExpression // smth.call()
//        originalNestedDotExpr.replace(dotExprWithoutCaretCopy) // smth.call() -> smth.call<TypeA, TYpeB>
//
//        // IntellijIdeaRulezzz as before
//        val newPosition = (originalDotExpr.selectorExpression as? CjNameReferenceExpression)?.getReferencedNameElement() ?: return null
//        val typeArguments = InsertExplicitTypeArgumentsIntention.createTypeArguments(callExpression, bindingContext) ?: return null
//
//        return typeArguments to newPosition
        return null
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
var UserDataHolder.argList: TypeArgsWithOffset? by UserDataProperty(Key("KotlinInsertTypeArgument.ARG_LIST"))
data class TypeArgsWithOffset(val args: CjTypeArgumentList, val offset: Int)
private fun CjExpression.findLastCallExpression() =
    ((this as? CjQualifiedExpression)?.selectorExpression ?: this) as? CjCallExpression

fun PrefixMatcher.asStringNameFilter() = { name: String -> prefixMatches(name) }

var CjCodeFragment.extraCompletionFilter: ((LookupElement) -> Boolean)? by CopyablePsiUserDataProperty(Key.create("EXTRA_COMPLETION_FILTER"))
