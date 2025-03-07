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

import cn.cangnova.cangjie.analyzer.CangJieModificationTrackerService
import cn.cangnova.cangjie.psi.CjFile
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.psi.impl.PsiModificationTrackerImpl
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.annotations.TestOnly

val PER_FILE_MODIFICATION_TRACKER = Key<SimpleModificationTracker>("FILE_OUT_OF_BLOCK_MODIFICATION_COUNT")

val CjFile.perFileModificationTracker: ModificationTracker
    get() = putUserDataIfAbsent(PER_FILE_MODIFICATION_TRACKER, SimpleModificationTracker())

class CangJieIDEModificationTrackerService(project: Project) : CangJieModificationTrackerService() {
    override val modificationTracker: ModificationTracker = PsiModificationTracker.getInstance(project)

    override val outOfBlockModificationTracker: ModificationTracker =
        CangJieCodeBlockModificationListener.getInstance(project).cangjieOutOfCodeBlockTracker

    override fun fileModificationTracker(file: CjFile): ModificationTracker =
        file.perFileModificationTracker

    companion object {
        @TestOnly
        fun invalidateCaches(project: Project) {
            project.getService(CangJieModificationTrackerService::class.java).apply {
                (outOfBlockModificationTracker as SimpleModificationTracker).incModificationCount()

                @Suppress("DEPRECATION")
                (modificationTracker as PsiModificationTrackerImpl).incCounter()
            }
        }
    }
}
