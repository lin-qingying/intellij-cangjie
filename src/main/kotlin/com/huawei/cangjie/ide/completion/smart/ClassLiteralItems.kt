package com.huawei.cangjie.ide.completion.smart

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.ide.ExpectedInfo
import com.huawei.cangjie.ide.completion.BasicLookupElementFactory
import com.huawei.cangjie.ide.completion.createLookupElementForType
import com.huawei.cangjie.ide.fuzzyType
import com.huawei.cangjie.psi.psiUtil.moveCaret
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.Variance
import com.huawei.cangjie.types.util.makeNotNullable
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
