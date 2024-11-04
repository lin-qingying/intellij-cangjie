package com.linqingying.cangjie.debugger.breakpoint.types

import com.huawei.bitfun.intellij.breakpoint.type.DataBreakpointTypeBase
import com.huawei.bitfun.utils.CodeCheckByPassUtils
import com.intellij.xdebugger.impl.breakpoints.XBreakpointUtil
import java.util.Objects
import one.util.streamex.StreamEx
import com.huawei.bitfun.intellij.breakpoint.type.InstructionBreakpointTypeBase
import com.huawei.bitfun.intellij.breakpoint.type.SourceBreakpointTypeBase
import com.huawei.bitfun.intellij.disassembly.DisassemblyVirtualFile

import com.linqingying.cangjie.debugger.impl.CangjieXDebugProcess
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

import com.huawei.bitfun.intellij.breakpoint.properties.SourceXBreakpointProperties
import com.linqingying.cangjie.debugger.breakpoint.properties.CangjieSymbolicBreakpointProperties

import com.linqingying.cangjie.debugger.deveco.lsp.LspUtils
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.WriteAction
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.breakpoints.XBreakpoint
import com.intellij.xdebugger.breakpoints.XBreakpointType

import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import com.intellij.xdebugger.breakpoints.ui.XBreakpointCustomPropertiesPanel
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider
import javax.swing.Icon
import javax.swing.JComponent


class CangjieDataBreakpointType : DataBreakpointTypeBase("cangjie-data", "Cangjie Data Breakpoint") {

    companion object {
        fun getInstance(): CangjieDataBreakpointType? {
            return XBreakpointUtil.breakpointTypes().firstNotNullOfOrNull { it as? CangjieDataBreakpointType }
        }
    }
}
class CangjieInstructionBreakpointType : InstructionBreakpointTypeBase("cangjie-instruction", "Cangjie Instruction Breakpoint") {

    companion object {
        fun getInstance(): CangjieInstructionBreakpointType? {
            return XBreakpointUtil.breakpointTypes()
                .mapNotNull { it as? CangjieInstructionBreakpointType }
                .firstOrNull()
        }
    }

    override fun canPutAt(virtualFile: VirtualFile, line: Int, project: Project): Boolean {
        if (super.canPutAt(virtualFile, line, project) && virtualFile is DisassemblyVirtualFile) {
            if (virtualFile.dapProcess is CangjieXDebugProcess) {
                return true
            }
        }
        return false
    }
}
class CangjieSourceBreakpointType : SourceBreakpointTypeBase("cangjie-line", "Cangjie Line Breakpoint") {

    override fun canPutAt(file: VirtualFile, line: Int, project: Project): Boolean {
        return LspUtils.isVirtualFileSupportedByLsp(file)
    }

    override fun getEditorsProvider(breakpoint: XLineBreakpoint<SourceXBreakpointProperties>, project: Project): XDebuggerEditorsProvider? {
        return LspUtils.getXDebuggerEditorsProvider()
    }

    companion object {
        fun getInstance(): CangjieSourceBreakpointType? {
            return XBreakpointUtil.breakpointTypes()
                .mapNotNull { it as? CangjieSourceBreakpointType }
                .firstOrNull()
        }
    }
}
class CangjieSymbolicBreakpointType :
    XBreakpointType<XBreakpoint<CangjieSymbolicBreakpointProperties>, CangjieSymbolicBreakpointProperties>("cangjie-symbolic-method", "Cangjie Symbolic Breakpoints") {

    private var properties: CangjieSymbolicBreakpointProperties? = null

    override fun isAddBreakpointButtonVisible(): Boolean = true

    companion object {
        fun getInstance(): CangjieSymbolicBreakpointType? {
            return XBreakpointUtil.breakpointTypes()
                .mapNotNull { it as? CangjieSymbolicBreakpointType }
                .firstOrNull()
        }
    }

    override fun isSuspendThreadSupported(): Boolean = false

    override fun getDisplayText(breakpoint: XBreakpoint<CangjieSymbolicBreakpointProperties>): String {
        properties = breakpoint.properties
        return properties?.symbolName?.takeIf { it.isNotEmpty() } ?: "<Empty>"
    }

    override fun addBreakpoint(project: Project, parentComponent: JComponent): XBreakpoint<CangjieSymbolicBreakpointProperties>? {
        val dialog = AddCangjieSymbolicBreakpointDialog(project)
        return if (!dialog.showAndGet()) {
            null
        } else {
            WriteAction.compute<XBreakpoint<CangjieSymbolicBreakpointProperties>, Throwable> {
                properties = CangjieSymbolicBreakpointProperties(dialog.symbolName)
                XDebuggerManager.getInstance(project).breakpointManager.addBreakpoint(this, properties!!)
            }
        }
    }

    override fun createProperties(): CangjieSymbolicBreakpointProperties = CangjieSymbolicBreakpointProperties()

    override fun getEditorsProvider(breakpoint: XBreakpoint<CangjieSymbolicBreakpointProperties>, project: Project): XDebuggerEditorsProvider? {
        return LspUtils.getXDebuggerEditorsProvider()
    }

    override fun createCustomTopPropertiesPanel(project: Project): XBreakpointCustomPropertiesPanel<XBreakpoint<CangjieSymbolicBreakpointProperties>> {
        return CangjieSymbolicBreakpointPropertiesPanel(properties!!)
    }

    override fun getEnabledIcon(): Icon = AllIcons.Debugger.Db_method_breakpoint

    override fun getDisabledIcon(): Icon = AllIcons.Debugger.Db_disabled_method_breakpoint

    override fun createCustomRightPropertiesPanel(project: Project): XBreakpointCustomPropertiesPanel<XBreakpoint<CangjieSymbolicBreakpointProperties>> {
        return CangjieBreakpointFiltersPanel()
    }
}
