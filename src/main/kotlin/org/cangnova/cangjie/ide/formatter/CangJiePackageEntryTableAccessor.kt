/*
 * Copyright 2024 LinQingYing. and contributors.
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



package org.cangnova.cangjie.ide.formatter

import com.intellij.application.options.codeStyle.properties.CodeStylePropertiesUtil
import com.intellij.application.options.codeStyle.properties.ValueListPropertyAccessor

import java.lang.reflect.Field

class CangJiePackageEntryTableAccessor(cangjieCodeStyle: CangJieCodeStyleSettings, field: Field) :
    ValueListPropertyAccessor<CangJiePackageEntryTable>(cangjieCodeStyle, field) {
    override fun valueToString(value: List<String>): String = CodeStylePropertiesUtil.toCommaSeparatedString(value)

    override fun fromExternal(extVal: List<String>): CangJiePackageEntryTable = CangJiePackageEntryTable(
        extVal.asSequence().map(String::trim).map(::readPackageEntry).toMutableList()
    )

    override fun isEmptyListAllowed(): Boolean = false

    override fun toExternal(value: CangJiePackageEntryTable): List<String> = value.getEntries().map(::writePackageEntry)

    companion object {
        private const val ALIAS_CHAR = "^"
        private const val OTHER_CHAR = "*"

        private fun readPackageEntry(string: String): CangJiePackageEntry = when {
            string == ALIAS_CHAR -> CangJiePackageEntry.ALL_OTHER_ALIAS_IMPORTS_ENTRY
            string == OTHER_CHAR -> CangJiePackageEntry.ALL_OTHER_IMPORTS_ENTRY
            string.endsWith("**") -> CangJiePackageEntry(string.substring(0, string.length - 1), true)
            else -> CangJiePackageEntry(string, false)
        }

        private fun writePackageEntry(entry: CangJiePackageEntry): String = when (entry) {
            CangJiePackageEntry.ALL_OTHER_ALIAS_IMPORTS_ENTRY -> ALIAS_CHAR
            CangJiePackageEntry.ALL_OTHER_IMPORTS_ENTRY -> OTHER_CHAR
            else -> "${entry.packageName}.*" + if (entry.withSubpackages) "*" else ""
        }
    }
}

