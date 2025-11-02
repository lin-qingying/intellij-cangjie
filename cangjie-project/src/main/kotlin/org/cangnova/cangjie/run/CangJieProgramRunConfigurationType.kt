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

package org.cangnova.cangjie.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue
import org.cangnova.cangjie.project.CjProjectBundle

/**
 * Configuration type for CangJie program run configurations.
 * Used for running compiled CangJie programs directly.
 */
class CangJieProgramRunConfigurationType : ConfigurationTypeBase(
    "CangJieProgramRunConfigurationType",
    CjProjectBundle.message("run.configuration.type.program.display.name"),
    CjProjectBundle.message("run.configuration.type.program.description"),
    NotNullLazyValue.createValue { AllIcons.RunConfigurations.Application }
) {
    val factory: ConfigurationFactory get() = configurationFactories.single()

    init {
        addFactory(CangJieProgramConfigurationFactory(this))
    }

    companion object {
        val instance: CangJieProgramRunConfigurationType
            get() = ConfigurationTypeUtil.findConfigurationType(CangJieProgramRunConfigurationType::class.java)
    }
}

/**
 * Factory for creating CangJie program run configurations
 */
class CangJieProgramConfigurationFactory(type: CangJieProgramRunConfigurationType) : ConfigurationFactory(type) {

    override fun getId(): String = ID

    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return CangJieProgramRunConfiguration(project, this, "CangJie Program")
    }

    companion object {
        const val ID: String = "CangJie Program"
    }
}