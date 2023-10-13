package com.huawei.cangjie.lang.core.completion

import com.intellij.codeInsight.completion.CompletionUtil
import com.intellij.psi.PsiElement

fun <T: PsiElement> T.getOriginalOrSelf(): T = CompletionUtil.getOriginalOrSelf(this)


