package com.linqingying.cangjie.ide.completion.handlers

import com.linqingying.cangjie.analyzer.withRootPrefixIfNeeded
import com.linqingying.cangjie.descriptors.CallableDescriptor
import com.linqingying.cangjie.ide.ShortenReferences
import com.linqingying.cangjie.ide.completion.DescriptorBasedDeclarationLookupObject
import com.linqingying.cangjie.ide.completion.isArtificialImportAliasedDescriptor
import com.linqingying.cangjie.ide.completion.shortenReferences
import com.linqingying.cangjie.ide.imports.ImportInsertHelper
import com.linqingying.cangjie.ide.imports.importableFqName
import com.linqingying.cangjie.psi.CjFile
import com.linqingying.cangjie.renderer.render
import com.linqingying.cangjie.resolve.DescriptorUtils
import com.linqingying.cangjie.utils.CallType
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
