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

import java.io.File

/**
 * 对位 Kotlin `TestMetadataUtil` 的测试数据定位工具。
 *
 * 当前 IntelliJ 插件测试先对齐 Kotlin 的核心路径链：
 * 1. `@TestRoot` 提供测试目录根；
 * 2. `@TestMetadata` 提供相对测试数据路径；
 * 3. `getTestDataPath()` 在缺少元数据时回退到仓库根，保持平台夹具可初始化。
 */
object TestMetadataUtil {
    @JvmStatic
    fun getTestData(testClass: Class<*>): File? {
        val testMetadata = getTestMetadata(testClass) ?: return null
        val testRoot = getTestRoot(testClass) ?: return null
        return File(testRoot, testMetadata)
    }

    @JvmStatic
    fun getTestDataPath(testClass: Class<*>): String {
        val path = (getTestData(testClass) ?: CangJiePluginTestCaseBase.locateRepositoryRoot().toFile()).absolutePath
        return if (path.endsWith(File.separator)) path else path + File.separator
    }

    @JvmStatic
    fun getFile(testClass: Class<*>, path: String): File {
        return File(requireNotNull(getTestData(testClass)) { "No test data for ${testClass.name}" }, path)
    }

    @JvmStatic
    fun <A : Annotation> getAnnotationValue(
        testClass: Class<*>,
        annotationClass: Class<A>,
        lookupEnclosingClass: Boolean = true,
    ): A? {
        var current: Class<*>? = testClass
        if (lookupEnclosingClass) {
            while (current?.enclosingClass != null) {
                current = current.enclosingClass
            }
        }
        while (current != null && current != Any::class.java) {
            current.getAnnotation(annotationClass)?.let { return it }
            current = current.superclass
        }
        return null
    }

    @JvmStatic
    fun getTestMetadata(testClass: Class<*>): String? {
        return getAnnotationValue(testClass, TestMetadata::class.java, lookupEnclosingClass = false)?.value
    }

    @JvmStatic
    fun getTestRoot(testClass: Class<*>): File? {
        val relativeRoot = getAnnotationValue(testClass, TestRoot::class.java)?.value ?: return null
        return CangJiePluginTestCaseBase.locateRepositoryRoot().resolve(relativeRoot).toFile()
    }
}
