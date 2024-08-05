package com.huawei.cangjie.analyzer

import com.huawei.cangjie.ide.projectStructure.CangJieResolveScopeEnlarger
import com.huawei.cangjie.name.Name
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope

interface ModuleInfo {

    val name: Name
    val project: Project
    val module: Module?


    val contentScope: GlobalSearchScope

    val moduleContentScope: GlobalSearchScope
        get() = contentScope

}


data class CangJieModuleInfo(

    override val module: Module
) : ModuleInfo,DerivedModuleInfo {


    override val name: Name =  Name.special("<production sources for module ${module.name}>")
    override val project: Project = module.project
    override val originalModule: ModuleInfo
        get() = this

    override val contentScope: GlobalSearchScope
        get() = CangJieResolveScopeEnlarger.enlargeScope(module.moduleProductionSourceScope, module, isTestScope = false)

}
