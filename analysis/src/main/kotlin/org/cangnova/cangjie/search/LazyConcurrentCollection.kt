/*
 * Copyright 2024 LinQingYing. and contributors.
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

package org.cangnova.cangjie.search

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.progress.ProgressManager
import com.intellij.util.concurrency.Semaphore
import com.intellij.util.containers.HashSetQueue
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 元素类型为 V 的集合，具有以下特性:
 * - 惰性计算: 按需计算元素，当调用相应的 .iterator().next() 方法时才进行计算
 * - 线程安全: 多个线程可以并发迭代此集合。已计算的元素在这些线程之间共享。
 *   如果需要计算更多元素，这些计算会并发进行；不同的线程可以处理不同的元素，相互协作
 * - 内部使用另一种类型 T 来存储已计算的元素，例如用于减少内存占用
 *
 * 当需要更多元素时，此集合会迭代尚未处理的元素，对每个元素调用适用的过滤器，
 * 对适用的元素调用生成器，将生成的元素添加回集合。
 *
 * 客户端必须提供:
 * - convertor: T->V 的转换器
 * - filter: 在 V 上应用的过滤器，用于查找集合中适用的元素以计算更多元素
 * - generator: 对于适用的元素:V，生成更多元素:T
 *
 * @param T 内部存储类型（如 PsiAnchor）
 * @param V 外部值类型（如 CjTypeStatement）
 * @param seedElement 种子元素，集合的起始点
 * @param convertor T->V 的转换函数
 * @param applicableForGenerationFilter 用于判断元素是否适用于生成的过滤器
 * @param generator 更多元素的生成器
 */
internal class LazyConcurrentCollection<T, V>(
    seedElement: T,
    private val anchorToValueConvertor: (T) -> V?,
    private val applicableForGenerationFilter: (V) -> Boolean,
    private val generator: MoreElementsGenerator<T, V>
) : Iterable<V> {

    /**
     * 更多元素的生成器函数接口
     */
    fun interface MoreElementsGenerator<T, V> {
        /**
         * 为给定元素生成更多元素
         *
         * @param element 当前元素
         * @param processor 处理生成的元素的消费者
         */
        fun generateMoreElementsFor(element: V, processor: (T) -> Unit)
    }

    /**
     * 计算 'baseClass' 的所有子类（通过重复调用 DirectClassInheritorsSearch 进行传递计算）。
     * 已计算的子类存储在此集合中。
     *
     * 为此集合维护两个迭代器:
     * - 'candidatesToFindSubclassesIterator' 指向下一个尚未搜索直接继承者的元素。
     * - 在 iterator() 中创建的 'subClassIterator' 以惰性方式维护迭代器的状态。
     *   如果该迭代器请求更多元素，则调用 processMoreSubclasses() 尝试用更多继承者填充 'subClasses'。
     */
    private val subClasses = HashSetQueue<T>() // 由 lock 保护

    private val lock = Any() // 重要: 不得在此锁内获取读操作

    private val currentlyProcessingClasses = Semaphore()

    /**
     * [subClasses] 队列的迭代器。指向下一个未处理的元素（即尚未调用 [generator] 的元素）
     */
    private val candidatesToFindSubclassesIterator: HashSetQueue.PositionalIterator<T> // 由 lock 保护

    /**
     * 正在运行 DirectClassInheritorsSearch 的类
     */
    private val classesBeingProcessed = HashSet<T>() // 由 lock 保护

    /**
     * DirectClassInheritorsSearch 已经运行的类（可能在其他线程中），
     * 但 candidatesToFindSubclassesIterator 尚未赶上它们。
     * 随着迭代器的移动，从该集合中删除元素。
     */
    private val classesProcessed = HashSet<T>() // 由 lock 保护

    init {
        seedElement?.let { subClasses.add(it) }
        candidatesToFindSubclassesIterator = subClasses.iterator()
    }

    override fun iterator(): Iterator<V> {
        return object : Iterator<V> {
            private val subClassIterator: Iterator<T> = subClasses.iterator() // 由 lock 保护

            init {
                synchronized(lock) {
                    subClassIterator.next() // 跳过存储在 subClasses 第一个元素中的 baseClass
                }
            }

            override fun hasNext(): Boolean {
                synchronized(lock) {
                    if (subClassIterator.hasNext()) return true
                }

                processMoreSubclasses(subClassIterator)

                synchronized(lock) {
                    return subClassIterator.hasNext()
                }
            }

            override fun next(): V {
                val next = synchronized(lock) {
                    subClassIterator.next()
                }
                return anchorToValueConvertor(next)!!
            }
        }
    }

    /**
     * 轮询 'subClasses' 以获取更多子类并对它们调用 generator.generateMoreElementsFor()
     * 将找到的类添加到 "subClasses" 队列中
     * 一旦添加了某些内容就立即返回
     */
    private fun processMoreSubclasses(subClassIterator: Iterator<T>) {
        while (true) {
            ProgressManager.checkCanceled()

            val pair = ReadAction.compute<Pair<T, V>?, RuntimeException> {
                ProgressManager.checkCanceled()
                synchronized(lock) {
                    // 在 subClasses 集合中查找要操作的类
                    // （不推进 candidatesToFindSubclassesIterator 迭代器 - 它将在类成功处理后移动 - 以防止 PCE、INRE 等）
                    // 找到的类将被标记为正在分析 - 放置在 classesBeingProcessed 集合中
                    val startPosition = candidatesToFindSubclassesIterator.position().next()
                    val next = startPosition?.let { findNextClassInQueue(it) }
                    if (next != null) {
                        currentlyProcessingClasses.down()
                        classesBeingProcessed.add(next.first)
                    }
                    next
                }
            }

            if (pair == null) {
                // 队列中没有剩余的候选项，退出
                // 但首先，等待其他线程处理它们的候选项
                synchronized(lock) {
                    advanceIteratorOnSuccess() // 跳过队列中不合适的类，如 final 等
                    if (subClassIterator.hasNext()) {
                        return
                    }
                }

                val producedSomething = waitForOtherThreadsToFinishProcessing(subClassIterator)
                if (producedSomething) {
                    return
                }

                // 其他线程无法产生任何东西。这可能是因为:
                // - 整个队列已被处理。=> 退出，返回 false
                // - 其他线程已被中断。=> 再次检查队列以接手它放弃的工作。
                synchronized(lock) {
                    advanceIteratorOnSuccess() // 跳过队列中不合适的类，如 final 等
                    if (!candidatesToFindSubclassesIterator.hasNext()) {
                        return
                    }
                }

                continue // 再次检查
            }

            val (anchor, candidate) = pair
            try {
                generator.generateMoreElementsFor(candidate) { generatedElement ->
                    ProgressManager.checkCanceled()
                    synchronized(lock) {
                        generatedElement?.let { subClasses.add(it) }
                    }
                }
                synchronized(lock) {
                    classesProcessed.add(anchor)
                    advanceIteratorOnSuccess()
                    if (subClassIterator.hasNext()) {
                        // 我们已经向 subClasses 添加了一些东西，所以我们可以返回，迭代器至少可以前进一次；
                        // 在后续调用 .next() 时将添加更多元素
                        return
                    }
                }
            } finally {
                synchronized(lock) {
                    classesBeingProcessed.remove(anchor)
                    currentlyProcessingClasses.up()
                }
            }
        }
    }

    /**
     * 等待其他线程完成处理
     */
    private fun waitForOtherThreadsToFinishProcessing(subClassIterator: Iterator<T>): Boolean {
        // 什么都没找到，必须等待其他线程，因为:
        // 第一个线程来了，从队列中取出一个类来搜索继承者，
        // 第二个线程来了，看到队列中没有类。
        // 第二个线程不应该返回空，而应该等待第一个线程完成。
        //
        // 在 managedBlock 内等待，以向 FJP 发出此线程已锁定的信号（以避免线程饥饿和死锁）
        val hasNext = AtomicBoolean()
        try {
            ForkJoinPool.managedBlock(object : ForkJoinPool.ManagedBlocker {
                override fun block(): Boolean {
                    while (!currentlyProcessingClasses.isUp) {
                        ProgressManager.checkCanceled()
                        currentlyProcessingClasses.waitFor(1) // 等待其他线程处理它们的类，然后再放弃
                    }
                    return isReleasable()
                }

                override fun isReleasable(): Boolean {
                    synchronized(lock) {
                        // 其他线程产生了一些东西或所有线程都到达了列表末尾
                        val producedSomething = subClassIterator.hasNext()
                        hasNext.set(producedSomething) // 存储结果以避免退出后再次锁定
                        return producedSomething || !candidatesToFindSubclassesIterator.hasNext() || classesBeingProcessed.isEmpty()
                    }
                }
            })
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        }
        return hasNext.get()
    }

    /**
     * 在队列中查找下一个类
     * 在锁保护下调用
     */
    private fun findNextClassInQueue(position: HashSetQueue.PositionalIterator.IteratorPosition<out T>): Pair<T, V>? {
        // 查找第一个适合分析继承者的类（不是匿名的，不是 final 的，可以从 PsiAnchor 检索）并且尚未处理或正在处理（由其他线程）
        // 在类被处理之前无法调用 iterator.next()，因此使用 position.peek()/position.next() 不推进迭代器
        var currentPosition: HashSetQueue.PositionalIterator.IteratorPosition<out T>? = position
        while (currentPosition != null) {
            ProgressManager.checkCanceled()
            val anchor = currentPosition.peek()
            if (!classesProcessed.contains(anchor) && !classesBeingProcessed.contains(anchor)) {
                val value = anchorToValueConvertor(anchor)
                val isAccepted = value != null && applicableForGenerationFilter(value)
                if (isAccepted) {
                    return anchor to value
                }
                classesProcessed.add(anchor)
            }
            // 候选项已经在其他线程中处理，尝试下一个（不推进迭代器！）
            currentPosition = currentPosition.next()
        }
        return null
    }

    /**
     * 成功后推进迭代器
     * 在锁保护下调用
     */
    private fun advanceIteratorOnSuccess() {
        while (candidatesToFindSubclassesIterator.hasNext()) {
            ProgressManager.checkCanceled()
            val next = candidatesToFindSubclassesIterator.position().next()!!.peek()
            val removed = classesProcessed.remove(next)
            if (removed) {
                candidatesToFindSubclassesIterator.next()
            } else {
                break
            }
        }
    }
}
