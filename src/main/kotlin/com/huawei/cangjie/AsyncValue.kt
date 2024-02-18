package com.huawei.cangjie


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
 *一个包含不可变值的容器，允许安全并发地读取和更新值。
 *[AsyncValue] 类似于Clojure的atom。
 *[updateAsync] 方法用于调度形式为(T) -> Promise<T>的修改。保证所有更新都是串行化的。
 */
class AsyncValue<T>(initial: T) {
    @Volatile
    private var current: T = initial

    private val updates: Queue<(T) -> CompletableFuture<Unit>> = ConcurrentLinkedQueue()
    private var running: Boolean = false

    val currentState: T get() = current

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

    fun updateSync(updater: (T) -> T): CompletableFuture<T> =
        updateAsync { CompletableFuture.completedFuture(updater(it)) }

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

    @Synchronized
    private fun stopUpdateProcessing() {
        check(running)
        running = false
    }

    companion object {
        private val LOG: Logger = logger<AsyncValue<*>>()
    }
}

class ThreadLocalDelegate<T>(initializer: () -> T) {
    private val tl: ThreadLocal<T> = ThreadLocal.withInitial(initializer)

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return tl.get()
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        tl.set(value)
    }
}

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

fun <T> Lock.withLockAndCheckingCancelled(action: () -> T): T =
    ProgressIndicatorUtils.computeWithLockAndCheckingCanceled<T, Exception>(this, 10, TimeUnit.MILLISECONDS, action)

fun Condition.awaitWithCheckCancelled() {
    ProgressIndicatorUtils.awaitWithCheckCanceled(this)
}
