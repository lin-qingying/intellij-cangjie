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

package org.cangnova.cangjie

import org.cangnova.cangjie.utils.rethrow
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.ModificationTracker
import org.cangnova.cangjie.storage.LockBasedStorageManager
import java.util.concurrent.atomic.AtomicLong


/**
 * 延迟类型计算包装异常基类
 *
 * 该抽象类用于包装在延迟类型计算过程中发生的异常。
 */
abstract class LazyWrappedTypeComputationException : RuntimeException()

/**
 * 检查异常是否为进程取消异常
 *
 * 通过检查异常类的完整类名来判断是否为 ProcessCanceledException。
 * 这种方式避免了直接依赖 IntelliJ 平台的具体类。
 *
 * @return 如果是进程取消异常返回 true，否则返回 false
 */
fun Throwable.isProcessCanceledException(): Boolean {
    var klass: Class<out Any?> = this.javaClass
    while (true) {
        if (klass.canonicalName == "com.intellij.openapi.progress.ProcessCanceledException") return true
        klass = klass.superclass ?: return false
    }
}

/**
 * 进程取消时的缓存重置配置
 *
 * 该对象管理是否在进程取消时重置缓存的配置。
 */
object CacheResetOnProcessCanceled {
    /**
     * 配置属性名
     */
    private const val PROPERTY = "cangjie.internal.cacheResetOnProcessCanceled"

    /**
     * 默认值
     */
    private const val DEFAULT_VALUE = false

    /**
     * 是否启用进程取消时重置缓存
     *
     * 该属性通过 IntelliJ 的 PropertiesComponent 持久化。
     */
    var enabled: Boolean
        get() = PropertiesComponent.getInstance()?.getBoolean(PROPERTY, DEFAULT_VALUE) ?: DEFAULT_VALUE
        set(value) {
            PropertiesComponent.getInstance()?.setValue(PROPERTY, value, DEFAULT_VALUE)
        }
}

/**
 * 重入延迟值计算异常
 *
 * 当检测到延迟值计算的重入（循环依赖）时抛出此异常。
 *
 * 该异常会根据应用程序的运行模式决定是否填充堆栈跟踪：
 * - 在内部模式或单元测试模式下，会填充完整的堆栈跟踪以便调试
 * - 在生产模式下，不填充堆栈跟踪以提高性能
 */
class ReenteringLazyValueComputationException : LazyWrappedTypeComputationException() {
    /**
     * 填充堆栈跟踪
     *
     * 该方法是线程安全的。
     *
     * @return 当前异常对象
     */
    @Synchronized
    override fun fillInStackTrace(): Throwable {
        val application = ApplicationManager.getApplication()
        if (application == null || application.isInternal || application.isUnitTestMode) {
            return super.fillInStackTrace()
        }
        return this
    }
}


/**
 * 异常跟踪器
 *
 * 该类实现了 [ModificationTracker] 和 [LockBasedStorageManager.ExceptionHandlingStrategy]，
 * 用于跟踪异常发生次数并触发缓存失效。
 *
 * 工作原理：
 * - 每当处理异常时，增加修改计数器
 * - 通过 ModificationTracker 机制，通知依赖缓存失效
 * - 特殊处理 ProcessCanceledException 和 ReenteringLazyValueComputationException
 *
 * 使用场景：
 * - 在延迟计算中处理异常
 * - 自动使缓存失效以响应错误
 * - 跟踪和监控异常发生频率
 */
open class ExceptionTracker : ModificationTracker, LockBasedStorageManager.ExceptionHandlingStrategy {

    /**
     * 取消操作的计数器
     *
     * 使用原子长整型确保线程安全。
     */
    private val cancelledTracker: AtomicLong = AtomicLong()

    /**
     * 获取修改计数
     *
     * 该方法用于 ModificationTracker 接口，返回当前的修改次数。
     * 每当异常被处理时，此计数会增加。
     *
     * @return 当前修改计数
     */
    override fun getModificationCount(): Long {
        return cancelledTracker.get()

    }

    /**
     * 增加计数器
     *
     * 该方法是私有的，在处理异常时被调用。
     */
    private fun incCounter() {
        cancelledTracker.andIncrement
    }

    /**
     * 处理异常
     *
     * 该方法实现了异常处理策略：
     * 1. 检查异常类型
     * 2. 对于特定类型的异常，增加计数器
     * 3. 重新抛出异常
     *
     * 不会增加计数器的情况：
     * - [ReenteringLazyValueComputationException]：这是正常的前端行为
     * - [ProcessCanceledException]（当禁用缓存重置时）
     *
     * @param throwable 要处理的异常
     * @return 永不返回，总是抛出异常
     * @throws RuntimeException 重新抛出原始异常或包装后的异常
     */
    override fun handleException(throwable: Throwable): RuntimeException {
        // should not increment counter when ReenteringLazyValueComputationException is thrown since it implements correct frontend behaviour
        if (throwable !is ReenteringLazyValueComputationException) {
            if (!throwable.isProcessCanceledException() || CacheResetOnProcessCanceled.enabled) {
                incCounter()
            }
        }
        throw rethrow(throwable)
    }

}
