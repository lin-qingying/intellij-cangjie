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

package org.cangnova.cangjie.debugger.protobuf.execution.async

import org.jetbrains.annotations.Nullable
import java.util.concurrent.CompletableFuture

/**
 * 异步执行结果
 *
 * 对CompletableFuture的简单封装，提供类型安全的异步操作结果。
 *
 * @param T 结果类型
 */
class AsyncResult<T> private constructor(
    private val future: CompletableFuture<T>
) {
    /**
     * 设置成功结果
     *
     * @throws IllegalStateException 如果结果已设置
     */
    fun complete(value: @Nullable T) {
        if (!future.complete(value)) {
            throw IllegalStateException("Result already set")
        }
    }

    /**
     * 设置失败结果
     *
     * @throws IllegalStateException 如果结果已设置
     */
    fun completeExceptionally(exception: Throwable) {
        if (!future.completeExceptionally(exception)) {
            throw IllegalStateException("Result already set")
        }
    }

    /**
     * 检查是否已完成
     */
    fun isDone(): Boolean = future.isDone

    /**
     * 检查是否成功完成
     */
    fun isCompletedSuccessfully(): Boolean =
        future.isDone && !future.isCompletedExceptionally

    /**
     * 检查是否异常完成
     */
    fun isCompletedExceptionally(): Boolean =
        future.isCompletedExceptionally

    /**
     * 转换为CompletableFuture
     */
    fun asCompletableFuture(): CompletableFuture<T> = future

    companion object {
        /**
         * 创建新的未完成结果
         */
        @JvmStatic
        fun <T> create(): AsyncResult<T> {
            return AsyncResult(CompletableFuture())
        }

        /**
         * 从已完成的值创建结果
         */
        @JvmStatic
        fun <T> completed(value: T): AsyncResult<T> {
            return AsyncResult(CompletableFuture.completedFuture(value))
        }

        /**
         * 从异常创建失败结果
         */
        @JvmStatic
        fun <T> failed(exception: Throwable): AsyncResult<T> {
            val future = CompletableFuture<T>()
            future.completeExceptionally(exception)
            return AsyncResult(future)
        }

        /**
         * 从CompletableFuture创建
         */
        @JvmStatic
        fun <T> from(future: CompletableFuture<T>): AsyncResult<T> {
            return AsyncResult(future)
        }
    }
}