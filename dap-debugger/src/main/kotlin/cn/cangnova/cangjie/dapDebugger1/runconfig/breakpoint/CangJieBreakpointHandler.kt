package cn.cangnova.cangjie.dapDebugger1.runconfig.breakpoint

import com.intellij.icons.AllIcons
import com.intellij.xdebugger.breakpoints.XBreakpoint
import com.intellij.xdebugger.breakpoints.XBreakpointHandler
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import cn.cangnova.cangjie.dapDebugger1.runconfig.CangJieDebugProcess
import org.eclipse.lsp4j.debug.SetBreakpointsArguments
import org.eclipse.lsp4j.debug.Source
import org.eclipse.lsp4j.debug.SourceBreakpoint
import java.util.concurrent.ConcurrentHashMap
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project

import com.intellij.xdebugger.XDebuggerManager
import org.eclipse.lsp4j.debug.Breakpoint

class CangJieBreakpointHandler(
    private val debugProcess: CangJieDebugProcess
) : XBreakpointHandler<XLineBreakpoint<CangJieLineBreakpointType.Properties>>(CangJieLineBreakpointType::class.java) {

    private val activeBreakpoints = ConcurrentHashMap<String, MutableSet<XLineBreakpoint<CangJieLineBreakpointType.Properties>>>()
    private var breakpointsEnabled = true

    override fun registerBreakpoint(breakpoint: XLineBreakpoint<CangJieLineBreakpointType.Properties>) {
        val file = breakpoint.sourcePosition?.file ?: return
        val filePath = file.path

        activeBreakpoints.computeIfAbsent(filePath) { mutableSetOf() }.add(breakpoint)

        if (breakpoint.isEnabled) {
            updateBreakpoints(filePath)
        }
    }

    private fun updateBreakpoints(filePath: String) {
        val breakpoints = activeBreakpoints[filePath] ?: emptyList()

        val args = SetBreakpointsArguments().apply {
            source = Source().apply {
                path = filePath
            }

            val sourceBreakpoints = breakpoints
                .filter { bp -> bp.isEnabled }
                .map { bp ->
                    SourceBreakpoint().apply {
                        line = bp.line + 1
                        column = 0
                        condition = bp.properties?.getCondition()
                            ?: bp.conditionExpression?.expression
                        logMessage = if (bp.properties?.isLogEnabled() == true) {
                            bp.properties?.getLogExpression()
                        } else null
                    }
                }.toList()

            this.breakpoints = sourceBreakpoints.toTypedArray()
        }

        debugProcess.getConnection().getServer().setBreakpoints(args).thenAccept { response ->
            response.breakpoints?.forEachIndexed { index, breakpointResponse ->
                val xBreakpoint = breakpoints.filter { it.isEnabled }[index]
                val properties = xBreakpoint.properties

                // 更新断点状态
                properties?.apply {
                    setValid(breakpointResponse.isVerified)
                    // 保存适配器返回的其他状态信息
                    setMessage(breakpointResponse.message)

                }

                updateBreakpointPresentation(xBreakpoint, breakpointResponse)
            }
        }
    }

    private fun updateBreakpointPresentation(
        breakpoint: XLineBreakpoint<CangJieLineBreakpointType.Properties>,
        response: Breakpoint
    ) {
        val project = debugProcess.session.project
        ApplicationManager.getApplication().invokeLater {
            val icon = when {
                !breakpoint.isEnabled -> AllIcons.Debugger.Db_disabled_breakpoint
                debugProcess.session.isStopped -> null  // 非调试状态显示默认图标
                response.isVerified -> AllIcons.Debugger.Db_verified_breakpoint  // 适配器验证成功
                else -> AllIcons.Debugger.Db_invalid_breakpoint  // 适配器验证失败
            }

            // 如果有错误消息，显示给用户
            val errorMessage = if (!response.isVerified) {
                response.message ?: "Breakpoint could not be verified"
            } else null

            XDebuggerManager.getInstance(project).breakpointManager.updateBreakpointPresentation(
                breakpoint,
                icon,
                errorMessage
            )
        }
    }

    // 在调试会话结束时调用
    fun sessionStopped() {
        // 更新所有断点的显示为默认状态
        activeBreakpoints.values.flatten().forEach { breakpoint ->
            val project = debugProcess.session.project
            ApplicationManager.getApplication().invokeLater {
                XDebuggerManager.getInstance(project).breakpointManager.updateBreakpointPresentation(
                    breakpoint,
                    null,  // 使用默认图标
                    null   // 清除错误消息
                )
            }
        }
    }

    fun toggleBreakpoint(breakpoint: XLineBreakpoint<CangJieLineBreakpointType.Properties>) {
        val newState = !breakpoint.isEnabled
        breakpoint.isEnabled = newState
        breakpoint.properties?.setEnabled(newState)
        val file = breakpoint.sourcePosition?.file ?: return
        updateBreakpoints(file.path)
    }

    fun disableAllBreakpoints() {
        breakpointsEnabled = false
        activeBreakpoints.values.flatten().forEach { breakpoint ->
            breakpoint.isEnabled = false
            breakpoint.properties?.setEnabled(false)
        }
        activeBreakpoints.keys.forEach { filePath ->
            updateBreakpoints(filePath)
        }
    }

    fun enableAllBreakpoints() {
        breakpointsEnabled = true
        activeBreakpoints.values.flatten().forEach { breakpoint ->
            breakpoint.isEnabled = true
            breakpoint.properties?.setEnabled(true)
        }
        activeBreakpoints.keys.forEach { filePath ->
            updateBreakpoints(filePath)
        }
    }

    override fun unregisterBreakpoint(breakpoint: XLineBreakpoint<CangJieLineBreakpointType.Properties>, temporary: Boolean) {
        val file = breakpoint.sourcePosition?.file ?: return
        val filePath = file.path

        // 从活动断点列表中移除断点
        activeBreakpoints[filePath]?.remove(breakpoint)
        if (activeBreakpoints[filePath]?.isEmpty() == true) {
            activeBreakpoints.remove(filePath)
        }

        // 更新文件的所有断点
        updateBreakpoints(filePath)
    }
}
