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

import com.intellij.java.workspace.entities.JavaModuleSettingsEntity
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.libraries.Library
import com.intellij.platform.backend.workspace.WorkspaceModelChangeListener
import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.SdkEntity
import com.intellij.platform.workspace.storage.EntityChange
import com.intellij.platform.workspace.storage.EntityStorage
import com.intellij.platform.workspace.storage.VersionedStorageChange
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.workspaceModel.ide.impl.legacyBridge.library.findLibraryBridge
import com.intellij.workspaceModel.ide.impl.legacyBridge.module.findModule
import com.intellij.workspaceModel.ide.impl.legacyBridge.sdk.SdkBridgeImpl.Companion.sdkMap

abstract class WorkspaceEntityChangeListener<Entity : WorkspaceEntity, Value : Any>(
    protected val project: Project,
    private val afterChangeApplied: Boolean = true
) : WorkspaceModelChangeListener {
    protected abstract val entityClass: Class<Entity>

    protected abstract fun map(storage: EntityStorage, entity: Entity): Value?

    protected abstract fun entitiesChanged(outdated: List<Value>)

    final override fun beforeChanged(event: VersionedStorageChange) {
        if (!afterChangeApplied) {
            handleEvent(event)
        }
    }

    final override fun changed(event: VersionedStorageChange) {
        if (afterChangeApplied) {
            handleEvent(event)
        }
    }

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

abstract class ModuleEntityChangeListener(project: Project, afterChangeApplied: Boolean = true) :
    WorkspaceEntityChangeListener<ModuleEntity, Module>(project, afterChangeApplied) {
    override val entityClass: Class<ModuleEntity>
        get() = ModuleEntity::class.java

    override fun map(storage: EntityStorage, entity: ModuleEntity): Module? =
        entity.findModule(storage)

    override fun handleEvent(event: VersionedStorageChange) {
        val storageBefore = event.storageBefore
        val moduleChanges = event.getChanges<ModuleEntity>()
        val moduleSettingChanges = event.getChanges<JavaModuleSettingsEntity>()

        val outdatedEntities = (moduleChanges.asSequence()
            .mapNotNull(EntityChange<ModuleEntity>::oldEntity) +
                moduleSettingChanges.asSequence()
                    .mapNotNull(EntityChange<JavaModuleSettingsEntity>::oldEntity)
                    .mapNotNull { it.module })
            .toSet()

        val outdatedModules: List<Module> = outdatedEntities
            .mapNotNull { it.findModule(storageBefore) }
            .toList()

        if (outdatedModules.isNotEmpty()) {
            entitiesChanged(outdatedModules)
        }
    }
}

abstract class LibraryEntityChangeListener(project: Project, afterChangeApplied: Boolean = true) :
    WorkspaceEntityChangeListener<LibraryEntity, Library>(project, afterChangeApplied) {
    override val entityClass: Class<LibraryEntity>
        get() = LibraryEntity::class.java

    override fun map(storage: EntityStorage, entity: LibraryEntity): Library? =
        entity.findLibraryBridge(storage)
}



inline fun <reified T : WorkspaceEntity> VersionedStorageChange.getChanges(): List<EntityChange<T>> =
    getChanges(T::class.java)