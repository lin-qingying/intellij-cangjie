/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package com.linqingying.cangjie.debugger.lang

import com.linqingying.cangjie.ide.run.cjpm.runconfig.CjResult
import com.linqingying.cangjie.ide.run.cjpm.runconfig.unwrapOrThrow
import com.intellij.xdebugger.XExpression
import com.intellij.xdebugger.XSourcePosition
import com.jetbrains.cidr.execution.debugger.CidrEvaluator
import com.jetbrains.cidr.execution.debugger.CidrStackFrame
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriver
import com.jetbrains.cidr.execution.debugger.evaluation.CidrEvaluatedValue

class CjEvaluator(frame: CidrStackFrame) : CidrEvaluator(frame) {
//    override fun getExpressionRangeAtOffset(
//        project: Project,
//        document: Document,
//        offset: Int,
//        sideEffectsAllowed: Boolean
//    ): TextRange? = runReadAction {
//        document.toPsiFile(project)?.let { file ->
//            findSuitableExpression(file, offset)?.textRange
//        }
//    }

//    private fun findSuitableExpression(file: PsiFile, offset: Int): PsiElement? {
//        val pathExpr = file.findElementAt(offset)?.ancestorOrSelf<RsPathExpr>() ?: return null
//        val resolved = pathExpr.path.reference?.resolve()
//        return if (resolved is CjPatBinding) pathExpr else null
//    }

    override fun doEvaluate(driver: DebuggerDriver, position: XSourcePosition?, expr: XExpression): CidrEvaluatedValue {
        val project = myFrame.process.project
        val result = try {
            val v = driver.evaluate(myFrame.thread, myFrame.frame, expr.expression)
            CjResult.Ok(CidrEvaluatedValue(v, myFrame.process, position, myFrame, expr.expression))
        } catch (e: Throwable) {
            CjResult.Err(e)
        }
        return result.unwrapOrThrow()
    }

}
