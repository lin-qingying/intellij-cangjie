package com.linqingying.cangjie.ide.completion.handlers

import com.linqingying.cangjie.descriptors.CallableDescriptor
import com.linqingying.cangjie.ide.IdeDescriptorRenderers
import com.linqingying.cangjie.ide.ShortenReferences
import com.linqingying.cangjie.ide.completion.DescriptorBasedDeclarationLookupObject
import com.linqingying.cangjie.psi.CjBinaryExpressionWithTypeRHS
import com.linqingying.cangjie.psi.CjParenthesizedExpression
import com.linqingying.cangjie.psi.CjQualifiedExpression
import com.linqingying.cangjie.psi.CjSimpleNameExpression
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiTreeUtil
import com.linqingying.cangjie.psi.CjPsiFactory

object CastReceiverInsertHandler {
    fun postHandleInsert(context: InsertionContext, item: LookupElement) {
        val expression =
            PsiTreeUtil.findElementOfClassAtOffset(context.file, context.startOffset, CjSimpleNameExpression::class.java, false)
        val qualifiedExpression = PsiTreeUtil.getParentOfType(expression, CjQualifiedExpression::class.java, true)
        if (qualifiedExpression != null) {
            val receiver = qualifiedExpression.receiverExpression

            val descriptor = (item.`object` as? DescriptorBasedDeclarationLookupObject)?.descriptor as CallableDescriptor
            val project = context.project

            val thisObj =
                if (descriptor.extensionReceiverParameter != null) descriptor.extensionReceiverParameter else descriptor.dispatchReceiverParameter
            val fqName = IdeDescriptorRenderers.SOURCE_CODE.renderClassifierName(thisObj!!.type.constructor.declarationDescriptor!!)

            val parentCast = CjPsiFactory(project).createExpression("(expr as $fqName)") as CjParenthesizedExpression
            val cast = parentCast.expression as CjBinaryExpressionWithTypeRHS
            cast.left.replace(receiver)

            val psiDocumentManager = PsiDocumentManager.getInstance(project)
            psiDocumentManager.commitDocument(context.document)
            psiDocumentManager.doPostponedOperationsAndUnblockDocument(context.document)

            val expr = receiver.replace(parentCast) as CjParenthesizedExpression

            ShortenReferences.DEFAULT.process((expr.expression as CjBinaryExpressionWithTypeRHS).right!!)
        }
    }
}
