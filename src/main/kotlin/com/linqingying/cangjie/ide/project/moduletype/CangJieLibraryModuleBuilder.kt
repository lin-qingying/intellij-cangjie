/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package com.linqingying.cangjie.ide.project.moduletype

import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.icon.CangJieIcons

import com.linqingying.cangjie.ide.module.CangJieModuleBuilder
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
