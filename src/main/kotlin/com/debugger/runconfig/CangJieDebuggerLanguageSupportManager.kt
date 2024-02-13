package com.debugger.runconfig

import com.huawei.cangjie.psi.psiUtil.toPsiFile
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator
import com.intellij.xdebugger.impl.evaluate.quick.XDebuggerPsiEvaluator
import dap.type.EvaluateArgumentsContext

object CangJieDebuggerLanguageSupportManager {


    fun createEvaluator(frame: CangJieStackFrame): CangJieEvaluator {
        return CangJieEvaluator(frame)
    }
}


class CangJieEvaluator(private val frame: CangJieStackFrame) : XDebuggerEvaluator(), XDebuggerPsiEvaluator {
    val process = frame.process
    override fun evaluate(expression: String, callback: XEvaluationCallback, expressionPosition: XSourcePosition?) {

        val res = process.evaluate(expression, frame.frame.id, EvaluateArgumentsContext.Hover)
        val value = res.body?.toVariable(expression)

        val cangJieValue = value?.let { it1 ->
            frame.scope?.body?.scopes?.get(0)
                ?.let { it2 -> CangJieValue(process, it1, it2.variablesReference) }
        }
        cangJieValue?.let { it1 -> callback.evaluated(it1) }

//        res.thenAccept {
//
//            if (it.success) {
//                val value = it.body?.toVariable(expression)
//
//                val cangJieValue = value?.let { it1 ->
//                    frame.scope?.body?.scopes?.get(0)
//                        ?.let { it2 -> CangJieValue(process, it1, it2.variablesReference) }
//                }
//
//                cangJieValue?.let { it1 -> callback.evaluated(it1) }
////            val value = CangJieValue(process, it)
//            } else {
//                callback.errorOccurred(it.message.toString())
//            }
//
//
//        }

    }


    private fun findElementAt(file: PsiFile?, offset: Int): PsiElement? {
        return if (file != null) file.findElementAt(offset)!! else null
    }

    override fun getExpressionRangeAtOffset(
        project: Project,
        document: Document,
        offset: Int,
        sideEffectsAllowed: Boolean
    ): TextRange? = runReadAction {
        document.toPsiFile(project)?.let { file ->
            findElementAt(file, offset)?.textRange
        }
    }

//    override fun getExpressionInfoAtOffsetAsync(
//        project: Project,
//        document: Document,
//        offset: Int,
//        sideEffectsAllowed: Boolean
//    ): Promise<ExpressionInfo> {
//
//
//
////        return ReadAction.nonBlocking<ExpressionInfo> {
////            null
////        }.submit(AppExecutorUtil.getAppExecutorService())
//        return ReadAction
//            .nonBlocking<ExpressionInfo?> {
//                val elementAtCursor: PsiElement? =  findElementAt(
//                    PsiDocumentManager.getInstance(project).getPsiFile(document),
//                    offset
//                )
////                return@nonBlocking ExpressionInfo(
////
////                )
//                if (elementAtCursor != null && elementAtCursor.isValid) {
//                    val textProvider: EditorTextProvider = EditorTextProvider.EP.forLanguage(elementAtCursor.language)
//                    if (textProvider != null) {
//                        val pair: Pair<PsiElement, TextRange> =
//                            textProvider.findExpression(elementAtCursor, sideEffectsAllowed)
//                        if (pair != null) {
//                            val element = pair.getFirst()
//                            return@nonBlocking ExpressionInfo(
//                                pair.getSecond(),
//                                null,
//                                null,
//                                element as? PsiExpression
//                            )
//                        }
//                    }
//                }
//                null
//            }
//            .inSmartMode(project)
//            .withDocumentsCommitted(project)
//            .submit(AppExecutorUtil.getAppExecutorService())
//    }

    override fun evaluate(element: PsiElement, callback: XEvaluationCallback) {
        TODO("Not yet implemented")
    }

}
