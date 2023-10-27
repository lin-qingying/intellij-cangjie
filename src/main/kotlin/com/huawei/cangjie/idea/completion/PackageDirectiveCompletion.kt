package com.huawei.cangjie.idea.completion

import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.psi.CjPackageDirective
import com.huawei.cangjie.psi.CjSimpleNameExpression
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PlainPrefixMatcher
import com.intellij.patterns.PlatformPatterns


object PackageDirectiveCompletion {
    val DUMMY_IDENTIFIER = "___package___"
    val ACTIVATION_PATTERN = PlatformPatterns.psiElement().inside(CjPackageDirective::class.java)

}
