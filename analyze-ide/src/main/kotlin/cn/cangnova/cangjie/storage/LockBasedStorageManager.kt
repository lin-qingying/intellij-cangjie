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

package cn.cangnova.cangjie.storage

import cn.cangnova.cangjie.storage.LockBasedStorageManager.Companion.sanitizeStackTrace
import cn.cangnova.cangjie.utils.WrappedValues
import cn.cangnova.cangjie.utils.WrappedValues.escapeThrowable
import cn.cangnova.cangjie.utils.WrappedValues.unescapeThrowable
import cn.cangnova.cangjie.utils.isProcessCanceledException
import cn.cangnova.cangjie.utils.rethrow
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import kotlin.concurrent.Volatile

open class LockBasedStorageManager(
    val debugText: String,
    val exceptionHandlingStrategy: ExceptionHandlingStrategy,
    val lock: SimpleLock
) : StorageManager {

    companion object {
        private fun <K> createConcurrentHashMap(): ConcurrentMap<K, Any> {
            // memory optimization: fewer segments and entries stored
            return ConcurrentHashMap(3, 1f, 2)
        }

        private
        val PACKAGE_NAME: String = LockBasedStorageManager::class.java.getCanonicalName().substringBeforeLast(".", "")

        internal fun <T : Throwable> sanitizeStackTrace(throwable: T): T {
            val stackTrace = throwable.getStackTrace()
            val size = stackTrace.size

            var firstNonStorage = -1
            for (i in 0..<size) {
                if (!stackTrace.get(i).getClassName().startsWith(PACKAGE_NAME)) {
                    firstNonStorage = i
                    break
                }
            }
            assert(firstNonStorage >= 0) { "This method should only be called on exceptions created in LockBasedStorageManager" }

            val list = Arrays.asList<StackTraceElement?>(*stackTrace).subList(firstNonStorage, size)
            throwable.setStackTrace(list.toTypedArray<StackTraceElement?>())
            return throwable
        }
    }


    open fun <K, V> recursionDetectedDefault(
        source: String,
        input: K
    ): RecursionDetectedResult<V> {
        throw sanitizeStackTrace(
            AssertionError(
                ("Recursion detected " + source +
                        (if (input == null)
                            ""
                        else
                            "on input: " + input
                                ) + " under " + this)
            )
        )
    }

    override fun <K, V : Any> createMemoizedFunction(
        compute: (K) -> V,
        onRecursiveCall: (K, Boolean) -> V
    ): MemoizedFunctionToNotNull<K, V> {
        return createMemoizedFunction(
            compute,
            onRecursiveCall,
            createConcurrentHashMap()
        )

    }

    override fun <T : Any> createNullableLazyValue(computable: () -> T?): NullableLazyValue<T> {
        return LockBasedNullLazyValue(this, computable)

    }

    override fun <K, V : Any> createCacheWithNotNullValues(): CacheWithNotNullValues<K, V> {
        return CacheWithNotNullValuesBasedOnMemoizedFunction(
            this,
            createConcurrentHashMap()
        )

    }

    override fun <T : Any> createRecursionTolerantLazyValue(
        computable: () -> T,
        onRecursiveCall: T
    ): NotNullLazyValue<T> {
        return object : LockBasedNotNullLazyValue<T>(this, computable) {
            override fun recursionDetected(firstTime: Boolean): RecursionDetectedResult<T?> {
                return RecursionDetectedResult.value(onRecursiveCall)
            }

            override fun presentableName(): String {
                return "RecursionTolerantLazyValue"
            }
        }

    }

    override fun <K, V : Any> createCacheWithNullableValues(): CacheWithNullableValues<K, V> {
        return CacheWithNullableValuesBasedOnMemoizedFunction(
            this, createConcurrentHashMap()
        )
    }

    override fun <T : Any> createRecursionTolerantNullableLazyValue(
        computable: () -> T?,
        onRecursiveCall: T?
    ): NullableLazyValue<T> {
        return object : LockBasedNullLazyValue<T>(this, computable) {
            override fun recursionDetected(firstTime: Boolean): RecursionDetectedResult<T?> {
                return RecursionDetectedResult.value(onRecursiveCall)
            }

            override fun presentableName(): String {
                return "RecursionTolerantNullableLazyValue"
            }
        }

    }

    override fun <T : Any> createLazyValueWithPostCompute(
        computable: () -> T,
        onRecursiveCall: ((Boolean) -> T)?,
        postCompute: (T) -> Unit
    ): NotNullLazyValue<T> {
        return object : LockBasedNotNullLazyValueWithPostCompute<T>(this, computable) {
            override fun recursionDetected(firstTime: Boolean): RecursionDetectedResult<T?> {
                if (onRecursiveCall == null) {
                    return super.recursionDetected(firstTime)
                }
                return RecursionDetectedResult.value(onRecursiveCall.invoke(firstTime))
            }

            override fun doPostCompute(value: T?) {
                value?.let { postCompute.invoke(it) }
            }

            override fun presentableName(): String {
                return "LockBasedNotNullLazyValueWithPostCompute"
            }


        }

    }

    override fun <T : Any> createLazyValue(
        computable: () -> T,
        onRecursiveCall: (Boolean) -> T
    ): NotNullLazyValue<T> {
        return object : LockBasedNotNullLazyValue<T>(this, computable) {
            override fun recursionDetected(firstTime: Boolean): RecursionDetectedResult<T?> {
                return RecursionDetectedResult.value(onRecursiveCall.invoke(firstTime))
            }
        }
    }

    override fun <T : Any> createLazyValue(computable: () -> T): NotNullLazyValue<T> {
        return LockBasedNotNullLazyValue(this, computable)
    }

    override fun <K, V : Any> createMemoizedFunctionWithNullableValues(compute: (K) -> V): MemoizedFunctionToNullable<K, V> {
        return createMemoizedFunctionWithNullableValues(compute, createConcurrentHashMap())
    }

    override fun <K, V : Any> createMemoizedFunctionWithNullableValues(
        compute: (K) -> V,
        map: ConcurrentMap<K, Any>
    ): MemoizedFunctionToNullable<K, V> {
        return MapBasedMemoizedFunctionToNull(this, map, compute)

    }

    override fun <K, V : Any> createMemoizedFunction(
        compute: (K) -> V,
        onRecursiveCall: (K, Boolean) -> V,
        map: ConcurrentMap<K, Any>
    ): MemoizedFunctionToNotNull<K, V> {
        return object : MapBasedMemoizedFunctionToNotNull<K, V>(this, map, compute) {
            override fun recursionDetected(
                input: K,
                firstTime: Boolean
            ): RecursionDetectedResult<V> {
                return RecursionDetectedResult.value(
                    onRecursiveCall.invoke(
                        input,
                        firstTime
                    )
                )
            }
        }

    }

    override fun <K, V : Any> createMemoizedFunction(
        compute: (K) -> V,
        map: ConcurrentMap<K, Any>
    ): MemoizedFunctionToNotNull<K, V> {
        return MapBasedMemoizedFunctionToNotNull(this, map, compute)

    }

    override fun <K, V : Any> createMemoizedFunction(compute: (K) -> V): MemoizedFunctionToNotNull<K, V> {
        return createMemoizedFunction(compute, createConcurrentHashMap())

    }

    override fun <T> compute(computable: () -> T): T {
        lock.lock()
        try {
            return computable.invoke()
        } catch (throwable: Throwable) {
            throw exceptionHandlingStrategy.handleException(throwable)
        } finally {
            lock.unlock()
        }
    }
}

internal abstract class LockBasedLazyValue<T : Any>(
    val storageManager: LockBasedStorageManager,
    val computable: () -> T?
) : LazyValue<T> {

    @Volatile
    private var value: Any = NotValue.NOT_COMPUTED

    override fun isComputed(): Boolean {
        return value !== NotValue.NOT_COMPUTED && value !== NotValue.COMPUTING

    }

    protected open fun presentableName(): String {
        return this.javaClass.getName()
    }

    override fun isComputing(): Boolean {
        return value === NotValue.COMPUTING

    }

    protected open fun recursionDetected(firstTime: Boolean): RecursionDetectedResult<T?> {
        return storageManager.recursionDetectedDefault("in a lazy value", null)
    }

    protected open fun postCompute(value: T?) {
        // Default post compute implementation doesn't publish the value till it is finished
    }

    override fun invoke(): T? {
        var _value = value
        if (_value !is NotValue) return unescapeThrowable(_value)

        storageManager.lock.lock()
        try {
            _value = value
            if (_value !is NotValue) return unescapeThrowable<T>(_value)

            if (_value === NotValue.COMPUTING) {
                value = NotValue.RECURSION_WAS_DETECTED
                val result =
                    recursionDetected( /*firstTime = */true)
                if (!result.isFallThrough) {
                    return result.getValue()
                }
            }

            if (_value === NotValue.RECURSION_WAS_DETECTED) {
                val result =
                    recursionDetected( /*firstTime = */false)
                if (!result.isFallThrough) {
                    return result.getValue()
                }
            }

            value = NotValue.COMPUTING
            try {
                val typedValue: T? = computable.invoke()

                // Don't publish computed value till post compute is finished as it may cause a race condition
                // if post compute modifies value internals.
                postCompute(typedValue)

                value = typedValue!!
                return typedValue
            } catch (throwable: Throwable) {
                if (throwable.isProcessCanceledException()) {
                    value = NotValue.NOT_COMPUTED
                    throw throwable as RuntimeException
                }

                if (value === NotValue.COMPUTING) {
                    // Store only if it's a genuine result, not something thrown through recursionDetected()
                    value = escapeThrowable(throwable)
                }
                throw storageManager.exceptionHandlingStrategy.handleException(throwable)
            }
        } finally {
            storageManager.lock.unlock()
        }
    }

}

private enum class NotValue {
    NOT_COMPUTED,
    COMPUTING,
    RECURSION_WAS_DETECTED
}


interface ExceptionHandlingStrategy {
    /*
         * The signature of this method is a trick: it is used as
         *
         *     throw strategy.handleException(...)
         *
         * most implementations of this method throw exceptions themselves, so it does not matter what they return
         */
    fun handleException(throwable: Throwable): RuntimeException

    companion object {
        val THROW: ExceptionHandlingStrategy = object : ExceptionHandlingStrategy {
            override fun handleException(throwable: Throwable): RuntimeException {
                throw rethrow(throwable)
            }
        }
    }
}

class RecursionDetectedResult<T> private constructor(private val value: T?, val isFallThrough: Boolean) {
    fun getValue(): T? {
        assert(!this.isFallThrough) { "A value requested from FALL_THROUGH in " + this }
        return value
    }

    override fun toString(): String {
        return if (this.isFallThrough) "FALL_THROUGH" else value.toString()
    }

    companion object {
        fun <T> value(value: T): RecursionDetectedResult<T> {
            return RecursionDetectedResult(value, false)
        }

        fun <T> fallThrough(): RecursionDetectedResult<T> {
            return RecursionDetectedResult(null, true)
        }
    }
}

internal open class CacheWithNotNullValuesBasedOnMemoizedFunction<K, V : Any>(
    storageManager: LockBasedStorageManager,
    map: ConcurrentMap<KeyWithComputation<K, V>, Any>
) :
    CacheWithNullableValuesBasedOnMemoizedFunction<K, V>(storageManager, map), CacheWithNotNullValues<K, V> {
    override fun computeIfAbsent(key: K, computation: () -> V): V {
        val result = super.computeIfAbsent(key, computation)
        checkNotNull(result) { "computeIfAbsent() returned null under " + storageManager }
        return result
    }
}

internal open class CacheWithNullableValuesBasedOnMemoizedFunction<K, V : Any>(
    storageManager: LockBasedStorageManager,
    map: ConcurrentMap<KeyWithComputation<K, V>, Any>
) :
    MapBasedMemoizedFunction<KeyWithComputation<K, V>, V>(storageManager, map, { computation ->
        computation.computation.invoke()
    }),
    CacheWithNullableValues<K, V> {
    override fun computeIfAbsent(key: K, computation: () -> V?): V? {
        return invoke(KeyWithComputation(key, computation))

    }
}

internal class KeyWithComputation<K, V>(
    private val key: K,
    val computation: () -> V
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        val that =
            other as KeyWithComputation<*, *>

        return key == that.key
    }

    override fun hashCode(): Int {
        return key.hashCode()
    }
}

internal abstract class MapBasedMemoizedFunction<K, V : Any>(
    val storageManager: LockBasedStorageManager,
    val cache: ConcurrentMap<K, Any>,
    val compute: (K) -> V
) : MemoizedFunction<K, V> {

    override fun isComputed(key: K): Boolean {
        val value = cache.get(key)
        return value != null && value !== NotValue.COMPUTING

    }

    protected open fun recursionDetected(
        input: K,
        firstTime: Boolean
    ): RecursionDetectedResult<V> {
        return storageManager.recursionDetectedDefault("", input)
    }

    private fun checkEmpty(value: Any?): Boolean {
        if (value is MutableList<*>) {
            return value.isEmpty()
        } else if (value is MutableMap<*, *>) {
            return value.isEmpty()
        }
        return true
    }


    private fun inconsistentComputingKey(input: K?, oldValue: Any?): AssertionError {
        return sanitizeStackTrace(
            AssertionError(
                ("Inconsistent key detected. "
                        + NotValue.COMPUTING + " is expected, was: " + oldValue
                        + ", most probably race condition detected on input " + input
                        + " under " + storageManager)
            )
        )
    }

    private fun unableToRemoveKey(input: K?, throwable: Throwable?): AssertionError {
        return sanitizeStackTrace(
            AssertionError(
                ("Unable to remove "
                        + input + " under " + storageManager), throwable
            )
        )
    }

    private fun raceCondition(input: K?, oldValue: Any?): AssertionError {
        return sanitizeStackTrace(
            AssertionError(
                "Race condition detected on input " + input + ". Old value is " + oldValue +
                        " under " + storageManager
            )
        )
    }


    @Override
    override fun invoke(input: K): V? {
        var value: Any? = cache.get(input)
        if (value != null && value !== NotValue.COMPUTING) return WrappedValues.unescapeExceptionOrNull(
            value
        )

        storageManager.lock.lock()
        try {
            value = cache.get(input)

            if (value === NotValue.COMPUTING) {
                value = NotValue.RECURSION_WAS_DETECTED
                val result =
                    recursionDetected(input,  /*firstTime = */true)
                if (!result.isFallThrough) {
                    return result.getValue()
                }
            }

            if (value === NotValue.RECURSION_WAS_DETECTED) {
                val result =
                    recursionDetected(input,  /*firstTime = */false)
                if (!result.isFallThrough) {
                    return result.getValue()
                }
            }

            if (value != null) return WrappedValues.unescapeExceptionOrNull(value)

            var error: AssertionError? = null
            try {
                cache.put(input, NotValue.COMPUTING)
                val typedValue = compute.invoke(input)
                val oldValue = cache.put(input, WrappedValues.escapeNull(typedValue))

                // This code effectively asserts that oldValue is null
                // The trickery is here because below we catch all exceptions thrown here, and this is the only exception that shouldn't be stored
                // A seemingly obvious way to come about this case would be to declare a special exception class, but the problem is that
                // one memoized function is likely to (indirectly) call another, and if this second one throws this exception, we are screwed
                if (oldValue !== NotValue.COMPUTING) {
                    error = raceCondition(input, oldValue)
                    throw error
                }

                return typedValue
            } catch (throwable: Throwable) {
                if (throwable.isProcessCanceledException()) {
                    val remove: Any?
                    try {
                        remove = cache.remove(input)
                    } catch (e: Throwable) {
                        throw unableToRemoveKey(input, e)
                    }
                    if (remove !== NotValue.COMPUTING) {
                        throw inconsistentComputingKey(input, remove)
                    }
                    throw throwable as RuntimeException
                }
                if (throwable === error) {
                    try {
                        cache.remove(input)
                    } catch (e: Throwable) {
                        throw unableToRemoveKey(input, e)
                    }
                    throw storageManager.exceptionHandlingStrategy.handleException(throwable)
                }

                val oldValue: Any? = cache.put(input, WrappedValues.escapeThrowable(throwable))
                if (oldValue !== NotValue.COMPUTING) {
                    throw raceCondition(input, oldValue)
                }

                throw storageManager.exceptionHandlingStrategy.handleException(throwable)
            }
        } finally {
            storageManager.lock.unlock()
        }
    }
}

internal open class LockBasedNullLazyValue<T : Any>(
    storageManager: LockBasedStorageManager,
    computable: () -> T?
) : LockBasedLazyValue<T>(
    storageManager, computable
), NullableLazyValue<T> {
    override fun invoke(): T? {
        val result = super.invoke()
        return result
    }

}

internal open class LockBasedNotNullLazyValue<T : Any>(
    storageManager: LockBasedStorageManager,
    computable: () -> T
) : LockBasedLazyValue<T>(
    storageManager, computable
), NotNullLazyValue<T> {
    override open fun recursionDetected(firstTime: Boolean): RecursionDetectedResult<T?> {
        return super.recursionDetected(firstTime)
    }


    override open fun isComputed(): Boolean {
        return super.isComputed()
    }

    override open fun isComputing(): Boolean {
        return super.isComputing()
    }


    override open fun invoke(): T {
        val result = super.invoke()
        checkNotNull(result) { "compute() returned null" }
        return result
    }

}

internal abstract class LockBasedLazyValueWithPostCompute<T : Any>(
    storageManager: LockBasedStorageManager,
    computable: () -> T?
) : LockBasedLazyValue<T>(storageManager, computable) {
    @Volatile
    private var valuePostCompute: SingleThreadValue<T?>? = null
    override fun postCompute(value: T?) {
        // Protected from rewrites in other threads because it is executed under lock in invoke().
        // May be overwritten when NO_LOCK is used.
        valuePostCompute = SingleThreadValue<T?>(value)
        try {
            doPostCompute(value)
        } finally {
            valuePostCompute = null
        }
    }

    override fun invoke(): T? {
        val postComputeCache = valuePostCompute
        if (postComputeCache != null && postComputeCache.hasValue()) {
            return postComputeCache.getValue()
        }

        return super.invoke()
    }

    protected abstract fun doPostCompute(value: T?)
}

internal abstract class LockBasedNullLazyValueWithPostCompute<T : Any>(
    storageManager: LockBasedStorageManager,
    computable: () -> T?
) : LockBasedLazyValueWithPostCompute<T>(
    storageManager, computable
), NullableLazyValue<T> {
    override fun invoke(): T? {
        return super.invoke()
    }


}

internal abstract class LockBasedNotNullLazyValueWithPostCompute<T : Any>(
    storageManager: LockBasedStorageManager,
    computable: () -> T?
) : LockBasedLazyValueWithPostCompute<T>(
    storageManager, computable
), NotNullLazyValue<T> {


    override fun invoke(): T {
        val result = checkNotNull(super.invoke()) { "compute() returned null" }
        return result
    }
}

internal open class MapBasedMemoizedFunctionToNull<K, V : Any>(

    storageManager: LockBasedStorageManager,
    map: ConcurrentMap<K, Any>,
    compute: (K) -> V

) : MapBasedMemoizedFunction<K, V>(
    storageManager, map, compute
), MemoizedFunctionToNullable<K, V>

internal open class MapBasedMemoizedFunctionToNotNull<K, V : Any>(
    storageManager: LockBasedStorageManager,
    map: ConcurrentMap<K, Any>,
    compute: (K) -> V
) : MapBasedMemoizedFunction<K, V>(
    storageManager, map, compute
), MemoizedFunctionToNotNull<K, V> {
    override fun invoke(input: K): V {
        val result = super.invoke(input)
        checkNotNull(result) { "compute() returned null under " + storageManager }
        return result
    }
}