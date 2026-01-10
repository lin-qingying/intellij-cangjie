/*
 * Copyright 2026 LinQingYing. and contributors.
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

/**
 * 在锁保护下执行计算
 *
 * 该扩展函数会先获取锁，然后执行计算，最后在 finally 块中释放锁，
 * 确保即使发生异常也能正确释放锁。
 *
 * 示例：
 * ```kotlin
 * val lock = SimpleLock.simpleLock()
 * val result = lock.guarded {
 *     // 受保护的计算
 *     compute()
 * }
 * ```
 *
 * @param T 计算结果类型
 * @param computable 要执行的计算
 * @return 计算结果
 */
inline fun <T> SimpleLock.guarded(crossinline computable: () -> T): T {
    lock()
    return try {
        computable()
    } finally {
        unlock()
    }
}

/**
 * 检查取消状态的时间间隔（毫秒）
 */
private const val CHECK_CANCELLATION_PERIOD_MS: Long = 50

/**
 * 空锁实现
 *
 * 该对象提供了一个不执行任何操作的锁实现，
 * 可用于需要锁接口但不需要实际同步的场景。
 */
object EmptySimpleLock : SimpleLock {
    override fun lock() {
    }

    override fun unlock() {
    }
}

/**
 * 简单锁接口
 *
 * 提供基本的锁定和解锁操作。
 */
interface SimpleLock {
    /**
     * 获取锁
     */
    fun lock()

    /**
     * 释放锁
     */
    fun unlock()

    companion object {
        /**
         * 创建简单锁实例
         *
         * 根据提供的参数创建不同类型的锁：
         * - 如果提供了取消检查和异常处理器，创建 [CancellableSimpleLock]
         * - 否则创建 [DefaultSimpleLock]
         *
         * @param checkCancelled 取消检查回调（可选）
         * @param interruptedExceptionHandler 中断异常处理器（可选）
         * @return 锁实例
         */
        fun simpleLock(checkCancelled: Runnable? = null, interruptedExceptionHandler: ((InterruptedException) -> Unit)? = null) =
            if (checkCancelled != null && interruptedExceptionHandler != null) {
                CancellableSimpleLock(checkCancelled, interruptedExceptionHandler)
            } else {
                DefaultSimpleLock()
            }
    }
}

/**
 * 可取消的简单锁
 *
 * 该锁在获取锁的过程中会定期检查取消状态，
 * 如果操作被取消，可以避免长时间阻塞。
 *
 * @property checkCancelled 取消检查回调
 * @property interruptedExceptionHandler 中断异常处理器
 */
class CancellableSimpleLock(
    lock: Lock,
    private val checkCancelled: Runnable,
    private val interruptedExceptionHandler: (InterruptedException) -> Unit
) : DefaultSimpleLock(lock){
    /**
     * 创建可取消的简单锁
     *
     * @param checkCancelled 取消检查回调
     * @param interruptedExceptionHandler 中断异常处理器
     */
    constructor(checkCancelled: Runnable, interruptedExceptionHandler: (InterruptedException) -> Unit) : this(
        checkCancelled = checkCancelled,
        lock = ReentrantLock(),
        interruptedExceptionHandler = interruptedExceptionHandler
    )

    /**
     * 获取锁
     *
     * 尝试在指定时间内获取锁，如果失败则检查取消状态。
     * 该方法会循环尝试，直到获取锁或被中断。
     */
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

/**
 * 默认简单锁实现
 *
 * 基于 [ReentrantLock] 的简单锁实现。
 *
 * @property lock 底层锁实例
 */
open class DefaultSimpleLock(protected val lock: Lock = ReentrantLock()) : SimpleLock {

    override fun lock() = lock.lock()

    override fun unlock() = lock.unlock()

}
