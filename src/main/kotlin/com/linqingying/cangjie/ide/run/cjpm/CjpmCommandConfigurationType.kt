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

package com.linqingying.cangjie.ide.run.cjpm

import com.linqingying.cangjie.icon.CangJieIcons
import com.intellij.execution.configurations.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue


class CjpmCommandConfigurationType : ConfigurationTypeBase("CjpmCommandConfigurationType",
    "Cjpm",
    "Cjpm",
    NotNullLazyValue.createValue { CangJieIcons.CANGJIE }

) {
    val factory: ConfigurationFactory get() = configurationFactories.single()

    init {
        addFactory(CjpmConfigurationFactory(this))
    }

    companion object {
        val instance: CjpmCommandConfigurationType
            get() = ConfigurationTypeUtil.findConfigurationType(CjpmCommandConfigurationType::class.java)
    }
}

class CjpmConfigurationFactory(type: CjpmCommandConfigurationType) : ConfigurationFactory(type) {

    override fun getId(): String = ID

    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return CjpmCommandConfiguration(project, this, "Cjpm")
    }

    companion object {
        const val ID: String = "Cjpm Command"
    }
}


//
//class CjpmCommandConfigurationType : SimpleConfigurationType("CjpmCommandConfigurationType",
//    "Cjpm",
//    "Cjpm",
//    NotNullLazyValue.createValue { CangJieIcons.CANGJIE_FILE }
//
//) {
//
//
//    override fun createTemplateConfiguration(project: Project): CjpmCommandConfiguration {
//
//        return CjpmCommandConfiguration(project, this, "Cjpm")
//    }
//
//    override fun isDumbAware(): Boolean = true
//
//    override fun isEditableInDumbMode(): Boolean = true
//
//
//    companion object {
//        val instance: CjpmCommandConfigurationType
//            get() = findConfigurationType(CjpmCommandConfigurationType::class.java)
//    }
//
//
//}
//
//
//
//
//
//
//
