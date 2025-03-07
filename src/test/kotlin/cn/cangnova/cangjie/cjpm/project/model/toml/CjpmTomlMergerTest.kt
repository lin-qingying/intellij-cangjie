package cn.cangnova.cangjie.cjpm.project.model.toml

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CjpmTomlMergerTest {
    
    @Test
    fun `test merge package configs`() {
        val base = CjpmTomlConfig(
            `package` = PackageConfig(
                name = "base",
                version = "0.1.0",
                cjcVersion = "0.55.3",
                outputType = OutputType.DYNAMIC,
                description = "base description"
            )
        )
        
        val other = CjpmTomlConfig(
            `package` = PackageConfig(
                name = "other",
                version = "0.2.0",
                cjcVersion = "0.55.3",
                outputType = OutputType.STATIC,
                compileOption = "-O2"
            )
        )
        
        val merged = CjpmTomlMerger.merge(base, other)
        
        assertNotNull(merged.`package`)
        with(merged.`package`!!) {
            assertEquals("other", name)
            assertEquals("0.2.0", version)
            assertEquals("0.55.3", cjcVersion)
            assertEquals(OutputType.STATIC, outputType)
            assertEquals("base description", description)
            assertEquals("-O2", compileOption)
        }
    }
    
    @Test
    fun `test merge dependencies`() {
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
        
        val merged = CjpmTomlMerger.merge(base, other)
        
        assertEquals(3, merged.dependencies.size)
        assertEquals("./dep1", merged.dependencies["dep1"]?.path)
        assertEquals("https://example.com/dep2-new.git", merged.dependencies["dep2"]?.git)
        assertEquals("./dep3", merged.dependencies["dep3"]?.path)
    }
    
    @Test
    fun `test merge target configs`() {
        val base = CjpmTomlConfig(
            target = mapOf(
                "x86_64-unknown-linux-gnu" to TargetConfig(
                    compileOption = "-O2",
                    binDependencies = BinDependenciesConfig(
                        pathOption = listOf("/usr/lib")
                    )
                )
            )
        )
        
        val other = CjpmTomlConfig(
            target = mapOf(
                "x86_64-unknown-linux-gnu" to TargetConfig(
                    compileOption = "-O3",
                    binDependencies = BinDependenciesConfig(
                        pathOption = listOf("/opt/lib")
                    )
                )
            )
        )
        
        val merged = CjpmTomlMerger.merge(base, other)
        
        val targetConfig = merged.target["x86_64-unknown-linux-gnu"]
        assertNotNull(targetConfig)
        assertEquals("-O3", targetConfig.compileOption)
        assertEquals(2, targetConfig.binDependencies?.pathOption?.size)
        assertEquals(listOf("/usr/lib", "/opt/lib"), targetConfig.binDependencies?.pathOption)
    }
} 