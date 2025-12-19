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

package org.cangnova.cangjie.moduleinfo

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.platform.workspace.jps.entities.LibraryDependency
import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.jps.entities.LibraryTableId
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.storage.EntityChange
import com.intellij.platform.workspace.storage.VersionedStorageChange
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.serviceContainer.AlreadyDisposedException
import com.intellij.util.PathUtil
import com.intellij.util.concurrency.ThreadingAssertions
import org.cangnova.cangjie.utils.addIfNotNull
import org.cangnova.cangjie.utils.exceptions.CangJieExceptionWithAttachmentsImpl
import org.cangnova.cangjie.utils.flattenTo

/**
 * 库信息缓存服务
 *
 * 该服务用于缓存和管理项目中所有库的 [LibraryInfo] 对象。
 * 它负责：
 * 1. 缓存库信息以避免重复计算
 * 2. 去重相同内容的库（基于根目录）
 * 3. 监听工作空间模型变更并更新缓存
 * 4. 发布库信息的添加和移除事件
 *
 * ## 缓存策略
 *
 * - **去重机制**: 具有相同根目录和内容的库会被去重，共享同一个 LibraryInfo 实例
 * - **自动失效**: 当库被删除或修改时，自动从缓存中移除
 * - **线程安全**: 使用读写锁保护缓存访问
 *
 * @param project 当前项目实例
 * @see LibraryInfo
 * @see LibraryInfoImpl
 */
@Service(Service.Level.PROJECT)

class LibraryInfoCache(project: Project) : Disposable {

    private val libraryInfoCache = LibraryInfoInnerCache(project)

    init {
        Disposer.register(this, libraryInfoCache)
    }

    /**
     * 库信息内部缓存实现
     *
     * 继承自 [SynchronizedFineGrainedEntityCache]，提供线程安全的细粒度缓存。
     *
     * ## 核心功能
     *
     * 1. **去重缓存**: 使用 [deduplicationCache] 避免相同内容的库重复缓存
     * 2. **工作空间监听**: 监听工作空间模型变更，自动更新缓存
     * 3. **有效性检查**: 确保缓存中的库对象未被释放
     * 4. **一致性保证**: 通过多层检查保证缓存状态一致
     */
    private class LibraryInfoInnerCache(project: Project) :
        SynchronizedFineGrainedEntityCache<LibraryEx, List<LibraryInfo>>(project) {

        /**
         * 移除库信息的修改追踪器
         *
         * 当库信息被从缓存中移除时，递增修改计数。
         * 用于通知依赖方缓存已失效。
         */
        val removedLibraryInfoTracker = SimpleModificationTracker()

        /**
         * 去重缓存
         *
         * key: 库的第一个根目录路径
         * value: 具有相同根目录的所有库实例列表
         *
         * 用于去重具有相同内容但不同实例的库。
         */
        private val deduplicationCache = hashMapOf<String, MutableList<LibraryEx>>()

        init {
            initialize()
        }

        // Due to historical issues with other workspace model listeners that accessed `LibraryInfoCache` in the FIR implementation,
        // workspace model changes for `LibraryInfoCache` are listened to via `Fe10WorkspaceModelChangeListener`. The FIR implementation
        // doesn't use `LibraryInfoCache` anymore.
        // Note that, even if the order was insignificant, `LibraryInfoCache`'s workspace model listener would still need to be registered
        // eagerly. The reason is that some other workspace model listener might access `LibraryInfoCache` and cause first-time
        // initialization (including a call to `subscribe`). Subscribing to workspace model events while a workspace model event is being
        // processed means that that event won't be propagated to the new subscription. Hence, for exactly that event, `LibraryInfoCache`
        // would not be cleared, as its workspace model listener wouldn't be called. This can lead to cache inconsistency if a cache entry
        // containing a changing library was added during that event by some other workspace listener.
        override fun subscribe() {}

        /**
         * 缓存失效时的处理
         *
         * 清空主缓存和去重缓存。
         *
         * @param cache 主缓存映射
         */
        override fun doInvalidate(cache: MutableMap<LibraryEx, List<LibraryInfo>>) {
            super.doInvalidate(cache)
            deduplicationCache.clear()
        }

        /**
         * 获取库信息列表
         *
         * 此方法实现了完整的缓存逻辑：
         * 1. 检查线程访问权限（应在读访问中调用）
         * 2. 检查键的有效性
         * 3. 尝试从缓存或去重缓存中获取
         * 4. 如果缓存未命中，计算新值
         * 5. 检查新值的有效性
         * 6. 将新值添加到缓存
         * 7. 执行后处理（发布事件）
         *
         * ## 去重机制
         *
         * 项目模型可能提供内容相同但名称不同的库实例，例如：
         * - 模块级库的名称为 `null`
         * - 项目级库的名称为 `cangjie-stdlib-1.0.0`
         *
         * 为了检查 LibraryInfo 的相等性（例如消除重复项），我们必须检查底层库的根目录。
         *
         * 为了实现更快的相等性检查，我们需要去重库：[deduplicationCache] 用于解决此问题。
         * 它使用库的第一个根目录作为键，值是在某些 LibraryInfo 中使用的唯一（内容方面）库。
         *
         * 如果我们有两个相同内容的不同库实例，我们总是返回相同的 LibraryInfo 实例。
         * 这甚至允许基于对象标识执行相等性检查。
         *
         * @param key 库对象
         * @return 库信息列表
         */
        override fun get(key: LibraryEx): List<LibraryInfo> {
            ThreadingAssertions.softAssertReadAccess()
            checkKeyAndDisposeIllegalEntry(key)

            getCachedOrPutNewValue(key, newValue = null)?.let { return it }

            ProgressManager.checkCanceled()

            val newValue = calculate(key)

            if (isValidityChecksEnabled) {
                checkValueValidity(newValue)
            }

            getCachedOrPutNewValue(key, newValue)?.let { return it }

            postProcessNewValue(key, newValue)

            return newValue
        }

        /**
         * 从缓存中获取值或添加新值
         *
         * @param key 库对象
         * @param newValue 新计算的值（如果有）
         * @return 缓存的值，如果未找到则返回 null
         */
        private fun getCachedOrPutNewValue(key: LibraryEx, newValue: List<LibraryInfo>?): List<LibraryInfo>? = useCache { cache ->
            checkEntitiesIfRequired(cache)

            cache[key]?.let { return@useCache it }

            val root = key.firstRoot()
            val deduplicatedValue = cachedDeduplicatedValue(cache, key, root)
            val resultValue = deduplicatedValue ?: newValue ?: return@useCache null
            addEntryToCache(cache, key, root, resultValue)

            deduplicatedValue
        }

        /**
         * 添加条目到缓存
         *
         * 同时更新主缓存和去重缓存。
         *
         * @param cache 主缓存映射
         * @param key 库对象
         * @param root 库的第一个根目录路径
         * @param value 库信息列表
         */
        private fun addEntryToCache(
            cache: MutableMap<LibraryEx, List<LibraryInfo>>,
            key: LibraryEx,
            root: String,
            value: List<LibraryInfo>,
        ) {
            cache[key] = value
            deduplicationCache.getOrPut<String, MutableList<LibraryEx>>(root) { mutableListOf<LibraryEx>() } += key
        }

        /**
         * 从去重缓存中获取已缓存的值
         *
         * 通过根目录路径和内容相等性检查，查找已缓存的库信息。
         *
         * @param cache 主缓存映射
         * @param key 库对象
         * @param root 库的第一个根目录路径
         * @return 已缓存的库信息列表，如果未找到则返回 null
         */
        private fun cachedDeduplicatedValue(
            cache: MutableMap<LibraryEx, List<LibraryInfo>>,
            key: LibraryEx,
            root: String,
        ): List<LibraryInfo>? {
            val deduplicatedLibraries = deduplicationCache[root]
            if (deduplicatedLibraries.isNullOrEmpty()) return null

            val keyUrlsByType = key.urlsByType()
            val deduplicatedLibrary = deduplicatedLibraries.find { keyUrlsByType.rootEquals(it) } ?: return null
            val cachedValue = cache[deduplicatedLibrary]
            if (cachedValue == null) {
                val exception = CangJieExceptionWithAttachmentsImpl(
                    """
                        inconsistent state:
                        is the same key: ${deduplicatedLibrary === key}
                        root consistent: ${key.firstRoot() == root}
                        urls consistent: ${key.urlsByType() == keyUrlsByType}
                        key name: ${key.presentableName}
                        deduplicated key name: ${deduplicatedLibrary.presentableName}
                    """.trimIndent()
                )
                    .withAttachment("key.txt", key.toString())
                    .withAttachment("deduplicated.txt", deduplicatedLibrary.toString())
                    .withAttachment("librariesBefore.txt", deduplicatedLibraries.joinToString(separator = "\n"))

                deduplicatedLibraries -= deduplicatedLibrary
                exception.withAttachment("librariesAfter.txt", deduplicatedLibraries.joinToString(separator = "\n"))
                logger.error(exception)

                return cachedDeduplicatedValue(cache, key, root)
            }

            return cachedValue
        }

        /**
         * 获取库的第一个根目录
         *
         * @return 第一个类文件根目录的 URL，如果没有则返回空字符串
         */
        private fun LibraryEx.firstRoot() = getUrls(OrderRootType.CLASSES).firstOrNull() ?: ""

        /**
         * 检查库键的有效性
         *
         * @param key 要检查的库对象
         * @throws AlreadyDisposedException 如果库已被释放
         */
        override fun checkKeyValidity(key: LibraryEx) {
            key.checkValidity()
        }

        /**
         * 检查库键的一致性
         *
         * 除了父类的一致性检查外，还检查缓存一致性。
         *
         * @param cache 缓存映射
         * @param key 库对象
         */
        override fun checkKeyConsistency(cache: MutableMap<LibraryEx, List<LibraryInfo>>, key: LibraryEx) {
            super.checkKeyConsistency(cache, key)
            checkCacheConsistency(cache, key)
        }

        /**
         * 检查缓存一致性
         *
         * 确保主缓存和去重缓存的状态一致。
         *
         * @param cache 主缓存映射
         * @param key 库对象
         * @throws IllegalStateException 如果缓存状态不一致
         */
        private fun checkCacheConsistency(cache: MutableMap<LibraryEx, List<LibraryInfo>>, key: LibraryEx) {
            val isCached = key in cache
            val isDeduplicated = deduplicationCache[key.firstRoot()]?.contains(key) == true
            if (isCached != isDeduplicated) {
                error("inconsistent state ${key.presentableName}: is cached: $isCached, is deduplicated: $isDeduplicated")
            }
        }

        /**
         * 额外的实体检查
         *
         * 检查去重缓存中的所有实体，移除不一致的条目。
         *
         * @param cache 主缓存映射
         */
        override fun additionalEntitiesCheck(cache: MutableMap<LibraryEx, List<LibraryInfo>>) {
            for (values in deduplicationCache.values) {
                val iterator = values.iterator()
                while (iterator.hasNext()) {
                    val library = iterator.next()
                    try {
                        checkCacheConsistency(cache, library)
                    } catch (e: Throwable) {
                        iterator.remove()
                        cache.remove(library)
                        logger.error(e)
                    }
                }
            }
        }

        /**
         * 释放非法条目
         *
         * 从主缓存和去重缓存中移除已释放的库。
         *
         * @param cache 主缓存映射
         * @param key 已释放的库对象
         */
        override fun disposeIllegalEntry(cache: MutableMap<LibraryEx, List<LibraryInfo>>, key: LibraryEx) {
            super.disposeIllegalEntry(cache, key)
            dropDisposedKey(key)
        }

        /**
         * 释放缓存条目
         *
         * 移除缓存条目及其所有关联的去重库。
         *
         * @param cache 主缓存映射
         * @param entry 要释放的缓存条目
         */
        override fun disposeEntry(
            cache: MutableMap<LibraryEx, List<LibraryInfo>>,
            entry: MutableMap.MutableEntry<LibraryEx, List<LibraryInfo>>,
        ) {
            dropDisposedKey(entry.key)

            val libInfoKey = entry.value.first().library
            if (libInfoKey == entry.key) return

            val iterator = cache.iterator()
            while (iterator.hasNext()) {
                val cacheEntry = iterator.next()
                if (cacheEntry.value.first().library == libInfoKey) {
                    iterator.remove()
                    dropDisposedKey(cacheEntry.key)
                }
            }
        }

        /**
         * 从去重缓存中删除已释放的键
         *
         * @param key 要删除的库对象
         */
        private fun dropDisposedKey(key: LibraryEx) {
            for (values in deduplicationCache.values) {
                if (values.remove(key)) break
            }
        }

        /**
         * 检查值的有效性
         *
         * 检查库信息列表中的所有库是否有效。
         *
         * @param value 库信息列表
         * @throws AlreadyDisposedException 如果任何库已被释放
         */
        override fun checkValueValidity(value: List<LibraryInfo>) {
            value.forEach(LibraryInfo::checkValidity)
        }

        /**
         * 计算库信息
         *
         * 为给定的库创建 LibraryInfo 实例。
         * 对于仓颉语言，所有库都使用 LibraryInfoImpl。
         *
         * @param key 库对象
         * @return 包含单个 LibraryInfoImpl 的列表
         */
        override fun calculate(key: LibraryEx): List<LibraryInfo> = listOf(LibraryInfoImpl(project, key))

        /**
         * 后处理新值
         *
         * 在新的库信息添加到缓存后，发布库信息添加事件。
         *
         * @param key 库对象
         * @param value 新添加的库信息列表
         */
        override fun postProcessNewValue(key: LibraryEx, value: List<LibraryInfo>) {
            project.messageBus.syncPublisher(LibraryInfoListener.TOPIC).libraryInfosAdded(value)
        }

        /**
         * 失效键并获取过时的值
         *
         * 从缓存中移除指定的键，并返回所有过时的库信息。
         * 同时处理去重缓存中的相关条目。
         *
         * @param keys 要失效的库对象集合
         * @param cache 主缓存映射
         * @return 所有过时的库信息列表
         */
        override fun doInvalidateKeysAndGetOutdatedValues(
            keys: Collection<LibraryEx>,
            cache: MutableMap<LibraryEx, List<LibraryInfo>>,
        ): Collection<List<LibraryInfo>> {
            val outdatedValues = mutableListOf<List<LibraryInfo>>()
            val groupBy = keys.groupBy { it.firstRoot() }
            for ((root, invalidatedLibraries) in groupBy) {
                val deduplicatedLibraries = deduplicationCache[root] ?: continue
                if (deduplicatedLibraries.isEmpty()) continue
                deduplicatedLibraries.removeAll(invalidatedLibraries)

                for (invalidatedLibrary in invalidatedLibraries) {
                    val anchorInfo = cache.remove(invalidatedLibrary)?.takeIf { it.first().library == invalidatedLibrary } ?: continue
                    outdatedValues += anchorInfo

                    if (deduplicatedLibraries.isEmpty()) continue
                    val invalidatedLibraryUrlsByType = invalidatedLibrary.urlsByType()
                    val deduplicatedLibrariesIterator = deduplicatedLibraries.iterator()
                    while (deduplicatedLibrariesIterator.hasNext()) {
                        val deduplicatedLibrary = deduplicatedLibrariesIterator.next()
                        if (invalidatedLibraryUrlsByType.rootEquals(deduplicatedLibrary)) {
                            deduplicatedLibrariesIterator.remove()
                            cache.remove(deduplicatedLibrary)
                        }
                    }
                }
            }

            return outdatedValues
        }

        /**
         * 在工作空间模型变更前处理缓存更新
         *
         * 监听工作空间模型的变更事件，当库或模块发生变化时：
         * 1. 找出所有过时的库
         * 2. 从缓存中移除这些库的信息
         * 3. 发布库信息移除事件
         *
         * @param event 工作空间存储变更事件
         */
        fun beforeWorkspaceModelChanged(event: VersionedStorageChange) {
            val libraryChanges = event.getChanges<LibraryEntity>()
            val moduleChanges = event.getChanges<ModuleEntity>()

            if (libraryChanges.none() && moduleChanges.none()) return

            val outdatedLibraries: MutableList<Library> = mutableListOf()

            // 处理库实体的变更
            for (change in libraryChanges) {
                val oldEntity = change.oldEntity?.takeIf { it.tableId !is LibraryTableId.GlobalLibraryTableId } ?: continue
                // 通过库名查找对应的 Library 对象
                val library = findLibraryByName(oldEntity.name)
                if (library != null) {
                    outdatedLibraries.add(library)
                }
            }

            // 处理模块实体的依赖变更
            val oldLibDependencies = moduleChanges.mapNotNull {
                it.oldEntity?.dependencies?.filterIsInstance<LibraryDependency>()
            }.flatten().associateBy { it.library }

            val newLibDependencies = moduleChanges.mapNotNullTo(LinkedHashSet()) {
                it.newEntity?.dependencies?.filterIsInstance<LibraryDependency>()
            }.flatten().associateBy { it.library }

            for (entry in oldLibDependencies.entries) {
                val value = entry.value.takeIf { it.library.tableId !is LibraryTableId.GlobalLibraryTableId } ?: continue
                if (value != newLibDependencies[entry.key]) {
                    val libraryEntity = value.library
                    val library = findLibraryByName(libraryEntity.name)
                    outdatedLibraries.addIfNotNull(library)
                }
            }

            if (outdatedLibraries.isNotEmpty()) {
                val droppedLibraryInfos =
                    invalidateKeysAndGetOutdatedValues(outdatedLibraries.map { it as LibraryEx }).flattenTo(hashSetOf())

                if (droppedLibraryInfos.isNotEmpty()) {
                    removedLibraryInfoTracker.incModificationCount()
                    project.messageBus.syncPublisher(LibraryInfoListener.TOPIC).libraryInfosRemoved(droppedLibraryInfos)
                }
            }
        }

        /**
         * 通过库名查找库对象
         *
         * 在项目级库表和应用级库表中查找指定名称的库。
         *
         * @param libraryName 库的名称
         * @return 找到的库对象，如果未找到则返回 null
         */
        private fun findLibraryByName(libraryName: String): Library? {
            // 先在项目级库表中查找
            val projectLibraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable(project)
            var library = projectLibraryTable.getLibraryByName(libraryName)

            if (library != null) return library

            // 如果项目级找不到，在应用级库表中查找
            val applicationLibraryTable = LibraryTablesRegistrar.getInstance().libraryTable
            return applicationLibraryTable.getLibraryByName(libraryName)
        }
    }

    /**
     * 获取库的信息列表
     *
     * 这是主要的公共访问点，用于获取库的 LibraryInfo。
     *
     * @param key 库对象
     * @return 库的 LibraryInfo 列表
     * @throws IllegalArgumentException 如果库不是 LibraryEx 实例
     */
    operator fun get(key: Library): List<LibraryInfo> {
        require(key is LibraryEx) { "Library '${key.presentableName}' does not implement LibraryEx which is not expected" }
        return libraryInfoCache[key]
    }

    /**
     * 获取去重后的库对象
     *
     * 返回缓存中的规范库实例。当多个库对象具有相同内容时，
     * 此方法返回用于所有这些库的单一规范实例。
     *
     * @param key 库对象
     * @return 去重后的库对象
     */
    fun deduplicatedLibrary(key: Library): Library = get(key).first().library

    /**
     * 在工作空间模型变更前的回调
     *
     * 此方法应在工作空间模型变更事件中调用，
     * 以便及时更新缓存并发布相关事件。
     *
     * @param event 工作空间存储变更事件
     */
    fun beforeWorkspaceModelChanged(event: VersionedStorageChange) {
        libraryInfoCache.beforeWorkspaceModelChanged(event)
    }

    /**
     * 释放资源
     *
     * 此服务不需要特殊的清理操作。
     */
    override fun dispose() = Unit

    /**
     * 获取移除库信息的修改追踪器
     *
     * 可用于监听库信息的移除事件。
     *
     * @return 修改追踪器实例
     */
    fun removedLibraryInfoTracker(): ModificationTracker = libraryInfoCache.removedLibraryInfoTracker

    /**
     * 获取所有缓存的库信息
     *
     * 返回当前缓存中的所有库信息列表。
     *
     * @return 所有缓存的库信息集合
     */
    fun values(): Collection<List<LibraryInfo>> = libraryInfoCache.values()

    companion object {
        /**
         * 获取项目的 LibraryInfoCache 实例
         *
         * @param project 项目对象
         * @return LibraryInfoCache 服务实例
         */
        fun getInstance(project: Project): LibraryInfoCache = project.service()
    }
}

/**
 * 检查库的有效性
 *
 * 扩展函数，用于检查库对象是否已被释放。
 *
 * @receiver 要检查的库对象
 * @throws AlreadyDisposedException 如果库已被释放
 */
fun Library.checkValidity() {
    if (this is LibraryEx && isDisposed) {
        throw AlreadyDisposedException("Library '${name}' is already disposed")
    }
}

/**
 * 获取库的所有根目录 URL（按类型分组）
 *
 * @receiver 库对象
 * @return 映射表，键为根目录类型，值为该类型的所有 URL 数组
 */
private fun LibraryEx.urlsByType(): Map<OrderRootType, Array<String>> = buildMap {
    for (orderRootType in OrderRootType.getAllTypes()) {
        put(orderRootType, getUrls(orderRootType))
    }
}

/**
 * 检查根目录是否与另一个库相等
 *
 * 通过比较所有类型的根目录 URL 来判断两个库是否具有相同的内容。
 *
 * @receiver 根目录 URL 映射表
 * @param another 要比较的另一个库对象
 * @return 如果所有类型的根目录都相等则返回 true
 */
private fun Map<OrderRootType, Array<String>>.rootEquals(another: LibraryEx): Boolean = all { (k, v) ->
    v.contentEquals(another.getUrls(k))
}


