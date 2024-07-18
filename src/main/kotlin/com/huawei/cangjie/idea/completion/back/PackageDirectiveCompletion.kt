package com.huawei.cangjie.idea.completion.back

import com.huawei.cangjie.psi.CjPackageDirective
import com.intellij.patterns.PlatformPatterns


object PackageDirectiveCompletion {
    val DUMMY_IDENTIFIER = "___package___"
    val ACTIVATION_PATTERN = PlatformPatterns.psiElement().inside(CjPackageDirective::class.java)

}
