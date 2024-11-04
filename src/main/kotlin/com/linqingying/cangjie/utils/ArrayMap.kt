package com.linqingying.cangjie.utils

sealed class ArrayMap<T : Any> : Iterable<T> {
    abstract val size: Int

    abstract operator fun set(index: Int, value: T)
    abstract operator fun get(index: Int): T?

    abstract fun copy(): ArrayMap<T>
}

internal object EmptyArrayMap : ArrayMap<Nothing>() {
    override val size: Int
        get() = 0

    override fun set(index: Int, value: Nothing) {
        throw IllegalStateException()
    }

    override fun get(index: Int): Nothing? {
        return null
    }

    override fun copy(): ArrayMap<Nothing> = this

    override fun iterator(): Iterator<Nothing> {
        return object : Iterator<Nothing> {
            override fun hasNext(): Boolean = false

            override fun next(): Nothing = throw NoSuchElementException()
        }
    }
}
