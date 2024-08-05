package com.huawei.cangjie.ide.completion

import com.huawei.cangjie.lang.CangJieLanguage
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.platform.ml.impl.turboComplete.KindVariety

object CangJieKindVariety : KindVariety {
    override fun kindsCorrespondToParameters(parameters: CompletionParameters): Boolean {
        return parameters.position.language == CangJieLanguage
    }

    override val actualCompletionContributorClass: Class<*>
        get() = CangJieCompletionContributor::class.java
}
