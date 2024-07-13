package com.huawei.cangjie.storage



import java.util.concurrent.ConcurrentMap

interface StorageManager{
    fun <K : Any, V : Any> createMemoizedFunction(compute: (K) -> V, onRecursiveCall: (K, Boolean) -> V): MemoizedFunctionToNotNull<K, V>
    fun <T : Any> createNullableLazyValue(computable: () -> T?): NullableLazyValue<T>

    fun <K, V : Any> createCacheWithNotNullValues(): CacheWithNotNullValues<K, V>


    /**
     * @param onRecursiveCall is called if the computation calls itself recursively.
     *                        The parameter to it is {@code true} for the first call, {@code false} otherwise.
     *                        If {@code onRecursiveCall} is {@code null}, an exception will be thrown on a recursive call,
     *                        otherwise it's executed and its result is returned
     *
     * @param postCompute is called after the value is computed AND published (and some clients rely on that
     *                    behavior - notably, AbstractTypeConstructor). It means that it is up to particular implementation
     *                    to provide (or not to provide) thread-safety guarantees on writes made in postCompute -- see javadoc for
     *                    LockBasedLazyValue for details.
     */
    fun <T : Any> createLazyValueWithPostCompute(computable: () -> T, onRecursiveCall: ((Boolean) -> T)?, postCompute: (T) -> Unit): NotNullLazyValue<T>
    fun <T : Any> createLazyValue(computable: () -> T, onRecursiveCall: (Boolean) -> T): NotNullLazyValue<T>

    fun <T : Any> createLazyValue(computable: () -> T): NotNullLazyValue<T>
    fun <K, V : Any> createMemoizedFunctionWithNullableValues(compute: (K) -> V?): MemoizedFunctionToNullable<K, V>

    fun <K, V : Any> createMemoizedFunctionWithNullableValues(compute: (K) -> V, map: ConcurrentMap<K, Any>): MemoizedFunctionToNullable<K, V>
    fun <K, V : Any> createMemoizedFunction(compute: (K) -> V, onRecursiveCall: (K, Boolean) -> V, map: ConcurrentMap<K, Any>): MemoizedFunctionToNotNull<K, V>

    fun <K, V : Any> createMemoizedFunction(compute: (K) -> V, map: ConcurrentMap<K, Any>): MemoizedFunctionToNotNull<K, V>
    fun <K, V : Any> createMemoizedFunction(compute: (K) -> V): MemoizedFunctionToNotNull<K, V>

    fun <T> compute(computable: () -> T): T


}
