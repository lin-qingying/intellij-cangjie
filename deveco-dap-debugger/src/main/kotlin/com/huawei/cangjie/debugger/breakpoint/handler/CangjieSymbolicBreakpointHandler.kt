package com.linqingying.cangjie.debugger.breakpoint.handler


import com.huawei.bitfun.DapFromServerService
import com.huawei.bitfun.DapToServerService
import com.huawei.bitfun.intellij.breakpoint.handler.BreakpointHandlerBase
import com.huawei.bitfun.intellij.breakpoint.utils.BreakpointUpdate
import com.huawei.bitfun.intellij.ex.DapXDebugProcess
import com.huawei.bitfun.intellij.utils.EdtRequestCallbacks
import com.huawei.bitfun.intellij.utils.IntellijThreadUtils
import com.huawei.bitfun.protocol.extend.FunctionBreakpoint
import com.huawei.bitfun.protocol.extend.SetFunctionBreakpointsArguments
import com.huawei.bitfun.utils.ExceptionUtils
import com.linqingying.cangjie.debugger.breakpoint.properties.CangjieSymbolicBreakpointProperties

import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.XExpression
import com.intellij.xdebugger.breakpoints.XBreakpoint
import com.intellij.xdebugger.breakpoints.XBreakpointType
import java.util.ArrayList
import org.eclipse.lsp4j.debug.Breakpoint
import org.eclipse.lsp4j.debug.SetFunctionBreakpointsResponse

class CangjieSymbolicBreakpointHandler(
    bpTypeClass: Class<out XBreakpointType<XBreakpoint<CangjieSymbolicBreakpointProperties>, *>>,
    process: DapXDebugProcess<out DapFromServerService, out DapToServerService<*>>
) : BreakpointHandlerBase<XBreakpoint<CangjieSymbolicBreakpointProperties>, CangjieSymbolicBreakpointProperties>(bpTypeClass, process) {

    override fun doSendDapRequest(pendingUpdates: List<BreakpointUpdate<XBreakpoint<CangjieSymbolicBreakpointProperties>>>) {
        val xDebuggerManager = XDebuggerManager.getInstance(debugProcess.session.project)
        val bpsInManager = IntellijThreadUtils.invokeAndGet {
            xDebuggerManager.breakpointManager.getBreakpoints(breakpointTypeClass).toMutableList()
        }

        debugProcess.toServerService.setFunctionBreakpointsRequester
            .requestAsync(
                createSetFunctionBreakpointArgs(bpsInManager),
                debugProcess.timeouts.setBreakpoints(),
                object : EdtRequestCallbacks<SetFunctionBreakpointsResponse>() {
                    override fun whenSuccessEdt(response: SetFunctionBreakpointsResponse) {
                        val breakpoints = response.breakpoints
                        val enabledIndices = bpsInManager.indices.filter { bpsInManager[it].isEnabled }

                        if (enabledIndices.size != breakpoints.size) {
                            debugProcess.reportError("Set function breakpoints response doesn't match request size")
                        } else {
                            breakpoints.forEachIndexed { i, breakpoint ->
                                breakpoint.id?.let { id ->
                                    bpsInManager[enabledIndices[i]].putUserData(BreakpointHandlerBase.DAP_BREAKPOINT_ID, id)
                                }
                            }
                        }
                    }

                    override fun whenErrorEdt(ex: Exception) {
                        debugProcess.reportError("Set function breakpoint error: ${ExceptionUtils.getNonNullMsg(ex)}")
                    }
                }
            )
    }

    private fun createSetFunctionBreakpointArgs(
        bpsInManager: List<XBreakpoint<CangjieSymbolicBreakpointProperties>>
    ): SetFunctionBreakpointsArguments {
        val functionBreakpoints = bpsInManager
            .filter { it.isEnabled && it.properties != null }
            .mapNotNull { xBreakpoint ->
                val properties = xBreakpoint.properties ?: return@mapNotNull null
                val functionBreakpoint = FunctionBreakpoint().apply {
                    name = properties.symbolName
                    xBreakpoint.conditionExpression?.expression?.takeIf { debugProcess.serverCapabilities.supportsConditionalBreakpoints == true }
                        ?.let { condition = it }
                    if (properties.isHitCountEnabled) {
                        hitCondition = properties.hitCount
                    }
                }
                functionBreakpoint
            }

        return SetFunctionBreakpointsArguments().apply {
            breakpoints = functionBreakpoints.toTypedArray()
        }
    }
}
