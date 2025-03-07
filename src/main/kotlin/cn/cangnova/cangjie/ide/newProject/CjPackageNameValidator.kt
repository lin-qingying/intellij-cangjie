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

package cn.cangnova.cangjie.ide.newProject

import cn.cangnova.cangjie.CangJieBundle
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.SystemInfo


object CjPackageNameValidator {


    private val KEYWORDS_BLACKLIST = setOf(
        "as", "abstract", "break",
        "Bool", "case", "catch",
        "class", "const", "continue",
        "Char", "do", "else",
        "enum", "extend", "for",
        "from", "func", "false",
        "finally", "foreign", "Float16",
        "Float32", "Float64", "if",
        "in", "is", "init",
        "import", "interface", "Int8",
        "Int16", "Int32", "Int64",
        "IntNative", "let", "mut",
        "main", "macro", "match",
        "Nothing", "open", "operator",
        "override", "prop", "public",
        "package", "private", "protected",
        "quote", "redef", "return",
        "spawn", "super", "static",
        "struct", "synchronized", "try",
        "this", "true", "type",
        "throw", "This", "unsafe",
        "Unit", "UInt8", "UInt16",
        "UInt32", "UInt64", "UIntNative",
        "var", "VArray", "where",
        "while"
    )

    private val BINARY_BLACKLIST = setOf("deps", "examples", "build", "incremental")

    private val WINDOWS_BLACKLIST = setOf(
        "con", "prn", "aux", "nul", "com1", "com2", "com3", "com4", "com5", "com6", "com7",
        "com8", "com9", "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9"
    )

    @Suppress("UnstableApiUsage")
    @NlsContexts.DialogMessage
    fun validate(name: String, isBinary: Boolean): String? = when {
        name.isEmpty() -> CangJieBundle.message("dialog.message.package.name.can.t.be.empty")
        name in KEYWORDS_BLACKLIST || name == "test" -> CangJieBundle.message(
            "dialog.message.name.cannot.be.used.as.crate.name2",
            name
        )

        isBinary && name in BINARY_BLACKLIST -> CangJieBundle.message(
            "dialog.message.name.cannot.be.used.as.crate.name",
            name
        )

        name[0].isDigit() -> CangJieBundle.message("dialog.message.package.names.starting.with.digit.cannot.be.used.as.crate.name")
        !name.all { it.isLetterOrDigit() || it == '-' || it == '_' } ->
            CangJieBundle.message("dialog.message.package.names.should.contain.only.letters.digits")

        SystemInfo.isWindows && name.lowercase() in WINDOWS_BLACKLIST -> CangJieBundle.message(
            "dialog.message.name.reserved.windows.filename",
            name
        )

        else -> null
    }
}
