package org.cangnova.cangjie.protodebugger.memory

import com.intellij.openapi.Disposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import org.jetbrains.annotations.Nls


/**
 * 内存数据管理类
 *
 * 该类负责管理和缓存调试目标的内存数据，提供异步的数据加载和缓存机制。
 * 它使用协程支持非阻塞的内存数据访问，并实现了智能的缓存策略。
 *
 * 使用场景：
 * - 调试器内存查看器中的数据显示
 * - 变量和表达式的值求值
 * - 内存断点和监视点的数据检查
 * - 大块内存数据的分块加载
 *
 * 主要功能：
 * - 异步内存数据加载
 * - 智能缓存和数据失效管理
 * - 地址空间的动态分配
 * - 支持内存数据的只读和读写操作
 *
 * 技术特点：
 * - 基于协程的异步操作
 * - 按需加载和懒加载策略
 * - 内存区域的精细化管理
 * - 支持数据块的并发访问
 *
 * @param T 数据类型，通常为ByteArray或其他内存数据类型
 * @param dataProvider 数据提供者接口，负责实际的数据加载和存储
 */
class MemoryData<T>(private val dataProvider: DataProvider<T>) {
    companion object {
        /**
         * 创建子范围的扩展函数
         *
         * 为可重新分配的区间提供子范围创建功能。
         * 支持类型安全的子范围操作。
         *
         * @param D 可重新分配的区间类型
         * @param subRange 要创建的子范围
         * @return 子范围实例
         */
        fun <D : ReallocatableInterval> D.subRange(
            subRange: AddressRange
        ): D {

            return if (subRange == range) {
                this
            } else {
                subRange.requireInRange(range)
                val result = this.subRangeImpl(subRange)
                result.checkInRange(subRange) as D
            }
        }
    }

    /**
     * 可变地址空间
     *
     * 用于管理内存数据的地址空间分配和数据区域。
     */
    private val addressSpace: MutableAddressSpace<DataRegion<T>> = mutableAddressSpace()

    /**
     * 获取或加载指定地址的数据区域
     *
     * 该方法会首先检查缓存中是否已有指定地址的数据区域，
     * 如果没有则创建一个加载任务并异步加载数据。
     *
     * @param coroutineScope 协程作用域，用于异步数据加载
     * @param address 要获取数据的内存地址
     * @return 数据区域对象，可能处于加载中或已完成状态
     */
    fun getOrFetchRangeForAddress(
        coroutineScope: CoroutineScope,
        address: Address
    ): DataRegion<T> {
        val region = addressSpace.getOrAllocate(address) { holeRange ->
            val dataPlaceholder = dataProvider.createDataPlaceholder(address, holeRange).checkInRange(
                holeRange
            )
            DataRegion.Loading(dataPlaceholder, coroutineScope) {
                loadAndSave(address)
            }
        }

        if (region is DataRegion.Loading) {
            region.start()
        }

        return region
    }

    /**
     * 使指定范围的缓存数据失效
     *
     * 当内存内容可能发生变化时，需要使缓存失效以确保数据的一致性。
         * 该方法会取消正在进行的加载任务并清理相关缓存。
     *
     * @param range 要失效的内存地址范围
     */
    fun invalidateRange(range: AddressRange): Unit {
        val intervals = addressSpace.unallocate(range).intervals

        for (interval in intervals) {
            if (interval is DataRegion.Loading<*>) {
                interval.cancel()
            }
        }
    }

    private suspend fun DataRegion.Loading<T>.loadAndCleanup(
        address: Address
    ): List<DataBlock<T>> {
        return try {
            val dataBlocks = dataProvider.loadData(address, data.requestRange)

            val regionsToUnallocate = addressSpace.filterRegions { it.range.intersects(range) }

            for (region in regionsToUnallocate) {
                addressSpace.unallocate(region.range)
            }

            dataBlocks
        } catch (e: Throwable) {
            val regionsToUnallocate = addressSpace.filterRegions { it.range.intersects(range) }


            for (region in regionsToUnallocate) {
                addressSpace.unallocate(region.range)
            }

            throw e
        }
    }

    private suspend fun DataRegion.Loading<T>.loadAndSave(
        address: Address
    ): DataRegion.Completed<T> {
        return try {
            val dataBlocks = loadAndCleanup(address)

            for (block in dataBlocks) {
                if (block.range.isEmpty()) {
                    throw IllegalStateException("$block range is empty")
                }
                val region = DataRegion.Completed.Loaded(block)
                addressSpace.reallocate(region)
            }

            val region = addressSpace.getRegion(address)
            if (region == null) {
                val unrelatedRanges = dataBlocks.map { it.range }
                val message = "Debugger reported unrelated range(s) for $address: $unrelatedRanges"
                throw DataLoadException(message)
            }

            region.awaitCompleted()
        } catch (e: DataLoadException) {
            val holeRange = data.range.intersectWith(data.range)
            val dataPlaceholder = data.subRange(holeRange)

            DataRegion.Completed.LoadError(dataPlaceholder, e)
        }

    }

    interface Data<out T> : ReallocatableInterval {
        val entries: Iterable<DataEntry<T>>

        override val range: AddressRange

        override fun subRangeImpl(subRange: AddressRange): Data<T>
    }

    interface DataBlock<out T> : Data<T> {
        override fun subRangeImpl(subRange: AddressRange): DataBlock<T>
    }

    interface DataEntry<out T> : Data<T> {
        val address: Address get() = range.start

        override val entries: Iterable<DataEntry<T>> get() = listOf(this)

        override val range: AddressRange

        val value: T

        override fun subRangeImpl(subRange: AddressRange): DataEntry<T> =
            this

    }

    open class DataLoadException : Exception {
        constructor(message: String, cause: Throwable) : super(message, cause)

        constructor(message: String) : super(message)
    }

    interface DataPlaceholder<out T> :
        ReallocatableInterval {
        val address: Address get() = range.start

        override val range: AddressRange

        val requestRange: AddressRange

        override fun subRangeImpl(subRange: AddressRange): DataPlaceholder<T>
    }

    /**
     * 数据提供者接口
     *
     * 该接口定义了内存数据的提供和存储机制，是MemoryData类的核心依赖。
     * 实现该接口的类负责与调试器后端通信，获取和存储内存数据。
     *
     * 使用场景：
     * - 从调试器后端读取内存数据
     * - 向调试目标写入内存数据
     * - 检查内存访问权限
     * - 管理数据提供者的生命周期
     *
     * 主要功能：
     - 异步数据加载和存储
     * - 数据占位符的创建和管理
     * - 只读权限检查和说明
     * - 资源的自动清理
     *
     * 技术特点：
     * - 支持协程的异步操作
     * - 可配置的数据块大小和策略
     * - 灵活的权限控制机制
     * - 自动资源管理
     */
    interface DataProvider<T> : Disposable {
        /**
         * 检查是否可以提供数据
         *
         * 用于验证数据提供者是否处于可用状态，
         * 可以提供所需的内存数据。
         *
         * @return 如果可以提供数据返回true，否则返回false
         */
        fun canProvideData(): Boolean

        /**
         * 创建数据占位符
         *
         * 为指定的地址范围创建一个数据占位符，
         * 用于在数据加载期间表示预期的数据区域。
         *
         * @param address 目标地址
         * @param holeRange 需要填充的数据范围
         * @return 数据占位符对象
         */
        fun createDataPlaceholder(
            address: Address,
            holeRange: AddressRange
        ): DataPlaceholder<T>

        /**
         * 异步加载数据
         *
         * 从调试器后端异步加载指定地址范围的内存数据。
         * 返回一个或多个数据块，每个块包含实际的数据内容。
         *
         * @param address 起始地址
         * @param range 要加载的地址范围
         * @return 数据块列表
         */
        suspend fun loadData(
            address: Address,
            range: AddressRange
        ): List<DataBlock<T>>

        /**
         * 获取只读原因
         *
         * 当数据提供者处于只读状态时，返回具体的原因说明。
         * 用于向用户显示为什么不能写入内存数据。
         *
         * @return 只读原因的描述，如果不是只读则返回null
         */
        @Nls
        fun readOnlyReason(): String?

        /**
         * 异步存储数据
         *
         * 向调试目标的内存地址写入数据。
         * 默认实现抛出异常，表示不支持写入操作。
         *
         * @param address 目标内存地址
         * @param bytes 要写入的字节数据
         * @throws UnsupportedOperationException 如果不支持写入操作
         */
        suspend fun storeData(
            address: Address,
            bytes: ByteArray
        ): Unit {
            throw UnsupportedOperationException("Storing memory is not supported")
        }
    }

    sealed class DataRegion<T> :
        AddressSpace.Region {
        abstract val data: ReallocatableInterval

        override val range: AddressRange = data.range

        abstract suspend fun awaitCompleted(): Completed<T>

        abstract fun subRange(subRange: AddressRange): DataRegion<T>


        sealed class Completed<T> :
            DataRegion<T>() {
            override suspend fun awaitCompleted(): Completed<T> = this

            class LoadError<T>(
                override val data: DataPlaceholder<T>,
                val exception: DataLoadException
            ) : Completed<T>() {
                override fun subRange(subRange: AddressRange) = LoadError(
                    data.subRange(subRange),
                    exception
                )
            }

            class Loaded<T>(override val data: Data<T>) :
                Completed<T>() {
                override fun subRange(subRange: AddressRange) = Loaded(
                    data.subRange(
                        subRange
                    )
                )
            }
        }

        class Loading<T> : DataRegion<T> {
            constructor(
                data: DataPlaceholder<T>,
                deferred: Deferred<Completed<T>>
            ) {
                this.data = data
                this.deferred = deferred
            }

            constructor(
                data: DataPlaceholder<T>,
                coroutineScope: CoroutineScope,
                load: suspend Loading<T>.() -> Completed<T>
            ) {
                this.data = data
                this.deferred = coroutineScope.async(start = CoroutineStart.LAZY) {


                    load()
                }

            }

            override val data: DataPlaceholder<T>


            private val deferred: Deferred<Completed<T>>

            override suspend fun awaitCompleted(): Completed<T> = deferred.await()

            fun cancel(): Unit {
                deferred.cancel()
            }

            operator fun contains(other: Loading<T>): Boolean {
                return deferred === other.deferred && range.contains(other.range)

            }

            fun start(): Boolean =
                deferred.start()


            override fun subRange(subRange: AddressRange): Loading<T> = Loading(
                data.subRange(subRange),
                deferred
            )

        }
    }

    interface ReallocatableInterval : Interval {
        fun subRangeImpl(subRange: AddressRange): ReallocatableInterval
    }
}

