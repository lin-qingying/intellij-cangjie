package com.linqingying.cangjie.ide.codeinsight

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.psi.PsiElement

class CangJieIconLineMarkerProvider: LineMarkerProviderDescriptor() {

    override fun getName(): String {
        return "Icon preview"
    }

    override fun collectSlowLineMarkers(
        elements: MutableList<out PsiElement>,
        result: MutableCollection<in LineMarkerInfo<*>>
    ) {

    }
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
       return null
    }
}
