package com.huawei.cangjie.idea.project.tools.projectWizard.wizard

import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.idea.icons.CangJieIcons
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.module.ModuleTypeManager
import org.jetbrains.jps.model.module.JpsModuleSourceRootType
import javax.swing.Icon


class CangJieModuleType : ModuleType<CangJieModuleBuilder>( ID) {
    override fun createModuleBuilder(): CangJieModuleBuilder {
        return CangJieModuleBuilder()
    }

    override fun getName(): String = CangJieBundle.message("CangJie")
    override fun getDescription(): String  = CangJieBundle.message("CangJie.module")

    override fun getNodeIcon(isOpened: Boolean): Icon {
        return CangJieIcons.CANGJIE_FILE
    }
    override fun isSupportedRootType(type: JpsModuleSourceRootType<*>?): Boolean {
        return super.isSupportedRootType(type)
    }
    companion object{
        val ID = "CANGJIE_MODULE"
        val INSTANCE: CangJieModuleType by lazy { ModuleTypeManager.getInstance().findByID(ID) as CangJieModuleType }

    }
}
