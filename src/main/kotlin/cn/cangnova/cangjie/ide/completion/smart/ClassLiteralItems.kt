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

package cn.cangnova.cangjie.ide.completion.smart

import cn.cangnova.cangjie.builtins.CangJieBuiltIns
import cn.cangnova.cangjie.descriptors.ClassDescriptor
import cn.cangnova.cangjie.ide.ExpectedInfo
import cn.cangnova.cangjie.ide.completion.BasicLookupElementFactory
import cn.cangnova.cangjie.ide.completion.createLookupElementForType
import cn.cangnova.cangjie.ide.fuzzyType
import cn.cangnova.cangjie.psi.psiUtil.moveCaret
import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.types.Variance
import cn.cangnova.cangjie.types.util.makeNotNullable
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.psi.PsiDocumentManager
import java.util.ArrayList
import java.util.LinkedHashMap

object ClassLiteralItems {
    fun addToCollection(
        collection: MutableCollection<LookupElement>,
        expectedInfos: Collection<ExpectedInfo>,
        lookupElementFactory: BasicLookupElementFactory,

    ) {
        val typeAndSuffixToExpectedInfos = LinkedHashMap<Pair<CangJieType, String>, MutableList<ExpectedInfo>>()

        for (expectedInfo in expectedInfos) {
            val fuzzyType = expectedInfo.fuzzyType ?: continue
            if (fuzzyType.freeParameters.isNotEmpty()) continue
            val typeConstructor = fuzzyType.type.constructor
            val klass = typeConstructor.declarationDescriptor as? ClassDescriptor ?: continue
            val typeArgument = fuzzyType.type.arguments.singleOrNull() ?: continue
            if (typeArgument.projectionKind != Variance.INVARIANT) continue



        }

        for ((pair, matchedExpectedInfos) in typeAndSuffixToExpectedInfos) {
            val (type, suffix) = pair
            val typeToUse = if (CangJieBuiltIns.isArray(type)) {
                type.makeNotNullable()
            } else {
                val classifier = (type.constructor.declarationDescriptor as? ClassDescriptor) ?: continue
                classifier.defaultType
            }

            var lookupElement = lookupElementFactory.createLookupElementForType(typeToUse) ?: continue

            lookupElement = object : LookupElementDecorator<LookupElement>(lookupElement) {
                override fun renderElement(presentation: LookupElementPresentation) {
                    super.renderElement(presentation)
                    presentation.itemText += suffix
                }

                override fun handleInsert(context: InsertionContext) {
                    super.handleInsert(context)

                    PsiDocumentManager.getInstance(context.project).doPostponedOperationsAndUnblockDocument(context.document)

                    val offset = context.tailOffset
                    context.document.insertString(offset, suffix)
                    context.editor.moveCaret(offset + suffix.length)
                }
            }
            lookupElement.assignSmartCompletionPriority(SmartCompletionItemPriority.CLASS_LITERAL)
            lookupElement = lookupElement.addTailAndNameSimilarity(matchedExpectedInfos, emptyList())
            collection.add(lookupElement)
        }
    }
}
