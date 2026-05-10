/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package org.cangnova.cangjie.test

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.testFramework.HeavyTestHelper
import com.intellij.testFramework.IndexingTestUtil
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * 对位 Kotlin `KotlinMultiFileHeavyProjectTestCase` 的仓颉 heavy 多文件 fixture。
 */
abstract class CangJieMultiFileHeavyProjectTestCase : AbstractCangJieMultiModuleTest() {
    private var mockLibraryFacility: CangJieMockLibraryFacility? = null

    override fun tearDown() {
        com.intellij.testFramework.RunAll.runAll(
            { mockLibraryFacility?.tearDown(module) },
            { super.tearDown() },
        )
    }

    protected open fun mainFile(): Path = testDataDirectory.toPath().resolve(fileName())

    protected open fun doTest(testDataPath: String) {
        doMultiFileTest(testDataPath)
    }

    private fun doMultiFileTest(testDataPath: String) {
        val mainFile = Path(testDataPath)
        val subFiles = createTestFiles(mainFile)
        val rootPath = tempDir.newPath()
        val srcPath = rootPath.resolve("src")
        srcPath.createDirectories()

        configureMultiFileTest(subFiles, srcPath)
        HeavyTestHelper.createTestProjectStructure(module, srcPath.toString(), srcPath, true)

        val libraryFile = Path("$testDataPath.lib")
        if (libraryFile.exists()) {
            configureLibrary(libraryFile, rootPath.resolve("lib"))
        }

        IndexingTestUtil.waitUntilIndexesAreReady(project)
        doMultiFileTest(testDataPath, subFiles.firstOrNull()?.directives ?: Directives())
    }

    private fun configureLibrary(libraryFile: Path, targetPath: Path) {
        val libraryFiles = createTestFiles(libraryFile)
        assertNotEmpty(libraryFiles)

        val sourcesPath = targetPath.resolve("sources")
        sourcesPath.createDirectories()
        libraryFiles.forEach { testFile ->
            val filePath = sourcesPath.resolve(testFile.name)
            filePath.parent.createDirectories()
            filePath.writeText(testFile.content)
        }

        val directives = libraryFiles.first().directives
        val libraryName = directives["LIBRARY_NAME"] ?: CangJieMockLibraryFacility.MOCK_LIBRARY_NAME
        mockLibraryFacility = CangJieMockLibraryFacility(
            classesRoot = sourcesPath.toFile(),
            sourcesRoot = sourcesPath.toFile().takeIf { "WITH_SOURCES" in directives },
            libraryName = libraryName,
        ).also { it.setUp(module) }
    }

    private fun createTestFiles(mainFile: Path): List<CangJieBaseTest.TestFile> = TestFiles.createTestFiles(
        mainFile.name.takeIf { it.endsWith("cj") } ?: "single.cj",
        mainFile.readText(),
        object : TestFiles.TestFileFactoryNoModules<CangJieBaseTest.TestFile>() {
            override fun create(fileName: String, text: String, directives: Directives): CangJieBaseTest.TestFile {
                return CangJieBaseTest.TestFile(fileName, text, directives)
            }
        },
    )

    protected open fun doMultiFileTest(testDataPath: String, globalDirectives: Directives) {
        throw UnsupportedOperationException()
    }

    private fun configureMultiFileTest(subFiles: List<CangJieBaseTest.TestFile>, contentPath: Path) {
        subFiles.forEach { file ->
            val newFile = contentPath.resolve(file.name)
            newFile.parent.createDirectories()
            newFile.writeText(file.content)
        }
        VfsUtil.findFile(contentPath, true)!!.refresh(false, true)
    }

    protected fun attachProjectLibrary(libraryName: String, classesRoot: java.io.File) {
        val libraryRoot = createLocalDirectory(classesRoot.toPath())
        runWriteAction {
            val library = CangJieConfigLibraryUtil.addProjectLibrary(project, libraryName) {
                addRoot(libraryRoot, com.intellij.openapi.roots.OrderRootType.CLASSES)
            }
            ModuleRootManager.getInstance(module).modifiableModel.apply {
                addLibraryEntry(library)
                commit()
            }
        }
    }
}

