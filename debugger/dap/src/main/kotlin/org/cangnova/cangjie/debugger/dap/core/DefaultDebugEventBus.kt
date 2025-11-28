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

package org.cangnova.cangjie.debugger.dap.core

import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 事件总线实现
 * 异步处理事件，监听器并发执行
 */
class DefaultDebugEventBus(
    private val scope: CoroutineScope
) : DebugEventBus {

    private val logger = Logger.getInstance(DefaultDebugEventBus::class.java)

    /**
     * 事件通道，无界缓冲
     */
    private val eventChannel = Channel<DebugEvent>(UNLIMITED)

    /**
     * 所有监听器
     */
    private val listeners = CopyOnWriteArrayList<DebugEventListener>()

    /**
     * 类型化监听器
     */
    private val typedListeners = CopyOnWriteArrayList<Pair<Class<*>, suspend (DebugEvent) -> Unit>>()

    init {
        // 启动事件处理循环
        scope.launch {
            for (event in eventChannel) {
                processEvent(event)
            }
        }
    }

    override fun subscribe(listener: DebugEventListener): () -> Unit {
        listeners.add(listener)
        return {
            listeners.remove(listener)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : DebugEvent> subscribe(
        eventClass: Class<T>,
        listener: suspend (T) -> Unit
    ): () -> Unit {
        val wrapper: suspend (DebugEvent) -> Unit = { event ->
            if (eventClass.isInstance(event)) {
                listener(event as T)
            }
        }
        val pair = eventClass to wrapper
        typedListeners.add(pair)
        return {
            typedListeners.remove(pair)
        }
    }

    override suspend fun publish(event: DebugEvent) {
        eventChannel.send(event)
    }

    override suspend fun publishAndWait(event: DebugEvent) {
        processEvent(event)
    }

    /**
     * 处理事件，分发给所有监听器
     * 监听器并发执行，单个监听器的异常不影响其他监听器
     */
    private suspend fun processEvent(event: DebugEvent) {
        coroutineScope {
            // 通用监听器
            listeners.forEach { listener ->
                launch {
                    try {
                        listener.onEvent(event)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        logger.error("Error in event listener for event: $event", e)
                    }
                }
            }

            // 类型化监听器
            typedListeners.forEach { (_, handler) ->
                launch {
                    try {
                        handler(event)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        logger.error("Error in typed event listener for event: $event", e)
                    }
                }
            }
        }
    }

    /**
     * 关闭事件总线
     */
    fun close() {
        eventChannel.close()
        listeners.clear()
        typedListeners.clear()
    }
}

/**
 * 便捷扩展函数用于订阅特定类型事件
 */
inline fun <reified T : DebugEvent> DebugEventBus.subscribe(
    noinline listener: suspend (T) -> Unit
): () -> Unit {
    return subscribe(T::class.java, listener)
}