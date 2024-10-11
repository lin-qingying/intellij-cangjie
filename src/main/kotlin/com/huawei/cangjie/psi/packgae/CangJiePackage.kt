package com.huawei.cangjie.psi.packgae

import com.intellij.navigation.NavigationItem
import com.intellij.psi.PsiCheckedRenameElement
import com.intellij.psi.PsiDirectoryContainer
import com.intellij.psi.PsiQualifiedNamedElement

/**
 * Represents a CangJie package.
 */
interface CangJiePackage : PsiCheckedRenameElement, NavigationItem,

    PsiDirectoryContainer, PsiQualifiedNamedElement
