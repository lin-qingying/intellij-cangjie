package com.linqingying.cangjie.ide.project.moduletype

import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.icon.CangJieIcons

import com.linqingying.cangjie.ide.project.tools.projectWizard.wizard.CangJieModuleBuilder
import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.module.ModuleTypeManager
import org.jetbrains.jps.model.module.JpsModuleSourceRootType
import javax.swing.Icon

class CangJieLibraryModuleBuilder : ModuleBuilder(){
    override fun getModuleType(): ModuleType<*> {
        return CangJieLibraryModuleType()
    }

}
class CangJieLibraryModuleType : ModuleType<CangJieModuleBuilder>( ID) {
    override fun createModuleBuilder(): CangJieModuleBuilder {
        return CangJieModuleBuilder()
    }

    override fun getName(): String = CangJieBundle.message("CangJie.module.library")
    override fun getDescription(): String  = CangJieBundle.message("CangJie.module.library")

    override fun getNodeIcon(isOpened: Boolean): Icon {
        return CangJieIcons.CANGJIE
    }
    override fun isSupportedRootType(type: JpsModuleSourceRootType<*>?): Boolean {
        return super.isSupportedRootType(type)
    }
    companion object{
        val ID = "CANGJIE_LIBRARY_MODULE"
        val INSTANCE: CangJieLibraryModuleType by lazy { ModuleTypeManager.getInstance().findByID(ID) as CangJieLibraryModuleType }

    }
}
