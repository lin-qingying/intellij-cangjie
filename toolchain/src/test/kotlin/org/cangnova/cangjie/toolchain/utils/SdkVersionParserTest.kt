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

package org.cangnova.cangjie.toolchain.utils

import org.cangnova.cangjie.CangJieNoPlatformTestBase
import org.cangnova.cangjie.toolchain.CangJieSdkVersion
import org.junit.Test

class SdkVersionParserTest : CangJieNoPlatformTestBase() {

    @Test
    fun `test parseVersionFromOutput with type`() {
        val output = listOf(
            "Cangjie Compiler: 1.0.1 (cjnative)",
            "Target: x86_64-w64-mingw32"
        )

        val version = SdkVersionParser.parseVersionFromOutput(output)

        assertNotNull(version)
        assertEquals("1.0.1", version!!.semver.toString())
        assertEquals("x86_64-w64-mingw32", version.targetPlatform)
        assertEquals("cjnative", version.type)
    }

    @Test
    fun `test parseVersionFromOutput without type`() {
        val output = listOf(
            "Cangjie Compiler: 0.53.13",
            "Target: aarch64-linux-ohos"
        )

        val version = SdkVersionParser.parseVersionFromOutput(output)

        assertNotNull(version)
        assertEquals("0.53.13", version!!.semver.toString())
        assertEquals("aarch64-linux-ohos", version.targetPlatform)
        assertNull(version.type)
    }

    @Test
    fun `test parseVersionFromOutput with empty output`() {
        val output = emptyList<String>()

        val version = SdkVersionParser.parseVersionFromOutput(output)

        assertNull(version)
    }

    @Test
    fun `test parseVersionFromOutput with invalid format`() {
        val output = listOf(
            "Invalid output",
            "No version info here"
        )

        val version = SdkVersionParser.parseVersionFromOutput(output)

        assertNull(version)
    }

    @Test
    fun `test parseVersionFromOutput with missing target`() {
        val output = listOf(
            "Cangjie Compiler: 0.53.13 (stable)"
        )

        val version = SdkVersionParser.parseVersionFromOutput(output)

        assertNull(version)
    }

    @Test
    fun `test generateSdkId with type`() {
        val version = CangJieSdkVersion(
            semver = com.intellij.util.text.SemVer.parseFromText("1.0.1")!!,
            targetPlatform = "x86_64-w64-mingw32",
            type = "cjnative"
        )

        val id = SdkVersionParser.generateSdkId(version)

        assertEquals("CangJie-1.0.1-cjnative", id)
    }

    @Test
    fun `test generateSdkId without type`() {
        val version = CangJieSdkVersion(
            semver = com.intellij.util.text.SemVer.parseFromText("0.53.13")!!,
            targetPlatform = "aarch64-linux-ohos",
            type = null
        )

        val id = SdkVersionParser.generateSdkId(version)

        assertEquals("CangJie-0.53.13", id)
    }

    @Test
    fun `test generateSdkId with null version`() {
        val id = SdkVersionParser.generateSdkId(null)

        assertEquals("CangJie-Unknown", id)
    }

    @Test
    fun `test generateSdkName with type`() {
        val version = CangJieSdkVersion(
            semver = com.intellij.util.text.SemVer.parseFromText("1.0.1")!!,
            targetPlatform = "x86_64-w64-mingw32",
            type = "cjnative"
        )

        val name = SdkVersionParser.generateSdkName(version)

        assertEquals("CangJie 1.0.1 (cjnative) - x86_64-w64-mingw32", name)
    }

    @Test
    fun `test generateSdkName without type`() {
        val version = CangJieSdkVersion(
            semver = com.intellij.util.text.SemVer.parseFromText("0.53.13")!!,
            targetPlatform = "aarch64-linux-ohos",
            type = null
        )

        val name = SdkVersionParser.generateSdkName(version)

        assertEquals("CangJie 0.53.13 - aarch64-linux-ohos", name)
    }

    @Test
    fun `test generateSdkName with null version`() {
        val name = SdkVersionParser.generateSdkName(null)

        assertEquals("CangJie SDK (Unknown)", name)
    }
}
