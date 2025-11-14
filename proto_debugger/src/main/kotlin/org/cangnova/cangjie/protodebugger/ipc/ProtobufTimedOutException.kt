package org.cangnova.cangjie.protodebugger.ipc

import com.intellij.execution.ExecutionException
import com.intellij.openapi.util.NlsSafe

/**
 * 调试器命令超时异常类
 *
 * 该异常类用于表示调试器命令执行超时的情况。
 * 当调试器命令在指定时间内未能完成时，会抛出此异常以防止调试器无限等待。
 *
 * 使用场景：
 * - 调试器响应超时处理
 * - 网络连接超时
 * - 长时间运行的调试操作
 * - 调试器无响应检测
 *
 * @param message 异常的详细消息，描述超时情况
 */
open class DebuggerCommandTimedOutException(message: String) : DebuggerFatalException(message)

/**
 * 调试器致命异常类
 *
 * 该类表示调试器运行过程中的致命错误，这些错误通常导致调试会话无法继续。
 * 它继承自ExecutionException，用于表示调试执行过程中的严重问题。
 *
 * 使用场景：
 * - 调试器与目标程序连接断开
 * - 调试器内部严重错误
 * - 无法恢复的调试状态错误
 * - 调试器进程崩溃或异常终止
 *
 * 主要特点：
 * - 表示调试过程的严重错误
 * - 通常需要重新启动调试会话
 * - 可能需要用户干预
 * - 错误信息应该清晰易懂
 */
open class DebuggerFatalException : ExecutionException {
    /**
     * 构造函数 - 仅包含错误消息
     *
     * @param message 异常的详细消息，描述致命错误的具体情况
     */
    constructor(message: @NlsSafe String?) : super(message)

    /**
     * 构造函数 - 仅包含根本原因
     *
     * @param cause 导致致命异常的根本原因异常
     */
    constructor(cause: Throwable?) : super(cause)

    /**
     * 构造函数 - 包含错误消息和根本原因
     *
     * @param s 异常的详细消息，描述致命错误的具体情况
     * @param cause 导致致命异常的根本原因异常
     */
    constructor(s: @NlsSafe String?, cause: Throwable?) : super(s, cause)
}

/**
 * Protocol Buffers 超时异常类
 *
 * 该异常类专门用于表示 Protocol Buffers 通信过程中的超时情况。
 * 当调试器与后端服务之间的 Protocol Buffers 消息传输超时时抛出此异常。
 *
 * 使用场景：
 * - 调试器与调试服务器之间的通信超时
     * - Protocol Buffers 消息序列化/反序列化超时
     * - 网络延迟或连接问题导致的超时
     * - 调试服务器响应超时
 *
 * 主要特点：
     * - 专门处理 Protocol Buffers 通信超时
     * - 提供统一的超时错误处理
     * - 支持调试器通信层的异常管理
     * - 便于超时问题的诊断和排查
     */
class ProtobufTimedOutException : DebuggerCommandTimedOutException("Protocol Timeout")