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

package org.cangnova.cangjie.storage

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
