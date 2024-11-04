package com.linqingying.cangjie.debugger.lang

import com.linqingying.cangjie.ide.run.cjpm.CjpmCommandConfiguration
import com.intellij.execution.configurations.RunProfile
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider
import com.jetbrains.cidr.execution.debugger.*
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriver
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriver.DebuggerLanguage
import com.jetbrains.cidr.execution.debugger.evaluation.CidrDebuggerTypesHelperBase


object CJ : DebuggerLanguage{

}

class CjDebuggerLanguageSupport: CidrDebuggerLanguageSupport() {
    override fun getSupportedDebuggerLanguages() =   setOf(CJ)

    override fun createEditor(profile: RunProfile?): XDebuggerEditorsProvider? {
        if (profile !is CjpmCommandConfiguration) return null
        return createEditorProvider()
    }

    override fun createDebuggerTypesHelper(process: CidrDebugProcess): CidrDebuggerTypesHelperBase =
        CjDebuggerTypesHelper(process)

    override fun createEvaluator(frame: CidrStackFrame): CidrEvaluator =
        CjEvaluator(frame)

//    override fun createFrameTypeDecorator(frame: CidrStackFrame): CidrFrameTypeDecorator {
//        return CjFrameTypeDecorator(frame)
//    }
}
