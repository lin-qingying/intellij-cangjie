package cn.cangnova.cangjie.resolve.caches

import com.intellij.util.NotNullFunction
import com.intellij.util.containers.SLRUMap
import com.intellij.util.containers.hash.EqualityPolicy
import org.jetbrains.annotations.NotNull
import java.util.function.Function


abstract class SLRUCache<K, V> protected constructor(protectedQueueSize: Int, probationalQueueSize: Int) : SLRUMap<K, V>(protectedQueueSize, probationalQueueSize) {

    protected constructor(protectedQueueSize: Int, probationalQueueSize: Int, @NotNull hashingStrategy: EqualityPolicy<in K>) : this(protectedQueueSize, probationalQueueSize)

    abstract fun createValue(key: K ): V

    override fun get(key: K ): V {
        var value = getIfCached(key)
        if (value != null) {
            return value
        }

        value = createValue(key)
        value?.let { put(key, it) }

        return value
    }

   open fun getIfCached(key: K ): V? = super.get(key)

    companion object {
        @JvmStatic
        fun <K, V> slruCache(protectedQueueSize: Int, probationalQueueSize: Int, @NotNull valueProducer: Function<@NotNull K, @NotNull V>): SLRUCache<K, V> {
            return object : SLRUCache<K, V>(protectedQueueSize, probationalQueueSize) {
                override fun createValue(key: K): V = valueProducer.apply(key)
            }
        }

        /**
         * @deprecated Use Caffeine.
         */
        @Deprecated("Use Caffeine.")
        @JvmStatic
        fun <K, V> create(
            protectedQueueSize: Int,
            probationalQueueSize: Int,
            @SuppressWarnings("UsagesOfObsoleteApi") @NotNull valueProducer: NotNullFunction<in K, V>
        ): SLRUCache<K, V> {
            return slruCache(protectedQueueSize, probationalQueueSize) { key -> valueProducer.`fun`(key) }
        }
    }
}
