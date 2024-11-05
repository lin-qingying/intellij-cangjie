package com.linqingying.cangjie.cjpm.project.model

import CjpmWorkspaceData
import com.linqingying.cangjie.cjpm.project.workspace.CjpmWorkspace
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.io.File
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CjpmDeserializerTest {

    @Test
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
