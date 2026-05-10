/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package org.cangnova.cangjie.test

import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File

/**
 * 对位 Kotlin `MockLibraryFacility` 的仓颉真实子集。
 *
 * 当前仓颉 IDE 测试只需要把已有目录/文件作为 project library roots 挂载，
 * 不在测试框架层编译 Kotlin jar 或伪造仓颉编译器产物。
 */
class CangJieMockLibraryFacility(
    private val classesRoot: File,
    private val sourcesRoot: File? = null,
    private val libraryName: String = MOCK_LIBRARY_NAME,
) {
    fun setUp(module: Module) {
        val classesVirtualFile = requireNotNull(LocalFileSystem.getInstance().refreshAndFindFileByIoFile(classesRoot)) {
            "Cannot find mock library classes root: $classesRoot"
        }
        val sourcesVirtualFile = sourcesRoot?.let { root ->
            requireNotNull(LocalFileSystem.getInstance().refreshAndFindFileByIoFile(root)) {
                "Cannot find mock library sources root: $root"
            }
        }
        CangJieConfigLibraryUtil.addLibrary(module, libraryName, classesVirtualFile, sourcesVirtualFile)
    }

    fun tearDown(module: Module) {
        CangJieConfigLibraryUtil.removeLibrary(module, libraryName)
    }

    companion object {
        const val MOCK_LIBRARY_NAME: String = "cangjieMockLibrary"
    }
}
