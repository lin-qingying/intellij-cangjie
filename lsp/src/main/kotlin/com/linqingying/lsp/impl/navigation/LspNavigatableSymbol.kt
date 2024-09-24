package com.linqingying.lsp.impl.navigation

import com.intellij.model.Pointer
import com.intellij.navigation.NavigatableSymbol
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.eclipse.lsp4j.LocationLink

class LspNavigatableSymbol(
    val targetFile: VirtualFile,
    val locationLink: LocationLink
) : NavigatableSymbol {

    override fun createPointer(): Pointer<LspNavigatableSymbol> {

        return Pointer {
            if (targetFile.isValid) LspNavigatableSymbol(
                targetFile,
                locationLink
            ) else null
        }


    }

    override fun getNavigationTargets(project: Project): List<LspNavigationTarget> {
        return listOf(LspNavigationTarget(project, this.targetFile, this.locationLink))

    }
}

