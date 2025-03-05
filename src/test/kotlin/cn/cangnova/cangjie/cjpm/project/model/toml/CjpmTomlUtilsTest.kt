package cn.cangnova.cangjie.cjpm.project.model.toml

import cn.cangnova.cangjie.CangJieTestBase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CjpmTomlUtilsTest: CangJieTestBase() {
    

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
        assertTrue(found.exists())
    }
} 