package org.cangnova.cangjie.protodebugger.execution

import com.intellij.execution.ExecutionException
import org.cangnova.cangjie.messages.DebuggerBundle
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.jetbrains.concurrency.Promise
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * 表示异步执行结果的包装类
 *
 * 该类封装了CompletableFuture，提供了更友好的API来处理异步操作结果。
 * 支持泛型，可以包装任意类型的执行结果，并提供了异常处理和超时控制。
 *
 * @param T 执行结果的类型
 *
 * 使用场景：
 * - 调试器命令执行：异步执行调试命令并获取结果
 * - 表达式求值：异步计算表达式并返回结果
 * - 进程控制：异步控制调试目标进程的执行
 * - 文件操作：异步进行文件读写和路径操作
 *
 * 示例用法：
 * ```
 * val result = ExecutionResult<String>()
 *
 * // 在其他地方设置结果
 * result.set("操作完成")
 *
 * // 等待结果
 * try {
 *     val value = result.get()
 *     println("结果: $value")
 * } catch (e: ExecutionException) {
 *     println("执行失败: ${e.message}")
 * }
 *
 * // 带超时的等待
 * try {
 *     val value = result.get(5, TimeUnit.SECONDS)
 * } catch (e: TimeoutException) {
 *     println("操作超时")
 * }
 * ```
 *
 * @see CompletableFuture Java的异步计算实现
 * @see Promise IntelliJ平台的异步Promise实现
 */
open class ExecutionResult<T> {
    private val myResult: @NotNull CompletableFuture<T>

    /** 创建一个新的未完成的执行结果 */
    constructor() {
        this.myResult = CompletableFuture()
    }

    /** 从现有的CompletableFuture创建执行结果（私有构造函数） */
    private constructor(result: @NotNull CompletableFuture<T>) {
        this.myResult = result
    }

    /**
     * 设置执行结果
     *
     * 如果结果已经设置，将抛出IllegalStateException异常。
     *
     * @param result 执行成功的结果值，可以为null
     * @throws IllegalStateException 如果结果已经设置过
     */
    open fun set(result: @Nullable T) {
        if (!myResult.complete(result)) {
            throw IllegalStateException("Result is already set")
        }
    }

    /**
     * 设置执行异常
     *
     * 将执行标记为失败状态，并设置异常信息。
     * 如果结果已经设置，将抛出IllegalStateException异常。
     *
     * @param e 执行过程中发生的异常
     * @throws IllegalStateException 如果结果已经设置过
     */
    open fun setException(e: Throwable) {
        if (!myResult.completeExceptionally(e)) {
            throw IllegalStateException("Result is already set")
        }
    }

    /**
     * 检查执行是否已完成
     *
     * @return true如果执行已完成（成功或失败），false如果还在执行中
     */
    fun isDone(): Boolean {
        return myResult.isDone
    }

    /**
     * 获取执行结果（无限等待）
     *
     * 阻塞当前线程直到执行完成，然后返回结果。
     * 如果执行失败，将抛出ExecutionException异常。
     *
     * @return 执行的结果值
     * @throws ExecutionException 如果执行被中断或失败
     */
    open fun get(): T {
        return get(myResult)
    }

    /**
     * 获取执行结果（带超时）
     *
     * 阻塞当前线程直到执行完成或超时，然后返回结果。
     * 如果执行失败或超时，将抛出相应的异常。
     *
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return 执行的结果值
     * @throws ExecutionException 如果执行被中断或失败
     * @throws TimeoutException 如果等待超时
     */
    fun get(timeout: Long, unit: TimeUnit): T {
        return get(myResult, timeout, unit)
    }

    companion object {
        /**
         * 从Future获取结果（无限等待）
         *
         * 静态工具方法，用于从任意Future对象获取结果。
         * 会处理异常并转换为ExecutionException。
         *
         * @param T 结果类型
         * @param future Future对象
         * @return 执行结果
         * @throws ExecutionException 如果执行被中断或失败
         */
        @JvmStatic
        fun <T> get(future: @NotNull Future<T>): T {
            try {
                return future.get()
            } catch (e: InterruptedException) {
                throw ExecutionException(DebuggerBundle.message("error.execution.interrupted"))
            } catch (e: java.util.concurrent.ExecutionException) {
                throw ExecutionException(e.message, e.cause)
            }
        }

        /**
         * 从Future获取结果（带超时）
         *
         * 静态工具方法，用于从任意Future对象获取结果，支持超时控制。
         * 会处理异常并转换为相应的异常类型。
         *
         * @param T 结果类型
         * @param future Future对象
         * @param timeout 超时时间
         * @param unit 时间单位
         * @return 执行结果
         * @throws ExecutionException 如果执行被中断或失败
         * @throws TimeoutException 如果等待超时
         */
        @JvmStatic
        fun <T> get(future: @NotNull Future<T>, timeout: Long, unit: TimeUnit): T {
            try {
                return future.get(timeout, unit)
            } catch (e: InterruptedException) {
                throw ExecutionException(DebuggerBundle.message("error.execution.interrupted"))
            } catch (e: java.util.concurrent.ExecutionException) {
                throw ExecutionException(e.message, e.cause)
            } catch (e: TimeoutException) {
                throw TimeoutException(e.message)
            }
        }

        /**
         * 从CompletableFuture创建ExecutionResult
         *
         * 将现有的CompletableFuture包装为ExecutionResult，
         * 提供统一的API接口。
         *
         * @param T 结果类型
         * @param future CompletableFuture对象
         * @return ExecutionResult包装器
         */
        @JvmStatic
        fun <T> fromCompletableFuture(future: @NotNull CompletableFuture<T>): ExecutionResult<T> {
            return ExecutionResult(future)
        }

        /**
         * 从Promise创建ExecutionResult
         *
         * 将IntelliJ平台的Promise转换为ExecutionResult，
         * 自动处理成功和失败的回调。
         *
         * @param T 结果类型
         * @param promise Promise对象
         * @return ExecutionResult包装器
         */
        @JvmStatic
        fun <T> fromPromise(promise: @NotNull Promise<T>): ExecutionResult<T> {
            val result = ExecutionResult<T>()
            promise.onSuccess(result::set)
            promise.onError(result::setException)
            return result
        }
    }

    /**
     * 转换为CompletableFuture
     *
     * 返回内部的CompletableFuture对象，允许与现有的Java异步API集成。
     *
     * @return CompletableFuture实例
     */
    fun asCompletableFuture(): @NotNull CompletableFuture<T> {
        return myResult
    }
}
