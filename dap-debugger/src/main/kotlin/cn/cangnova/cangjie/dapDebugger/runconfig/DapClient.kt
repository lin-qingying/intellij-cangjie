package cn.cangnova.cangjie.dapDebugger.runconfig

import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import cn.cangnova.cangjie.dapDebugger.runconfig.breakpoint.CangJieBreakpointHandler
import cn.cangnova.cangjie.dapDebugger.runconfig.breakpoint.CangJieLineBreakpointType
import org.eclipse.lsp4j.debug.*
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient
import java.io.File
import java.util.concurrent.CompletableFuture
import cn.cangnova.cangjie.dapDebugger.runconfig.stack.CangJieStackFrame
import cn.cangnova.cangjie.dapDebugger.runconfig.stack.CangJieSuspendContext

class DapClient(
    private val debugSession: XDebugSession,  // IntelliJ的调试会话
    private val connection: DapConnection,    // DAP连接
    private val parameters: RunParameters,    // 运行参数
    private val breakpointHandler: CangJieBreakpointHandler  // 添加断点管理器
) : IDebugProtocolClient {

    // 跟踪当前的线程和堆栈帧
    private var currentThreadId: Long = 0
    private var currentFrameId: Int = 0

    override fun initialized() {
        // 当debug adapter完成初始化时调用

        // 1. 注册现有的断点
        val breakpointManager = XDebuggerManager.getInstance(debugSession.project).breakpointManager
        val breakpoints = breakpointManager.allBreakpoints
            .filterIsInstance<XLineBreakpoint<CangJieLineBreakpointType.Properties>>()

        // 使用断点管理器注册所有断点
        breakpoints.forEach { breakpoint ->
            breakpointHandler.registerBreakpoint(breakpoint)
        }

        // 2. 发送配置完成请求
        connection.getServer().configurationDone(ConfigurationDoneArguments())
    }

    override fun stopped(args: StoppedEventArguments) {
        // 当程序停止时(断点、步进等)
        currentThreadId = (args.threadId ?: 0).toLong()

        // 获取暂停的线程信息
        connection.getServer().threads().thenAccept { threadsResponse ->
            val threads = threadsResponse.threads.toList()
            
            // 获取堆栈跟踪
            connection.getServer().stackTrace(StackTraceArguments().apply {
                threadId = currentThreadId.toInt()
            }).thenAccept { stackTraceResponse ->
                // 更新UI
                ApplicationManager.getApplication().invokeLater {
                    val frames = stackTraceResponse.stackFrames.toList()
                    if (frames.isNotEmpty()) {
                        val suspendContext = CangJieSuspendContext(
                            debugProcess = debugSession.debugProcess as CangJieDebugProcess,
                            activeThreadId = currentThreadId,
                            threads = threads,
                            frames = frames
                        )
                        debugSession.positionReached(suspendContext)
                    }
                }
            }
        }
    }

    override fun continued(args: ContinuedEventArguments) {
        // 当程序继续运行时
        ApplicationManager.getApplication().invokeLater {
            debugSession.resume()
        }
    }

    override fun exited(args: ExitedEventArguments) {
        // 当调试目标退出时调用
    }

    override fun terminated(args: TerminatedEventArguments?) {
        // 当调试会话终止时
        ApplicationManager.getApplication().invokeLater {
            debugSession.stop()
        }
    }

    override fun thread(args: ThreadEventArguments) {
        // 线程开始或结束时
        when (args.reason) {
            "started" -> {
                // 可以在这里处理新线程
            }

            "exited" -> {
                // 可以在这里处理线程退出
            }
        }
    }

    override fun output(args: OutputEventArguments) {
        // 处理程序输出
        ApplicationManager.getApplication().invokeLater {
            val content = args.output ?: return@invokeLater
            when (args.category) {
                "stdout" -> debugSession.consoleView.print(content, ConsoleViewContentType.NORMAL_OUTPUT)
                "stderr" -> debugSession.consoleView.print(content, ConsoleViewContentType.ERROR_OUTPUT)
                else -> debugSession.consoleView.print(content, ConsoleViewContentType.SYSTEM_OUTPUT)
            }
        }
    }

    override fun breakpoint(args: BreakpointEventArguments) {
        // 当断点状态改变时
        ApplicationManager.getApplication().invokeLater {
            // 更新断点状态
            val breakpoint = args.breakpoint
            // TODO: 更新UI中的断点状态
        }
    }

    override fun module(args: ModuleEventArguments) {
        // 当模块加载或卸载时调用
    }

    override fun loadedSource(args: LoadedSourceEventArguments) {
        // 当源文件加载时调用
    }

    override fun process(args: ProcessEventArguments) {
        // 当调试目标进程启动或终止时调用
    }

    override fun capabilities(args: CapabilitiesEventArguments) {
        // 当debug adapter的能力发生变化时调用
    }

    override fun progressStart(args: ProgressStartEventArguments) {
        // 进度开始
    }

    override fun progressUpdate(args: ProgressUpdateEventArguments) {
        // 进度更新
    }

    override fun progressEnd(args: ProgressEndEventArguments) {
        // 进度结束
    }

    override fun invalidated(args: InvalidatedEventArguments) {
        // 当调试状态失效需要重新获取时调用
    }

    override fun memory(args: MemoryEventArguments) {
        // 内存内容改变时调用
    }

    override fun runInTerminal(args: RunInTerminalRequestArguments): CompletableFuture<RunInTerminalResponse> {
        return CompletableFuture.supplyAsync {
            // 创建响应对象
            val response = RunInTerminalResponse()

            try {
                // 获取要运行的命令和参数
                val command = args.args.firstOrNull() ?: ""
                val arguments = args.args.drop(1)

                // 创建进程构建器
                val processBuilder = ProcessBuilder(*args.args)

                // 设置工作目录(如果提供)
                args.cwd?.let { processBuilder.directory(File(it)) }

                // 设置环境变量(如果提供)
                args.env?.forEach { (key, value) ->
                    processBuilder.environment()[key] = value
                }

                // 启动进程
                val process = processBuilder.start()

                // 设置响应
                response.processId = process.pid().toInt()
                // shellProcessId可以设置为null,因为我们不需要它
                response.shellProcessId = null

            } catch (e: Exception) {
                // 记录错误并返回空响应
                e.printStackTrace()
            }

            response
        }
    }
}
