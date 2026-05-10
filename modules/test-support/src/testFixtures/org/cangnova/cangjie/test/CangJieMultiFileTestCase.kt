/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package org.cangnova.cangjie.test

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.testFramework.PsiTestUtil
import java.io.File

/**
 * 对位 Kotlin `KotlinMultiFileTestCase` 的仓颉旧式 multi-file 测试入口。
 *
 * 当前 IntelliJ 253 测试依赖集中不暴露 `com.intellij.refactoring.MultiFileTestCase`，
 * 因此该层保留 Kotlin 入口职责，但基于仓颉已有 light platform fixture 实现。
 */
abstract class CangJieMultiFileTestCase : CangJieLightPlatformCodeInsightTestCase(), CangJieTestCase {
    protected var isMultiModule: Boolean = false

    protected open fun fileFilter(file: VirtualFile): Boolean = !CangJieTestUtils.isMultiExtensionName(file.name)

    protected open fun fileNameMapper(file: VirtualFile): String = file.name

    public override fun getTestName(lowercaseFirstLetter: Boolean): String =
        TestCase.camelOrWordsToSnake(super.getTestName(lowercaseFirstLetter))

    final override fun getTestDataPath(): String =
        CangJieTestUtils.toSlashEndingDirPath(getTestDataDirectory().absolutePath)

    protected open fun getTestDataDirectory(): File =
        TestMetadataUtil.getTestData(javaClass) ?: File(super.getTestDataPath())

    protected fun getTestDirName(lowercaseFirstLetter: Boolean): String {
        val testName = getTestName(lowercaseFirstLetter)
        val endIndex = testName.lastIndexOf('_')
        return if (endIndex < 0) testName else testName.substring(0, endIndex).replace('_', '/')
    }

    protected fun doTestCommittingDocuments(action: (VirtualFile, VirtualFile?) -> Unit) {
        val rootDir = getSourceRoot()
        action(rootDir, null)
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        FileDocumentManager.getInstance().saveAllDocuments()
    }

    protected fun prepareProject(rootDir: VirtualFile) {
        PsiTestUtil.addSourceContentToRoots(module, rootDir)
    }
}
