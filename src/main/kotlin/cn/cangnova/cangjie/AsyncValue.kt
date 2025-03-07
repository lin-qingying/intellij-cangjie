/*
 * Copyright 2024 LinQingYing. and contributors.
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

package cn.cangnova.cangjie


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
 * 一个包含不可变值的容器，允许安全并发地读取和更新值。
 * [AsyncValue] 类似于Clojure的atom。
 * [updateAsync] 方法用于调度形式为(T) -> Promise<T>的修改。保证所有更新都是串行化的。
 */
class AsyncValue<T>(initial: T) {
    @Volatile
    private var current: T = initial

    private val updates: Queue<(T) -> CompletableFuture<Unit>> = ConcurrentLinkedQueue()
    private var running: Boolean = false

    val currentState: T get() = current

    /**
     * 异步更新当前值。
     * @param updater 一个函数，接收当前值并返回一个完成后的未来值。
     * @return 一个完成后的未来值。
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
                        // Do not log `ProcessCanceledException`
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
     * 同步更新当前值。
     * @param updater 一个函数，接收当前值并返回一个新值。
     * @return 一个完成后的未来值。
     */
    fun updateSync(updater: (T) -> T): CompletableFuture<T> =
        updateAsync { CompletableFuture.completedFuture(updater(it)) }

    /**
     * 开始处理更新。
     * 如果已经有更新在处理中或者没有待处理的更新，则直接返回。
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
     * 停止处理更新。
     * 检查是否确实有更新在处理中，并将其标记为停止。
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
 * 提供基于ThreadLocal的属性委托，用于在每个线程之间隔离属性值。
 * @param initializer 初始化函数，每个线程首次访问属性时调用。
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
 * 获取Future的值，同时检查是否被取消。
 * 如果被取消，则抛出TimeoutException。
 * @param timeoutMillis 超时时间，单位为毫秒。
 * @return Future的值。
 * @throws TimeoutException 如果在指定时间内未完成且被取消。
 * @throws ExecutionException 如果计算完成，但是以异常结束。
 * @throws InterruptedException 如果在等待时被中断。
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
 * 执行给定的操作，同时检查是否被取消，并处理锁。
 * @param action 要执行的操作。
 * @return 操作的结果。
 */
fun <T> Lock.withLockAndCheckingCancelled(action: () -> T): T =
    ProgressIndicatorUtils.computeWithLockAndCheckingCanceled<T, Exception>(this, 10, TimeUnit.MILLISECONDS, action)

/**
 * 等待条件满足，同时检查是否被取消。
 */
fun Condition.awaitWithCheckCancelled() {
    ProgressIndicatorUtils.awaitWithCheckCanceled(this)
}
