package cn.cangnova.cangjie.dapDebugger1.runconfig.stack

import com.intellij.xdebugger.frame.XExecutionStack
import com.intellij.xdebugger.frame.XStackFrame
import cn.cangnova.cangjie.dapDebugger1.runconfig.CangJieDebugProcess
import org.eclipse.lsp4j.debug.StackFrame
import org.eclipse.lsp4j.debug.StackTraceArguments
import org.eclipse.lsp4j.debug.Thread

class CangJieExecutionStack(
    private val debugProcess: CangJieDebugProcess,
    private val thread: Thread,
    private var frames: List<StackFrame>? = null
) : XExecutionStack(thread.name ?: "Thread #${thread.id}") {

    private var myTopFrame: XStackFrame? = null
    private val stackFrames = mutableListOf<XStackFrame>()
    private var loading = false

    init {
        // 初始化顶部帧
        frames?.firstOrNull()?.let { frame ->
            myTopFrame = CangJieStackFrame.create(debugProcess, thread.id.toLong(), frame)
        }
    }

    override fun getTopFrame(): XStackFrame? {
        if (myTopFrame == null && frames == null   ) {
            // 如果没有帧数据，需要从调试器获取
            debugProcess.getConnection().getServer().stackTrace(StackTraceArguments().apply {
                threadId = thread.id
            }).thenAccept { response ->
                frames = response.stackFrames.toList()
                frames?.firstOrNull()?.let { frame ->
                    myTopFrame = CangJieStackFrame.create(debugProcess, thread.id.toLong(), frame)
                }
            }
        }
        return myTopFrame
    }

    override fun computeStackFrames(firstFrameIndex: Int, container: XStackFrameContainer) {
        if (loading) return
        loading = true

        if (frames == null || frames!!.isEmpty()) {
            // 如果还没有帧数据，需要从调试器获取
            debugProcess.getConnection().getServer().stackTrace(StackTraceArguments().apply {
                threadId = thread.id
            }).thenAccept { response ->
                frames = response.stackFrames.toList()
                computeAndAddFrames(firstFrameIndex, container)
                loading = false
            }.exceptionally { throwable ->
                container.errorOccurred("Failed to load frames: ${throwable.message}")
                loading = false
                null
            }
        } else {
            computeAndAddFrames(firstFrameIndex, container)
            loading = false
        }
    }

    private fun computeAndAddFrames(firstFrameIndex: Int, container: XStackFrameContainer) {
        if (stackFrames.isEmpty() && frames != null) {
            frames?.forEach { frame ->
                CangJieStackFrame.create(debugProcess, thread.id.toLong(), frame).let {
                    stackFrames.add(it)
                }
            }
        }

        if (firstFrameIndex < stackFrames.size) {
            container.addStackFrames(
                stackFrames.subList(firstFrameIndex, stackFrames.size),
                true
            )
        }
    }
}
