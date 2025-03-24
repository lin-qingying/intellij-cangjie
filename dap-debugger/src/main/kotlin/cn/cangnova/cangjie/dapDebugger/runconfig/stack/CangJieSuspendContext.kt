package cn.cangnova.cangjie.dapDebugger.runconfig.stack

import com.intellij.xdebugger.frame.XExecutionStack
import com.intellij.xdebugger.frame.XStackFrame
import com.intellij.xdebugger.frame.XSuspendContext
import cn.cangnova.cangjie.dapDebugger.runconfig.CangJieDebugProcess
import org.eclipse.lsp4j.debug.StackFrame

class CangJieSuspendContext(
    private val debugProcess: CangJieDebugProcess,
      val activeThreadId: Long,
    threads: List<org.eclipse.lsp4j.debug.Thread>,
    frames: List<StackFrame>
) : XSuspendContext() {

    private val executionStacks: MutableMap<Long, CangJieExecutionStack> = mutableMapOf()

    init {
        // 为活动线程创建执行栈
        val activeThread = threads.find { it.id.toLong() == activeThreadId }
        if (activeThread != null) {
            executionStacks[activeThreadId] = CangJieExecutionStack(debugProcess, activeThread, frames)
        }

        // 为其他线程创建执行栈（暂时用空的帧列表）
        threads.filter { it.id.toLong() != activeThreadId }.forEach { thread ->
            executionStacks[thread.id.toLong()] = CangJieExecutionStack(debugProcess, thread, emptyList())
        }
    }

    override fun getActiveExecutionStack(): XExecutionStack? {
        return executionStacks[activeThreadId]
    }

    override fun getExecutionStacks(): Array<XExecutionStack> {
        return executionStacks.values.toTypedArray()
    }
}
