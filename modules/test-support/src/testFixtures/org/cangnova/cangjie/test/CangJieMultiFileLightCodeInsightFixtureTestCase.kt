/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package org.cangnova.cangjie.test

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.readText

/**
 * 对位 Kotlin `KotlinMultiFileLightCodeInsightFixtureTestCase` 的仓颉 light 多文件 fixture。
 */
abstract class CangJieMultiFileLightCodeInsightFixtureTestCase : CangJieLightCodeInsightFixtureTestCase() {
    protected open val isLibraryByDefault: Boolean get() = false

    private var mockLibraryFacility: CangJieMockLibraryFacility? = null
    private var tempDirectory: Path? = null

    @OptIn(ExperimentalPathApi::class)
    override fun tearDown() {
        com.intellij.testFramework.RunAll.runAll(
            { mockLibraryFacility?.tearDown(module) },
            { tempDirectory?.deleteRecursively() },
            { super.tearDown() },
        )
    }

    protected open fun doTest(testDataPath: String) {
        doMultiFileTest(testDataPath)
    }

    private fun doMultiFileTest(testDataPath: String) {
        val mainFile = Path(testDataPath)
        val subFiles = if (isLibraryByDefault) {
            emptyList()
        } else {
            createTestFiles(mainFile)
        }

        val libraryFile = Path(if (isLibraryByDefault) testDataPath else "$testDataPath.lib")
        if (libraryFile.exists()) {
            configureLibrary(libraryFile)
        }

        val files = configureMultiFileTest(subFiles)
        doMultiFileTest(
            testDataPath = testDataPath,
            files = files,
            globalDirectives = files.firstOrNull()?.testFile?.directives ?: Directives(),
        )
    }

    private fun configureLibrary(libraryFile: Path) {
        val libraryFiles = createTestFiles(libraryFile)
        assertNotEmpty(libraryFiles)

        val directoryForLibFiles = createTempDirectory(prefix = fileName()).also { tempDirectory = it }
        val sourcesPath = directoryForLibFiles.resolve("sources")
        sourcesPath.createDirectories()

        libraryFiles.forEach { testFile ->
            val filePath = sourcesPath.resolve(testFile.name)
            filePath.parent.createDirectories()
            filePath.toFile().writeText(testFile.content)
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

    protected open fun doMultiFileTest(
        testDataPath: String,
        files: List<TestFileWithVirtualFile>,
        globalDirectives: Directives,
    ) {
        val psiFiles = runReadAction {
            files.mapNotNull { PsiManager.getInstance(project).findFile(it.virtualFile) }
        }
        doMultiFileTest(psiFiles, globalDirectives)
    }

    protected open fun doMultiFileTest(files: List<PsiFile>, globalDirectives: Directives) {
        throw UnsupportedOperationException()
    }

    private fun configureMultiFileTest(subFiles: List<CangJieBaseTest.TestFile>): List<TestFileWithVirtualFile> {
        return subFiles.map { TestFileWithVirtualFile(it, myFixture.tempDirFixture.createFile(it.name, it.content)) }
    }

    protected data class TestFileWithVirtualFile(
        val testFile: CangJieBaseTest.TestFile,
        val virtualFile: VirtualFile,
    )
}

