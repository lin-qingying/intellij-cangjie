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
 * 对位 Kotlin `KotlinLightCodeInsightTestCase` 的兼容入口。
 *
 * 仓颉不引入 `LightJavaCodeInsightTestCase`，因此该旧入口落在 platform light code insight 链上，
 * 只保留 Kotlin 旧测试基类真正依赖的 test-data 组织能力。
 */
@Deprecated("Use CangJieLightCodeInsightFixtureTestCase instead")
abstract class CangJieLightCodeInsightTestCase : CangJieLightPlatformCodeInsightTestCase() {
    protected open val filesBasedTest: Boolean = false

    open fun getTestDataDirectory(): File {
        val clazz = this::class.java
        val root = CangJieTestUtils.getTestsRoot(clazz)
        if (filesBasedTest) {
            return File(root)
        }

        val test = CangJieTestUtils.getTestDataFileName(clazz, name)
            ?: error("No @TestMetadata for ${clazz.name}")
        return File(root, test)
    }

    final override fun getTestDataPath(): String {
        return CangJieTestUtils.toSlashEndingDirPath(getTestDataDirectory().path)
    }

    protected fun dataFile(fileName: String): File = File(testDataPath, fileName)

    protected fun dataFile(): File = dataFile(fileName())

    protected fun dataPath(fileName: String = fileName()): String = dataFile(fileName).toString()

    protected fun dataPath(): String = dataPath(fileName())

    protected open fun fileName(): String =
        CangJieTestUtils.getTestDataFileName(this::class.java, this.name) ?: (getTestName(false) + ".cj")
}
