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

package com.linqingying.cangjie.cjpm.toolchain.impl

import com.google.common.annotations.VisibleForTesting
import com.intellij.util.text.SemVer


data class CjcVersion(
    val semver: SemVer,
    val host: String,
    val type: String?

//    val commitHash: String? = null,
//    val commitDate: LocalDate? = null
)

@VisibleForTesting
fun parseCjcVersion(lines: List<String>): CjcVersion? {

    val cangjieComiler = """Cangjie Compiler: (\d+\.\d+\.\d+.*)""".toRegex()


    val find = { re: Regex -> lines.firstNotNullOfOrNull { re.matchEntire(it) } }
    val releaseMatch = find(cangjieComiler) ?: return null

    val hostRe = "Target:(.*)".toRegex()
    val hostText = find(hostRe)?.groups?.get(1)?.value?.trim() ?: return null
    var versionText = releaseMatch.groups[1]?.value ?: return null

//    0.48.2 (cjnative)
//    分割
    var type: String? = null
    return try {

//去掉括号
        val typeRegex = Regex("\\(([^()]*)\\)")

        type = typeRegex.find(versionText.split(" ")[1])?.groups?.get(1)?.value
        versionText = versionText.split(" ")[0]

        val semVer = SemVer.parseFromText(versionText) ?: return null
        CjcVersion(semVer, hostText, type)

    } catch (iex: IndexOutOfBoundsException) {
        null
    }

}
