package org.cangnova.cangjie.project.listener

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.registerServiceInstance
import kotlinx.coroutines.runBlocking
import org.cangnova.cangjie.project.model.CjModule
import org.cangnova.cangjie.project.model.CjProject
import org.cangnova.cangjie.project.service.CjProjectsService
import org.cangnova.cangjie.project.service.GeneratedFilesHolder
import org.cangnova.cangjie.result.CjProcessResult
import org.cangnova.cangjie.test.AbstractCangJieMultiModuleTest
import kotlin.test.assertEquals

class CjProjectOpenedActivityTest : AbstractCangJieMultiModuleTest() {
    override val runTestInDispatchThread: Boolean = false

    fun testProjectOpenRefreshesAlreadyLoadedCangJieProject() {
        val rootDir = createLocalDirectory(createTempDirectory().toPath().resolve("opened-cangjie-project"))
        val service = TestCjProjectsService(project, TestCjProject(project, rootDir, isValid = true))
        project.registerServiceInstance(CjProjectsService::class.java, service)

        runBlocking {
            CjProjectOpenedActivity().execute(project)
        }

        assertEquals(1, service.refreshProjectCount)
        assertEquals(emptyList<VirtualFile>(), service.discoveredRoots)
    }

    fun testProjectOpenDiscoversProjectWhenServiceHasNoLoadedProject() {
        val service = TestCjProjectsService(project, TestCjProject(project, createLocalDirectory(createTempDirectory().toPath().resolve("invalid-cangjie-project")), isValid = false))
        project.registerServiceInstance(CjProjectsService::class.java, service)

        runBlocking {
            CjProjectOpenedActivity().execute(project)
        }

        assertEquals(0, service.refreshProjectCount)
        assertEquals(listOf(requireNotNull(project.guessProjectDir())), service.discoveredRoots)
    }

    private class TestCjProjectsService(
        override val intellijProject: Project,
        override val cjProject: CjProject,
    ) : CjProjectsService {
        val discoveredRoots = mutableListOf<VirtualFile>()
        var refreshProjectCount: Int = 0

        override fun discoverProject(rootDir: VirtualFile) {
            discoveredRoots += rootDir
        }

        override fun refreshProject() {
            refreshProjectCount++
        }

        override fun addModule(module: CjModule) {}

        override fun removeModule(module: CjModule) {}

        override fun findModuleForFile(file: VirtualFile): CjModule? = null

        override val initialized: Boolean = true

        override fun createProject(
            sdkId: String,
            owner: Disposable,
            directory: VirtualFile,
            projectType: String,
            name: String?,
        ): CjProcessResult<GeneratedFilesHolder> = error("Project creation is not used by project-open bootstrap tests")
    }

    private class TestCjProject(
        override val intellijProject: Project,
        override val rootDir: VirtualFile,
        override val isValid: Boolean,
    ) : CjProject {
        override val name: String = "test-cangjie-project"
        override val isWorkspace: Boolean = false
        override val module: CjModule? = null
        override val workspace = null
        override fun refresh(onComplete: (() -> Unit)?) {}
        override fun findModule(name: String): CjModule? = null
    }
}
