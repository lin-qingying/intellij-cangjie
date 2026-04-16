/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.project.wizard


import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.module.ModuleTypeManager
import org.cangnova.cangjie.icon.CangJieIcons
import org.cangnova.cangjie.project.CjProjectBundle
import org.cangnova.cangjie.project.extension.CjModuleBuilderProvider
import javax.swing.Icon


class CangJieModuleType : ModuleType<CjModuleBuilder>(ID) {
    override fun createModuleBuilder(): CjModuleBuilder {
        return CjModuleBuilderProvider.EP_NAME.extensions.firstOrNull()?.createModuleBuilder()
            ?: error("CJModuleBuilder not found")
    }

    override fun getName(): String = CjProjectBundle.message("CangJie")
    override fun getDescription(): String = CjProjectBundle.message("CangJie.module")

    override fun getNodeIcon(isOpened: Boolean): Icon {
        return CangJieIcons.CANGJIE
    }

    companion object {
        val ID = "CANGJIE_MODULE"
        val INSTANCE: CangJieModuleType by lazy { ModuleTypeManager.getInstance().findByID(ID) as CangJieModuleType }

    }
}
