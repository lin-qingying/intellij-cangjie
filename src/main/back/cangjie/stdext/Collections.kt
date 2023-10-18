package com.huawei.cangjie.stdext


typealias LookbackValue<T> = Pair<T, T?>
fun makeBitMask(bitToSet: Int): Int = 1 shl bitToSet
fun <T> Sequence<T>.withPrevious(): Sequence<LookbackValue<T>> = LookbackSequence(this)

private class LookbackSequence<T>(private val sequence: Sequence<T>) : Sequence<LookbackValue<T>> {

    override fun iterator(): Iterator<LookbackValue<T>> = LookbackIterator(sequence.iterator())
}
private class LookbackIterator<T>(private val iterator: Iterator<T>) : Iterator<LookbackValue<T>> {

    private var previous: T? = null

    override fun hasNext() = iterator.hasNext()

    override fun next(): LookbackValue<T> {
        val next = iterator.next()
        val result = LookbackValue(next, previous)
        previous = next
        return result
    }
}
