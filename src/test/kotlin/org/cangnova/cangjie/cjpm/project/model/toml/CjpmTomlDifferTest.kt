package org.cangnova.cangjie.cjpm.project.model.toml

import org.cangnova.cangjie.CangJieNoPlatformTestBase
import org.cangnova.cangjie.CangJieTestBase

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