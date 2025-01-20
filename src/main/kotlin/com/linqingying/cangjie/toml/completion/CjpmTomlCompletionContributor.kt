package com.linqingying.cangjie.toml.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.linqingying.cangjie.toml.CjpmTomlPsiPattern.inKey
import com.linqingying.cangjie.toml.tomlPluginIsAbiCompatible

class CjpmTomlCompletionContributor : CompletionContributor() {

    init {
        if (tomlPluginIsAbiCompatible()) {
            extend(CompletionType.BASIC, inKey, CjpmTomlKeysCompletionProvider())
//            extend(
//                CompletionType.BASIC,
//                inValueWithKey("edition"),
//                CjpmTomlKnownValuesCompletionProvider(listOf("2015", "2018", "2021"))
//            )
//            extend(CompletionType.BASIC, inValueWithKey("license"), CjpmTomlKnownValuesCompletionProvider(popularSpdxLicenses))
//            extend(CompletionType.BASIC, inFeatureDependencyArray, CjpmTomlFeatureDependencyCompletionProvider())
//            extend(CompletionType.BASIC, inDependencyPackageFeatureArray, CjpmTomlDependencyFeaturesCompletionProvider())
//            extend(CompletionType.BASIC, inDependencyTableKey, CjpmTomlDependencyKeysCompletionProvider())
//
//            // Available using both Crates.io API & Crates Local Index
//            extend(CompletionType.BASIC, inDependencyKeyValue, CjpmTomlDependencyCompletionProvider())
//            extend(CompletionType.BASIC, inSpecificDependencyHeaderKey, CjpmTomlSpecificDependencyHeaderCompletionProvider())
//            extend(CompletionType.BASIC, inSpecificDependencyKeyValue, CjpmTomlSpecificDependencyVersionCompletionProvider())
//
//            // Available only using Crates Local Index
//            extend(CompletionType.BASIC, inDependencyInlineTableVersion, LocalCjpmTomlInlineTableVersionCompletionProvider())
        }
    }
}