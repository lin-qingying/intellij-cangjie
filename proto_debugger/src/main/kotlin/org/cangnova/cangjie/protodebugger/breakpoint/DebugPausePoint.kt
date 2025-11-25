package org.cangnova.cangjie.protodebugger.breakpoint

import lldbprotobuf.Model
import org.cangnova.cangjie.messages.DebuggerBundle
import org.cangnova.cangjie.protodebugger.core.CangJieSuspendContext
import org.cangnova.cangjie.protodebugger.core.DebuggerDriverFacade
import org.cangnova.cangjie.protodebugger.data.LLDBFrame
import org.cangnova.cangjie.protodebugger.data.LLDBThread
import org.cangnova.cangjie.protodebugger.data.LLDBVariable
import org.cangnova.cangjie.protodebugger.memory.Address

/**
 * 信号枚举
 *
 * 定义了常见的信号及其编号和含义说明
 */
enum class Signal(
    /** 信号编号 */
    val number: Int,
    /** 信号名称 */
    val signalName: String,
    /** 信号的详细含义说明 */
    val meaning: String
) {
    /** 用户中断信号 - 用户按下 Ctrl+C */
    SIGINT(2, "SIGINT", DebuggerBundle.message("signal.sigint")),

    /** 杀死信号 - 强制终止进程 */
    SIGKILL(9, "SIGKILL", DebuggerBundle.message("signal.sigkill")),

    /** 段错误信号 - 内存访问错误 */
    SIGSEGV(11, "SIGSEGV", DebuggerBundle.message("signal.sigsegv")),

    /** 终止信号 - 正常终止请求 */
    SIGTERM(15, "SIGTERM", DebuggerBundle.message("signal.sigterm")),

    /** 挂起信号 - 终端挂起或控制进程结束 */
    SIGHUP(1, "SIGHUP", DebuggerBundle.message("signal.sighup")),

    /** 非法指令信号 - 执行了非法指令 */
    SIGILL(4, "SIGILL", DebuggerBundle.message("signal.sigill")),

    /** 跟踪/陷阱信号 - 调试器断点 */
    SIGTRAP(5, "SIGTRAP", DebuggerBundle.message("signal.sigtrap")),

    /** 中止信号 - 致命的程序错误 */
    SIGABRT(6, "SIGABRT", DebuggerBundle.message("signal.sigabrt")),

    /** 总线错误 - 内存总线访问错误 */
    SIGBUS(7, "SIGBUS", DebuggerBundle.message("signal.sigbus")),

    /** 浮点异常 - 浮点运算错误 */
    SIGFPE(8, "SIGFPE", DebuggerBundle.message("signal.sigfpe")),

    /** 用户自定义信号1 */
    SIGUSR1(10, "SIGUSR1", DebuggerBundle.message("signal.sigusr1")),

    /** 管道破裂信号 - 向已关闭的管道写入 */
    SIGPIPE(13, "SIGPIPE", DebuggerBundle.message("signal.sigpipe")),

    /** 算法定时器信号 */
    SIGALRM(14, "SIGALRM", DebuggerBundle.message("signal.sigalrm")),

    /** 子进程状态改变信号 */
    SIGCHLD(17, "SIGCHLD", DebuggerBundle.message("signal.sigchld")),

    /** 继续信号 */
    SIGCONT(18, "SIGCONT", DebuggerBundle.message("signal.sigcont")),

    /** 停止信号 - 终端停止键（Ctrl+Z） */
    SIGSTOP(19, "SIGSTOP", DebuggerBundle.message("signal.sigstop")),

    /** 终端停止信号 - 终端停止键 */
    SIGTSTP(20, "SIGTSTP", DebuggerBundle.message("signal.sigtstp")),

    /** 后台进程尝试读取 */
    SIGTTIN(21, "SIGTTIN", DebuggerBundle.message("signal.sigttin")),

    /** 后台进程尝试写入 */
    SIGTTOU(22, "SIGTTOU", DebuggerBundle.message("signal.sigttou")),

    /** 用户自定义信号2 */
    SIGUSR2(12, "SIGUSR2", DebuggerBundle.message("signal.sigusr2"));

    companion object {
        /**
         * 根据信号编号获取对应的信号枚举
         *
         * @param number 信号编号
         * @return 对应的信号枚举，如果未找到则返回 null
         */
        fun fromNumber(number: Int): Signal? {
            return values().find { it.number == number }
        }

        /**
         * 根据信号名称获取对应的信号枚举
         *
         * @param name 信号名称
         * @return 对应的信号枚举，如果未找到则返回 null
         */
        fun fromName(name: String): Signal? {
            return values().find { it.signalName.equals(name, ignoreCase = true) }
        }

        /**
         * 获取所有信号的编号范围
         */
        val validRange: IntRange = 1..64 // POSIX 信号通常在 1-64 范围内
    }
}

/**
 * 调试器暂停点（DebugPausePoint）
 *
 * 表示调试器执行过程中暂停的具体位置，包含完整的停止原因信息。
 * 该类对应 protobuf 中的 ThreadStopInfo，提供了丰富的停止上下文信息。
 *
 * 主要信息包括：
 * - 当前暂停的线程和栈帧
 * - 暂停的根本原因（断点、单步、信号、异常等）
 * - 暂停的详细描述和上下文
 * - 相关的调试对象信息（断点ID、信号类型等）
 */
data class DebugPausePoint(
    /** 当前暂停的线程 */
    val thread: LLDBThread,

    /** 当前暂停的栈帧 */
    val frame: LLDBFrame,

    /**
     * 函数返回值（可选）
     * 仅当暂停事件为"函数返回"（StepOut/Return）时才会有值。
     */
    val returnValue: LLDBVariable? = null,

    /** 线程停止信息（私有），包含所有详细的停止原因信息 */
    private val stopInfo: Model.ThreadStopInfo? = null
) {

    /** 创建一个没有返回值的暂停点 */
    constructor(thread: LLDBThread, frame: LLDBFrame) : this(thread, frame, null)

    /** 创建带有停止信息的暂停点 */
    constructor(thread: LLDBThread, frame: LLDBFrame, stopInfo: Model.ThreadStopInfo) :
        this(thread, frame, null, stopInfo)

    // ==================== 基本信息获取方法 ====================

    /** 获取停止原因枚举值 */
    fun getStopReason(): Model.StopReason = stopInfo?.reason ?: Model.StopReason.STOP_REASON_NONE

    /** 获取停止原因描述 */
    fun getStopDescription(): String? = stopInfo?.description

    /** 获取停止原因的简要描述 */
    fun getStopReasonDescription(): String {
        return when (getStopReason()) {
            Model.StopReason.STOP_REASON_BREAKPOINT -> DebuggerBundle.message("stop.reason.breakpoint")
            Model.StopReason.STOP_REASON_WATCHPOINT -> DebuggerBundle.message("stop.reason.watchpoint")
            Model.StopReason.STOP_REASON_SIGNAL -> DebuggerBundle.message("stop.reason.signal")
            Model.StopReason.STOP_REASON_EXCEPTION -> DebuggerBundle.message("stop.reason.exception")
            Model.StopReason.STOP_REASON_TRACE -> DebuggerBundle.message("stop.reason.trace")
            Model.StopReason.STOP_REASON_EXEC -> DebuggerBundle.message("stop.reason.exec")
            Model.StopReason.STOP_REASON_FORK -> DebuggerBundle.message("stop.reason.fork")
            Model.StopReason.STOP_REASON_VFORK -> DebuggerBundle.message("stop.reason.vfork")
            Model.StopReason.STOP_REASON_VFORK_DONE -> DebuggerBundle.message("stop.reason.vfork.done")
            Model.StopReason.STOP_REASON_PLAN_COMPLETE -> DebuggerBundle.message("stop.reason.plan.complete")
            Model.StopReason.STOP_REASON_THREAD_EXITING -> DebuggerBundle.message("stop.reason.thread.exiting")
            Model.StopReason.STOP_REASON_INSTRUMENTATION -> DebuggerBundle.message("stop.reason.instrumentation")
            else -> DebuggerBundle.message("stop.reason.unknown")
        }
    }

    // ==================== 断点相关信息获取方法 ====================

    /** 是否为断点命中 */
    fun isBreakpoint(): Boolean = getStopReason() == Model.StopReason.STOP_REASON_BREAKPOINT

    /** 获取断点信息 */
    fun getBreakpointInfo(): Model.BreakpointStopInfo? =
        if (isBreakpoint()) stopInfo?.breakpointInfo else null

    /** 获取断点ID */
    fun getBreakpointId(): Long? = getBreakpointInfo()?.breakpointId

    /** 获取断点类型 */
    fun getBreakpointType(): Model.BreakpointType? = getBreakpointInfo()?.type

    /** 获取断点命中次数 */
    fun getBreakpointHitCount(): Int? = getBreakpointInfo()?.hitCount?.toInt()

    /** 获取断点条件 */
    fun getBreakpointCondition(): String? = getBreakpointInfo()?.condition

    // ==================== 观察点相关信息获取方法 ====================

    /** 是否为观察点命中 */
    fun isWatchpoint(): Boolean = getStopReason() == Model.StopReason.STOP_REASON_WATCHPOINT

    /** 获取观察点信息 */
    fun getWatchpointInfo(): Model.WatchpointStopInfo? =
        if (isWatchpoint()) stopInfo?.watchpointInfo else null

    /** 获取观察点ID */
    fun getWatchpointId(): Long? = getWatchpointInfo()?.watchpointId

    /** 获取观察点类型 */
    fun getWatchpointType(): Model.WatchType? = getWatchpointInfo()?.watchType

    /** 获取观察点地址 */
    fun getWatchpointAddress(): Address? =
        getWatchpointInfo()?.address?.let { Address.Companion.Factory.fromLong(it) }

    /** 获取观察点大小 */
    fun getWatchpointSize(): Int? = getWatchpointInfo()?.size?.toInt()

    // ==================== 信号相关信息获取方法 ====================

    /** 是否为信号中断 */
    fun isSignal(): Boolean = getStopReason() == Model.StopReason.STOP_REASON_SIGNAL

    /** 获取信号信息 */
    fun getSignalInfo(): Model.SignalStopInfo? =
        if (isSignal()) stopInfo?.signalInfo else null

    /** 获取信号编号 */
    fun getSignalNumber(): Int? = getSignalInfo()?.signalNumber?.toInt()

    /** 获取信号名称 */
    fun getSignalName(): String? = getSignalInfo()?.signalName

    /** 获取信号枚举 */
    fun getSignal(): Signal? {
        val number = getSignalNumber()
        return if (number != null) {
            Signal.fromNumber(number) ?: Signal.fromName(getSignalName() ?: "")
        } else {
            getSignalName()?.let { Signal.fromName(it) }
        }
    }

    /** 获取信号的含义说明 */
    fun getSignalMeaning(): String {
        return getSignal()?.meaning ?: getSignalName() ?: DebuggerBundle.message("signal.unknown")
    }

    // ==================== 异常相关信息获取方法 ====================

    /** 是否为异常发生 */
    fun isException(): Boolean = getStopReason() == Model.StopReason.STOP_REASON_EXCEPTION

    /** 获取异常信息 */
    fun getExceptionInfo(): Model.ExceptionStopInfo? =
        if (isException()) stopInfo?.exceptionStopInfo else null

    /** 获取异常类型 */
    fun getExceptionType(): String? {
        val info = getExceptionInfo()
        return info?.exceptionType ?: info?.exceptionName
    }

    /** 获取异常消息 */
    fun getExceptionMessage(): String? = getExceptionInfo()?.message

    /** 获取异常地址 */
    fun getExceptionAddress(): Address? =
        getExceptionInfo()?.exceptionAddress?.let { Address.Companion.Factory.fromLong(it) }

    /** 获取异常代码 */
    fun getExceptionCode(): Int? = getExceptionInfo()?.exceptionCode?.toInt()

    /** 获取异常名称 */
    fun getExceptionName(): String? = getExceptionInfo()?.exceptionName

    // ==================== 单步相关信息获取方法 ====================

    /** 是否为单步执行 */
    fun isStep(): Boolean = getStopReason() == Model.StopReason.STOP_REASON_TRACE

    /** 获取单步信息 */
    fun getStepInfo(): Model.StepStopInfo? =
        if (isStep()) stopInfo?.stepInfo else null

    /** 获取单步类型 */
    fun getStepType(): Model.StepType? = getStepInfo()?.stepType

    /** 获取单步范围 */
    fun getStepRange(): Model.StepRange? = getStepInfo()?.stepRange

    // ==================== 工具化事件相关信息获取方法 ====================

    /** 是否为工具化事件 */
    fun isInstrumentation(): Boolean = getStopReason() == Model.StopReason.STOP_REASON_INSTRUMENTATION

    /** 获取工具化事件信息 */
    fun getInstrumentationInfo(): Model.InstrumentationStopInfo? =
        if (isInstrumentation()) stopInfo?.instrumentationInfo else null

    /** 获取工具名称 */
    fun getInstrumentToolName(): String? = getInstrumentationInfo()?.toolName

    /** 获取事件类型 */
    fun getInstrumentEventType(): String? = getInstrumentationInfo()?.eventType

    /** 获取事件ID */
    fun getInstrumentEventId(): Long? = getInstrumentationInfo()?.eventId

    /** 获取事件数据 */
    fun getInstrumentEventData(): String? = getInstrumentationInfo()?.eventData

    // ==================== 便利判断方法 ====================

    /** 是否有相关的调试对象（断点、观察点等） */
    fun hasDebugObject(): Boolean = isBreakpoint() || isWatchpoint()

    /** 是否为致命停止（需要用户干预） */
    fun isFatalStop(): Boolean = isException() || isSignal()

    /** 是否为正常的调试暂停（断点、单步） */
    fun isNormalDebugStop(): Boolean = isBreakpoint() || isStep()

    /**
     * 获取停止原因的完整描述
     */
    fun getFullDescription(): String {
        val baseDesc = getStopReasonDescription()

        return when (getStopReason()) {
            Model.StopReason.STOP_REASON_BREAKPOINT -> {
                "$baseDesc: 断点 ${getBreakpointId()}${getBreakpointType()?.let { " ($it)" } ?: ""}"
            }

            Model.StopReason.STOP_REASON_WATCHPOINT -> {
                "$baseDesc: 观察点 ${getWatchpointId()}${getWatchpointType()?.let { " ($it)" } ?: ""}"
            }

            Model.StopReason.STOP_REASON_SIGNAL -> {
                "$baseDesc: ${getSignalMeaning()}${getSignalName()?.let { " ($it)" } ?: ""}"
            }

            Model.StopReason.STOP_REASON_EXCEPTION -> {
                "$baseDesc: ${getExceptionType()}${getExceptionMessage()?.let { " - $it" } ?: ""}"
            }

            Model.StopReason.STOP_REASON_TRACE -> {
                "$baseDesc${getStepType()?.let { " ($it)" } ?: ""}"
            }

            Model.StopReason.STOP_REASON_INSTRUMENTATION -> {
                "$baseDesc: ${getInstrumentEventType()}${getInstrumentToolName()?.let { " ($it)" } ?: ""}"
            }

            else -> getStopDescription() ?: baseDesc
        }
    }

    /**
     * 将暂停点转换为字符串，通常用于日志输出
     */
    override fun toString(): String {
        val locationStr = "$thread @ $frame"
        val reasonStr = getFullDescription()
        return "$locationStr - $reasonStr"
    }

      // ==================== 其他事件相关信息获取方法 ====================

    /** 是否为线程退出 */
    fun isThreadExiting(): Boolean = getStopReason() == Model.StopReason.STOP_REASON_THREAD_EXITING

    /** 获取线程退出信息 */
    fun getThreadExitInfo(): Model.ThreadExitStopInfo? =
        if (isThreadExiting()) stopInfo?.threadExitInfo else null

    /** 是否为执行计划完成 */
    fun isPlanComplete(): Boolean = getStopReason() == Model.StopReason.STOP_REASON_PLAN_COMPLETE

    /** 获取执行计划信息 */
    fun getPlanCompleteInfo(): Model.PlanCompleteStopInfo? =
        if (isPlanComplete()) stopInfo?.planCompleteInfo else null
}

/**
 * 将 DebugPausePoint 转换为 CangJieSuspendContext
 *
 * 用于向 IDE 提供标准的暂停上下文对象，使调试 UI 能正确显示线程、栈帧等信息。
 */
fun DebugPausePoint.toCangJieSuspendContext(facade: DebuggerDriverFacade): CangJieSuspendContext {
    return CangJieSuspendContext(
        activeThread = thread,
        topFrame = frame,
        facade = facade
    )
}