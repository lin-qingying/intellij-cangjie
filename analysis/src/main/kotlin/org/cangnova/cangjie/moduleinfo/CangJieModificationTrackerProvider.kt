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

import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import org.cangnova.cangjie.cache.trackers.CangJieCodeBlockModificationListener
import org.cangnova.cangjie.cache.trackers.CangJieModuleOutOfCodeBlockModificationTracker

interface CangJieModificationTrackerProvider {
    companion object {
        fun getInstance(project: Project): CangJieModificationTrackerProvider = project.service()
    }

    val projectTracker: ModificationTracker

    fun getModuleSelfModificationCount(module: Module): Long

    fun createModuleModificationTracker(module: Module): ModificationTracker
}

class CangJieModificationTrackerProviderImpl(private val project: Project) : CangJieModificationTrackerProvider {
    override val projectTracker: ModificationTracker
        get() = CangJieCodeBlockModificationListener.getInstance(project).cangjieOutOfCodeBlockTracker

    override fun getModuleSelfModificationCount(module: Module): Long {
        return CangJieModuleOutOfCodeBlockModificationTracker.getUpdaterInstance(module.project).getModificationCount(module)
    }

    override fun createModuleModificationTracker(module: Module): ModificationTracker {
        return CangJieModuleOutOfCodeBlockModificationTracker(module)
    }
}