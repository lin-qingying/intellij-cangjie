package com.huawei.cangjie.idea.projectStructure.moduleInfo

import com.huawei.cangjie.cjpm.toolchain.CjToolchainBase
import com.huawei.cangjie.name.Name
import com.intellij.openapi.project.Project

data class ToolchainInfo(override val project: Project,val toolchainInfo: CjToolchainBase):IdeaModuleInfo {

    override val name: Name
        get() = Name.special("<sdk ${toolchainInfo.sdkHome}>")
}
