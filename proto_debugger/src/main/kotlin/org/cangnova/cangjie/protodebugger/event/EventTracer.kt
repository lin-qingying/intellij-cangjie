package org.cangnova.cangjie.protodebugger.event

import org.cangnova.cangjie.protodebugger.event.EventTracer.Companion.currentThreadName
import org.jetbrains.annotations.NonNls
import java.io.Writer
import java.util.function.BiConsumer

/**
 * 事件跟踪器接口
 *
 * 该接口定义了调试器事件跟踪的核心功能，用于记录和追踪调试过程中的各种事件。
 * 通过实现该接口，可以收集调试器性能数据、执行流程信息等，用于调试器优化和问题诊断。
 *
 * 使用场景：
 * - 追踪调试器命令的执行时间
 * - 记录调试器与被调试程序的交互过程
 * - 监控调试器性能瓶颈
 * - 收集调试器内部操作的详细信息
 * - 生成调试器执行的时间线报告
 */
interface EventTracer {

    /**
     * 开始事件跟踪
     *
     * 开始记录一个新事件的执行过程。该方法返回一个BiConsumer，用于在事件执行过程中
     * 添加额外的标记信息。
     *
     * 使用场景：
     * - 开始追踪一个调试器命令的执行
     * - 记录复杂操作的开始和详细信息
     * - 为长时间运行的操作提供进度标记
     *
     * @param eventCategory 事件类别，如"debugger.command"、"evaluation"等
     * @param eventName 事件名称的懒加载函数，避免不必要的字符串构建
     * @param details 事件详细信息的懒加载函数，包含事件的具体描述
     * @param threadName 线程名称，默认使用当前线程名
     * @return BiConsumer对象，用于在事件执行过程中添加标记信息
     */
    fun begin(
        @NonNls eventCategory: String,
        @NonNls eventName: () -> String,
        @NonNls details: () -> String,
        @NonNls threadName: String = currentThreadName()
    ): BiConsumer<String?, Any?>

    /**
     * 结束事件跟踪
     *
     * 结束当前正在跟踪的事件。通常与begin方法配对使用。
     *
     * 使用场景：
     * - 结束调试器命令的执行记录
     * - 标记异步操作的完成
     * - 在异常情况下强制结束事件记录
     *
     * @param threadName 线程名称，默认使用当前线程名
     */
    fun end(@NonNls threadName: String = currentThreadName())

    /**
     * 写入事件跟踪数据
     *
     * 将收集到的事件跟踪数据写入到指定的输出流中。
     * 通常用于生成调试器性能报告或调试日志。
     *
     * 使用场景：
     * - 生成调试器性能分析报告
     * - 保存调试器执行日志
     * - 导出事件数据用于后续分析
     *
     * @param out 输出流，用于写入事件跟踪数据
     */
    fun write(out: Writer)

    companion object {

        /**
         * 获取当前线程名称
         *
         * 返回当前线程的名称和ID，用于在事件跟踪中标识执行线程。
         * 格式为"线程名称 (线程ID)"。
         *
         * 使用场景：
         * - 在多线程调试环境中标识事件来源
         * - 分析并发调试操作
         * - 调试线程相关问题
         *
         * @return 格式为"线程名称 (线程ID)"的字符串
         */
        fun currentThreadName(): String {
            val thread = Thread.currentThread()
            return "${thread.name} (${thread.id})"
        }
    }
}


/**
 * 记录事件标记
 *
 * 创建一个即时完成的事件标记，用于标记某个时间点的操作。
 * 该函数创建一个EventSpan并立即关闭，适用于记录不需要持续时间的简单事件。
 *
 * 使用场景：
 * - 记录调试器状态变化
 * - 标记重要的执行节点
 * - 记录用户操作
 * - 调试器配置变更
 * - 错误或警告事件记录
 *
 * @param eventCategory 事件类别，如"debugger.state"、"user.action"等
 * @param eventName 事件名称，简洁描述事件内容
 * @param details 事件详细信息，可选参数，提供额外的上下文信息
 * @param threadName 线程名称，默认使用当前线程名
 */
@JvmOverloads
fun traceMarker(
    @NonNls eventCategory: String,
    @NonNls eventName: String,
    @NonNls details: Any? = null,
    @NonNls threadName: String = currentThreadName()
) {
    EventSpan(eventCategory, eventName, details, threadName).close()
}

/**
 * 跟踪事件时间跨度
 *
 * 创建一个事件跨度，用于测量和记录具有持续时间的操作。
 * 该函数自动管理事件跨度的生命周期，包括异常处理和资源清理。
 *
 * 使用场景：
 * - 测量调试器命令的执行时间
 * - 跟踪复杂操作的完整过程
 * - 性能分析和优化
 * - 异常操作的错误追踪
 * - 异步操作的执行监控
 *
 * 工作原理：
 * 1. 创建EventSpan开始计时
 * 2. 执行指定的action函数
 * 3. 如果action抛出异常且提供了failedEventName，则记录失败事件
 * 4. 在finally块中关闭EventSpan，确保资源正确释放
 *
 * @param T 返回值类型
 * @param eventCategory 事件类别，如"debugger.command"、"evaluation"等
 * @param eventName 事件名称，描述被跟踪的操作
 * @param details 事件详细信息，提供额外的上下文信息
 * @param failedEventName 失败事件名称，可选参数，当action抛出异常时记录
 * @param action 要执行的操作函数，接收EventSpan参数并返回结果
 * @return action函数的执行结果
 * @throws Throwable 如果action执行失败，抛出相应的异常
 */
inline fun <T> traceSpan(
    @NonNls eventCategory: String,
    @NonNls eventName: String,
    @NonNls details: Any?,
    @NonNls failedEventName: String? = null,
    action: (EventSpan) -> T
): T {
    var span: EventSpan? = null

    return try {
        span = EventSpan(eventCategory, eventName, details)
        action(span)
    } catch (e: Throwable) {
        if (failedEventName != null) {
            span?.accept(failedEventName, null)
        }
        throw e
    } finally {
        span?.close()
    }
}

