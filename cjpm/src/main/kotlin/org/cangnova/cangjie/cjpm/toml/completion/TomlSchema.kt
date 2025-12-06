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

package org.cangnova.cangjie.cjpm.toml.completion

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import org.intellij.lang.annotations.Language
import org.toml.lang.psi.TomlArrayTable
import org.toml.lang.psi.TomlFileType
import org.toml.lang.psi.TomlKeyValueOwner
import org.toml.lang.psi.TomlTable


class TomlSchema private constructor(
    private val tables: List<TomlTableSchema>
) {

    fun topLevelKeys(isArray: Boolean): Collection<String> =
        tables.filter { it.isArray == isArray }.map { it.name }

    fun keysForTable(tableName: String): Collection<String> =
        tables.find { it.name == tableName }?.keys.orEmpty()

    companion object {
        fun parse(project: Project, @Language("TOML") example: String): TomlSchema {
            val toml = PsiFileFactory.getInstance(project)
                .createFileFromText("cjpm.toml", TomlFileType, example)

            val tables = toml.children
                .filterIsInstance<TomlKeyValueOwner>()
                .mapNotNull { it.schema }

            return TomlSchema(tables)
        }
    }
}

private val TomlKeyValueOwner.schema: TomlTableSchema?
    get() {
        val (name, isArray) = when (this) {
            is TomlTable -> header.key?.segments?.firstOrNull()?.name to false
            is TomlArrayTable -> header.key?.segments?.firstOrNull()?.name to true
            else -> return null
        }
        if (name == null) return null

        val keys = entries.mapNotNull { it.key.text }.filter { it != "foo" }
        return TomlTableSchema(name, isArray, keys)
    }

private class TomlTableSchema(
    val name: String,
    val isArray: Boolean,
    val keys: Collection<String>
)
