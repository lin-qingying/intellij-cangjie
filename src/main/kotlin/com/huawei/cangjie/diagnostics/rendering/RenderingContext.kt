package com.huawei.cangjie.diagnostics.rendering

sealed class RenderingContext {

    abstract operator fun <T> get(key: Key<T>): T


    abstract class Key<out T>(val name: String) {
        abstract fun compute(objectsToRender: Collection<Any?>): T
    }

    object Empty : RenderingContext() {
        override fun <T> get(key: Key<T>): T {
            return key.compute(emptyList())
        }
    }
    class Impl(private val objectsToRender: Collection<Any?>) : RenderingContext() {
        private val data = linkedMapOf<Key<*>, Any?>()

        @Suppress("UNCHECKED_CAST")
        override fun <T> get(key: Key<T>): T {
            return data[key] as? T ?: key.compute(objectsToRender).also { data[key] = it }
        }
    }

    companion object {
        @JvmStatic
        fun of(vararg objectsToRender: Any?): RenderingContext {
            return Impl(objectsToRender.toList())
        }
    }
}
