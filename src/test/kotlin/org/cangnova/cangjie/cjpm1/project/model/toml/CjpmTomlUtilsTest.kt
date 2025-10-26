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

package org.cangnova.cangjie.cjpm1.project.model.toml

import org.cangnova.cangjie.CangJieNoPlatformTestBase
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class CjpmTomlUtilsTest:  CangJieNoPlatformTestBase(){
    

    fun `test create basic cjpm toml`(@TempDir tempDir: Path) {
        val file = CjpmTomlUtils.createBasicCjpmToml(
            directory = tempDir.toFile(),
            packageName = "test-package"
        )
        
        assertTrue(file.exists())
        val config = CjpmTomlParser.parse(file)
        assertEquals("test-package", config.`package`?.name)
    }
    

    fun `test validate valid config`() {
        val config = CjpmTomlConfig(
            `package` = PackageConfig(
                name = "test",
                version = "0.0.1",
                cjcVersion = "0.55.3",
                outputType = OutputType.DYNAMIC
            )
        )
        
        val result = CjpmTomlUtils.validate(config)
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
    

    fun `test validate invalid config`() {
        val config = CjpmTomlConfig(
            dependencies = mapOf(
                "invalid-dep" to DependencyConfig()
            )
        )
        
        val result = CjpmTomlUtils.validate(config)
        assertFalse(result.isValid)
        assertTrue(result.errors.isNotEmpty())
        assertTrue(result.errors.any { it.contains("Missing [package] section") })
        assertTrue(result.errors.any { it.contains("invalid-dep") })
    }
    

    fun `test find cjpm toml`(@TempDir tempDir: Path) {
        // 创建一个测试文件
        CjpmTomlUtils.createBasicCjpmToml(
            directory = tempDir.toFile(),
            packageName = "test-package"
        )
        
        val found = CjpmTomlUtils.findCjpmToml(tempDir)
        assertNotNull(found)
        assertTrue(found!!.exists())
    }
} 