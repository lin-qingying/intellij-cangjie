package com.linqingying.cangjie.cjpm.project.model.impl

import com.google.common.annotations.VisibleForTesting
import com.linqingying.cangjie.cjpm.CjpmConstants
import com.linqingying.cangjie.cjpm.project.model.CjpmProjectsService
import com.linqingying.cangjie.cjpm.project.pathAsPath
import com.linqingying.cangjie.lang.CjConstants.MAIN_CJ_FILE
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import com.intellij.util.PathUtil
import java.nio.file.Paths

/**
 *文件更改监听器，检测`module.json`文件内部的更改
 *和创建`*.cj`文件作
 */
class CjpmJsonWatcher(
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
                event.pathEndsWith(CjpmConstants.LOCK_FILE) -> {
                    val projectDir = Paths.get(event.path).parent
                    val timestamp = CjpmEventService.getInstance(project).extractTimestamp(projectDir) ?: 0
                    // Non-null requestor means a change from IDE itself
                    if (event.requestor != null) return true
                    val current = System.currentTimeMillis()
                    val delayThreshold = Registry.intValue("org.cangjie.cjpm.lock.update.delay.threshold")
                    val delay = current - timestamp
                    return if (delay > delayThreshold) {
                        LOG.info("External change in ${event.path}. Previous Cjpm metadata call was $delay ms before")
                        true
                    } else {
                        LOG.info("Skip external change for ${event.path}. Previous Cjpm metadata call was $delay ms before")
                        false
                    }
                }

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


        private val LOG = logger<CjpmJsonWatcher>()
    }

}
