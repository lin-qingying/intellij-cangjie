package com.linqingying.lsp.impl.navigation

import com.intellij.model.Pointer
import com.intellij.model.Symbol
import com.intellij.navigation.NavigatableSymbol
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.navigation.NavigationTarget
import org.eclipse.lsp4j.LocationLink

class LspNavigatableSymbol(val targetFile: VirtualFile, val locationLink: LocationLink) : NavigatableSymbol {
    override fun createPointer(): Pointer<out Symbol> {
        return if (targetFile.isValid) {
            Pointer { LspNavigatableSymbol(targetFile, locationLink) }
        } else {
            Pointer { null }
        }

    }

    override fun getNavigationTargets(project: Project): MutableCollection<out NavigationTarget> =
        mutableListOf(LspNavigationTarget(project, targetFile, locationLink))


}
