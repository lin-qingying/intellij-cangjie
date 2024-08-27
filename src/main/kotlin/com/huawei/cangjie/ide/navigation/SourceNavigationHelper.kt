package com.huawei.cangjie.ide.navigation

import com.huawei.cangjie.ide.base.projectStructure.RootKindFilter
import com.huawei.cangjie.ide.base.projectStructure.matches
import com.huawei.cangjie.psi.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbService

object SourceNavigationHelper {
    private val LOG = Logger.getInstance(SourceNavigationHelper::class.java)

    fun getOriginalElement(declaration: CjDeclaration): CjElement {
        return navigateToDeclaration(declaration, NavigationKind.SOURCES_TO_CLASS_FILES)

    }

    fun getNavigationElement(declaration: CjDeclaration): CjElement {
        return navigateToDeclaration(declaration, NavigationKind.CLASS_FILES_TO_SOURCES)


    }

    enum class NavigationKind {
        CLASS_FILES_TO_SOURCES,
        SOURCES_TO_CLASS_FILES
    }

    private fun navigateToDeclaration(
        from: CjDeclaration,
        navigationKind: NavigationKind
    ): CjDeclaration {
        if (!from.isValid || DumbService.isDumb(from.project)) return from

        when (navigationKind) {
            NavigationKind.CLASS_FILES_TO_SOURCES -> if (!from.getContainingCjFile().isCompiled) return from
            NavigationKind.SOURCES_TO_CLASS_FILES -> {
                val file = from.containingFile
                if (file is CjFile && file.isCompiled) return from
                if (!RootKindFilter.librarySources.matches(from)) return from
                if (CjPsiUtil.isLocal(from)) return from
            }
        }

        return from.accept(SourceAndDecompiledConversionVisitor(navigationKind), Unit) ?: from
    }

    private class SourceAndDecompiledConversionVisitor(private val navigationKind: NavigationKind) :
        CjVisitor<CjDeclaration?, Unit>()
}
