package com.huawei.cangjie.ide.lineMarkers

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.impl.LineMarkerNavigator
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiElement

class MarkerType(
    val debugMessage: String, val tooltip: (PsiElement) -> String?,   navigator: LineMarkerNavigator
)
{
    override fun toString(): String {
        return debugMessage
    }





      val navigationHandler: GutterIconNavigationHandler<PsiElement> =
        if (ApplicationManager.getApplication().isUnitTestMode && navigator is GutterIconNavigationHandler<*>) {
            @Suppress("UNCHECKED_CAST")
            navigator as GutterIconNavigationHandler<PsiElement>
        } else {
            GutterIconNavigationHandler { e, elt ->
                DumbService.getInstance(elt.project).withAlternativeResolveEnabled { navigator.browse(e, elt) }
            }
        }

}


