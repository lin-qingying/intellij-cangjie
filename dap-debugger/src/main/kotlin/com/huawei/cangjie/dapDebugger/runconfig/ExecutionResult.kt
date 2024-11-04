package com.linqingying.cangjie.dapDebugger.runconfig

import com.intellij.execution.ExecutionException
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.jetbrains.concurrency.Promise
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

open class ExecutionResult<T> {
    private val myResult: @NotNull CompletableFuture<T>

    constructor() {
        this.myResult = CompletableFuture()
    }

    private constructor(result: @NotNull CompletableFuture<T>) {
        this.myResult = result
    }

    open fun set(result: @Nullable T) {
        if (!myResult.complete(result)) {
            throw IllegalStateException("Result is already set")
        }
    }

    open fun setException(e: Throwable) {
        if (!myResult.completeExceptionally(e)) {
            throw IllegalStateException("Result is already set")
        }
    }

    fun isDone(): Boolean {
        return myResult.isDone
    }

    open fun get(): T {
        return get(myResult)
    }

    fun get(timeout: Long, unit: TimeUnit): T {
        return get(myResult, timeout, unit)
    }

    companion object {
        @JvmStatic
        fun <T> get(future: @NotNull Future<T>): T {
            try {
                return future.get()
            } catch (e: InterruptedException) {
                throw ExecutionException("Execution interrupted")
            } catch (e: java.util.concurrent.ExecutionException) {
                throw ExecutionException(e.message, e.cause)
            }
        }

        @JvmStatic
        fun <T> get(future: @NotNull Future<T>, timeout: Long, unit: TimeUnit): T {
            try {
                return future.get(timeout, unit)
            } catch (e: InterruptedException) {
                throw ExecutionException("Execution interrupted")

            } catch (e: java.util.concurrent.ExecutionException) {
                throw ExecutionException(e.message, e.cause)
            } catch (e: TimeoutException) {
                throw TimeoutException(e.message)
            }
        }

        @JvmStatic
        fun <T> fromCompletableFuture(future: @NotNull CompletableFuture<T>): ExecutionResult<T> {
            return ExecutionResult(future)
        }

        @JvmStatic
        fun <T> fromPromise(promise: @NotNull Promise<T>): ExecutionResult<T> {
            val result = ExecutionResult<T>()
            promise.onSuccess(result::set)
            promise.onError(result::setException)
            return result
        }
    }

    fun asCompletableFuture(): @NotNull CompletableFuture<T> {
        return myResult
    }
}
