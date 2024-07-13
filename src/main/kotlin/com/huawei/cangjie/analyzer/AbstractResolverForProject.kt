package com.huawei.cangjie.analyzer

import com.huawei.cangjie.builtins.BuiltInsLoader
import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.cjpm.project.model.currentCjpmProject
import com.huawei.cangjie.context.ProjectContext
import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.huawei.cangjie.descriptors.impl.ModuleDescriptorImpl
import com.huawei.cangjie.name.Name

import com.intellij.openapi.project.Project

fun createModuleDescriptor(projectContext: ProjectContext, project: Project): ModuleDescriptor {

    return ModuleDescriptorImpl(
        Name.identifier(project.currentCjpmProject?.presentableName!!), projectContext.storageManager, CangJieBuiltIns(projectContext.storageManager)
    )
}

abstract class AbstractResolverForProject<M : ModuleInfo> {

//    private fun createModuleDescriptor(module: M): ModuleData
}
