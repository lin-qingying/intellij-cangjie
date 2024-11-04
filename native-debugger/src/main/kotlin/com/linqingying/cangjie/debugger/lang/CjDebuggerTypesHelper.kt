package com.linqingying.cangjie.debugger.lang

import com.linqingying.cangjie.psi.CjElement
import com.linqingying.cangjie.psi.psiUtil.ancestorOrSelf
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.xdebugger.XSourcePosition
import com.jetbrains.cidr.execution.debugger.CidrDebugProcess
import com.jetbrains.cidr.execution.debugger.backend.LLValue
import com.jetbrains.cidr.execution.debugger.evaluation.CidrDebuggerTypesHelper
import com.jetbrains.cidr.execution.debugger.evaluation.CidrMemberValue

class CjDebuggerTypesHelper(process: CidrDebugProcess) : CidrDebuggerTypesHelper(process) {
    override fun createReferenceFromText(`var`: LLValue, context: PsiElement): PsiReference? = null

    override fun computeSourcePosition(value: CidrMemberValue): XSourcePosition? = null

    override fun isImplicitContextVariable(position: XSourcePosition, `var`: LLValue): Boolean = false

    override fun resolveProperty(value: CidrMemberValue, dynamicTypeName: String?): XSourcePosition? = null

//    override fun resolveToDeclaration(position: XSourcePosition?, `var`: LLValue): PsiElement? {
//        val context = getContextElement(position)
//        return resolveToDeclaration(context, `var`.name)
//    }
}

//private fun resolveToDeclaration(ctx: PsiElement?, name: String): PsiElement? {
//    val composite = ctx?.ancestorOrSelf<CjElement>() ?: return null
//    return composite.findInScope(name, VALUES)
//}
