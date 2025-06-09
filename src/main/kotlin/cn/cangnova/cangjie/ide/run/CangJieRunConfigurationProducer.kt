/*
 * Copyright 2025 LinQingYing. and contributors.
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

package cn.cangnova.cangjie.ide.run

import cn.cangnova.cangjie.icon.CangJieIcons
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement

abstract class CangJieRunConfigurationProducer<T : CjCommandConfiguration> : LazyRunConfigurationProducer<T>() {

    override fun getConfigurationFactory(): ConfigurationFactory {
        return CangJieCommandConfigurationType.instance.factory

    }
//  abstract  fun isApplicable( ): Boolean
//    override fun setupConfigurationFromContext(
//        configuration: T,
//        context: ConfigurationContext,
//        sourceElement: Ref<PsiElement?>
//    ): Boolean {
//        TODO("Not yet implemented")
//    }

    override fun isConfigurationFromContext(configuration: T, context: ConfigurationContext): Boolean {
        return false
    }

}

class CangJieCommandConfigurationType : ConfigurationTypeBase(
    "CangJieCommandConfigurationType",
    "CangJie",
    "CangJie",
    NotNullLazyValue.createValue { CangJieIcons.CANGJIE }

) {
    val factory: ConfigurationFactory get() = configurationFactories.single()

    init {
        addFactory(CangJieConfigurationFactory(this))
    }

    companion object {
        val instance: CangJieCommandConfigurationType
            get() = ConfigurationTypeUtil.findConfigurationType(CangJieCommandConfigurationType::class.java)
    }
}

class CangJieConfigurationFactory(type: CangJieCommandConfigurationType) : ConfigurationFactory(type) {

    override fun getId(): String = ID

    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return CjCommandConfiguration.Default(project, this)
    }

    companion object {
        const val ID: String = "CangJie Command"
    }
}
