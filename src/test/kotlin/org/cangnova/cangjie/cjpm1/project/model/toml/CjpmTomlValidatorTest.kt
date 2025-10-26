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

class CjpmTomlValidatorTest : CangJieNoPlatformTestBase(){
    

    fun `test valid config`() {
        val config = CjpmTomlConfig(
            `package` = PackageConfig(
                name = "test-package",
                version = "0.1.0",
                cjcVersion = "0.55.3",
                outputType = OutputType.DYNAMIC
            )
        )
        
        val result = CjpmTomlValidator.validate(config)
        
        assertFalse(result.hasErrors)
        assertFalse(result.hasWarnings)
        assertTrue(result.violations.isEmpty())
    }
    

    fun `test invalid package name`() {
        val config = CjpmTomlConfig(
            `package` = PackageConfig(
                name = "Test_Package",  // Invalid: contains uppercase and underscore
                version = "0.1.0",
                cjcVersion = "0.55.3",
                outputType = OutputType.DYNAMIC
            )
        )
        
        val result = CjpmTomlValidator.validate(config)
        
        assertTrue(result.hasErrors)
        assertEquals(1, result.getErrors().size)
        assertTrue(result.getErrors()[0].message.contains("can only contain lowercase"))
    }
    

    fun `test invalid dependency configuration`() {
        val config = CjpmTomlConfig(
            `package` = PackageConfig(
                name = "test-package",
                version = "0.1.0",
                cjcVersion = "0.55.3",
                outputType = OutputType.DYNAMIC
            ),
            dependencies = mapOf(
                "dep1" to DependencyConfig(
                    path = "./dep1",
                    git = "https://example.com/dep1.git"  // Invalid: both path and git
                )
            )
        )
        
        val result = CjpmTomlValidator.validate(config)
        
        assertTrue(result.hasErrors)
        assertEquals(1, result.getErrors().size)
        assertTrue(result.getErrors()[0].message.contains("cannot specify both"))
    }
    

    fun `test invalid workspace configuration`() {
        val config = CjpmTomlConfig(
            workspace = WorkspaceConfig(
                members = listOf("pkg1", "pkg2"),
                buildMembers = listOf("pkg1", "pkg3"),  // Invalid: pkg3 not in members
                testMembers = listOf("pkg1")
            ),
            `package` = PackageConfig(  // Invalid: both workspace and package
                name = "test-package",
                version = "0.1.0",
                cjcVersion = "0.55.3",
                outputType = OutputType.DYNAMIC
            )
        )
        
        val result = CjpmTomlValidator.validate(config)
        
        assertTrue(result.hasErrors)
        assertEquals(2, result.getErrors().size)
        assertTrue(result.getErrors().any { it.message.contains("Cannot have both workspace") })
        assertTrue(result.getErrors().any { it.message.contains("Build members must be a subset") })
    }
    

    fun `test target configuration warnings`() {
        val config = CjpmTomlConfig(
            `package` = PackageConfig(
                name = "test-package",
                version = "0.1.0",
                cjcVersion = "0.55.3",
                outputType = OutputType.DYNAMIC
            ),
            target = mapOf(
                "invalid-target" to TargetConfig()  // Invalid target triple format
            )
        )
        
        val result = CjpmTomlValidator.validate(config)
        
        assertFalse(result.hasErrors)
        assertTrue(result.hasWarnings)
        assertEquals(1, result.getWarnings().size)
        assertTrue(result.getWarnings()[0].message.contains("Target triple format"))
    }
} 