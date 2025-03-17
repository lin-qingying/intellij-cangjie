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

package cn.cangnova.cangjie.ide.completion.handlers

import cn.cangnova.cangjie.utils.withRootPrefixIfNeeded
import cn.cangnova.cangjie.descriptors.CallableDescriptor
import cn.cangnova.cangjie.ide.ShortenReferences
import cn.cangnova.cangjie.ide.completion.DescriptorBasedDeclarationLookupObject
import cn.cangnova.cangjie.ide.completion.isArtificialImportAliasedDescriptor
import cn.cangnova.cangjie.ide.completion.shortenReferences
import cn.cangnova.cangjie.ide.imports.ImportInsertHelper
import cn.cangnova.cangjie.ide.imports.importableFqName
import cn.cangnova.cangjie.psi.CjFile
import cn.cangnova.cangjie.renderer.render
import cn.cangnova.cangjie.resolve.DescriptorUtils
import cn.cangnova.cangjie.utils.CallType
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.psi.PsiDocumentManager

abstract class CangJieCallableInsertHandler(val callType: CallType<*>) : BaseDeclarationInsertHandler() {
    companion object {
        val SHORTEN_REFERENCES = ShortenReferences { ShortenReferences.Options.DEFAULT.copy(dropBracesInStringTemplates = false) }

        fun addImport(context: InsertionContext, item: LookupElement, callType: CallType<*>) {
            val psiDocumentManager = PsiDocumentManager.getInstance(context.project)
            psiDocumentManager.commitDocument(context.document)

            val file = context.file
            val o = item.`object`
            if (file is CjFile && o is DescriptorBasedDeclarationLookupObject) {
                val descriptor = o.descriptor as? CallableDescriptor ?: return
                if (descriptor.extensionReceiverParameter != null || callType is CallType.CallableReference) {
                    if (DescriptorUtils.isTopLevelDeclaration(descriptor) && !descriptor.isArtificialImportAliasedDescriptor) {
                        ImportInsertHelper.getInstance(context.project).importDescriptor(file, descriptor)
                    }
                } else if (callType == CallType.DEFAULT) {
                    if (descriptor.isArtificialImportAliasedDescriptor) return
                    val fqName = descriptor.importableFqName ?: return
                    context.document.replaceString(
                        context.startOffset,
                        context.tailOffset,
                        fqName.withRootPrefixIfNeeded().render() + " "
                    ) // insert space after for correct parsing

                    psiDocumentManager.commitDocument(context.document)

                    shortenReferences(context, context.startOffset, context.tailOffset - 1, SHORTEN_REFERENCES)

                    psiDocumentManager.doPostponedOperationsAndUnblockDocument(context.document)

                    // delete space
                    if (context.document.isTextAt(context.tailOffset - 1, " ")) { // sometimes space can be lost because of reformatting
                        context.document.deleteString(context.tailOffset - 1, context.tailOffset)
                    }
                }
            }
        }
    }

    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        super.handleInsert(context, item)

        addImport(context, item, callType)
    }
}
