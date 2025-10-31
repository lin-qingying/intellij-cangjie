/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.project.configure

import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.impl.ContentEntryImpl
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.cangnova.cangjie.project.model.CjModule
import org.cangnova.cangjie.project.model.CjProject
import org.cangnova.cangjie.project.model.CjSourceSet
import org.cangnova.cangjie.project.model.CjTarget
import org.mockito.Mockito.*
import org.mockito.kotlin.mock

/**
 * ProjectSourceConfigurator 测试类
 */
class ProjectSourceConfiguratorTest : BasePlatformTestCase() {

    private lateinit var contentEntryWrapper: ContentEntryWrapper
    private lateinit var mockContentEntry: ContentEntry
    private lateinit var mockContentRoot: VirtualFile
    private lateinit var mockCjProject: CjProject
    private lateinit var mockCjModule: CjModule
    private lateinit var mockCjSourceSet: CjSourceSet
    private lateinit var mockCjTarget: CjTarget

    override fun setUp() {
        super.setUp()

        mockContentEntry = mock<ContentEntryImpl>()
        mockContentRoot = mock<VirtualFile>()
        mockCjProject = mock<CjProject>()
        mockCjModule = mock<CjModule>()
        mockCjSourceSet = mock<CjSourceSet>()
        mockCjTarget = mock<CjTarget>()

        contentEntryWrapper = ContentEntryWrapper(mockContentEntry)
    }

    fun testSetupWithEmptyProject() {
        // 当项目没有模块时，应该使用默认布局
        `when`(mockCjProject.modules).thenReturn(emptyList())
        `when`(mockContentRoot.findChild(any())).thenReturn(mock<VirtualFile>())

        contentEntryWrapper.setup(mockContentRoot, mockCjProject)

        // 验证默认源文件夹被添加
        verify(mockContentEntry, atLeastOnce()).addSourceFolder(any(), eq(false))
        verify(mockContentEntry, atLeastOnce()).addSourceFolder(any(), eq(true))
        verify(mockContentEntry).addExcludeFolder(any())
    }

    fun testSetupWithModuleAndSourceSet() {
        // 创建有模块和源码集的项目
        val mockSourceRoot = mock<VirtualFile>()
        val mockResourceRoot = mock<VirtualFile>()
        val mockOutputDir = mock<VirtualFile>()

        `when`(mockSourceRoot.url).thenReturn("file://test/src")
        `when`(mockResourceRoot.url).thenReturn("file://test/resources")
        `when`(mockOutputDir.url).thenReturn("file://test/target")

        `when`(mockCjProject.modules).thenReturn(listOf(mockCjModule))
        `when`(mockCjModule.sourceSets).thenReturn(listOf(mockCjSourceSet))
        `when`(mockCjModule.targets).thenReturn(listOf(mockCjTarget))

        `when`(mockCjSourceSet.sourceRoots).thenReturn(listOf(mockSourceRoot))
        `when`(mockCjSourceSet.resourceRoots).thenReturn(listOf(mockResourceRoot))
        `when`(mockCjSourceSet.isTest).thenReturn(false)

        `when`(mockCjTarget.outputDirectory).thenReturn(mockOutputDir)

        contentEntryWrapper.setup(mockContentRoot, mockCjProject)

        // 验证源码根目录被添加
        verify(mockContentEntry).addSourceFolder("file://test/src", false)
        // 验证资源根目录被添加
        verify(mockContentEntry).addSourceFolder("file://test/resources", false)
        // 验证输出目录被排除
        verify(mockContentEntry).addExcludeFolder("file://test/target")
    }

    fun testSetupWithTestSourceSet() {
        // 创建测试源码集
        val mockTestSourceRoot = mock<VirtualFile>()

        `when`(mockTestSourceRoot.url).thenReturn("file://test/tests")

        `when`(mockCjProject.modules).thenReturn(listOf(mockCjModule))
        `when`(mockCjModule.sourceSets).thenReturn(listOf(mockCjSourceSet))
        `when`(mockCjModule.targets).thenReturn(emptyList())

        `when`(mockCjSourceSet.sourceRoots).thenReturn(listOf(mockTestSourceRoot))
        `when`(mockCjSourceSet.resourceRoots).thenReturn(emptyList())
        `when`(mockCjSourceSet.isTest).thenReturn(true)

        contentEntryWrapper.setup(mockContentRoot, mockCjProject)

        // 验证测试源码根目录被添加
        verify(mockContentEntry).addSourceFolder("file://test/tests", true)
    }
}