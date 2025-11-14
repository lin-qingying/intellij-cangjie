package org.cangnova.cangjie.protodebugger.memory

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.DocumentEx
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.progress.runBlockingMaybeCancellable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.pom.Navigatable
import com.intellij.util.EventDispatcher
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.impl.XSourcePositionEx
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.cangnova.cangjie.protodebugger.settings.MemoryDocOptions
import org.jetbrains.annotations.TestOnly
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.cancellation.CancellationException

/**
 * 内存文档类
 *
 * 该类负责管理调试过程中的内存视图，提供内存地址与文档行的映射关系。
 * 它支持异步加载内存数据、处理内存区域分配、以及提供内存浏览和编辑功能。
 */
class MemoryDoc<T>(
    val project: Project,
    parentDisposable: Disposable,
    val dataProvider: MemoryData.DataProvider<T>,
    val docRegionFactory: DocRegionFactory<T>,
    fileType: FileType,
    val name: String,
    val options: MemoryDocOptions = MemoryDocOptions.DEFAULT
) : MemoryLineInfoProvider {
    private val coroutineScope: CoroutineScope = CoroutineScope(
        Dispatchers.EDT + SupervisorJob() + CoroutineName("MemoryDoc")
    )

    val eventDispatcher = EventDispatcher.create(DocRegionListener::class.java) as EventDispatcher<DocRegionListener<T>>
    private val lastUsedRangeForLine: AtomicReference<AddressRange?> = AtomicReference()

    val virtualFile: MemoryViewFile = MemoryViewVirtualFileSystem.getInstance().createMemoryViewFile(
        this, this.name, fileType
    )

    val docAccess: MemoryDocAccess = MemoryDocAccess.READ_ONLY
    private val docAccessHelper: MemoryDocAccessHelper<T> = MemoryDocAccessHelper(this, docAccess)

    private val data: MemoryData<T> = MemoryData(dataProvider)
    private val addressSpace: MutableAddressSpace<DocRegion> = mutableAddressSpace()

    // Helper methods
    private fun AddressRange.requireNotEmpty() {
        if (start > endInclusive) {
            throw IllegalArgumentException("AddressRange cannot be empty")
        }
    }

    private fun <R> Collection<R>.binarySearchBy(key: Any, selector: (R) -> Comparable<*>): Int {
        return -1 // 简化实现
    }

    private fun <R> Collection<R>.getOrNull(index: Int): R? {
        return this.elementAtOrNull(index)
    }

    // 缺失的方法实现
    private fun MutableAddressSpace<DocRegion>.getRegions(range: AddressRange): Collection<DocRegion> {
        return this.getRegions(range.start, range.endInclusive)
    }

    private fun MutableAddressSpace<DocRegion>.getRegions(start: Address, end: Address): Collection<DocRegion> {
        return this.getRegions(start..end)
    }

    private fun MutableAddressSpace<DocRegion>.allocate(region: DocRegion) {
        // 简化实现
    }

    private fun AddressSpace<DocRegion>.getNeighbors(range: AddressRange): Pair<DocRegion?, DocRegion?> {
        val allRegions = this.filterRegions().toList()
        val currentIndex = allRegions.indexOfFirst { it.range == range }
        val prev = if (currentIndex > 0) allRegions[currentIndex - 1] else null
        val next = if (currentIndex < allRegions.size - 1) allRegions[currentIndex + 1] else null
        return Pair(prev, next)
    }

    private fun unallocateAndDeleteDocRegion(region: DocRegion) {
        // 简化实现
        region.deleteFromDocument()
    }

    fun addDocRegionListener(listener: DocRegionListener<T>) {
        eventDispatcher.addListener(listener)
    }

    fun cleanupErrors(range: AddressRange = AddressRange.WHOLE) {
        docAccessHelper.edit {
            val regions = addressSpace.getRegions(range)
            for (element in regions) {
                unallocateAndDeleteDocRegion(element)
            }
        }
    }

    fun findRegionForLine(line: Int): DocRegion? =
        docAccessHelper.read {
            val regions = addressSpace.filterRegions().toList()
            return@read regions.firstOrNull()
        }

    fun getAddress(sourcePosition: XSourcePosition): Address? {
        return if (sourcePosition.file == virtualFile) getAddressForLineNumber(sourcePosition.line) else null
    }

    override fun getAddressForLineNumber(lineNumber: Int): Address? {
        val region = findRegionForLine(lineNumber)
        return region?.getAddressForLineNumberInDocument(lineNumber)
    }

    private fun isAddressLoaded(address: Address): Boolean = docAccessHelper.read {
        return@read addressSpace.getRegion(address) is BaseDocRegion
    }

    suspend fun loadAddress(address: Address): AddressRange? {
        return null // 简化实现
    }

    suspend fun <R : Any> loadAddress(
        address: Address,
        computeInReadAction: (BaseDocRegion) -> R
    ): R? {
        return loadAddressInternal(address, computeInReadAction)?.result
    }

    private suspend fun <R : Any> loadAddressInternal(
        address: Address,
        computeInReadAction: (BaseDocRegion) -> R
    ): RegionInfo<R?>? {
        val region = addressSpace.getRegion(address)
        return if (region is BaseDocRegion) {
            RegionInfo(region, computeInReadAction(region))
        } else {
            null
        }
    }

    fun loadHigherAddresses(line: Int, count: Int) {
        // 简化实现
    }

    fun loadLowerAddresses(line: Int, count: Int) {
        // 简化实现
    }

    suspend fun loadRange(range: AddressRange): AddressRange? {
        // 简化实现
        return null
    }

    fun markOutdated(range: AddressRange = AddressRange.WHOLE): Unit {
        docAccessHelper.edit {
            val regions = addressSpace.getRegions(range)
            regions.forEach { region ->
                region.markOutdated()
            }
        }
    }

    fun refresh(range: AddressRange = AddressRange.WHOLE): Unit {
        docAccessHelper.edit {
            data.invalidateRange(range)
            val regions = addressSpace.getRegions(range)
            regions.forEach { region ->
                // 简化实现
            }
        }
    }

    fun storeData(
        address: Address,
        dataToStore: ByteArray,
        completionCallback: (Throwable?) -> Unit = {}
    ) {
        coroutineScope.launch {
            try {
                dataProvider.storeData(address, dataToStore)
                completionCallback(null)
            } catch (err: MemoryData.DataLoadException) {
                completionCallback(err)
            }
        }
    }

    fun getAddressPosition(address: Address): XSourcePositionEx = AddressPosition(address)

    inner class AddressPosition(val address: Address) :
        OpenFileDescriptor(this@MemoryDoc.project, this@MemoryDoc.virtualFile), XSourcePositionEx {

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
        override fun getLine(): Int = 0
        override fun getOffset(): Int = 0
        override fun navigateIn(e: Editor) {
            requestLoading()
            super.navigateIn(e)
        }

        fun requestLoading() {
            loadRequested.complete()
        }
    }

    interface DocRegionFactory<T> {
        fun createDocRegion(
            project: Project,
            document: DocumentEx,
            textRange: TextRange,
            dataRegion: MemoryData.DataRegion.Completed.Loaded<T>,
            reallocatedSpace: AddressSpace<DocRegion>
        ): DocRegion
    }

    interface DocRegionListener<T> : EventListener {
        fun onDocRegionAllocated(
            unallocatedSpace: AddressSpace<DocRegion>,
            newDocRegion: DocRegion
        ) {}
    }

    // Implementation of MemoryLineInfoProvider interface methods
    override fun getLineNumberForAddress(address: Address): Int? {
        val region = addressSpace.getRegion(address)
        return region?.getLineNumberInDocument(address)
    }

    override fun getOffsetForAddress(address: Address): Int? {
        val region = addressSpace.getRegion(address)
        return region?.getOffsetInDocument(address)
    }

    override fun getLineRange(lineNumber: Int): AddressRange? {
        val region = findRegionForLine(lineNumber)
        return region?.range
    }

    override fun getDocumentRange(address: Address): TextRange? {
        val region = addressSpace.getRegion(address)
        return region?.textRange
    }

    override fun getLineRangeInDocument(address: Address): IntRange? {
        val region = addressSpace.getRegion(address)
        return region?.getLineRangeInDocument()
    }

    private data class RegionInfo<R>(
        val region: DocRegion,
        val result: R
    )
}