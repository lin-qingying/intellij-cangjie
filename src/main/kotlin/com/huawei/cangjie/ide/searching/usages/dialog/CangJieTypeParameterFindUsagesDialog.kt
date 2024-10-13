package com.huawei.cangjie.ide.searching.usages.dialog

import com.huawei.cangjie.psi.CjNamedDeclaration
import com.intellij.find.findUsages.CommonFindUsagesDialog
import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleColoredComponent

class CangJieTypeParameterFindUsagesDialog<T : CjNamedDeclaration>(
    element: T,
    project: Project?,
    findUsagesOptions: FindUsagesOptions?,
    toShowInNewTab: Boolean,
    mustOpenInNewTab: Boolean,
    isSingleFile: Boolean,
    handler: FindUsagesHandler?
) : CommonFindUsagesDialog(
    element,
    project!!, findUsagesOptions!!, toShowInNewTab, mustOpenInNewTab, isSingleFile, handler!!
) {
    override fun configureLabelComponent(coloredComponent: SimpleColoredComponent) {
        Utils.configureLabelComponent(coloredComponent, myPsiElement as  CjNamedDeclaration)
    }
}
