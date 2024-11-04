package com.linqingying.cangjie.ide.completion.handlers

import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.CjValueArgument
import com.linqingying.cangjie.psi.CjValueArgumentList
import com.linqingying.cangjie.psi.psiUtil.firstIsInstanceOrNull
import com.linqingying.cangjie.psi.psiUtil.parentsWithSelf
import com.linqingying.cangjie.psi.psiUtil.siblings
import com.linqingying.cangjie.renderer.render
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiWhiteSpace

class NamedArgumentInsertHandler(private val parameterName: Name) : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val editor = context.editor

        val (textAfterCompletionArea, doNeedTrailingSpace) = context.file.findElementAt(context.tailOffset).let { psi ->
            psi?.siblings()?.firstOrNull { it !is PsiWhiteSpace }?.text to (psi !is PsiWhiteSpace)
        }

        var text: String
        var caretOffset: Int
        if (textAfterCompletionArea == "=") {
            // User tries to manually rename existing named argument. We shouldn't add trailing `=` in such case
            text = parameterName.render()
            caretOffset = text.length
        } else {
            // For complicated cases let's try to normalize the document firstly in order to avoid parsing errors due to incomplete code
            editor.document.replaceString(context.startOffset, context.tailOffset, "")
            PsiDocumentManager.getInstance(context.project).commitDocument(editor.document)

            val nextArgument = context.file.findElementAt(context.startOffset)?.siblings()
                ?.firstOrNull { it !is PsiWhiteSpace }?.parentsWithSelf?.takeWhile { it !is CjValueArgumentList }
                ?.firstIsInstanceOrNull<CjValueArgument>()

            if (nextArgument?.isNamed() == true) {
                if (doNeedTrailingSpace) {
                    text = "${parameterName.render()} = , "
                    caretOffset = text.length - 2
                } else {
                    text = "${parameterName.render()} = ,"
                    caretOffset = text.length - 1
                }
            } else {
                text = "${parameterName.render()} = "
                caretOffset = text.length
            }
        }

        if (context.file.findElementAt(context.startOffset - 1)?.let { it !is PsiWhiteSpace && it.text != "(" } == true) {
            text = " $text"
            caretOffset++
        }

        editor.document.replaceString(context.startOffset, context.tailOffset, text)
        editor.caretModel.moveToOffset(context.startOffset + caretOffset)
    }
}
