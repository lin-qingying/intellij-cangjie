package cn.cangnova.cangjie.toml.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import cn.cangnova.cangjie.toml.CjpmTomlPsiPattern.inKey
import cn.cangnova.cangjie.toml.CjpmTomlPsiPattern.inValueWithKey
import cn.cangnova.cangjie.toml.tomlPluginIsAbiCompatible

internal class CjpmTomlCompletionContributor : CompletionContributor() {

    init {
        if (tomlPluginIsAbiCompatible()) {
            extend(CompletionType.BASIC, inKey, CjpmTomlKeysCompletionProvider())

            extend(
                CompletionType.BASIC,
                inValueWithKey("license"),
                CjpmTomlKnownValuesCompletionProvider(popularSpdxLicenses)
            )
        }
    }
}

private val popularSpdxLicenses = listOf(
    "AGPL-3",
    "Apache-2.0",
    "BSD-2",
    "BSD-3",
    "BSL-1",
    "CC0-1",
    "EPL-2",
    "GPL-2",
    "GPL-3",
    "LGPL-2",
    "MIT",
    "MPL-2"
)
