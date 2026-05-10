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

@file:JvmName("CangJieTestUtils")

package org.cangnova.cangjie.test

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.testFramework.UsefulTestCase
import java.io.File
import java.lang.reflect.Method
import java.util.regex.Pattern

/**
 * 对位 Kotlin `KotlinTestUtils` 的仓颉测试工具入口。
 *
 * 这里只保留与仓颉 IntelliJ 插件测试框架直接相关、且不引入 Java/JDK 语义的能力：
 * 1. test data 元数据解析；
 * 2. VFS root access 生命周期管理；
 * 3. 目录/文件名的公共规整逻辑。
 */
object CangJieTestUtils {
    private val DIRECTIVE_PATTERN: Pattern = Pattern.compile("^//\\s*[!]?([A-Z0-9_]+)(:[ \\t]*(.*))?$", Pattern.MULTILINE)

    @JvmStatic
    fun allowProjectRootAccess(testCase: UsefulTestCase): Ref<Disposable> {
        val repositoryRoot = CangJiePluginTestCaseBase.locateRepositoryRoot().toString()
        return allowRootAccess(testCase, repositoryRoot)
    }

    @JvmStatic
    fun allowRootAccess(testCase: UsefulTestCase, vararg roots: String): Ref<Disposable> {
        val disposable = Disposer.newDisposable(testCase.testRootDisposable, testCase.javaClass.name)
        VfsRootAccess.allowRootAccess(disposable, *roots)
        return Ref.create(disposable)
    }

    @JvmStatic
    fun disposeVfsRootAccess(vfsDisposableRef: Ref<Disposable>?) {
        val vfsDisposable = vfsDisposableRef?.get()
        if (vfsDisposable != null && !Disposer.isDisposed(vfsDisposable)) {
            Disposer.dispose(vfsDisposable)
            vfsDisposableRef.set(null)
        }
    }

    @JvmStatic
    fun getTestsRoot(testCaseClass: Class<*>): String {
        return requireNotNull(TestMetadataUtil.getTestData(testCaseClass)) {
            "No metadata for class: $testCaseClass"
        }.toString()
    }

    @JvmStatic
    fun toSlashEndingDirPath(path: String): String {
        return if (path.endsWith(File.separator)) path else path + File.separatorChar
    }

    /**
     * 返回测试方法上通过 `@TestMetadata` 声明的 test data 文件名。
     */
    @JvmStatic
    fun getTestDataFileName(testCaseClass: Class<*>, testName: String): String? {
        val method = try {
            testCaseClass.getMethod(testName)
        } catch (e: NoSuchMethodException) {
            throw RuntimeException(e)
        }
        return getMethodMetadata(method)
    }

    @JvmStatic
    fun getMethodMetadata(method: Method): String? {
        return method.getAnnotation(TestMetadata::class.java)?.value
    }

    @JvmStatic
    fun parseDirectives(expectedText: String): Directives {
        return parseDirectives(expectedText, Directives())
    }

    @JvmStatic
    fun parseDirectives(expectedText: String, directives: Directives): Directives {
        val directiveMatcher = DIRECTIVE_PATTERN.matcher(expectedText)
        while (directiveMatcher.find()) {
            val name = directiveMatcher.group(1)
            val value = directiveMatcher.group(3)
            directives.put(name, value)
        }
        return directives
    }

    @JvmStatic
    fun isMultiExtensionName(name: String): Boolean {
        val firstDotIndex = name.indexOf('.')
        if (firstDotIndex == -1) {
            return false
        }
        return name.indexOf('.', firstDotIndex + 1) != -1
    }
}
