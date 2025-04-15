package cn.cangnova.cangjie.lang.toml.completion

import cn.cangnova.cangjie.lang.toml.CjpmTomlPsiPattern
import cn.cangnova.cangjie.lang.toml.tomlPluginIsAbiCompatible
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType

internal class CjpmTomlCompletionContributor : CompletionContributor() {

    init {
        if (tomlPluginIsAbiCompatible()) {
            extend(CompletionType.BASIC, CjpmTomlPsiPattern.inKey, CjpmTomlKeysCompletionProvider())

            extend(
                CompletionType.BASIC,
                CjpmTomlPsiPattern.inValueWithKey("license"),
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
