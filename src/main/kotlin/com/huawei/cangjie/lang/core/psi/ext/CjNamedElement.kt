package com.huawei.cangjie.lang.core.psi.ext

import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiNamedElement

interface CjNamedElement : CjElement, PsiNamedElement, NavigatablePsiElement


interface CjNameIdentifierOwner : CjNamedElement, PsiNameIdentifierOwner
