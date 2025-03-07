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

package cn.cangnova.cangjie.ide.cache.trackers

import cn.cangnova.cangjie.ide.cache.project.getChanges
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.platform.backend.workspace.WorkspaceModelChangeListener
import com.intellij.platform.backend.workspace.WorkspaceModelTopics
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.storage.VersionedStorageChange
import com.intellij.util.messages.Topic

val CHANGED: Topic<WorkspaceModelChangeListener> =
    Topic(WorkspaceModelChangeListener::class.java, Topic.BroadcastDirection.NONE, true)

@Service(Service.Level.PROJECT)
class ModuleModificationTracker(project: Project) :
    SimpleModificationTracker(), WorkspaceModelChangeListener, Disposable {

    init {
        project.messageBus.connect(this).subscribe( CHANGED, this)
    }

    override fun changed(event: VersionedStorageChange) {
        event.getChanges<ModuleEntity>().ifEmpty { return }
        incModificationCount()
    }

    override fun dispose() = Unit

    companion object {
        @JvmStatic
        fun getInstance(project: Project): ModuleModificationTracker = project.service()
    }
}
