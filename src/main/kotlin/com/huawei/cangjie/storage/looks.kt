package com.huawei.cangjie.storage

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

inline fun <T> SimpleLock.guarded(crossinline computable: () -> T): T {
    lock()
    return try {
        computable()
    } finally {
        unlock()
    }
}
private const val CHECK_CANCELLATION_PERIOD_MS: Long = 50
object EmptySimpleLock : SimpleLock {
    override fun lock() {
    }

    override fun unlock() {
    }
}
interface SimpleLock {
    fun lock()

    fun unlock()

    companion object {
        fun simpleLock(checkCancelled: Runnable? = null, interruptedExceptionHandler: ((InterruptedException) -> Unit)? = null) =
            if (checkCancelled != null && interruptedExceptionHandler != null) {
                CancellableSimpleLock(checkCancelled, interruptedExceptionHandler)
            } else {
                DefaultSimpleLock()
            }
    }
}
class CancellableSimpleLock(
    lock: Lock,
    private val checkCancelled: Runnable,
    private val interruptedExceptionHandler: (InterruptedException) -> Unit
) : DefaultSimpleLock(lock){
    constructor(checkCancelled: Runnable, interruptedExceptionHandler: (InterruptedException) -> Unit) : this(
        checkCancelled = checkCancelled,
        lock = ReentrantLock(),
        interruptedExceptionHandler = interruptedExceptionHandler
    )
    override fun lock() {
        try {
            while (!lock.tryLock(CHECK_CANCELLATION_PERIOD_MS, TimeUnit.MILLISECONDS)) {
                //ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
                checkCancelled.run()
            }
        } catch (e: InterruptedException) {
            interruptedExceptionHandler(e)
        }
    }
}
open class DefaultSimpleLock(protected val lock: Lock = ReentrantLock()) : SimpleLock {

    override fun lock() = lock.lock()

    override fun unlock() = lock.unlock()

}