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

import com.intellij.testFramework.TestDataPath
import java.io.File

/**
 * 对位 Kotlin `TestMetadataUtil` 的测试数据定位工具。
 *
 * 当前仓颉插件测试还没有 Kotlin 那套 `@TestRoot`/Bazel 路径分发体系，
 * 因此这里只保留本地 IntelliJ 测试实际需要的能力：
 * 1. 读取 `@TestDataPath`
 * 2. 读取方法/类上的 `@TestMetadata`
 * 2. 统一返回带结尾分隔符的 test data 根目录
 * 3. 按 Kotlin 同名工具提供 `getTestData` / `getFile` 入口
 */
object TestMetadataUtil {
    @JvmStatic
    fun getTestData(testClass: Class<*>): File = File(getTestDataPath(testClass))

    @JvmStatic
    fun getTestDataPath(testClass: Class<*>): String {
        val testMetadataPath = getTestMetadata(testClass)
        val annotationPath = getAnnotationValue(testClass, TestDataPath::class.java)?.value
        val path = when {
            !testMetadataPath.isNullOrBlank() -> File(testMetadataPath).absolutePath
            annotationPath.isNullOrBlank() -> File(TestCase.testResourcesPath).absolutePath
            annotationPath == "\$CONTENT_ROOT" -> File(TestCase.testResourcesPath).absolutePath
            else -> File(annotationPath).absolutePath
        }

        return if (path.endsWith(File.separator)) path else path + File.separator
    }

    @JvmStatic
    fun getFile(testClass: Class<*>, path: String): File {
        return File(getTestData(testClass), path)
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
}
