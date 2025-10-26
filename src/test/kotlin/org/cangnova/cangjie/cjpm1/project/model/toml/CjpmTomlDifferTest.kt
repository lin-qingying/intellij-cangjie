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

class CjpmTomlDifferTest: CangJieNoPlatformTestBase(){
    


    

    fun `test diff dependencies`() {
        val base = CjpmTomlConfig(
            dependencies = mapOf(
                "dep1" to DependencyConfig(path = "./dep1"),
                "dep2" to DependencyConfig(git = "https://example.com/dep2.git")
            )
        )
        
        val other = CjpmTomlConfig(
            dependencies = mapOf(
                "dep2" to DependencyConfig(git = "https://example.com/dep2-new.git"),
                "dep3" to DependencyConfig(path = "./dep3")
            )
        )
        
        val result = CjpmTomlDiffer.diff(base, other)
        
        assertTrue(result.hasDifferences)
        
        val removedDeps = result.getDifferencesOfType(CjpmTomlDiffer.DiffType.REMOVED)
        assertEquals(1, removedDeps.size)
        assertEquals("dependencies.dep1", removedDeps[0].path)
        
        val addedDeps = result.getDifferencesOfType(CjpmTomlDiffer.DiffType.ADDED)
        assertEquals(1, addedDeps.size)
        assertEquals("dependencies.dep3", addedDeps[0].path)
        
        val modifiedDeps = result.getDifferencesOfType(CjpmTomlDiffer.DiffType.MODIFIED)
        assertEquals(1, modifiedDeps.size)
        assertEquals("dependencies.dep2", modifiedDeps[0].path)
    }
    

    fun `test no differences`() {
        val config = CjpmTomlConfig(
            `package` = PackageConfig(
                name = "test",
                version = "0.1.0",
                cjcVersion = "0.55.3",
                outputType = OutputType.DYNAMIC
            )
        )
        
        val result = CjpmTomlDiffer.diff(config, config)
        
        assertFalse(result.hasDifferences)
        assertTrue(result.differences.isEmpty())
    }

    fun `test diff package configs`() {
        val base = CjpmTomlConfig(
            `package` = PackageConfig(
                name = "base",
                version = "0.1.0",
                cjcVersion = "0.55.3",
                outputType = OutputType.DYNAMIC
            )
        )

        val other = CjpmTomlConfig(
            `package` = PackageConfig(
                name = "other",
                version = "0.2.0",
                cjcVersion = "0.55.3",
                outputType = OutputType.STATIC
            )
        )

        val result = CjpmTomlDiffer.diff(base, other)

        assertTrue(result.hasDifferences)
        assertEquals(3, result.differences.size)

        val modifiedDiffs = result.getDifferencesOfType(CjpmTomlDiffer.DiffType.MODIFIED)
        assertEquals(3, modifiedDiffs.size)
        assertTrue(modifiedDiffs.any { it.path == "package.name" })
        assertTrue(modifiedDiffs.any { it.path == "package.version" })
        assertTrue(modifiedDiffs.any { it.path == "package.output-type" })
    }
} 