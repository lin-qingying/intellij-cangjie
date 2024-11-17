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
