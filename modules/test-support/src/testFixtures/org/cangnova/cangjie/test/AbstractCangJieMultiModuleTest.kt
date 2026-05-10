/*
 * Copyright 2026 LinQingYing. and contributors.
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
 */

package org.cangnova.cangjie.test

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.SourceFolder
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.HeavyPlatformTestCase
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.RunAll
import org.junit.runner.RunWith
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.asserter

/**
 * 对位 Kotlin `AbstractMultiModuleTest` 的仓颉多模块项目级测试基类。
 *
 * 该层只承载需要真实 IntelliJ Module / Workspace Model / Library 变更的测试能力：
 * 1. 统一仓颉 JUnit runner、测试数据路径和 `.cj` 文件名约定；
 * 2. 提供真实模块、source root、模块依赖和 project library 装配工具；
 * 3. 统一仓库根 VFS 访问授权与释放；
 * 4. 阻止业务测试直接继承 IntelliJ 原生 `HeavyPlatformTestCase`。
 *
 * Kotlin 的 `ExpectedPluginModeProvider` 属于 Kotlin K1/K2 插件选择语义，仓颉没有对应模式，
 * 因此这里不引入伪造的 plugin mode 层。
 */
@RunWith(CangJieJUnit4TestRunner::class)
abstract class AbstractCangJieMultiModuleTest : HeavyPlatformTestCase(), CangJieTestCase {
    private var vfsDisposable = Ref<Disposable>()

    open val testDataDirectory: File by lazy { File(testDataPath) }

    /**
     * 是否按 IntelliJ heavy project 默认语义在 EDT 执行测试体。
     *
     * 大多数 IDE / project-structure 测试应保持默认值；需要调用 suspend workspace-model 同步的测试
     * 可以在仓颉框架层覆盖该开关，避免在 EDT 上 `runBlocking` 造成写模型更新互等。
     */
    protected open val runTestInDispatchThread: Boolean = true

    val testDataPath: String
        get() = TestMetadataUtil.getTestDataPath(javaClass)

    override fun setUp() {
        super.setUp()
        vfsDisposable = CangJieTestUtils.allowProjectRootAccess(this)
    }

    override fun tearDown() {
        RunAll.runAll(
            { CangJieTestUtils.disposeVfsRootAccess(vfsDisposable) },
            { super.tearDown() },
        )
    }

    protected open fun fileName(): String =
        CangJieTestUtils.getTestDataFileName(javaClass, name) ?: "$testName.$testFileExtension"

    protected val fileNameWithExtension: String
        get() = fileName()

    protected val testName: String
        get() = getTestName(true)

    override fun getTestName(lowercaseFirstLetter: Boolean): String {
        val camelCase = super.getTestName(lowercaseFirstLetter)
        return TestCase.camelOrWordsToSnake(camelCase)
    }

    override fun runInDispatchThread(): Boolean = runTestInDispatchThread

    protected fun dataFile(fileName: String): File = File(testDataDirectory, fileName)

    protected fun dataFilePath(fileName: String = fileName()): String = dataFile(fileName).path

    /**
     * 在 project-structure / workspace file index 读路径中执行断言或查询。
     *
     * 多模块测试允许测试体不运行在 EDT；此时 IntelliJ project model 读操作必须显式进入 read action。
     * 业务测试应通过该入口表达 project-structure 读语义，避免直接依赖 IntelliJ 原生线程细节。
     */
    protected fun <T> runProjectStructureReadAction(action: () -> T): T = runReadAction(action)

    protected fun createModuleInTmpDir(
        name: String,
        createFiles: () -> List<FileWithText> = { emptyList() },
    ): Module {
        val rootPath = createTempDirectory().toPath().resolve(name)
        val sourcePath = rootPath.resolve("src")
        sourcePath.createDirectories()

        createFiles().forEach { file ->
            val filePath = sourcePath.resolve(file.name)
            filePath.parent.createDirectories()
            filePath.writeText(file.text)
        }

        val module = createModule(rootPath.toString())
        module.addSourceContentRoot(sourcePath)
        return module
    }

    protected fun Module.addSourceContentRoot(rootPath: Path): SourceFolder {
        val root = requireNotNull(LocalFileSystem.getInstance().refreshAndFindFileByNioFile(rootPath)) {
            "Cannot find source root: $rootPath"
        }
        root.refresh(false, true)
        return runWriteAction {
            PsiTestUtil.addSourceContentToRoots(this, root)
        }
    }

    protected fun Module.addDependency(
        other: Module,
        dependencyScope: DependencyScope = DependencyScope.COMPILE,
        exported: Boolean = false,
        productionOnTest: Boolean = false,
    ): Module = apply {
        ModuleRootModificationUtil.addDependency(this, other, dependencyScope, exported, productionOnTest)
    }

    protected fun Module.addProjectLibrary(
        libraryName: String,
        classesRoot: VirtualFile,
        sourcesRoot: VirtualFile? = null,
        dependencyScope: DependencyScope = DependencyScope.COMPILE,
        exported: Boolean = false,
    ): Library = runWriteAction {
        val library = LibraryTablesRegistrar.getInstance().getLibraryTable(project).createLibrary(libraryName)
        library.modifiableModel.apply {
            addRoot(classesRoot, OrderRootType.CLASSES)
            if (sourcesRoot != null) {
                addRoot(sourcesRoot, OrderRootType.SOURCES)
            }
            commit()
        }
        ModuleRootModificationUtil.addDependency(this, library, dependencyScope, exported)
        library
    }

    protected fun createLocalDirectory(path: Path): VirtualFile {
        Files.createDirectories(path)
        return requireNotNull(LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path)) {
            "Cannot find local test directory: $path"
        }
    }

    protected data class FileWithText(val name: String, val text: String)

    /** Asserts that the [actual] value is not `null`, with an optional [message]. */
    @OptIn(ExperimentalContracts::class)
    fun <T : Any> assertNotNull(actual: T?, message: String? = null): T {
        contract { returns() implies (actual != null) }
        asserter.assertNotNull(message, actual)
        return actual!!
    }
}
