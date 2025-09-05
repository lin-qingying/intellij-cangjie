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

package org.cangnova.cangjie.buildsystem.impl.cjpm.project.model.impl

import com.google.common.annotations.VisibleForTesting
import org.cangnova.cangjie.cjpm.CjpmConstants
import org.cangnova.cangjie.cjpm.project.model.CjpmProjectsService
import org.cangnova.cangjie.lang.CjConstants.MAIN_CJ_FILE
import org.cangnova.cangjie.utils.pathAsPath

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import com.intellij.util.PathUtil

/**
 *文件更改监听器，检测`cjpm.toml`文件内部的更改
 *和创建`*.cj`文件作
 */
class CjpmTomlWatcher(
    private val cjpmProjects: CjpmProjectsService,
    private val onCjpmJsonChange: () -> Unit
) : BulkFileListener {
    override fun before(events: List<VFileEvent>) = Unit
    override fun after(events: List<VFileEvent>) {
        if (events.any { isInterestingEvent(it) }) onCjpmJsonChange()
    }

    private fun isInterestingEvent(event: VFileEvent): Boolean {
        if (!isInterestingEvent(cjpmProjects.project, event)) return false


        val file = when (event) {
            is VFileContentChangeEvent -> event.file
            else -> return true
        }
        val fileParentPath = file.pathAsPath.parent

        return cjpmProjects.allProjects.any { it.manifest.parent == fileParentPath }
//     ||   cjpmProjects.findPackageForFile(file)?.origin == PackageOrigin.WORKSPACE

    }

    companion object {

        private val IMPLICIT_TARGET_FILES = listOf(
            "/build.cj", "/src/main.cj", "/src/lib.cj"
        )

        private val IMPLICIT_TARGET_DIRS = listOf(
            "/src/bin", "/examples", "/tests", "/benches"
        )

        @VisibleForTesting
        fun isInterestingEvent(project: Project, event: VFileEvent): Boolean {
            return when {
                event.pathEndsWith(CjpmConstants.MANIFEST_FILE) -> true
//                event.pathEndsWith(CjpmConstants.LOCK_FILE) -> {
//                    val projectDir = Paths.get(event.path).parent
//                    val timestamp = CjpmEventService.getInstance(project).extractTimestamp(projectDir) ?: 0
//                    // Non-null requestor means a change from IDE itself
//                    if (event.requestor != null) return true
//                    val current = System.currentTimeMillis()
//                    val delayThreshold = Registry.intValue("org.cangjie.cjpm.lock.update.delay.threshold")
//                    val delay = current - timestamp
//                    return if (delay > delayThreshold) {
//                        LOG.info("External change in ${event.path}. Previous Cjpm metadata call was $delay ms before")
//                        true
//                    } else {
//                        LOG.info("Skip external change for ${event.path}. Previous Cjpm metadata call was $delay ms before")
//                        false
//                    }
//                }

                event is VFileContentChangeEvent -> false
                !event.pathEndsWith(".cj") -> false
                event is VFilePropertyChangeEvent && event.propertyName != VirtualFile.PROP_NAME -> false
                IMPLICIT_TARGET_FILES.any { event.pathEndsWith(it) } -> true
                else -> {
                    val parent = PathUtil.getParentPath(event.path)
                    val grandParent = PathUtil.getParentPath(parent)
                    IMPLICIT_TARGET_DIRS.any {
                        parent.endsWith(it) || (event.pathEndsWith(MAIN_CJ_FILE) && grandParent.endsWith(
                            it
                        ))
                    }
                }
            }
        }

        private fun VFileEvent.pathEndsWith(suffix: String): Boolean = path.endsWith(suffix) ||
                this is VFilePropertyChangeEvent && oldPath.endsWith(suffix)

        private fun VFileEvent.pathEndsWith(suffix: List<String>): Boolean {
            //list中只要有一个为true就是true
            return suffix.any { pathEndsWith(it) }
        }


        private val LOG = logger<CjpmTomlWatcher>()
    }

}
