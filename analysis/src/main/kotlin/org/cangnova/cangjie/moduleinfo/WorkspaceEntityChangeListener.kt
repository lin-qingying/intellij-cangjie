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

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.platform.backend.workspace.WorkspaceModelChangeListener
import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.storage.EntityChange
import com.intellij.platform.workspace.storage.EntityStorage
import com.intellij.platform.workspace.storage.VersionedStorageChange
import com.intellij.platform.workspace.storage.WorkspaceEntity

/**
 * 工作空间实体变更监听器抽象类
 *
 * 该类为工作空间模型变更提供统一的监听和处理机制。
 * 它监听特定类型实体的变更事件，并将实体映射为业务对象。
 *
 * @param Entity 工作空间实体类型
 * @param Value 映射后的业务对象类型
 * @param project 当前项目实例
 * @param afterChangeApplied 是否在变更应用后处理（true）或之前处理（false）
 */
abstract class WorkspaceEntityChangeListener<Entity : WorkspaceEntity, Value : Any>(
    protected val project: Project,
    private val afterChangeApplied: Boolean = true
) : WorkspaceModelChangeListener {
    /**
     * 要监听的实体类
     */
    protected abstract val entityClass: Class<Entity>

    /**
     * 将工作空间实体映射为业务对象
     *
     * @param storage 实体存储
     * @param entity 要映射的实体
     * @return 映射后的业务对象，如果映射失败则返回 null
     */
    protected abstract fun map(storage: EntityStorage, entity: Entity): Value?

    /**
     * 处理实体变更事件
     *
     * @param outdated 已过时的业务对象列表
     */
    protected abstract fun entitiesChanged(outdated: List<Value>)

    /**
     * 变更前回调
     */
    final override fun beforeChanged(event: VersionedStorageChange) {
        if (!afterChangeApplied) {
            handleEvent(event)
        }
    }

    /**
     * 变更后回调
     */
    final override fun changed(event: VersionedStorageChange) {
        if (afterChangeApplied) {
            handleEvent(event)
        }
    }

    /**
     * 处理变更事件
     *
     * @param event 存储变更事件
     */
    protected open fun handleEvent(event: VersionedStorageChange) {
        val storageBefore = event.storageBefore
        val changes = event.getChanges(entityClass).ifEmpty { return }

        val outdatedEntities: List<Value> = changes.asSequence()
            .mapNotNull(EntityChange<Entity>::oldEntity)
            .mapNotNull { map(storageBefore, it) }
            .toList()

        if (outdatedEntities.isNotEmpty()) {
            entitiesChanged(outdatedEntities)
        }
    }
}

/**
 * 模块实体变更监听器
 *
 * 监听模块实体的变更事件，并将 [ModuleEntity] 映射为 [Module]。
 *
 * @param project 当前项目实例
 * @param afterChangeApplied 是否在变更应用后处理
 */
abstract class ModuleEntityChangeListener(project: Project, afterChangeApplied: Boolean = true) :
    WorkspaceEntityChangeListener<ModuleEntity, Module>(project, afterChangeApplied) {

    override val entityClass: Class<ModuleEntity>
        get() = ModuleEntity::class.java

    /**
     * 将模块实体映射为模块对象
     *
     * 通过模块名在 ModuleManager 中查找对应的模块。
     * 使用稳定的公共 API，避免使用标记为 Internal 的 API。
     *
     * @param storage 实体存储（本实现中未使用，保留以符合接口）
     * @param entity 模块实体
     * @return 对应的模块对象，如果未找到则返回 null
     */
    override fun map(storage: EntityStorage, entity: ModuleEntity): Module? {
        val moduleName = entity.name
        return ModuleManager.getInstance(project).findModuleByName(moduleName)
    }
}

/**
 * 库实体变更监听器
 *
 * 监听库实体的变更事件，并将 [LibraryEntity] 映射为 [Library]。
 *
 * @param project 当前项目实例
 * @param afterChangeApplied 是否在变更应用后处理
 */
abstract class LibraryEntityChangeListener(project: Project, afterChangeApplied: Boolean = true) :
    WorkspaceEntityChangeListener<LibraryEntity, Library>(project, afterChangeApplied) {

    override val entityClass: Class<LibraryEntity>
        get() = LibraryEntity::class.java

    /**
     * 将库实体映射为库对象
     *
     * 通过库名在项目级和应用级库表中查找对应的库。
     * 使用稳定的公共 API，避免使用标记为 Internal 的 API。
     *
     * @param storage 实体存储（本实现中未使用，保留以符合接口）
     * @param entity 库实体
     * @return 对应的库对象，如果未找到则返回 null
     */
    override fun map(storage: EntityStorage, entity: LibraryEntity): Library? {
        val libraryName = entity.name

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
 * 获取指定类型的实体变更列表
 *
 * 泛型扩展函数，用于简化获取变更列表的代码。
 *
 * @param T 实体类型
 * @return 实体变更列表
 */
inline fun <reified T : WorkspaceEntity> VersionedStorageChange.getChanges(): List<EntityChange<T>> =
    getChanges(T::class.java)