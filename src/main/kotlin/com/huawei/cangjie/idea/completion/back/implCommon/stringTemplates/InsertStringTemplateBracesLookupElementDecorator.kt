package com.huawei.cangjie.idea.completion.back.implCommon.stringTemplates

import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.CjNameReferenceExpression
import com.huawei.cangjie.psi.psiUtil.startOffset
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile


fun wrapLookupElementForStringTemplateAfterDotCompletion(originLookupElement: LookupElement): LookupElement {
    return LookupElementDecorator.withDelegateInsertHandler(originLookupElement) { insertionContext: InsertionContext, lookupElement: LookupElement ->
        insertStringTemplateBraces(insertionContext, lookupElement)
    }
}
private fun insertStringTemplateBraces(insertionContext: InsertionContext, lookupElement: LookupElement) {
    val document = insertionContext.document
    val startOffset = insertionContext.startOffset

    val psiDocumentManager = PsiDocumentManager.getInstance(insertionContext.project)
    psiDocumentManager.commitAllDocuments()

    val token = getToken(insertionContext.file, document.charsSequence, startOffset)
    val nameRef = token.parent as CjNameReferenceExpression

    document.insertString(nameRef.startOffset, "{")

    val tailOffset = insertionContext.tailOffset
    document.insertString(tailOffset, "}")
    insertionContext.tailOffset = tailOffset

    lookupElement.handleInsert(insertionContext)
}
private fun getToken(file: PsiFile, charsSequence: CharSequence, startOffset: Int): PsiElement {
    assert(startOffset > 1 && charsSequence[startOffset - 1] == '.')
    val token = file.findElementAt(startOffset - 2)!!
    return if (token.node.elementType == CjTokens.IDENTIFIER || token.node.elementType == CjTokens.THIS_KEYWORD)
        token
    else
        getToken(file, charsSequence, token.startOffset + 1)
}
