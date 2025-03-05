package cn.cangnova.cangjie.cjpm.project.model

import cn.cangnova.cangjie.CangJieTestBase

import java.io.File

class CjpmDeserializerTest :CangJieTestBase(){


    fun should_deserialize_project_toml_file_success() {
        val resourcePath = this.javaClass.getResource("/project_toml_files")?.path
        val files = resourcePath?.let { File(it).walk().filter { it.isFile && it.name.endsWith(".toml") } }

        assertNotNull(files)
        assertTrue { files.count() > 0 }
        files.forEach {
            assertDoesNotThrow(it.absolutePath) {
                CjpmProjectInfo.deserialize(it.toPath())
            }
        }
    }
}
