/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package cn.cangnova.cangjie.debugger.lang

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import cn.cangnova.cangjie.ide.run.cjpm.runconfig.CjResult
import cn.cangnova.cangjie.ide.run.cjpm.runconfig.unwrapOrThrow
import com.intellij.xdebugger.XExpression
import com.intellij.xdebugger.XSourcePosition
import com.jetbrains.cidr.execution.debugger.CidrEvaluator
import com.jetbrains.cidr.execution.debugger.CidrStackFrame
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriver
import com.jetbrains.cidr.execution.debugger.evaluation.CidrEvaluatedValue
import cn.cangnova.cangjie.psi.CjExpression
import cn.cangnova.cangjie.psi.psiUtil.ancestorOrSelf

class CjEvaluator(frame: CidrStackFrame) : CidrEvaluator(frame) {



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
