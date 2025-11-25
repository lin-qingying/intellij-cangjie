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

package org.cangnova.cangjie.protodebugger.execution.async

import com.intellij.execution.ExecutionException
import org.cangnova.cangjie.messages.DebuggerBundle
import org.jetbrains.concurrency.Promise
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Future 工具类
 *
 * 提供统一的Future操作工具方法，处理异常转换和超时控制。
 */
object FutureUtils {
    /**
     * 从Future获取结果（无限等待）
     *
     * @throws ExecutionException 如果执行被中断或失败
     */
    @JvmStatic
    @Throws(ExecutionException::class)
    fun <T> get(future: Future<T>): T {
        try {
            return future.get()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw ExecutionException(
                DebuggerBundle.message("error.execution.interrupted"),
                e
            )
        } catch (e: java.util.concurrent.ExecutionException) {
            throw ExecutionException(e.message, e.cause)
        }
    }

    /**
     * 从Future获取结果（带超时）
     *
     * @throws ExecutionException 如果执行被中断或失败
     * @throws TimeoutException 如果等待超时
     */
    @JvmStatic
    @Throws(ExecutionException::class, TimeoutException::class)
    fun <T> get(future: Future<T>, timeout: Long, unit: TimeUnit): T {
        try {
            return future.get(timeout, unit)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw ExecutionException(
                DebuggerBundle.message("error.execution.interrupted"),
                e
            )
        } catch (e: java.util.concurrent.ExecutionException) {
            throw ExecutionException(e.message, e.cause)
        } catch (e: TimeoutException) {
            throw TimeoutException("Operation timed out after $timeout ${unit.name.lowercase()}")
        }
    }

    /**
     * 从Promise创建AsyncResult
     */
    @JvmStatic
    fun <T> fromPromise(promise: Promise<T>): AsyncResult<T> {
        val result = AsyncResult.create<T>()
        promise.onSuccess(result::complete)
        promise.onError(result::completeExceptionally)
        return result
    }

    /**
     * 从CompletableFuture创建AsyncResult
     */
    @JvmStatic
    fun <T> fromCompletableFuture(future: CompletableFuture<T>): AsyncResult<T> {
        return AsyncResult.from(future)
    }
}