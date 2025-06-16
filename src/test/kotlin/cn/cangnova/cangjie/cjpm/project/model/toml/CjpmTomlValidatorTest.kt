package cn.cangnova.cangjie.cjpm.project.model.toml

import cn.cangnova.cangjie.CangJieNoPlatformTestBase
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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