package com.huawei.cangjie.idea.projectStructure.moduleInfo

import com.huawei.cangjie.analyzer.ModuleInfo
import com.intellij.openapi.project.Project

interface IdeaModuleInfo : ModuleInfo
{
    val project: Project

}
