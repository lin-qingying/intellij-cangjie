package com.linqingying.cangjie.ide.completion

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.highlighter.AbstractCangJieHighlightVisitor
import com.linqingying.cangjie.psi.CjCallExpression
import com.linqingying.cangjie.psi.CjElement
import com.linqingying.cangjie.psi.CjNameReferenceExpression
import com.linqingying.cangjie.psi.psiUtil.forEachDescendantOfType
import com.linqingying.cangjie.psi.psiUtil.startOffset
import com.linqingying.cangjie.references.resolveMainReferenceToDescriptors
import com.linqingying.cangjie.utils.CallTypeAndReceiver
import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.progress.ProgressManager
import java.util.HashSet

class FromUnresolvedNamesCompletion(
    private val collector: LookupElementsCollector,
    private val prefixMatcher: PrefixMatcher
) {
    fun addNameSuggestions(scope: CjElement, afterOffset: Int?, sampleDescriptor: DeclarationDescriptor?) {
        val names = HashSet<String>()
        scope.forEachDescendantOfType<CjNameReferenceExpression> { refExpr ->
            ProgressManager.checkCanceled()

            if (AbstractCangJieHighlightVisitor.wasUnresolved(refExpr)) {
                val callTypeAndReceiver = CallTypeAndReceiver.detect(refExpr)
                if (callTypeAndReceiver.receiver != null) return@forEachDescendantOfType
                if (sampleDescriptor != null) {
                    if (!callTypeAndReceiver.callType.descriptorKindFilter.accepts(sampleDescriptor)) return@forEachDescendantOfType

                    if (callTypeAndReceiver is CallTypeAndReceiver.DEFAULT) {
                        val isCall = refExpr.parent is CjCallExpression
                        val canBeUsage = when (sampleDescriptor) {
                            is FunctionDescriptor -> isCall // cannot use simply function name without arguments
                            is VariableDescriptor -> true // variable can as well be used with arguments when it has invoke()
                            is ClassDescriptor -> if (isCall)
                                sampleDescriptor.kind == ClassKind.CLASS
                            else
                                sampleDescriptor.kind.isSingleton
                            else -> false // what else it can be?
                        }
                        if (!canBeUsage) return@forEachDescendantOfType
                    }
                }

                val name = refExpr.getReferencedName()
                if (!prefixMatcher.prefixMatches(name)) return@forEachDescendantOfType

                if (afterOffset != null && refExpr.startOffset < afterOffset) return@forEachDescendantOfType

                if (refExpr.resolveMainReferenceToDescriptors().isEmpty()) {
                    names.add(name)
                }
            }
        }

        for (name in names.sorted()) {
            val lookupElement =
                LookupElementBuilder.create(name).suppressAutoInsertion().assignPriority(ItemPriority.FROM_UNRESOLVED_NAME_SUGGESTION)
            lookupElement.suppressItemSelectionByCharsOnTyping = true
            collector.addElement(lookupElement)
        }
    }
}
