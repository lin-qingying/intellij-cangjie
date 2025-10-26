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

package org.cangnova.cangjie.cjpm1.project.model

import org.cangnova.cangjie.CangJieNoPlatformTestBase
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
