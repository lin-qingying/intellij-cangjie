/*
 * Copyright 2025 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.utils


import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.util.ProgressIndicatorUtils
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import kotlin.reflect.KProperty


/**
 * 一个线程安全的可变容器，封装了当前值并支持异步或同步的更新操作。
 * 更新操作会被串行化处理，确保并发场景下的顺序一致性。
 *
 * 该类类似于 Clojure 的 atom，提供了 updateAsync 与 updateSync 两种更新方式。
 *
 * @param T 容器内保存的值的类型。实例化时传入初始值。
 */
class AsyncValue<T>(initial: T) {
    @Volatile
    private var current: T = initial

    private val updates: Queue<(T) -> CompletableFuture<Unit>> = ConcurrentLinkedQueue()
    private var running: Boolean = false

    val currentState: T get() = current

    /**
     * 异步更新当前值。
     *
     * @param updater 接收当前值并返回一个在完成时包含新值的 CompletableFuture。
     * @return 返回表示更新完成的新值的 CompletableFuture。
     */
    fun updateAsync(updater: (T) -> CompletableFuture<T>): CompletableFuture<T> {
        val result = CompletableFuture<T>()
        updates.add { current ->
            updater(current)
                .handle { next, err ->
                    if (err == null) {
                        this.current = next
                        result.complete(next)
                    } else {
                        // 不记录 ProcessCanceledException 的日志
                        if (!(err is ProcessCanceledException || err is CompletionException && err.cause is ProcessCanceledException)) {
                            LOG.error(err)
                        }
                        result.completeExceptionally(err)
                    }
                }
        }
        startUpdateProcessing()
        return result
    }

    /**
     * 同步更新当前值，通过将同步计算封装为已完成的 CompletableFuture 实现。
     *
     * @param updater 接收当前值并返回新值。
     * @return 返回表示更新完成的新值的 CompletableFuture。
     */
    fun updateSync(updater: (T) -> T): CompletableFuture<T> =
        updateAsync { CompletableFuture.completedFuture(updater(it)) }

    /**
     * 开始处理队列中的更新。如果已经在运行或队列为空则立即返回。
     */
    @Synchronized
    private fun startUpdateProcessing() {
        if (running || updates.isEmpty()) return
        val nextUpdate = updates.remove()
        running = true
        nextUpdate(current)
            .whenComplete { _, _ ->
                stopUpdateProcessing()
                startUpdateProcessing()
            }
    }

    /**
     * 停止处理更新，确保当前处于运行状态后将其置为停止。
     */
    @Synchronized
    private fun stopUpdateProcessing() {
        check(running)
        running = false
    }

    companion object {
        private val LOG: Logger = logger<AsyncValue<*>>()
    }
}

/**
 * 基于 ThreadLocal 的属性委托，用于为每个线程维护独立的属性值。
 *
 * @param initializer 当线程首次访问该属性时调用以初始化值。
 */
class ThreadLocalDelegate<T>(initializer: () -> T) {
    private val tl: ThreadLocal<T> = ThreadLocal.withInitial(initializer)

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return tl.get()
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        tl.set(value)
    }
}

/**
 * 从 Future 中获取值，同时检查是否被取消；如果超时且已取消则抛出 TimeoutException。
 *
 * @param timeoutMillis 超时时间（毫秒）。
 * @throws TimeoutException 如果在截止时间未完成且被取消。
 * @throws ExecutionException 如果计算以异常结束。
 * @throws InterruptedException 如果在等待过程中被中断。
 */
@Throws(TimeoutException::class, ExecutionException::class, InterruptedException::class)
fun <V> Future<V>.getWithCheckCanceled(timeoutMillis: Long): V {
    val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis)
    while (true) {
        try {
            return get(10, TimeUnit.MILLISECONDS)
        } catch (e: TimeoutException) {
            ProgressManager.checkCanceled()
            if (System.nanoTime() >= deadline) {
                throw e
            }
        }
    }
}

/**
 * 在获取锁时执行操作，并在执行过程中检查是否已取消。
 * 该函数会以短超时循环尝试获取锁并执行给定操作。
 */
fun <T> Lock.withLockAndCheckingCancelled(action: () -> T): T =
    ProgressIndicatorUtils.computeWithLockAndCheckingCanceled<T, Exception>(this, 10, TimeUnit.MILLISECONDS, action)

/**
 * 在等待条件满足时检查是否被取消。
 */
fun Condition.awaitWithCheckCancelled() {
    ProgressIndicatorUtils.awaitWithCheckCanceled(this)
}
