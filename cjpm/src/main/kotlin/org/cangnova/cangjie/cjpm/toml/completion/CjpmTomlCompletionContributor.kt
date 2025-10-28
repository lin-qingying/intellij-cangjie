package org.cangnova.cangjie.cjpm.toml.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import org.cangnova.cangjie.cjpm.toml.CjpmTomlPsiPattern
import org.cangnova.cangjie.cjpm.toml.tomlPluginIsAbiCompatible

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
