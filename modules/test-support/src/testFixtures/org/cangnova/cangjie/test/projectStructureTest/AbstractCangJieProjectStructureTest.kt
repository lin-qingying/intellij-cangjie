/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package org.cangnova.cangjie.test.projectStructureTest

import com.google.gson.JsonObject
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.PsiTestUtil
import org.cangnova.cangjie.test.AbstractCangJieMultiModuleTest
import org.cangnova.cangjie.test.CangJieConfigLibraryUtil
import org.cangnova.cangjie.test.addDependency
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

typealias ProjectLibrariesByName = Map<String, Library>
typealias ModulesByName = Map<String, Module>
typealias ModuleContentRoots = Map<Module, List<VirtualFile>>

/**
 * 对位 Kotlin `AbstractProjectStructureTest` 的仓颉 project-structure 测试基类。
 *
 * 使用 `structure.json` 创建 IntelliJ project libraries、modules、content roots 和依赖关系。
 */
abstract class AbstractCangJieProjectStructureTest<S : TestProjectStructure>(
    private val testProjectStructureParser: TestProjectStructureParser<S>,
) : AbstractCangJieMultiModuleTest() {
    private lateinit var _testProjectStructure: S
    protected val testProjectStructure: S get() = _testProjectStructure

    private lateinit var _projectLibrariesByName: ProjectLibrariesByName
    protected val projectLibrariesByName: ProjectLibrariesByName get() = _projectLibrariesByName

    private lateinit var _modulesByName: ModulesByName
    protected val modulesByName: ModulesByName get() = _modulesByName

    private lateinit var _moduleContentRoots: ModuleContentRoots
    protected val moduleContentRoots: ModuleContentRoots get() = _moduleContentRoots

    protected abstract fun doTestWithProjectStructure(testDirectory: String)

    protected fun doTest(testDirectory: String) {
        val jsonFile = Paths.get(testDirectory).resolve("structure.json")
        val json = TestProjectStructureReader.readJsonFile(jsonFile)
        val isDisabled = json.getAsJsonPrimitive(TestProjectStructureFields.IS_DISABLED_FIELD)?.asBoolean == true
        if (isDisabled) return

        initializeProjectStructure(testDirectory, json, testProjectStructureParser)
        doTestWithProjectStructure(testDirectory)
    }

    private fun initializeProjectStructure(
        testDirectory: String,
        json: JsonObject,
        parser: TestProjectStructureParser<S>,
    ) {
        val testStructure = TestProjectStructureReader.parseTestStructure(json, parser)
        val libraryRootsByLabel = testStructure.libraries
            .flatMapTo(mutableSetOf()) { it.roots }
            .associateWith { rootLabel -> createLibraryRoot(rootLabel, testDirectory) }

        val projectLibrariesByName = testStructure.libraries.associate { libraryData ->
            libraryData.name to CangJieConfigLibraryUtil.addProjectLibrary(project, libraryData.name) {
                libraryData.roots.forEach { rootLabel ->
                    val libraryRoot = libraryRootsByLabel.getValue(rootLabel)
                    addRoot(libraryRoot.classRoot, com.intellij.openapi.roots.OrderRootType.CLASSES)
                    libraryRoot.sourceRoot?.let { addRoot(it, com.intellij.openapi.roots.OrderRootType.SOURCES) }
                }
            }
        }

        val modulesByName = mutableMapOf<String, Module>()
        val moduleContentRoots = mutableMapOf<Module, List<VirtualFile>>()

        testStructure.modules.forEach { testModule ->
            val contentRootVirtualFiles = mutableListOf<VirtualFile>()
            val module = createModuleWithSources(testModule, testDirectory, contentRootVirtualFiles)
            modulesByName[testModule.name] = module
            moduleContentRoots[module] = contentRootVirtualFiles
        }

        val duplicateNames = projectLibrariesByName.keys.intersect(modulesByName.keys)
        require(duplicateNames.isEmpty()) {
            "Test project libraries and modules may not share names. Duplicate names: ${duplicateNames.joinToString()}."
        }

        testStructure.modules.forEach { moduleData ->
            val module = modulesByName.getValue(moduleData.name)
            moduleData.dependencies.filter { it.kind == DependencyKind.REGULAR }.forEach { dependency ->
                addRegularDependency(module, dependency, modulesByName, projectLibrariesByName)
            }
        }

        _testProjectStructure = testStructure
        _projectLibrariesByName = projectLibrariesByName
        _modulesByName = modulesByName
        _moduleContentRoots = moduleContentRoots
    }

    private data class LibraryRoot(val classRoot: VirtualFile, val sourceRoot: VirtualFile?)

    private fun createLibraryRoot(rootLabel: String, testDirectory: String): LibraryRoot {
        val librarySources = Paths.get(testDirectory).resolve(rootLabel)
        val classRootPath = if (librarySources.isDirectory()) {
            librarySources
        } else {
            Files.createTempDirectory("cangjie-project-library-root").also { it.createDirectories() }
        }

        val classRoot = requireNotNull(LocalFileSystem.getInstance().refreshAndFindFileByNioFile(classRootPath)) {
            "Cannot find library root: $classRootPath"
        }
        val sourceRoot = librarySources.takeIf { it.isDirectory() }?.let { path ->
            requireNotNull(LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path)) {
                "Cannot find library source root: $path"
            }
        }
        return LibraryRoot(classRoot, sourceRoot)
    }

    private fun createModuleWithSources(
        testModule: TestProjectModule,
        testDirectory: String,
        contentRootVirtualFiles: MutableList<VirtualFile>,
    ): Module {
        val tmpPath = createTempDirectory().toPath()
        val rootPath = tmpPath.resolve(testModule.name)
        rootPath.createDirectories()
        copyModuleContent(testModule, testDirectory, rootPath)

        val module = createModule(rootPath.toString())
        val root = requireNotNull(LocalFileSystem.getInstance().refreshAndFindFileByNioFile(rootPath)) {
            "Cannot find module root: $rootPath"
        }
        WriteCommandAction.writeCommandAction(module.project).run<RuntimeException> {
            root.refresh(false, true)
        }

        val contentRoots = testModule.contentRoots ?: listOf(TestContentRoot(null, TestContentRootKind.PRODUCTION))
        contentRoots.forEach { testContentRoot ->
            val contentRootPath = testContentRoot.path?.let(rootPath::resolve) ?: rootPath
            if (!contentRootPath.exists()) {
                contentRootPath.createDirectories()
            }
            require(contentRootPath.isDirectory()) {
                "Expected the content root directory `$contentRootPath` to be a directory."
            }

            val contentRoot = requireNotNull(LocalFileSystem.getInstance().refreshAndFindFileByNioFile(contentRootPath))
            when (testContentRoot.kind) {
                TestContentRootKind.PRODUCTION -> PsiTestUtil.addSourceRoot(module, contentRoot)
                TestContentRootKind.TESTS -> PsiTestUtil.addSourceRoot(module, contentRoot, true)
                TestContentRootKind.RESOURCES -> PsiTestUtil.addResourceContentToRoots(module, contentRoot, false)
                TestContentRootKind.TEST_RESOURCES -> PsiTestUtil.addResourceContentToRoots(module, contentRoot, true)
            }
            contentRootVirtualFiles += contentRoot
        }

        return module
    }

    private fun copyModuleContent(testModule: TestProjectModule, testDirectory: String, destinationRoot: Path) {
        val moduleRootPath = Paths.get(testDirectory).resolve(testModule.name)
        if (!moduleRootPath.isDirectory()) return

        Files.walk(moduleRootPath).use { stream ->
            stream.filter(Files::isRegularFile).forEach { file ->
                val relativePath = moduleRootPath.relativize(file)
                val destinationPath = destinationRoot.resolve(relativePath)
                destinationPath.parent.createDirectories()
                Files.copy(file, destinationPath)
            }
        }
    }

    private fun addRegularDependency(
        module: Module,
        dependency: Dependency,
        modulesByName: ModulesByName,
        projectLibrariesByName: ProjectLibrariesByName,
    ) {
        val dependencyScope = when (dependency.scope) {
            DependencyScope.COMPILE -> com.intellij.openapi.roots.DependencyScope.COMPILE
            DependencyScope.TEST -> com.intellij.openapi.roots.DependencyScope.TEST
            DependencyScope.RUNTIME -> com.intellij.openapi.roots.DependencyScope.RUNTIME
            DependencyScope.PROVIDED -> com.intellij.openapi.roots.DependencyScope.PROVIDED
        }

        modulesByName[dependency.name]?.let { dependencyModule ->
            module.addDependency(
                dependencyModule,
                dependencyScope = dependencyScope,
                exported = dependency.isExported,
                productionOnTest = dependency.productionOnTest,
            )
            return
        }

        projectLibrariesByName[dependency.name]?.let { library ->
            module.addDependency(library, dependencyScope, dependency.isExported)
            return
        }

        error("Cannot resolve dependency `${dependency.name}` for module `${module.name}`")
    }
}
