package com.linqingying.cangjie.ide.completion.smart

import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.ide.ExpectedInfo
import com.linqingying.cangjie.ide.fuzzyType
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.psi.CjAnnotationEntry
import com.linqingying.cangjie.psi.CjClass
import com.linqingying.cangjie.psi.CjParameter
import com.linqingying.cangjie.psi.CjTypeStatement
import com.linqingying.cangjie.psi.psiUtil.getParentOfType
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiElement

object ArrayLiteralsInAnnotationItems {

    private fun MutableCollection<LookupElement>.addForUsage(
        expectedInfos: Collection<ExpectedInfo>,
        position: PsiElement
    ) {
        if (position.getParentOfType<CjAnnotationEntry>(false) != null) {
            expectedInfos.asSequence()
                .filter { it.fuzzyType?.type?.let { type -> CangJieBuiltIns.isArray(type) } == true }
                .filterNot { it.itemOptions.starPrefix }
                .mapTo(this) { createLookupElement() }
        }
    }

    private fun MutableCollection<LookupElement>.addForDefaultArguments(
        expectedInfos: Collection<ExpectedInfo>,
        position: PsiElement
    ) {

        // CLASS [MODIFIER_LIST, PRIMARY_CONSTRUCTOR [VALUE_PARAMETER_LIST [VALUE_PARAMETER [..., REFERENCE_EXPRESSION=position]]]]
        val valueParameter = position.parent as? CjParameter ?: return
        val klass = position.getParentOfType<CjTypeStatement>(true) ?: return
//        if (!klass.hasModifier(CjTokens.ANNOTATION_KEYWORD)) return
        val primaryConstructor = klass.primaryConstructor ?: return

        if (primaryConstructor.valueParameterList == valueParameter.parent) {
            expectedInfos.filter { it.fuzzyType?.type?.let { type -> CangJieBuiltIns.isArray(type) } == true }
                .mapTo(this) { createLookupElement() }
        }
    }

    private fun createLookupElement(): LookupElement = LookupElementBuilder.create("[]")
        .withInsertHandler { context, _ ->
            context.editor.caretModel.moveToOffset(context.tailOffset - 1)
        }
        .apply { putUserData(SMART_COMPLETION_ITEM_PRIORITY_KEY, SmartCompletionItemPriority.ARRAY_LITERAL_IN_ANNOTATION) }

    fun collect(expectedInfos: Collection<ExpectedInfo>, position: PsiElement): Collection<LookupElement> =
        mutableListOf<LookupElement>().apply {
            addForUsage(expectedInfos, position)
            addForDefaultArguments(expectedInfos, position)
        }
}
