package cn.cangnova.cangjie.cjpm.project.model

import cn.cangnova.cangjie.CangJieNoPlatformTestBase
import org.junit.jupiter.api.assertDoesNotThrow

import java.io.File
import kotlin.test.assertTrue

class CjpmDeserializerTest : CangJieNoPlatformTestBase(){


    fun `test should_deserialize_project_toml_file_success`() {
        val resourcePath = this.javaClass.getResource("/project_toml_files")?.path
        val files = resourcePath?.let { File(it).walk().filter { it.isFile && it.name.endsWith(".toml") } } ?: error("files not found")

        assertNotNull(files)
        assertTrue { files.count() > 0 }
        files.forEach {
            assertDoesNotThrow(it.absolutePath) {
                CjpmProjectInfo.deserialize(it.toPath())
            }
        }
    }


}
