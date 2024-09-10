package com.huawei.cangjie.analyzer

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

abstract class ProjectStructureProvider {

    /**
     * Returns a [CjModule] for a given [element] in the context of the [contextualModule].
     *
     * The contextual module is the [CjModule] from which [getModule] is called. It is a way to disambiguate the [CjModule] of [element]s
     * with whom multiple modules might be associated. In particular:
     *
     *  1. It allows replacing the original [CjModule] of [element] with another module, e.g. for supporting outsider files (see below).
     *  2. It helps to distinguish between multiple possible [CjModule]s for library elements.
     *
     * #### Outsider Modules
     *
     * Normally, every CangJie source file either belongs to some module (e.g. a source module, or a library module), or is self-contained
     * (a script file, or a file outside content roots). However, in certain cases there might be special modules that include both
     * existing source files, and also some additional files.
     *
     * An example of such a module is one that owns an 'outsider' source file. Outsiders are used in IntelliJ for displaying files that
     * technically belong to some module, but are not included in the module's content roots (e.g. a file from a previous VCS revision).
     * As there might be cross-references between the outsider file and other files in the module, they need to be analyzed as a single
     * synthetic module. Inside an analysis session for such a module (which would be the [contextualModule]), sources that originally
     * belong to a source module should be treated rather as a part of the synthetic one.
     */
    abstract fun getModule(element: PsiElement, contextualModule: CjModule?): CjModule

    companion object{
        public fun getInstance(project: Project): ProjectStructureProvider {
            return project.getService(ProjectStructureProvider::class.java)
        }

        public fun getModule(project: Project, element: PsiElement, contextualModule: CjModule?): CjModule {
            return getInstance(project).getModule(element, contextualModule)
        }
    }
}

internal class ProjectStructureProviderIdeImpl(private val project: Project) : ProjectStructureProvider() {
    override fun getModule(element: PsiElement, contextualModule: CjModule?): CjModule {
        return DefaultCjModule(project)
    }
}
