# DataRegion 内存数据可视化系统完整设计文档

## 目录
1. [系统概述](#系统概述)
2. [核心架构](#核心架构)
3. [数据存储结构](#数据存储结构)
4. [核心接口定义](#核心接口定义)
5. [状态机设计](#状态机设计)
6. [地址空间管理](#地址空间管理)
7. [数据提供者](#数据提供者)
8. [文档渲染系统](#文档渲染系统)
9. [内存数据管理器](#内存数据管理器)
10. [具体实现](#具体实现)
11. [使用示例](#使用示例)
12. [性能分析](#性能分析)
13. [扩展性设计](#扩展性设计)

## 系统概述

本文档描述了一个基于DataRegion设计模式的内存数据可视化系统的完整实现。该系统通过状态机模式、异步加载、区间映射等技术，实现了高效的大规模内存数据的可视化和交互功能。

### 设计目标
- **高效性**：支持按需加载，避免内存溢出
- **可扩展性**：支持多种数据类型和显示格式
- **用户体验**：提供无缝更新和实时状态反馈
- **可靠性**：完善的错误处理和恢复机制

## 核心架构

### 系统架构图

```
┌─────────────────────────────────────────────────────────────┐
│                    Memory Document Layer                     │
├─────────────────────────────────────────────────────────────┤
│  MemoryDocument<T>                                          │
│  ├── loadAddress()                                          │
│  ├── loadHigherAddresses()                                  │
│  └── loadLowerAddresses()                                   │
├─────────────────────────────────────────────────────────────┤
│                    Document Management Layer                 │
├─────────────────────────────────────────────────────────────┤
│  CidrMemoryDocAccess<T>                                     │
│  ├── DocAccessScope                                         │
│  │   ├── data: CidrMemoryData<T>                            │
│  │   ├── document: DocumentEx                               │
│  │   ├── addressSpace: MutableAddressSpace<DocRegion<T>>   │
│  │   └── file: VirtualFile                                 │
│  └── read()/edit() operations                              │
├─────────────────────────────────────────────────────────────┤
│                     Data Management Layer                   │
├─────────────────────────────────────────────────────────────┤
│  CidrMemoryData<T>                                          │
│  ├── addressSpace: MutableAddressSpace<DataRegion<T>>      │
│  └── dataProvider: DataProvider<T>                         │
├─────────────────────────────────────────────────────────────┤
│                    Address Space Management                 │
├─────────────────────────────────────────────────────────────┤
│  MutableAddressSpace<G>                                     │
│  ├── IntervalTreeMap (TreeMap<Address, Interval>)          │
│  ├── Hole (未分配区域)                                      │
│  └── Region (已分配区域)                                    │
├─────────────────────────────────────────────────────────────┤
│                     Data Region States                     │
├─────────────────────────────────────────────────────────────┤
│  DataRegion<T>                                              │
│  ├── DataRegion.Loading<T>                                  │
│  ├── DataRegion.Completed.Loaded<T>                         │
│  └── DataRegion.Completed.LoadError<T>                      │
├─────────────────────────────────────────────────────────────┤
│                     Document Regions                        │
├─────────────────────────────────────────────────────────────┤
│  DocRegion<T>                                               │
│  ├── BaseDocRegion<T> (HexDocRegion, DisasmDocRegion)      │
│  ├── LoadingDocRegion<T>                                    │
│  └── ErrorDocRegion<T>                                      │
└─────────────────────────────────────────────────────────────┘
```

## 数据存储结构

### 1. 地址空间存储结构

#### IntervalTreeMap 核心存储
```kotlin
class IntervalTreeMap(
    override val range: AddressRange,                    // 整个地址空间范围
    map: NavigableMap<Address, Interval> = EMPTY_NAVIGABLE_MAP
) : TreeMap<Address, Interval>(map) {

    // 存储结构: Address -> Interval
    // Key: 区间起始地址
    // Value: 区间对象 (Hole 或 Region)

    override val intervals: Collection<Interval> = this.values

    // 示例存储内容：
    // TreeMap<Address, Interval> {
    //     0x00000000 -> Hole(0x00000000-0x00000FFF)
    //     0x00001000 -> LoadingDataRegion(0x00001000-0x00001FFF)
    //     0x00002000 -> LoadedDataRegion(0x00002000-0x00002FFF)
    //     0x00003000 -> ErrorDataRegion(0x00003000-0x00003FFF)
    //     0x00004000 -> Hole(0x00004000-0xFFFFFFFF)
    // }
}
```

#### 地址空间层次结构
```
AddressSpace Management
├── CidrMemoryData<T>.addressSpace (数据层地址空间)
│   └── MutableAddressSpace<DataRegion<T>>
│       ├── DataRegion.Loading<T> (加载中状态)
│       ├── DataRegion.Completed.Loaded<T> (已加载数据)
│       └── DataRegion.Completed.LoadError<T> (加载错误)
└── CidrMemoryDocAccess<T>.addressSpace (文档层地址空间)
    └── MutableAddressSpace<DocRegion<T>>
        ├── LoadingDocRegion<T> (加载中文档区域)
        ├── BaseDocRegion<T> (已加载文档区域)
        │   ├── HexDocRegion (十六进制显示)
        │   └── DisasmDocRegion (反汇编显示)
        └── ErrorDocRegion<T> (错误文档区域)
```

### 2. 数据层次存储

#### 内存数据存储层次
```
Memory Data Storage Hierarchy
├── CidrMemoryData<T> (数据管理器)
│   └── addressSpace: MutableAddressSpace<DataRegion<T>>
│       └── DataRegion.Completed.Loaded<T>
│           └── data: Data<T> (数据容器)
│               └── entries: Iterable<DataEntry<T>> (数据项集合)
│                   └── DataEntry<T> (最小数据单元)
│                       ├── address: Address (内存地址)
│                       ├── range: AddressRange (地址范围)
│                       └── value: T (实际数据值)
│                           ├── Byte (十六进制视图)
│                           ├── LLInstruction (反汇编视图)
│                           └── CustomType (自定义数据类型)
```

#### 文档数据存储层次
```
Document Data Storage Hierarchy
├── CidrMemoryDocAccess<T> (文档访问管理器)
│   └── addressSpace: MutableAddressSpace<DocRegion<T>>
│       └── DocRegion<T> (文档区域)
│           ├── dataRegion: DataRegion<T> (关联的数据状态)
│           ├── document: DocumentEx (文档对象)
│           ├── textRange: TextRange (文本范围)
│           └── rangeMarker: RangeMarker (位置标记)
│               ├── addressToLineMap (地址到行号映射)
│               └── lineToAddressMap (行号到地址映射)
```

## 核心接口定义

### 1. 基础类型接口

```kotlin
// 地址类
data class Address(val value: Long) {
    fun rangeTo(end: Address): AddressRange = AddressRange(this, end)
    fun increment(): Address = Address(value + 1)
    override fun toString(): String = String.format("0x%08X", value)

    companion object {
        val MIN_VALUE = Address(0L)
        val MAX_VALUE = Address(Long.MAX_VALUE)
    }
}

// 地址范围类
data class AddressRange(val start: Address, val endInclusive: Address) {
    fun contains(address: Address): Boolean =
        address.value >= start.value && address.value <= endInclusive.value

    fun intersects(other: AddressRange): Boolean =
        start.value <= other.endInclusive.value && other.start.value <= endInclusive.value

    fun isEmpty(): Boolean = start.value > endInclusive.value

    val size: Long get() = if (isEmpty()) 0L else endInclusive.value - start.value + 1L

    companion object {
        val EMPTY = AddressRange(Address(1), Address(0))
        val WHOLE = AddressRange(Address.MIN_VALUE, Address.MAX_VALUE)
    }
}

// 区间接口
interface Interval {
    val range: AddressRange
}

// 可重分配区间
interface ReallocatableInterval : Interval {
    fun subRange(subRange: AddressRange): ReallocatableInterval
}

// 地址空间区域接口
interface AddressSpaceRegion : Interval
```

### 2. 地址空间管理接口

```kotlin
// 地址空间接口
interface AddressSpace<G : AddressSpace.Region> : IntervalMap {
    fun contiguousRegions(): List<G>?
    fun filterRegions(): List<G>
    override operator fun get(range: AddressRange): AddressSpace<G>
    fun getRegion(address: Address): G?
    fun regionSpan(range: AddressRange = this.range): AddressRange

    interface Region : Interval
}

// 可变地址空间接口
interface MutableAddressSpace<G : AddressSpace.Region> : AddressSpace<G> {
    val modificationCount: Long

    fun <R : G> allocate(region: R): R
    fun <R : G> allocate(address: Address, createRegion: (AddressRange) -> R): R
    fun <R : G> allocateMissing(range: AddressRange, createRegion: (AddressRange) -> R): List<R>

    override operator fun get(range: AddressRange): MutableAddressSpace<G>

    fun getNeighbors(address: Address): Pair<G?, G?>
    fun getNeighbors(range: AddressRange): Pair<G?, G?>
    fun getOrAllocate(address: Address, createRegion: (AddressRange) -> G): G

    fun reallocate(region: G): MutableAddressSpace<G>
    fun reallocate(region: G, shrinkEdgeRegion: (G, AddressRange) -> G?): MutableAddressSpace<G>

    fun unallocate(region: G): G
    fun unallocate(address: Address): G?
    fun unallocate(range: AddressRange): MutableAddressSpace<G>
}

// 区间映射接口
interface IntervalMap : Interval {
    val intervals: Collection<Interval>

    operator fun contains(interval: Interval): Boolean
    operator fun get(address: Address): Interval
    operator fun get(range: AddressRange): IntervalMap
    fun span(range: AddressRange): AddressRange
}
```

### 3. 数据接口层次

```kotlin
// 数据提供者接口
interface DataProvider<T> : Disposable {
    fun canProvideData(): Boolean

    fun createDataPlaceholder(address: Address, holeRange: AddressRange): DataPlaceholder<T>

    suspend fun loadData(address: Address, range: AddressRange): List<DataBlock<T>>

    fun readOnlyReason(): String?

    suspend fun storeData(address: Address, bytes: ByteArray): Unit {
        throw UnsupportedOperationException("Memory writing not supported")
    }
}

// 数据占位符接口
interface DataPlaceholder<T> : ReallocatableInterval {
    val address: Address get() = range.start
    val requestRange: AddressRange

    override fun subRangeImpl(subRange: AddressRange): DataPlaceholder<T>
}

// 数据接口
interface Data<out T> : ReallocatableInterval {
    val entries: Iterable<DataEntry<T>>

    override val range: AddressRange
    override fun subRangeImpl(subRange: AddressRange): Data<T>
}

// 数据条目接口
interface DataEntry<out T> : Data<T> {
    val address: Address get() = range.start
    val value: T

    override val entries: Iterable<DataEntry<T>> get() = listOf(this)
    override val range: AddressRange get() = address.rangeTo(address)

    override fun subRangeImpl(subRange: AddressRange): DataEntry<T> = this
}

// 数据块接口
interface DataBlock<out T> : Data<T> {
    override fun subRangeImpl(subRange: AddressRange): DataBlock<T>
}

// 数据加载异常
open class DataLoadException : Exception {
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(message: String) : super(message)
}
```

### 4. 文档接口

```kotlin
// 文档区间接口
interface DocInterval : Interval {
    val document: DocumentEx
    val rangeMarker: RangeMarker
    val textRange: TextRange
}

// 内存行信息提供者
interface MemoryLineInfoProvider {
    fun getAddressForLine(line: Int): Address?
    fun getAddressRangeForLine(line: Int): AddressRange?
}

// 文档区域接口
interface DocRegion<T> : DocInterval, AddressSpace.Region, MemoryLineInfoProvider {
    val dataRegion: CidrMemoryData.DataRegion<T>

    // 地址与文档位置转换
    fun getAddressForLineNumberInDocument(line: Int): Address?
    fun getLineNumberInDocument(address: Address): Int
    fun getOffsetInDocument(address: Address): Int
    fun getLineRangeInDocument(): IntRange

    // 文档操作
    fun deleteFromDocument()
    fun markOutdated()

    // 生命周期管理
    suspend fun awaitUnallocation()
}

// 文档区域工厂接口
interface DocRegionFactory<T> {
    fun createDocRegion(
        project: Project,
        document: DocumentEx,
        textRange: TextRange,
        dataRegion: CidrMemoryData.DataRegion.Completed.Loaded<T>,
        reallocatedSpace: AddressSpace<DocRegion<T>>
    ): DocRegion<T>
}

// 文档区域监听器
interface DocRegionListener<T> : EventListener {
    fun onDocRegionAllocated(unallocatedSpace: AddressSpace<DocRegion<T>>, newDocRegion: DocRegion<T>) {}
}
```

## 状态机设计

### DataRegion 状态机

```kotlin
// 核心状态机接口
sealed class DataRegion<T> : AddressSpace.Region {
    abstract val data: ReallocatableInterval
    override val range: AddressRange = data.range

    abstract suspend fun awaitCompleted(): Completed<T>
    abstract fun subRange(subRange: AddressRange): DataRegion<T>

    // 完成状态
    sealed class Completed<T> : DataRegion<T>() {
        override suspend fun awaitCompleted(): Completed<T> = this

        // 已加载状态 - 包含实际数据
        class Loaded<T>(override val data: Data<T>) : Completed<T>() {
            override fun subRange(subRange: AddressRange): Loaded<T> =
                Loaded(data.subRange(subRange))
        }

        // 加载错误状态 - 包含错误信息
        class LoadError<T>(
            override val data: DataPlaceholder<T>,
            val exception: DataLoadException
        ) : Completed<T>() {
            override fun subRange(subRange: AddressRange): LoadError<T> =
                LoadError(data.subRange(subRange), exception)
        }
    }

    // 加载中状态 - 异步加载占位符
    class Loading<T> : DataRegion<T>() {
        constructor(data: DataPlaceholder<T>, deferred: Deferred<Completed<T>>) {
            this.data = data
            this.deferred = deferred
        }

        constructor(
            data: DataPlaceholder<T>,
            coroutineScope: CoroutineScope,
            load: suspend Loading<T>.() -> Completed<T>
        ) {
            this.data = data
            this.deferred = coroutineScope.async(start = CoroutineStart.LAZY) { load() }
        }

        override val data: DataPlaceholder<T>
        private val deferred: Deferred<Completed<T>>

        override suspend fun awaitCompleted(): Completed<T> = deferred.await()

        fun cancel(): Unit = deferred.cancel()
        fun start(): Boolean = deferred.start()

        operator fun contains(other: Loading<T>): Boolean =
            deferred === other.deferred && range.contains(other.range)

        override fun subRange(subRange: AddressRange): Loading<T> =
            Loading(data.subRange(subRange), deferred)
    }
}
```

### 状态转换流程

```
状态转换图:
[空洞(Hole)]
    ↓ (getOrFetchRangeForAddress)
[DataRegion.Loading]
    ↓ (loadAndSave 异步执行)
    ├──┬─── [DataRegion.Completed.Loaded] (成功)
    │   └── [DataRegion.Completed.LoadError] (失败)
    ↓ (invalidateRange)
[空洞] (重新循环)

状态转换关键方法:
1. createDataPlaceholder() -> 创建Loading状态
2. loadData() -> 异步数据加载
3. loadAndSave() -> 处理加载结果，转换为Loaded或LoadError
4. invalidateRange() -> 清理状态，转换为空洞
```

## 地址空间管理

### IntervalTreeMap 实现

```kotlin
// 基于TreeMap的区间树映射实现
class IntervalTreeMap(
    override val range: AddressRange,
    map: NavigableMap<Address, Interval> = EMPTY_NAVIGABLE_MAP
) : TreeMap<Address, Interval>(map), InternalMutableIntervalMap {

    override val intervals: Collection<Interval> = this.values
    override val modificationCount: Long get() = myModificationCount.get()
    private val myModificationCount: AtomicLong = AtomicLong(0)

    // 核心存储: Address -> Interval
    // 包含两种Interval类型:
    // 1. Hole - 空洞（未分配区域）
    // 2. Region - 区域（已分配区域）

    override operator fun get(address: Address): Interval =
        floorEntry(requireAddressInArena(address)).value

    override operator fun get(range: AddressRange): InternalMutableIntervalMap {
        val rangeSpan = span(range)
        val subMap = subMapView(rangeSpan)
        return IntervalTreeMap(rangeSpan, subMap)
    }

    override fun put(interval: Interval): Interval? {
        val range = interval.range
        val start = range.start

        if (!this.range.contains(range)) {
            throw IllegalArgumentException("Interval must be allocated within arena range")
        }

        val oldInterval = this.put(start, interval)
        myModificationCount.incrementAndGet()

        // 安全性检查
        if (oldInterval != null && !oldInterval.range.contains(range)) {
            throw IllegalStateException("Attempting unsafe region replacement")
        }

        // 连续性检查
        if (start > this.range.start && this[start - 1].range.endInclusive != start - 1) {
            throw IllegalStateException("New region put next to a hole (must be adjacent)")
        }

        if (this[range.endInclusive] != interval) {
            throw IllegalStateException("New region overlaps a subsequent")
        }

        return oldInterval
    }

    override fun remove(interval: Interval) {
        val oldInterval = this.remove(interval.range.start)
        if (oldInterval != interval) {
            throw IllegalStateException("Removed unexpected interval $oldInterval")
        }
        myModificationCount.incrementAndGet()
    }

    override fun removeAll(range: AddressRange) {
        subMapView(span(range)).clear()
        myModificationCount.incrementAndGet()
    }

    override fun span(range: AddressRange): AddressRange {
        requireAddressRangeIntersectsArena(range)

        val start = floorKey(range.start) ?: this.range.start
        val endInclusive = higherKey(range.endInclusive)?.minus(1) ?: this.range.endInclusive

        return start.rangeTo(endInclusive).checkNotEmpty()
    }

    private fun subMapView(rangeSpan: AddressRange): NavigableMap<Address, Interval> {
        return this.subMap(rangeSpan.start, true, rangeSpan.endInclusive, true)
    }
}

// 空洞数据类
private data class Hole(override val range: AddressRange) : Interval
```

### AddressSpaceImpl 核心实现

```kotlin
// 地址空间实现类
internal open class AddressSpaceImpl<G : AddressSpace.Region> private constructor(
    private val intervalMap: InternalMutableIntervalMap
) : MutableAddressSpace<G> {

    constructor(arena: AddressRange) : this(IntervalTreeMap(arena).apply {
        put(Hole(arena))  // 初始化整个地址空间为空洞
    })

    override val intervals: Collection<Interval> = intervalMap.intervals
    override val modificationCount: Long = intervalMap.modificationCount
    override val range: AddressRange = intervalMap.range

    // 类型转换辅助属性
    private val Interval.asRegion: G?
        get() = if (this is AddressSpace.Region) this as G else null

    // 分配操作 - 将空洞替换为区域
    override fun <R : G> allocate(region: R): R {
        val range = region.range
        val intervals = intervalMap[range].intervals
        val hole = intervals.singleOrNull() as? Hole

        if (hole == null) {
            throw IllegalArgumentException("Range $range is occupied with $intervals")
        }

        val holeRange = hole.range
        val regionInHole = region.checkInRange(holeRange)
        intervalMap.remove(hole)

        val regionRange = regionInHole.range
        val precedingRange = holeRange.headUntil(regionRange.start)
        val followingRange = holeRange.tailAfter(regionRange.endInclusive)

        // 添加前后的空洞
        if (!precedingRange.isEmpty()) {
            intervalMap.put(Hole(precedingRange))
        }

        // 添加新区域
        intervalMap.put(regionInHole)

        if (!followingRange.isEmpty()) {
            intervalMap.put(Hole(followingRange))
        }

        check(regionInHole == region) { "Assertion failed" }
        return regionInHole
    }

    // 根据地址创建并分配区域
    override fun <R : G> allocate(address: Address, createRegion: (AddressRange) -> R): R {
        intervalMap.requireAddressInArena(address)
        val hole = allocationHoleFor(address)
        val holeRange = hole.range
        val region = createRegion(holeRange)
        region.checkInRange(holeRange)

        intervalMap.remove(hole)
        val regionRange = region.range
        val precedingRange = holeRange.headUntil(regionRange.start)
        val followingRange = holeRange.tailAfter(regionRange.endInclusive)

        if (!precedingRange.isEmpty()) {
            intervalMap.put(Hole(precedingRange))
        }

        intervalMap.put(region)

        if (!followingRange.isEmpty()) {
            intervalMap.put(Hole(followingRange))
        }

        return region
    }

    // 取消分配 - 将区域替换为空洞
    override fun unallocate(region: G): G {
        return replaceIntervalWithHole(region)
    }

    private fun <I : Interval> replaceIntervalWithHole(interval: I): I {
        val holeRange: AddressRange = interval.range.spanAdjacentHoles()
        intervalMap.removeAll(holeRange)
        intervalMap.put(Hole(holeRange) as Interval)
        return interval
    }

    // 获取指定地址的区域
    override fun getRegion(address: Address): G? {
        return intervalMap[address].asRegion
    }

    // 过滤出所有区域，忽略空洞
    override fun filterRegions(): List<G> {
        val regions = mutableListOf<G>()
        for (interval in intervals) {
            val region = interval.asRegion
            if (region != null) {
                regions.add(region)
            }
        }
        return regions
    }

    // 获取相邻区域
    override fun getNeighbors(range: AddressRange): Pair<G?, G?> {
        val (prev, next) = range.spanAdjacentHoles().span(true)
        val prevRegion = if (range.contains(prev)) null else getRegion(prev)
        val nextRegion = if (range.contains(next)) null else getRegion(next)
        return Pair(prevRegion, nextRegion)
    }

    // 获取或分配区域
    override fun getOrAllocate(address: Address, createRegion: (AddressRange) -> G): G {
        val existingRegion = getRegion(address)
        return existingRegion ?: allocate(address, createRegion)
    }

    // 其他方法实现...
}
```

## 数据提供者

### 调试器数据提供者基础类

```kotlin
// 调试器数据提供者接口
interface CidrDebuggerDataProvider<T> :
    CidrMemoryData.DataProvider<T>,
    MemoryLiveExecutionAware {
    val process: CidrDebugProcess
}

// 抽象数据提供者基类
abstract class AbstractDataProvider<T> : CidrMemoryData.DataProvider<T> {
    override fun createDataPlaceholder(address: Address, holeRange: AddressRange): DataPlaceholder<T> {
        return AbstractDataPlaceholder(address, holeRange)
    }

    protected abstract data class AbstractDataPlaceholder<T>(
        override val address: Address,
        override val range: AddressRange,
        override val requestRange: AddressRange = range
    ) : DataPlaceholder<T> {
        override fun subRangeImpl(subRange: AddressRange): DataPlaceholder<T> {
            return AbstractDataPlaceholder(address, subRange, subRange)
        }
    }
}
```

### 十六进制数据提供者

```kotlin
// 十六进制数据提供者
class CidrDebuggerHexdumpDataProvider(
    override val process: CidrDebugProcess
) : AbstractDataProvider<Byte>() {

    override val blockSize: Int
        get() = (process.getMemory().hexdumpDoc.options as CidrHexdumpOptions).blockSize

    override fun dispose() {}

    override suspend fun loadData(
        address: Address,
        range: AddressRange
    ): List<CidrMemoryData.DataBlock<Byte>> {
        val debuggerCommandExecutor = process.debuggerCommandExecutor

        return debuggerCommandExecutor.executeCommand(false, false) {
            try {
                val memoryDump = it.dumpMemory(range)  // 从调试器读取内存
                fromMemoryHunkList(memoryDump)          // 转换为数据块
            } catch (e: DebuggerCommandException) {
                val errorMessage = createErrorMessage(e, range)
                throw CidrMemoryData.DataLoadException(errorMessage, e)
            }
        }
    }

    override fun readOnlyReason(): String? {
        return if (!process.supportsMemoryWrite()) {
            CidrDebuggerBundle.message("debug.memory.view.modify.not.supported", "memory")
        } else {
            when {
                process.session.isStopped ->
                    CidrDebuggerBundle.message("debug.memory.modify.in.stopped.session.is.not.supported")
                !process.session.isSuspended ->
                    CidrDebuggerBundle.message("debug.memory.modify.in.running.session.is.not.supported")
                else -> null
            }
        }
    }

    override suspend fun storeData(address: Address, bytes: ByteArray) {
        val readOnlyReason = readOnlyReason()
        if (readOnlyReason != null) {
            throw CidrMemoryData.DataLoadException(readOnlyReason)
        }

        val debuggerCommandExecutor = process.debuggerCommandExecutor
        debuggerCommandExecutor.executeCommand(false, false) {
            try {
                it.writeMemory(address, bytes)
            } catch (e: DebuggerCommandException) {
                val errorMessage = e.message ?: "Failed to store data at $address"
                throw CidrMemoryData.DataLoadException(errorMessage, e)
            }
        }
    }

    // 内存转储转换为数据块
    private fun fromMemoryHunkList(memoryHunks: List<MemoryHunk>): List<CidrMemoryData.DataBlock<Byte>> {
        return memoryHunks.map { hunk ->
            val bytes = hunk.bytes
            val address = hunk.address
            HexDataBlock(bytes, address)
        }
    }

    private fun createErrorMessage(e: DebuggerCommandException, range: AddressRange): String {
        val reason = e.message
        return if (reason != null && reason != "Unable to read memory $range") {
            CidrDebuggerBundle.message("debug.error.failedToReadMemory", range, reason)
        } else {
            CidrDebuggerBundle.message("debug.error.failedToReadMemory.unknownReason", range)
        }
    }
}

// 十六进制数据块
class HexDataBlock(
    private val bytes: ByteArray,
    private val address: Address
) : CidrMemoryData.DataBlock<Byte> {

    override val range: AddressRange = AddressRange(
        address,
        Address(address.value + bytes.size - 1)
    )

    override val entries: Iterable<CidrMemoryData.DataEntry<Byte>>
        get() = bytes.mapIndexed { index, byte ->
            HexDataEntry(Address(address.value + index), byte)
        }

    override fun subRangeImpl(subRange: AddressRange): HexDataBlock {
        val intersectedRange = range.intersectWith(subRange)
        if (intersectedRange.isEmpty()) {
            return HexDataBlock(ByteArray(0), intersectedRange.start)
        }

        val startOffset = (intersectedRange.start.value - address.value).toInt()
        val endOffset = (intersectedRange.endInclusive.value - address.value).toInt() + 1

        val subBytes = bytes.copyOfRange(startOffset, endOffset)
        return HexDataBlock(subBytes, intersectedRange.start)
    }
}

// 十六进制数据条目
class HexDataEntry(
    override val address: Address,
    val byteValue: Byte
) : CidrMemoryData.DataEntry<Byte> {

    override val value: Byte = byteValue
    override val range: AddressRange = AddressRange(address, address)

    fun toHexString(): String = String.format("%02X", byteValue.toInt() and 0xFF)
    fun toAsciiString(): String {
        val c = byteValue.toInt() and 0xFF
        return if (c >= 32 && c <= 126) c.toChar().toString() else "."
    }
}
```

### 反汇编数据提供者

```kotlin
// 反汇编数据提供者
class CidrDebuggerDisasmDataProvider(
    override val process: CidrDebugProcess
) : AbstractDataProvider<LLInstruction>() {

    override fun canProvideData(): Boolean = process.supportsDisassembly()

    override suspend fun loadData(
        address: Address,
        range: AddressRange
    ): List<CidrMemoryData.DataBlock<LLInstruction>> {
        val debuggerCommandExecutor = process.debuggerCommandExecutor

        return debuggerCommandExecutor.executeCommand(false, false) {
            try {
                val instructions = it.disassemble(range)
                fromInstructionList(instructions)
            } catch (e: DebuggerCommandException) {
                val errorMessage = createErrorMessage(e, range)
                throw CidrMemoryData.DataLoadException(errorMessage, e)
            }
        }
    }

    override fun readOnlyReason(): String? = "Disassembly is read-only"

    // 指令列表转换为数据块
    private fun fromInstructionList(instructions: List<LLInstruction>): List<DisasmDataBlock> {
        return instructions.map { instruction ->
            DisasmDataBlock(instruction)
        }
    }
}

// 反汇编数据块
class DisasmDataBlock(
    private val instruction: LLInstruction
) : CidrMemoryData.DataBlock<LLInstruction> {

    override val range: AddressRange = instruction.range
    override val entries: Iterable<CidrMemoryData.DataEntry<LLInstruction>>
        get() = listOf(DisasmDataEntry(instruction))

    override fun subRangeImpl(subRange: AddressRange): DisasmDataBlock {
        // 反汇编通常不支持部分指令的子范围
        return if (range.intersects(subRange)) this
        else DisasmDataBlock(createEmptyInstruction(subRange.start))
    }
}

// 反汇编数据条目
class DisasmDataEntry(
    private val instruction: LLInstruction
) : CidrMemoryData.DataEntry<LLInstruction> {

    override val address: Address = instruction.address
    override val value: LLInstruction = instruction
    override val range: AddressRange = instruction.range
}
```

## 文档渲染系统

### 抽象文档区域基类

```kotlin
// 抽象文档区域基类
abstract class AbstractDocRegion<T>(
    final override val document: DocumentEx,
    override val textRange: TextRange,
    override val dataRegion: CidrMemoryData.DataRegion<T>
) : DocRegion<T> {

    override val range: AddressRange = dataRegion.range
    override val rangeMarker: RangeMarker = document.createRangeMarker(textRange)

    private val lifetimeJob: CompletableJob = Job()

    override suspend fun awaitUnallocation() {
        lifetimeJob.join()
    }

    override fun deleteFromDocument() {
        try {
            if (rangeMarker.isValid) {
                document.deleteString(rangeMarker.startOffset, rangeMarker.endOffset)
            }
        } finally {
            disposeOnDeletion()
        }
    }

    protected open fun disposeOnDeletion() {
        if (rangeMarker.isValid) {
            rangeMarker.dispose()
        }
        lifetimeJob.complete()
    }

    override fun getLineRangeInDocument(): IntRange {
        if (!rangeMarker.isValid) {
            throw IllegalStateException("Region is disposed or otherwise removed from the Document")
        }

        val regionStartLineNumber = document.getLineNumber(rangeMarker.startOffset)
        val regionEndLineNumber = document.getLineNumber(rangeMarker.endOffset)
        return IntRange(regionStartLineNumber, regionEndLineNumber)
    }

    override fun toString(): String = dataRegion.range.toString()
}
```

### 基础文档区域

```kotlin
// 基础文档区域 - 处理已加载的数据
abstract class BaseDocRegion<T>(
    document: DocumentEx,
    textRange: TextRange,
    dataRegion: CidrMemoryData.DataRegion.Completed.Loaded<T>
) : AbstractDocRegion<T>(document, textRange, dataRegion) {

    protected val data: CidrMemoryData.Data<T> = dataRegion.data

    // 检查是否可以无缝更新
    open fun canBeSeamlesslyUpdatedBy(newRegion: BaseDocRegion<*>): Boolean {
        return this::class.java == newRegion::class.java &&
               range == newRegion.range &&
               newRegion.rangeMarker.isValid &&
               textRange == newRegion.textRange
    }

    override fun deleteFromDocument() {
        try {
            if (rangeMarker.isValid) {
                val protectedRanges = getProtectedRanges(rangeMarker.startOffset, rangeMarker.endOffset)
                protectedRanges.asReversed().forEach { (startOffset, endOffset) ->
                    document.deleteString(startOffset, endOffset)
                }

                if (!textRange.isEmpty) {
                    error("DocRegion text range is not empty after deleting it")
                }
            }
        } finally {
            disposeOnDeletion()
        }
    }

    // 地址到行号转换
    override fun getAddressForLineNumberInDocument(line: Int): Address? {
        val regionLineRange = getLineRangeInDocument()
        val coercedLine = line.coerceIn(regionLineRange)

        if (coercedLine != line) {
            CidrDebuggerLog.LOG.warn("Line number $line is outside the region line range $regionLineRange")
        }

        val lineNumberInsideRegion = coercedLine - regionLineRange.first
        return getAddressForLineNumberInsideRegion(lineNumberInsideRegion)
    }

    // 行号到地址转换
    override fun getLineNumberInDocument(address: Address): Int {
        if (!rangeMarker.isValid) {
            error("Region is disposed or otherwise removed from the Document")
        }

        val regionLineNumber = document.getLineNumber(rangeMarker.startOffset)
        val lineNumber = if (!range.contains(address)) 0 else getLineNumberInsideRegion(address)
        val coercedLineNumber = (regionLineNumber + lineNumber).coerceIn(0, document.lineCount - 1)

        if (regionLineNumber + lineNumber != coercedLineNumber) {
            CidrDebuggerLog.LOG.warn(
                "Computed line number $regionLineNumber + $lineNumber is outside the document boundary $coercedLineNumber"
            )
        }

        return coercedLineNumber
    }

    // 地址到偏移量转换
    override fun getOffsetInDocument(address: Address): Int {
        val lineNumber = getLineNumberInDocument(address)
        val offsetInLine = getOffsetInsideLine(address, lineNumber)
        return (document.getLineStartOffset(lineNumber) + offsetInLine).coerceAtMost(
            document.textLength - 1
        ).coerceAtLeast(0)
    }

    // 子类需要实现的抽象方法
    protected abstract fun getAddressForLineNumberInsideRegion(lineNumber: Int): Address?
    protected abstract fun getLineNumberInsideRegion(address: Address): Int
    protected open fun getOffsetInsideLine(address: Address, lineNumber: Int): Int = 0

    // 渲染数据到文档
    fun render(reallocatedSpace: AddressSpace<DocRegion<T>>) {
        val seamlessRegionUpdate = reallocatedSpace.intervals
            .singleOrNull { it is BaseDocRegion<*> && it.canBeSeamlesslyUpdatedBy(this) } as? BaseDocRegion<*>

        val withMarkerGreedy = rangeMarker
        val savedToLeft = withMarkerGreedy.isGreedyToLeft
        val savedToRight = withMarkerGreedy.isGreedyToRight

        withMarkerGreedy.isGreedyToLeft = true
        withMarkerGreedy.isGreedyToRight = true

        try {
            val newText = this.renderToText()
            if (seamlessRegionUpdate != null && this.textRange.length == newText.length) {
                this.replaceSeamlessly(seamlessRegionUpdate, newText)
            } else {
                this.document.replaceString(withMarkerGreedy.startOffset, withMarkerGreedy.endOffset, newText)
            }
        } finally {
            withMarkerGreedy.isGreedyToLeft = savedToLeft
            withMarkerGreedy.isGreedyToRight = savedToRight
        }

        if (withMarkerGreedy.startOffset == withMarkerGreedy.endOffset) {
            error("DocRegion text range is empty after rendering")
        }

        // 清理旧的区间
        reallocatedSpace.intervals.asSequence()
            .filterIsInstance<AbstractDocRegion<T>>()
            .forEach { it.rangeMarker.dispose() }
    }

    // 渲染到文本
    private fun renderToText(): CharSequence {
        val sb = StringBuilder()
        renderHeader(sb)

        val entries = data.entries
        for (entry in entries) {
            renderEntry(sb, entry)
        }

        renderFooter(sb)
        return sb
    }

    // 子类可重写的渲染方法
    protected open fun renderHeader(buffer: Appendable) {}
    protected open fun renderFooter(buffer: Appendable) {}
    protected abstract fun renderEntry(buffer: Appendable, dataEntry: CidrMemoryData.DataEntry<T>)

    // 无缝更新
    protected open fun replaceSeamlessly(oldRegion: BaseDocRegion<*>, newText: CharSequence) {
        val offset = this.rangeMarker.startOffset
        val protectedRanges = this.getProtectedRanges(offset, this.rangeMarker.endOffset)

        for (textRange in protectedRanges) {
            val (startOffset, endOffset) = textRange
            val subSequence = newText.subSequence(startOffset - offset, endOffset - offset).toString()
            document.replaceString(startOffset, endOffset, subSequence)
        }
    }

    // 获取需要保护的区域（光标、选择等）
    private fun getProtectedRanges(startOffset: Int, endOffset: Int): List<TextRange> {
        val protectedOffsets = linkedSetOf<Int>()

        // 处理标记
        fun addOffset(range: IntRange, offset: Int) {
            if (offset <= range.last && range.first <= offset) {
                protectedOffsets.add(offset - startOffset)
            }
        }

        fun processMarkupModel(processor: Processor<RangeMarker>, markupModel: MarkupModel) {
            (markupModel as? MarkupModelEx)?.processRangeHighlightersOverlappingWith(startOffset, endOffset) {
                markupModel.processRangeHighlightersOverlappingWith(startOffset, endOffset, processor)
            }
        }

        val length = endOffset - startOffset
        if (length == 0) return emptyList()

        val range = IntRange(startOffset, endOffset)
        val processor: Processor<RangeMarker> = Processor {
            addOffset(range, it.startOffset)
            addOffset(range, it.endOffset)
            true
        }

        document.processRangeMarkersOverlappingWith(startOffset, endOffset, processor)
        DocumentMarkupModel.getExistingMarkupModels(document).forEach { element ->
            processMarkupModel(processor, element)
        }

        EditorFactory.getInstance().editors(document).forEach {
            processMarkupModel(processor, it.markupModel)
            val allCarets = it.caretModel.allCarets
            for (caret in allCarets) {
                addOffset(range, caret.offset)
                addOffset(range, caret.selectionStart)
                addOffset(range, caret.selectionEnd)
            }
        }

        return protectedOffsets.sorted().map { TextRange(startOffset + it, startOffset + it) }
    }
}
```

### 十六进制文档区域

```kotlin
// 十六进制文档区域
class CidrHexdumpRegion(
    val project: Project,
    document: DocumentEx,
    textRange: TextRange,
    dataRegion: CidrMemoryData.DataRegion.Completed.Loaded<Byte>,
    val highlightingSessionId: Long
) : BaseDocRegion<Byte>(document, textRange, dataRegion) {

    companion object {
        private const val ASCII_PREFIX: String = "   │ "
        private const val ASCII_SUFFIX: String = " │"
        const val BYTE_SEPARATOR: String = " "

        // 高亮键
        private val CHANGED_BYTE_HIGHLIGHTER_KEY: Key<Boolean> =
            Key.create("CidrHexdumpRegion.changedByteHighlighter")

        private const val HEX_BYTE_WITH_SEPARATOR_LEN: Int = 3
        const val PAGE_BYTES: Int = 4096
        private const val WORD_BYTES: Int = 4
        const val WORD_SEPARATOR: String = "  "
        private const val WORD_TEXT_LEN: Int = 14

        fun Address.isAlignedTo(n: Int): Boolean = unsignedLongValue % n.toLong() == 0L

        fun Byte.toUnsignedHexString(): String =
            Integer.toUnsignedString(toInt() and 0xFF, 16).padStart(2, '0')
    }

    private val lineLen: Int
    private val asciiRangeInLine: TextRange
    private val afterAsciiRangeInLine: TextRange
    private val betweenHexAndAsciiRangeInLine: TextRange
    private val columnsCount: Int = CidrHexdumpOptions.getColumnsCount(document)
    private val hexRangeInLine: TextRange
    private var myAsciiBuffer: StringBuilder
    private val rangeMarkersToDispose: MutableList<RangeMarker> = mutableListOf()

    // 修改跟踪
    private val updateHighlighters: ConcurrentHashMap<Address, List<RangeHighlighter>> = ConcurrentHashMap()
    private val updateVersions: ConcurrentHashMap<Address, Int> = ConcurrentHashMap()
    private val updatedBytes: ConcurrentHashMap<Address, Byte> = ConcurrentHashMap()

    init {
        val bytesPerLine = columnsCount
        val wordsPerLine = if (bytesPerLine >= 4) bytesPerLine / 4 else 1
        val hexRangeLength = 3 * bytesPerLine + 2 * wordsPerLine
        lineLen = hexRangeLength + 5 + bytesPerLine + 2 + 1

        hexRangeInLine = TextRange.from(0, hexRangeLength)
        val asciiOffset = hexRangeLength + 5
        asciiRangeInLine = TextRange.from(asciiOffset, bytesPerLine)
        afterAsciiRangeInLine = TextRange.from(asciiOffset + bytesPerLine, 2)
        betweenHexAndAsciiRangeInLine = TextRange.from(hexRangeLength, 5)

        myAsciiBuffer = StringBuilder(columnsCount)
    }

    override fun getAddressForLineNumberInsideRegion(lineNumber: Int): Address? {
        val regionLineRange = getLineRangeInDocument()
        val lineInsideRegion = lineNumber - regionLineRange.first

        if (lineInsideRegion < 0 || lineInsideRegion >= data.entries.count()) {
            return null
        }

        val entries = data.entries.toList()
        return if (lineInsideRegion < entries.size) {
            entries[lineInsideRegion].address
        } else null
    }

    override fun getLineNumberInsideRegion(address: Address): Int {
        val entries = data.entries.toList()
        return entries.indexOfFirst { entry -> entry.address == address }.takeIf { it >= 0 } ?: 0
    }

    override fun getOffsetInsideLine(address: Address, lineNumber: Int): Int {
        val entries = data.entries.toList()
        return entries.indexOfFirst { entry -> entry.address == address }.takeIf { it >= 0 } ?: 0
    }

    override fun renderEntry(buffer: Appendable, dataEntry: CidrMemoryData.DataEntry<Byte>) {
        if (dataEntry !is HexDataEntry) return

        myAsciiBuffer.clear()

        // 渲染十六进制部分
        for (col in 0 until columnsCount) {
            val byteAddress = Address(dataEntry.address.value + col)
            val entry = data.entries.find { it.address == byteAddress } as? HexDataEntry

            if (entry != null) {
                buffer.append(entry.toHexString())
                myAsciiBuffer.append(entry.toAsciiString())
            } else {
                buffer.append("  ")
                myAsciiBuffer.append(" ")
            }

            // 添加分隔符
            if ((col + 1) % WORD_BYTES == 0 && col < columnsCount - 1) {
                buffer.append(WORD_SEPARATOR)
            } else if (col < columnsCount - 1) {
                buffer.append(BYTE_SEPARATOR)
            }
        }

        // 添加分隔符和ASCII部分
        buffer.append(betweenHexAndAsciiRangeInLine.startOffset.let { "  │ " })
        buffer.append(myAsciiBuffer.toString())
        buffer.append(ASCII_SUFFIX)
    }

    override fun renderHeader(buffer: Appendable) {
        // 可以在这里添加每块的头部信息，如块地址等
    }

    // 更新高亮显示
    fun updateHighlighters() {
        val markupModel = document.getMarkupModel(project) as MarkupModelEx

        // 清理旧的高亮
        rangeMarkersToDispose.forEach { it.dispose() }
        rangeMarkersToDispose.clear()

        // 添加新的高亮
        updatedBytes.forEach { (address, newValue) ->
            val line = getLineNumberInDocument(address)
            val offset = getOffsetInDocument(address)

            if (offset >= 0) {
                val highlighter = markupModel.addRangeHighlighter(
                    offset, offset + 2,  // 一个字节占2个字符
                    HighlighterLevel.WARNING,
                    TextAttributes().apply {
                        backgroundColor = JBColor.YELLOW
                    },
                    HighlighterTargetArea.EXACT_RANGE
                )

                highlighter.putUserData(CHANGED_BYTE_HIGHLIGHTER_KEY, true)
                rangeMarkersToDispose.add(highlighter)
            }
        }
    }

    // 存储修改的字节
    fun storeUpdatedByte(address: Address, value: Byte) {
        updatedBytes[address] = value
        updateHighlighters()
    }
}
```

### 辅助文档区域

```kotlin
// 辅助文档区域 - 用于Loading和Error状态
sealed class AuxDocRegion<T>(
    document: DocumentEx,
    textRange: TextRange,
    dataRegion: CidrMemoryData.DataRegion<T>
) : AbstractDocRegion<T>(document, textRange, dataRegion) {

    override fun getAddressForLineNumberInDocument(line: Int): Address? =
        if (line == getLineNumberInDocument(range.start)) range.start else null

    override fun getLineNumberInDocument(address: Address): Int {
        return document.getLineNumber(getOffsetInDocument(address))
    }

    override fun getOffsetInDocument(address: Address): Int {
        return if (!rangeMarker.isValid) {
            throw IllegalStateException("Region is disposed or otherwise removed from the Document")
        } else {
            rangeMarker.startOffset
        }
    }
}

// 加载中文档区域
class LoadingDocRegion<T>(
    document: DocumentEx,
    textRange: TextRange,
    dataRegion: CidrMemoryData.DataRegion.Loading<T>
) : AuxDocRegion<T>(document, textRange, dataRegion) {

    init {
        renderLoadingIndicator()
    }

    private fun renderLoadingIndicator() {
        val loadingText = "Loading memory at ${dataRegion.data.address}..."
        document.replaceString(textRange.startOffset, textRange.length, loadingText)
    }
}

// 错误文档区域
class ErrorDocRegion<T>(
    document: DocumentEx,
    textRange: TextRange,
    dataRegion: CidrMemoryData.DataRegion.Completed.LoadError<T>
) : AuxDocRegion<T>(document, textRange, dataRegion) {

    init {
        renderErrorMessage()
    }

    private fun renderErrorMessage() {
        val errorText = "Error loading memory at ${dataRegion.data.address}: ${dataRegion.exception.message}"
        document.replaceString(textRange.startOffset, textRange.length, errorText)
    }
}
```

## 内存数据管理器

### CidrMemoryData 核心实现

```kotlin
// 内存数据管理器
class CidrMemoryData<T>(private val dataProvider: DataProvider<T>) {

    companion object {
        // 范围操作辅助函数
        fun <D : ReallocatableInterval> D.subRange(subRange: AddressRange): D {
            return if (subRange == range) {
                this
            } else {
                subRange.requireInRange(range)
                val result = this.subRangeImpl(subRange)
                result.checkInRange(subRange) as D
            }
        }
    }

    // 核心地址空间
    private val addressSpace: MutableAddressSpace<DataRegion<T>> = mutableAddressSpace()

    // 获取或创建指定地址的数据区域
    fun getOrFetchRangeForAddress(
        coroutineScope: CoroutineScope,
        address: Address
    ): DataRegion<T> {
        val region = addressSpace.getOrAllocate(address) { holeRange ->
            val dataPlaceholder = dataProvider.createDataPlaceholder(address, holeRange)
                .checkInRange(holeRange)

            DataRegion.Loading(dataPlaceholder, coroutineScope) {
                loadAndSave(address)
            }
        }

        if (region is DataRegion.Loading) {
            region.start()
        }

        return region
    }

    // 使指定范围的数据失效
    fun invalidateRange(range: AddressRange) {
        val intervals = addressSpace.unallocate(range).intervals

        for (interval in intervals) {
            if (interval is DataRegion.Loading<*>) {
                interval.cancel()
            }
        }
    }

    // 加载并清理数据
    private suspend fun DataRegion.Loading<T>.loadAndCleanup(address: Address): List<DataBlock<T>> {
        return try {
            val dataBlocks = dataProvider.loadData(address, data.requestRange)

            // 清理相关区域
            val regionsToUnallocate = addressSpace[range].filterRegions()
            for (region in regionsToUnallocate) {
                addressSpace.unallocate(region)
            }

            dataBlocks
        } catch (e: Throwable) {
            // 错误时也要清理
            val regionsToUnallocate = addressSpace[range].filterRegions()
            for (region in regionsToUnallocate) {
                addressSpace.unallocate(region)
            }
            throw e
        }
    }

    // 加载并保存数据
    private suspend fun DataRegion.Loading<T>.loadAndSave(address: Address): DataRegion.Completed<T> {
        return try {
            val dataBlocks = loadAndCleanup(address)

            // 保存新加载的数据块
            for (block in dataBlocks) {
                if (block.range.isEmpty()) {
                    throw IllegalStateException("$block range is empty")
                }
                val region = DataRegion.Completed.Loaded(block)
                addressSpace.reallocate(region)
            }

            // 获取结果区域
            val region = addressSpace.getRegion(address)
            if (region == null) {
                val unrelatedRanges = dataBlocks.map { it.range }
                val message = "Debugger reported unrelated range(s) for $address: $unrelatedRanges"
                throw DataLoadException(message)
            }

            region.awaitCompleted()
        } catch (e: DataLoadException) {
            // 创建错误状态
            val holeRange = data.range.intersectWith(data.range)
            val dataPlaceholder = data.subRange(holeRange)
            DataRegion.Completed.LoadError(dataPlaceholder, e)
        }
    }
}

// CidrMemoryDocAccess - 文档访问管理器
class CidrMemoryDocAccess<T>(
    dataProvider: DataProvider<T>,
    virtualFile: VirtualFile
) {

    companion object {
        // 文档写操作辅助函数
        private fun <R> withWritable(doc: Document, file: VirtualFile, block: () -> R): R {
            val fileWasWritable = file.isWritable
            file.isWritable = true

            return try {
                doc.withWritable(true, block)
            } finally {
                file.isWritable = fileWasWritable
            }
        }

        private fun <R> Document.withWritable(value: Boolean, block: () -> R): R {
            val wasWritable = this.isWritable
            this.setReadOnly(!value)

            return try {
                block()
            } finally {
                this.setReadOnly(!wasWritable)
            }
        }
    }

    private val docAccessScope: DocAccessScope<T>

    init {
        val data = CidrMemoryData(dataProvider)
        val addressSpace: MutableAddressSpace<DocRegion<T>> = mutableAddressSpace()
        val document = runReadAction {
            SlowOperations.allowSlowOperations("generic").use {
                FileDocumentManager.getInstance().getDocument(virtualFile)
            }
        } as? DocumentEx ?: throw IllegalStateException(
            "Null or non-DocumentEx document returned for newly created LightVirtualFile"
        )

        UndoUtil.disableUndoFor(document)
        document.setReadOnly(true)

        val docAccessScope = DocAccessScope(data, document, virtualFile, addressSpace)
        this.docAccessScope = docAccessScope
    }

    // 读操作
    fun <R> read(block: DocAccessScope<T>.() -> R): R =
        runReadAction { block(docAccessScope) }

    // 写操作
    fun <R> edit(block: DocAccessScope<T>.() -> R): R {
        val docAccessScope = docAccessScope
        return invokeAndWaitIfNeeded {
            runUndoTransparentWriteAction {
                withWritable(docAccessScope.document, docAccessScope.file) {
                    block.invoke(docAccessScope)
                }
            }
        }
    }

    // 异步写操作
    suspend fun <R> editSuspend(block: DocAccessScope<T>.() -> R): R {
        val docAccessScope = this@CidrMemoryDocAccess.docAccessScope
        return runUndoTransparentWriteAction {
            withWritable(docAccessScope.document, docAccessScope.file) {
                block.invoke(docAccessScope)
            }
        }
    }

    // 文档访问作用域
    data class DocAccessScope<T>(
        val data: CidrMemoryData<T>,
        val document: DocumentEx,
        val file: VirtualFile,
        val addressSpace: MutableAddressSpace<DocRegion<T>>
    )
}
```

## 具体实现

### CidrMemoryDoc - 文档管理器

```kotlin
// 内存文档管理器
class CidrMemoryDoc<T>(
    val project: Project,
    parentDisposable: Disposable,
    val dataProvider: CidrMemoryData.DataProvider<T>,
    val docRegionFactory: DocRegionFactory<T>,
    fileType: FileType,
    val name: String,
    val options: CidrMemoryDocOptions = CidrMemoryDocOptions.DEFAULT
) : MemoryLineInfoProvider {

    private val coroutineScope: CoroutineScope = CoroutineScope(
        Dispatchers.EDT + SupervisorJob() + CoroutineName("CidrMemoryDoc")
    )

    // 事件分发器
    val eventDispatcher = EventDispatcher.create(DocRegionListener::class.java) as EventDispatcher<DocRegionListener<T>>

    // 虚拟文件和文档访问
    val virtualFile: MemoryViewFile = MemoryViewVirtualFileSystem.getInstance()
        .createMemoryViewFile(this, this.name, fileType)

    val docAccess: CidrMemoryDocAccess<T> = CidrMemoryDocAccess(dataProvider, virtualFile)

    // 缓存最近使用的范围
    private val lastUsedRangeForLine: AtomicReference<AddressRange?> = AtomicReference()

    // 添加文档区域监听器
    fun addDocRegionListener(listener: DocRegionListener<T>) {
        eventDispatcher.addListener(listener)
    }

    // 清理错误区域
    fun cleanupErrors(range: AddressRange = AddressRange.WHOLE) {
        docAccess.edit {
            val regions = addressSpace[range].filterRegions()

            for (element in regions) {
                if (element.dataRegion is CidrMemoryData.DataRegion.Completed.LoadError) {
                    unallocateAndDeleteDocRegion(element)
                }
            }
        }
    }

    // 查找边缘区域
    private fun findEdgeRegion(
        startRegion: DocRegion<*>,
        nextRegionAddressFn: (DocRegion<*>) -> Address?
    ): DocRegion<*>? {
        return docAccess.read {
            var region = startRegion
            if (region !is ErrorDocRegion && region.dataRegion !is CidrMemoryData.DataRegion.Loading) {
                while (true) {
                    val nextRegionAddress = nextRegionAddressFn(region)
                    val nextDocRegion = if (nextRegionAddress != null) {
                        val interval = addressSpace[nextRegionAddress]
                        if (interval is DocRegion<*>) interval else null
                    } else null

                    if (nextDocRegion is ErrorDocRegion ||
                        (nextDocRegion?.dataRegion is CidrMemoryData.DataRegion.Loading)) {
                        return@read null
                    }

                    if (nextDocRegion == null) {
                        return@read region
                    }

                    region = nextDocRegion
                }
                null
            } else null
        }
    }

    // 查找行对应的区域
    fun findRegionForLine(line: Int): DocRegion<T>? = docAccess.read {
        val lastUsedRange = lastUsedRangeForLine.get()
        val lastUsedRegion = lastUsedRange?.let { addressSpace.getRegion(it.start) }

        if (lastUsedRegion != null && lastUsedRegion.getLineRangeInDocument().contains(line)) {
            val nextRegion = addressSpace.getNeighbors(lastUsedRegion.range).second
            val nextRegionStartLine = nextRegion?.getLineRangeInDocument()?.first
            val nextRegionx = if (nextRegionStartLine != null && nextRegionStartLine <= line)
                nextRegion else lastUsedRegion
            lastUsedRangeForLine.set(nextRegionx.range)
            return@read nextRegionx
        } else {
            val regions = addressSpace.filterRegions()
            val foundIndex = regions.binarySearchBy(0) {
                val lineRange = it.getLineRangeInDocument()
                when {
                    lineRange.last < line -> -1
                    line < lineRange.first -> 1
                    else -> 0
                }
            }

            if (foundIndex < 0) {
                lastUsedRangeForLine.set(null)
                return@read null
            } else {
                val nextRegionx = regions.getOrNull(foundIndex + 1)
                val nextRegionStartLine = nextRegionx?.getLineRangeInDocument()?.first
                val result = if (nextRegionStartLine != null && nextRegionStartLine <= line)
                    nextRegionx else regions[foundIndex]
                lastUsedRangeForLine.set(result.range)
                return@read result
            }
        }
    }

    // 触发文档区域分配事件
    private fun fireDocRegionAllocated(
        unallocatedSpace: AddressSpace<DocRegion<T>>,
        newDocRegion: DocRegion<T>
    ) {
        eventDispatcher.getMulticaster().onDocRegionAllocated(unallocatedSpace, newDocRegion)
    }

    // 地址到位置转换
    fun getAddress(sourcePosition: XSourcePosition): Address? {
        return if (sourcePosition.file == virtualFile) getAddressForLine(sourcePosition.line) else null
    }

    override fun getAddressForLine(line: Int): Address? = docAccess.read {
        val region = findRegionForLine(line)
        return@read region?.getAddressForLineNumberInDocument(line)
    }

    // 地址位置对象
    fun getAddressPosition(address: Address): XSourcePositionEx = AddressPosition(address)

    // 处理数据加载完成
    private fun handleDataLoadCompletion(
        loading: CidrMemoryData.DataRegion.Loading<T>,
        docRegion: DocRegion<T>,
        unallocateOnCancellation: Boolean
    ) {
        coroutineScope.launch {
            try {
                val completedData = loading.awaitCompleted()
                docAccess.edit {
                    if (addressSpace.contains(docRegion as Interval)) {
                        createAndAllocateDocRegion(completedData)
                    }
                }
            } catch (e: CancellationException) {
                docAccess.edit {
                    if (addressSpace.contains(docRegion as Interval)) {
                        if (unallocateOnCancellation) {
                            unallocateAndDeleteDocRegion(docRegion)
                        }
                    }
                }
                throw e
            }
        }
    }

    // 检查地址是否已加载
    private fun isAddressLoaded(address: Address): Boolean = docAccess.read {
        return@read addressSpace.getRegion(address) is BaseDocRegion
    }

    // 加载地址数据
    suspend fun loadAddress(address: Address): AddressRange? {
        return loadAddress(address) { it.range }
    }

    suspend fun <R : Any> loadAddress(
        address: Address,
        computeInReadAction: (BaseDocRegion<T>) -> R
    ): R? {
        return loadAddressInternal(address, computeInReadAction)?.result
    }

    // 内部加载地址实现
    private suspend fun <R : Any> loadAddressInternal(
        address: Address,
        computeInReadAction: (BaseDocRegion<T>) -> R
    ): RegionInfo<T, R?>? {
        val computeIfLoaded: (DocRegion<T>) -> R? = { region ->
            if (region is BaseDocRegion) {
                computeInReadAction(region)
            } else {
                null
            }
        }

        var regionInfo = tryReadRegionInfo(address, computeIfLoaded)

        while (regionInfo?.region is LoadingDocRegion) {
            regionInfo.region.awaitUnallocation()
            regionInfo = tryReadRegionInfo(address, computeIfLoaded)
        }

        return regionInfo
    }

    // 加载更高地址的数据
    fun loadHigherAddresses(line: Int, count: Int) {
        val lineRegion = findRegionForLine(line)
        if (lineRegion != null) {
            val edgeRegion = findNextEdgeRegion(lineRegion)
            if (edgeRegion != null) {
                if (edgeRegion.range.endInclusive != Address.MAX_VALUE) {
                    val edgeLine = docAccess.read {
                        document.getLineNumber(edgeRegion.textRange.endOffset)
                    }.toInt()
                    if (edgeLine - line <= count) {
                        docAccess.edit {
                            requestRegion(edgeRegion.range.endInclusive + 1, null)
                        }
                    }
                }
            }
        }
    }

    // 加载更低地址的数据
    fun loadLowerAddresses(line: Int, count: Int) {
        val lineRegion = findRegionForLine(line)
        if (lineRegion != null) {
            val edgeRegion = findPrevEdgeRegion(lineRegion)
            if (edgeRegion != null && edgeRegion.range.start != Address.MIN_VALUE) {
                val edgeLine = docAccess.read {
                    document.getLineNumber(edgeRegion.textRange.startOffset)
                }.toInt()
                if (line - edgeLine <= count) {
                    docAccess.edit {
                        requestRegion(edgeRegion.range.start - 1, null)
                    }
                }
            }
        }
    }

    // 加载范围数据
    suspend fun loadRange(range: AddressRange): AddressRange? {
        var loadedStart: Address
        var loadedEndInclusive: Address
        var nextAddress: Address
        var prevAddress: Address

        range.requireNotEmpty()

        loadedStart = range.start
        loadedEndInclusive = range.endInclusive
        nextAddress = range.start
        prevAddress = nextAddress

        do {
            val loadedRange = loadAddress(nextAddress) ?: return null

            if (loadedRange.contains(range.start)) {
                loadedStart = minOf(loadedRange.start, loadedStart)
            }

            if (loadedRange.contains(range.endInclusive)) {
                loadedEndInclusive = maxOf(loadedRange.endInclusive, loadedEndInclusive)
            }

            nextAddress = loadedRange.endInclusive + 1

            if (nextAddress > range.endInclusive || nextAddress == prevAddress) {
                return loadedStart.rangeTo(loadedEndInclusive)
            }

            prevAddress = nextAddress
        } while (true)
    }

    // 标记数据为过时
    fun markOutdated(range: AddressRange = AddressRange.WHOLE) {
        docAccess.edit {
            data.invalidateRange(range)
            val regions = addressSpace.get(range).filterRegions()

            regions.forEach { region ->
                requestRegion(region.range.start, region)
            }
        }
    }

    // 读取或请求区域信息
    private fun <R> readOrRequestRegionInfo(
        address: Address,
        computeInReadAction: (DocRegion<T>) -> R
    ): RegionInfo<T, R> {
        var result = tryReadRegionInfo(address, computeInReadAction)
        if (result == null) {
            result = requestRegionInfo(address, computeInReadAction)
        }
        return result
    }

    // 刷新数据
    fun refresh(range: AddressRange = AddressRange.WHOLE) {
        docAccess.edit {
            data.invalidateRange(range)
            val regions = addressSpace.get(range).filterRegions()

            regions.forEach { region ->
                requestRegion(region.range.start, region)
            }
        }
    }

    // 存储数据
    fun storeData(address: Address, dataToStore: ByteArray, completionCallback: (Throwable?) -> Unit = {}) {
        coroutineScope.launch {
            try {
                dataProvider.storeData(address, dataToStore)
                completionCallback(null)
            } catch (err: CidrMemoryData.DataLoadException) {
                completionCallback(err)
            }
        }
    }

    // 请求区域信息
    private fun <R> requestRegionInfo(
        address: Address,
        computeInReadAction: (DocRegion<T>) -> R
    ): RegionInfo<T, R> {
        return docAccess.edit {
            val region = addressSpace.getRegion(address) ?: requestRegion(address = address, oldDocRegion = null)
            val result = computeInReadAction(region)
            RegionInfo(region, result)
        }
    }

    // 文本范围计算
    private fun textRangeBetween(first: DocRegion<T>?, last: DocRegion<*>?): TextRange {
        val firstEndOffset = first?.textRange?.endOffset
        val lastStartOffset = last?.textRange?.startOffset

        val startOffset = firstEndOffset ?: (lastStartOffset ?: 0)
        val endOffset = lastStartOffset ?: (firstEndOffset ?: 0)

        return TextRange(startOffset, endOffset)
    }

    private fun textRangeSpan(first: DocRegion<T>, last: DocRegion<*>): TextRange {
        val startOffset: Int = first.textRange.startOffset
        val endOffset: Int = last.textRange.endOffset
        return TextRange(startOffset, endOffset)
    }

    // 尝试读取区域信息
    private fun <R> tryReadRegionInfo(
        address: Address,
        computeInReadAction: (DocRegion<T>) -> R
    ): RegionInfo<T, R>? {
        return docAccess.read {
            val region = addressSpace.getRegion(address)
            if (region == null) {
                null
            } else {
                val result = computeInReadAction(region)
                RegionInfo(region, result)
            }
        }
    }

    // 地址位置内部类
    inner class AddressPosition(val address: Address) :
        OpenFileDescriptor(project, virtualFile), XSourcePositionEx {

        private val loadRequested: CompletableJob = Job()

        override val positionUpdateFlow: Flow<Boolean> = flow {
            if (isAddressLoaded(address)) {
                return@flow
            }

            loadAddress(address)
            emit(true)
        }

        override fun createNavigatable(project: Project): Navigatable = this
        override fun getColumn(): Int = 0

        override fun getLine(): Int {
            val regionInfo = tryReadRegionInfo(address) { region ->
                region.getLineNumberInDocument(address)
            }
            return regionInfo?.result ?: 0
        }

        override fun getOffset(): Int {
            val regionInfo = tryReadRegionInfo(address) { region ->
                region.getOffsetInDocument(address)
            }
            return regionInfo?.result ?: 0
        }

        override fun navigateIn(e: Editor) {
            requestLoading()
            super.navigateIn(e)
        }

        fun requestLoading() {
            readOrRequestRegionInfo(address, {})
            loadRequested.complete()
        }
    }

    // 文档区域工厂接口
    interface DocRegionFactory<T> {
        fun createDocRegion(
            project: Project,
            document: DocumentEx,
            textRange: TextRange,
            dataRegion: CidrMemoryData.DataRegion.Completed.Loaded<T>,
            reallocatedSpace: AddressSpace<DocRegion<T>>
        ): DocRegion<T>
    }

    // 文档区域监听器接口
    interface DocRegionListener<T> : EventListener {
        fun onDocRegionAllocated(
            unallocatedSpace: AddressSpace<DocRegion<T>>,
            newDocRegion: DocRegion<T>
        ) {}
    }

    // 区域信息数据类
    private data class RegionInfo<T, R>(
        val region: DocRegion<T>,
        val result: R
    )

    // 文档访问作用域中的私有方法
    private fun CidrMemoryDocAccess<T>.DocAccessScope<T>.createAndAllocateDocRegion(
        dataRegion: CidrMemoryData.DataRegion<T>
    ): DocRegion<T> {
        val unallocatedSpace = addressSpace.unallocate(dataRegion.range)
        val textRange = textRangeOf(unallocatedSpace)
        val newDocRegion = createDocRegion(textRange, dataRegion, unallocatedSpace)
        addressSpace.allocate(newDocRegion)

        // 清理未分配的区域
        val unallocatedRegions = unallocatedSpace.filterRegions()
        for (docRegion in unallocatedRegions) {
            docRegion.deleteFromDocument()
        }

        fireDocRegionAllocated(unallocatedSpace, newDocRegion)
        return newDocRegion
    }

    private fun CidrMemoryDocAccess<T>.DocAccessScope<T>.createDocRegion(
        textRange: TextRange,
        dataRegion: CidrMemoryData.DataRegion<T>,
        unallocatedSpace: MutableAddressSpace<DocRegion<T>>
    ): DocRegion<T> {
        return when (dataRegion) {
            is CidrMemoryData.DataRegion.Loading<T> ->
                LoadingDocRegion(document, textRange, dataRegion)

            is CidrMemoryData.DataRegion.Completed.LoadError<T> ->
                ErrorDocRegion(document, textRange, dataRegion)

            is CidrMemoryData.DataRegion.Completed.Loaded<T> ->
                docRegionFactory.createDocRegion(
                    project, document, textRange, dataRegion, unallocatedSpace
                )
        }
    }

    private fun CidrMemoryDocAccess<T>.DocAccessScope<T>.requestRegion(
        address: Address,
        oldDocRegion: DocRegion<T>? = null
    ): DocRegion<T> {
        val dataRegion = data.getOrFetchRangeForAddress(coroutineScope, address)
        val docRegion = if (dataRegion is CidrMemoryData.DataRegion.Loading<T>) {
            oldDocRegion ?: createAndAllocateDocRegion(dataRegion)
        } else {
            null
        }

        address.requireInRange(docRegion?.range ?: throw IllegalStateException("DocRegion should not be null"))

        if (dataRegion is CidrMemoryData.DataRegion.Loading<T>) {
            ApplicationManager.getApplication().invokeLater {
                handleDataLoadCompletion(dataRegion, docRegion, oldDocRegion == null)
            }
        }

        return docRegion
    }

    private fun CidrMemoryDocAccess<T>.DocAccessScope<T>.textRangeOf(
        space: AddressSpace<DocRegion<T>>
    ): TextRange {
        val regions = space.filterRegions()

        return if (regions.none { it is LoadingDocRegion }) {
            textRangeSpan(regions.first(), regions.last())
        } else {
            val (prev, next) = addressSpace.getNeighbors(space.range)
            textRangeBetween(prev, next)
        }
    }

    private fun CidrMemoryDocAccess<T>.DocAccessScope<T>.unallocateAndDeleteDocRegion(
        docRegion: DocRegion<T>
    ) {
        docRegion.deleteFromDocument()
    }
}
```

## 使用示例

### 基本使用流程

```kotlin
// 1. 创建调试后端
val debugBackend = CidrDebuggerBackend(process)

// 2. 创建数据提供者
val hexDataProvider = CidrDebuggerHexdumpDataProvider(process)

// 3. 创建文档区域工厂
val hexDocRegionFactory = CidrHexdumpRegionFactory()

// 4. 创建内存文档
val memoryDoc = CidrMemoryDoc<Byte>(
    project = project,
    parentDisposable = disposable,
    dataProvider = hexDataProvider,
    docRegionFactory = hexDocRegionFactory,
    fileType = MemoryViewFileType.INSTANCE,
    name = "Memory View",
    options = CidrHexdumpOptions.DEFAULT
)

// 5. 加载特定地址的数据
val address = Address(0x1000)
GlobalScope.launch {
    try {
        val loadedRange = memoryDoc.loadAddress(address)
        println("Loaded memory range: $loadedRange")

        // 获取文档内容
        val document = FileDocumentManager.getInstance().getDocument(memoryDoc.virtualFile)
        println("Document content: ${document?.text}")

    } catch (e: Exception) {
        println("Failed to load memory: ${e.message}")
    }
}

// 6. 监听文档区域分配事件
memoryDoc.addDocRegionListener(object : CidrMemoryDoc.DocRegionListener<Byte> {
    override fun onDocRegionAllocated(
        unallocatedSpace: AddressSpace<DocRegion<Byte>>,
        newDocRegion: DocRegion<Byte>
    ) {
        println("New doc region allocated: ${newDocRegion.range}")
    }
})

// 7. 模拟滚动加载
memoryDoc.loadHigherAddresses(0, 16)  // 加载更高地址
memoryDoc.loadLowerAddresses(0, 16)   // 加载更低地址

// 8. 刷新数据
memoryDoc.refresh()

// 9. 存储数据
val dataToStore = byteArrayOf(0x48, 0x65, 0x6C, 0x6C, 0x6F)  // "Hello"
memoryDoc.storeData(Address(0x2000), dataToStore) { error ->
    if (error != null) {
        println("Failed to store data: ${error.message}")
    } else {
        println("Data stored successfully")
    }
}
```

### 高级使用场景

```kotlin
// 自定义数据提供者
class CustomDataProvider : CidrMemoryData.DataProvider<MyCustomData> {
    override suspend fun loadData(address: Address, range: AddressRange): List<DataBlock<MyCustomData>> {
        // 从自定义源加载数据
        return listOf(CustomDataBlock(loadCustomData(range), address))
    }

    override fun createDataPlaceholder(address: Address, holeRange: AddressRange): DataPlaceholder<MyCustomData> {
        return CustomDataPlaceholder(address, holeRange)
    }
}

// 自定义文档区域
class CustomDocRegion(
    document: DocumentEx,
    textRange: TextRange,
    dataRegion: CidrMemoryData.DataRegion.Completed.Loaded<MyCustomData>
) : BaseDocRegion<MyCustomData>(document, textRange, dataRegion) {

    override fun renderEntry(buffer: Appendable, dataEntry: CidrMemoryData.DataEntry<MyCustomData>) {
        val customData = dataEntry.value
        buffer.append("Address: ${dataEntry.address}, Data: ${customData.render()}\n")
    }

    override fun getAddressForLineNumberInsideRegion(lineNumber: Int): Address? {
        // 自定义地址到行号的映射逻辑
        return null
    }

    override fun getLineNumberInsideRegion(address: Address): Int {
        // 自�行行号到地址的映射逻辑
        return 0
    }
}

// 自定义文档区域工厂
class CustomDocRegionFactory : CidrMemoryDoc.DocRegionFactory<MyCustomData> {
    override fun createDocRegion(
        project: Project,
        document: DocumentEx,
        textRange: TextRange,
        dataRegion: CidrMemoryData.DataRegion.Completed.Loaded<MyCustomData>,
        reallocatedSpace: AddressSpace<DocRegion<MyCustomData>>
    ): DocRegion<MyCustomData> {
        return CustomDocRegion(document, textRange, dataRegion)
    }
}
```

## 性能分析

### 时间复杂度分析

| 操作类型 | 时间复杂度 | 说明 |
|---------|-----------|------|
| 地址查找 | O(log n) | 基于TreeMap的红黑树查找 |
| 区域分配 | O(log n) | TreeMap插入操作 |
| 区域取消分配 | O(log n + k) | k为受影响的区间数量 |
| 范围查询 | O(log n + m) | m为范围内的区间数量 |
| 文档渲染 | O(k) | k为数据项数量 |

### 内存使用分析

#### 内存优化策略
1. **按需加载**: 只在需要时加载数据，避免内存溢出
2. **空洞合并**: 相邻的空洞自动合并，减少碎片
3. **区间压缩**: 用单个区间表示连续的相同状态区域
4. **延迟清理**: 使用协程延迟清理不需要的资源

#### 内存使用估算
```
对于1GB地址空间，典型内存使用:
- IntervalTreeMap: ~100KB (区间映射)
- DataRegion对象: ~1MB (已加载的数据区域)
- 文档缓存: ~10MB (可见区域)
- 总计: ~11MB (相比1GB地址空间，内存使用率 < 1%)
```

### 性能测试结果

```kotlin
// 性能测试示例
@Test
fun performanceTest() {
    val memoryDoc = createLargeMemoryDocument() // 创建包含1M个区域的内存文档

    // 地址查找性能测试
    val startTime = System.currentTimeMillis()
    repeat(10000) {
        memoryDoc.findRegionForLine(Random.nextInt(100000))
    }
    val lookupTime = System.currentTimeMillis() - startTime

    // 加载性能测试
    val loadStartTime = System.currentTimeMillis()
    repeat(100) {
        memoryDoc.loadAddress(Address(Random.nextLong(0x1000, 0x10000)))
    }
    val loadTime = System.currentTimeMillis() - loadStartTime

    println("10000次地址查找耗时: ${lookupTime}ms")
    println("100次数据加载耗时: ${loadTime}ms")
}

// 预期结果:
// 10000次地址查找耗时: < 100ms (平均 < 0.01ms/次)
// 100次数据加载耗时: < 1000ms (平均 < 10ms/次)
```

## 扩展性设计

### 数据类型扩展

```kotlin
// 支持新的数据类型
sealed class ExtendedDataType {
    object NetworkPacket : ExtendedDataType()
    object GpuBuffer : ExtendedDataType()
    object FileContent : ExtendedDataType()
}

// 扩展数据提供者
class ExtendedDataProvider : CidrMemoryData.DataProvider<ExtendedData> {
    override suspend fun loadData(address: Address, range: AddressRange): List<DataBlock<ExtendedData>> {
        return when (detectDataType(range)) {
            ExtendedDataType.NetworkPacket -> loadNetworkPackets(range)
            ExtendedDataType.GpuBuffer -> loadGpuBuffers(range)
            ExtendedDataType.FileContent -> loadFileContent(range)
        }
    }
}
```

### 显示格式扩展

```kotlin
// 支持新的显示格式
class GraphicalDocRegion : BaseDocRegion<GraphicalData> {
    override fun renderEntry(buffer: Appendable, dataEntry: CidrMemoryData.DataEntry<GraphicalData>) {
        // 渲染图形化数据
        buffer.append("[GRAPH: ${dataEntry.value.renderAsImage()}]")
    }
}

class TableDocRegion : BaseDocRegion<TableData> {
    override fun renderEntry(buffer: Appendable, dataEntry: CidrMemoryData.DataEntry<TableData>) {
        // 渲染表格数据
        buffer.append(dataEntry.value.renderAsTable())
    }
}
```

### 存储后端扩展

```kotlin
// 支持不同的存储后端
interface DataStorageBackend {
    suspend fun read(address: Address, size: Int): ByteArray
    suspend fun write(address: Address, data: ByteArray): Boolean
    suspend fun invalidate(range: AddressRange): Unit
}

// 云端存储后端
class CloudStorageBackend : DataStorageBackend {
    override suspend fun read(address: Address, size: Int): ByteArray {
        return cloudApiClient.readMemory(address, size)
    }
}

// 分布式缓存后端
class DistributedCacheBackend : DataStorageBackend {
    override suspend fun read(address: Address, size: Int): ByteArray {
        return cacheClient.getOrLoad(address, size)
    }
}
```

### 插件系统设计

```kotlin
// 插件接口
interface MemoryVisualizationPlugin {
    fun getName(): String
    fun getVersion(): String
    fun getDataProvider(): DataProvider<*>
    fun getDocRegionFactory(): DocRegionFactory<*>
    fun isSupported(dataType: Class<*>): Boolean
}

// 插件管理器
class MemoryVisualizationPluginManager {
    private val plugins = mutableListOf<MemoryVisualizationPlugin>()

    fun registerPlugin(plugin: MemoryVisualizationPlugin) {
        plugins.add(plugin)
    }

    fun getPlugin(dataType: Class<*>): MemoryVisualizationPlugin? {
        return plugins.find { it.isSupported(dataType) }
    }
}
```

## 总结

本DataRegion内存数据可视化系统通过以下关键技术实现了高效、可扩展的内存数据可视化：

### 核心技术特点

1. **状态机模式**: 清晰的数据生命周期管理
2. **区间映射**: 基于TreeMap的高效地址空间管理
3. **异步加载**: 基于协程的非阻塞数据处理
4. **按需渲染**: 根据可见性动态生成文档内容
5. **无缝更新**: 支持在不中断用户操作的情况下更新内容

### 性能优势

- **内存效率**: 相比全量加载节省99%以上的内存使用
- **响应速度**: 地址查找在毫秒级完成
- **可扩展性**: 支持TB级地址空间的可视化

### 应用场景

- **调试器内存视图**: 支持十六进制、反汇编等多种显示格式
- **性能分析工具**: 可视化内存分配和使用情况
- **系统监控**: 实时显示系统内存状态
- **数据分析**: 支持大规模数据集的可视化分析

该设计为内存数据的可视化和交互提供了完整、高效的解决方案，具有良好的扩展性和可维护性。