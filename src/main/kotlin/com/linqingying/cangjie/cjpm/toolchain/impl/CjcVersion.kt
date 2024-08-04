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
